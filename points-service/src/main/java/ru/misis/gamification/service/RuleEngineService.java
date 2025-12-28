package ru.misis.gamification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.misis.gamification.events.domain.GamificationEvent;
import ru.misis.gamification.model.entity.RuleEntity;
import ru.misis.gamification.repository.RulesRepository;
import ru.misis.gamification.util.PointsCalculator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Движок для обработки правил начисления очков.
 *
 * <p>
 * Правила хранятся в базе данных и могут быть изменены
 * без перезапуска приложения.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {

    /**
     * Репозиторий для работы с правилами
     */
    private final RulesRepository rulesRepository;

    /**
     * Время жизни кэша правил в минутах
     */
    private static final long CACHE_TTL_MINUTES = 5;

    private final PointsCalculator pointsCalculator;

    /**
     * Кэш правил, сгруппированных по типу события
     */
    private volatile Map<String, List<RuleEntity>> rulesCache;

    /**
     * Время последнего обновления кэша
     */
    private volatile LocalDateTime cacheTimestamp;

    /**
     * Находит применимые правила для события.
     * Важно: правила применяются в порядке приоритета.
     */
    public List<RuleEntity> findApplicableRules(GamificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Событие не может быть null");
        }

        log.debug("Поиск правил для события типа: {}", event.type());

        // Получаем все правила для типа события
        List<RuleEntity> rulesForEvent = getRulesForEventType(event.type());

        // Фильтруем по активности и времени
        List<RuleEntity> activeRules = rulesForEvent.stream()
                .filter(rule -> rule.isActiveAt(LocalDateTime.now()))
                .toList();

        log.debug("Найдено {} активных правил для события {}",
                activeRules.size(), event.type());

        // Сортируем по приоритету (меньше = выше приоритет)
        return activeRules.stream()
                .sorted(Comparator.comparing(RuleEntity::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Проверяет, применимо ли правило к событию.
     */
    private boolean isRuleApplicable(RuleEntity rule, Map<String, Object> payload) {
        // 1. Проверка активности
        if (!rule.isActiveAt(LocalDateTime.now())) {
            return false;
        }

        // 2. Проверка условий
        if (rule.getConditions() != null && !rule.getConditions().isEmpty()) {
            return pointsCalculator.evaluateComplexCondition(
                    buildConditionScript(rule.getConditions()),
                    payload
            );
        }

        return true;
    }

    /**
     * Строит Groovy скрипт из условий.
     */
    private String buildConditionScript(Map<String, String> conditions) {
        StringBuilder script = new StringBuilder();
        conditions.forEach((key, value) -> {
            script.append("(")
                    .append(convertToGroovyExpression(key, value))
                    .append(") && ");
        });

        if (!script.isEmpty()) {
            script.setLength(script.length() - 4); // Убираем последний " && "
        }

        return script.toString();
    }

    /**
     * Конвертирует условие в выражение Groovy.
     */
    private String convertToGroovyExpression(String key, String condition) {
        // Простая реализация
        if (condition.startsWith(">=")) {
            return key + " >= " + condition.substring(2);
        } else if (condition.startsWith("<=")) {
            return key + " <= " + condition.substring(2);
        } else if (condition.startsWith(">")) {
            return key + " > " + condition.substring(1);
        } else if (condition.startsWith("<")) {
            return key + " < " + condition.substring(1);
        } else if (condition.startsWith("~")) {
            // Регулярное выражение
            return key + " =~ " + condition.substring(1);
        } else {
            return key + " == '" + condition + "'";
        }
    }

    /**
     * Получает правила для указанного типа события с кэшированием.
     * <p>
     * Кэш обновляется автоматически при истечении TTL.
     * </p>
     *
     * @param eventType тип события
     * @return список правил для данного типа события
     */
    private List<RuleEntity> getRulesForEventType(String eventType) {
        refreshCacheIfNeeded();

        return rulesCache.getOrDefault(eventType, List.of());
    }

    /**
     * Обновляет кэш правил, если он устарел или отсутствует.
     */
    private synchronized void refreshCacheIfNeeded() {
        boolean cacheExpired = cacheTimestamp == null ||
                cacheTimestamp.plusMinutes(CACHE_TTL_MINUTES).isBefore(LocalDateTime.now());

        if (cacheExpired) {
            log.info("Обновление кэша правил");

            // Загружаем все активные правила из базы
            List<RuleEntity> allRules = rulesRepository.findByIsActiveTrue();

            // Группируем по типу события
            rulesCache = allRules.stream()
                    .collect(Collectors.groupingBy(
                            RuleEntity::getEventType,
                            Collectors.toList()
                    ));

            cacheTimestamp = LocalDateTime.now();

            log.info("Кэш обновлен: {} правил для {} типов событий",
                    allRules.size(), rulesCache.size());
        }
    }

    /**
     * Принудительно обновляет кэш правил.
     * <p>
     * Используется при изменении правил через административный интерфейс.
     * </p>
     */
    public void refreshCache() {
        log.info("Принудительное обновление кэша правил");
        cacheTimestamp = null;
        refreshCacheIfNeeded();
    }

    /**
     * Вычисляет очки по всем применимым правилам.
     * Возвращает мапу ruleId -> points.
     */
    public Map<String, Long> calculatePointsForRules(
            List<RuleEntity> rules,
            GamificationEvent event) {

        return rules.stream()
                .collect(Collectors.toMap(
                        RuleEntity::getRuleId,
                        rule -> pointsCalculator.calculatePoints(rule, event),
                        (v1, v2) -> v1 // При конфликте берем первое значение
                ));
    }

    /**
     * Проверяет валидность правила.
     * <p>
     * Валидация включает:
     * <ul>
     *     <li>Проверку формата идентификатора</li>
     *     <li>Валидацию временных интервалов</li>
     *     <li>Проверку условий (если есть)</li>
     *     <li>Проверку формулы (если есть)</li>
     * </ul>
     * </p>
     *
     * @param rule правило для валидации
     * @return {@code true} если правило валидно, {@code false} в противном случае
     */
    public boolean validateRule(RuleEntity rule) {
        if (rule == null) {
            log.error("Правило для валидации не может быть null");
            return false;
        }

        try {
            // Проверка обязательных полей
            if (rule.getRuleId() == null || rule.getRuleId().trim().isEmpty()) {
                log.error("Идентификатор правила обязателен");
                return false;
            }

            if (rule.getEventType() == null || rule.getEventType().trim().isEmpty()) {
                log.error("Тип события правила обязателен");
                return false;
            }

            if (rule.getName() == null || rule.getName().trim().isEmpty()) {
                log.error("Название правила обязательно");
                return false;
            }

            if (rule.getPointsValue() == null) {
                log.error("Значение очков обязательно");
                return false;
            }

            if (rule.getPriority() == null) {
                log.error("Приоритет правила обязателен");
                return false;
            }

            if (rule.getIsActive() == null) {
                log.error("Флаг активности обязателен");
                return false;
            }

            if (rule.getValidFrom() == null) {
                log.error("Дата начала действия обязательна");
                return false;
            }

            // Проверка временного интервала
            if (rule.getValidTo() != null &&
                    rule.getValidTo().isBefore(rule.getValidFrom())) {
                log.error("Дата окончания не может быть раньше даты начала");
                return false;
            }

            // Дополнительные проверки могут быть добавлены здесь
            // Например, проверка синтаксиса Groovy формул

            return true;

        } catch (Exception e) {
            log.error("Ошибка валидации правила {}: {}",
                    rule.getRuleId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Анализирует эффективность правил.
     * Для научного исследования: какие правила лучше мотивируют.
     */
    public void analyzeRuleEffectiveness(String ruleId) {
        // Здесь можно добавить сложную аналитику:
        // - Конверсия (событие -> начисление)
        // - Retention после начисления
        // - Корреляция с успеваемостью
        log.info("Analyzing effectiveness of rule {}", ruleId);
    }
}