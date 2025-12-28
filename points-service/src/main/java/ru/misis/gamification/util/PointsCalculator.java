package ru.misis.gamification.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.stereotype.Component;
import ru.misis.gamification.events.domain.GamificationEvent;
import ru.misis.gamification.model.entity.RuleEntity;
import ru.misis.gamification.util.cache.SimpleScriptCache;
import ru.misis.gamification.util.comparison.ComparisonOperator;

import java.util.HashMap;
import java.util.Map;

/**
 * Калькулятор очков с поддержкой Groovy скриптов.
 * Выбран Groovy потому что:
 * 1. Легко встраивается в Java
 * 2. Безопасный sandbox (можно ограничить)
 * 3. Позволяет исследователям-непрограммистам писать формулы
 * 4. Динамическая загрузка формул без перезапуска
 * <p>
 * Для научного исследования: возможность быстро тестировать
 * разные математические модели мотивации.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointsCalculator {

    private final ObjectMapper objectMapper;
    private final SimpleScriptCache scriptCache;
    private final CompilerConfiguration groovyCompilerConfiguration;

    /**
     * Вычисляет количество очков по правилу.
     * Поддерживает:
     * 1. Фиксированные значения
     * 2. Groovy формулы
     * 3. Условия применения
     */
    public Long calculatePoints(RuleEntity rule, GamificationEvent event) {
        try {
            log.debug("Рассчитываем баллы по правилу: {}", rule.getRuleId());

            // Конвертируем событие в Map для использования в формулах
            Map<String, Object> eventData = convertEventToMap(event);

            // 1. Проверяем условия
            if (!checkConditions(rule, eventData)) {
                log.debug("Не соблюдены условия для правила: {}", rule.getRuleId());
                return 0L;
            }

            // 2. Вычисляем значение
            Long points;
            if (rule.getFormula() != null && !rule.getFormula().isBlank()) {
                points = calculateByFormula(rule.getFormula(), eventData);
            } else {
                points = rule.getPointsValue().longValue();
            }

            // 3. Применяем лимиты
            points = applyLimits(points, rule);

            // 4. Округляем (для дробных формул)
            points = roundPoints(points, rule);

            log.debug("Вычислено {} очков для правила {}", points, rule.getRuleId());
            return points;

        } catch (Exception e) {
            log.error("Ошибка расчета очков для правила {}: {}",
                    rule.getRuleId(), e.getMessage(), e);
            return 0L; // Безопасное значение при ошибке
        }
    }

    /**
     * Конвертирует событие в Map для использования в формулах
     */
    private Map<String, Object> convertEventToMap(GamificationEvent event) {
        try {
            // Сериализуем и десериализуем через ObjectMapper
            String json = objectMapper.writeValueAsString(event);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            log.warn("Не удалось сконвертировать событие в Map, используем базовые свойства: {}",
                    e.getMessage());

            Map<String, Object> basicProperties = new HashMap<>();
            basicProperties.put("eventId", event.eventId().toString());
            basicProperties.put("type", event.type());
            basicProperties.put("userId", event.userId());
            basicProperties.put("occurredAt", event.occurredAt());

            return basicProperties;
        }
    }

    /**
     * Проверка условий применения правила.
     * Поддерживает операторы: =, !=, >, <, >=, <=, ~ (регулярки)
     */
    private boolean checkConditions(RuleEntity rule, Map<String, Object> payload) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> condition : rule.getConditions().entrySet()) {
            String key = condition.getKey();
            String expected = condition.getValue();

            Object actualValue = getValueByPath(payload, key);
            if (actualValue == null) {
                return false;
            }

            if (!evaluateCondition(actualValue.toString(), expected)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Динамическое вычисление по формуле Groovy.
     * Пример формулы: "score * difficulty * 10 + bonus"
     * Доступные переменные: все поля из payload
     */
    private Long calculateByFormula(String formula, Map<String, Object> payload) {
        try {
            Object result = scriptCache.executeScript(formula, payload);

            if (result instanceof Number) {
                return ((Number) result).longValue();
            } else {
                log.warn("Formula returned non-numeric: {}", result);
                return 0L;
            }

        } catch (Exception e) {
            log.error("Formula error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Применение лимитов правила
     */
    private Long applyLimits(Long points, RuleEntity rule) {
        // Не позволяем списывать очки по правилу начисления
        if (points < 0 && rule.getPointsValue() > 0) {
            return 0L;
        }
        return points;
    }

    /**
     * Округление очков
     */
    private Long roundPoints(Long points, RuleEntity rule) {
        // Простое округление до 10
        if (points > 100) {
            return (points / 10) * 10;
        }
        return points;
    }

    /**
     * Получение значения по пути (dot notation).
     */
    private Object getValueByPath(Map<String, Object> map, String path) {
        try {
            JsonNode node = objectMapper.valueToTree(map);
            JsonNode target = node.at("/" + path.replace(".", "/"));

            if (target.isMissingNode()) {
                return null;
            }

            if (target.isNumber()) {
                return target.numberValue();
            } else if (target.isBoolean()) {
                return target.booleanValue();
            } else {
                return target.asText();
            }

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Оценка условия
     */
    private boolean evaluateCondition(String actualValue, String condition) {
        if (actualValue == null) {
            return false;
        }

        try {
            ComparisonOperator operator = ComparisonOperator.fromSymbol(condition);
            String expectedValue = ComparisonOperator.extractValue(condition, operator);
            return operator.evaluate(actualValue, expectedValue);
        } catch (Exception e) {
            log.warn("Failed to evaluate condition '{}': {}", condition, e.getMessage());
            return false;
        } // По умолчанию сравнение строк
    }

    /**
     * Расчет уровня на основе очков (нелинейная прогрессия)
     */
    public int calculateLevel(Long totalPoints) {
        // Формула: level = floor(sqrt(points / 1000)) + 1
        // Дает медленный рост на высоких уровнях
        if (totalPoints <= 0) return 1;

        double level = Math.sqrt(totalPoints / 1000.0);
        return (int) Math.floor(level) + 1;
    }

    /**
     * Очищает кэши (опционально, для управления).
     */
    public void clearCache() {
        scriptCache.clearCache();
        log.info("PointsCalculator cache cleared");
    }

    /**
     * Вспомогательный метод для проверки сложных условий.
     */
    public boolean evaluateComplexCondition(String conditionScript, Map<String, Object> context) {
        try {
            // Создаем Binding с контекстом
            Binding binding = new Binding();
            context.forEach(binding::setVariable);

            // Создаем GroovyShell с безопасной конфигурацией
            GroovyShell shell = new GroovyShell(groovyCompilerConfiguration);
            Object result = shell.evaluate(conditionScript);

            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            log.error("Error evaluating condition: {}", e.getMessage());
            return false;
        }
    }

}