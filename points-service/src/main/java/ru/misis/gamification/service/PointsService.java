package ru.misis.gamification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misis.gamification.events.domain.GamificationEvent;
import ru.misis.gamification.events.domain.internal.LevelUpEvent;
import ru.misis.gamification.events.domain.internal.PointsChangedEvent;
import ru.misis.gamification.exception.AccountNotFoundException;
import ru.misis.gamification.exception.DuplicateEventException;
import ru.misis.gamification.exception.InsufficientPointsException;
import ru.misis.gamification.exception.PointsLimitException;
import ru.misis.gamification.exception.ValidationException;
import ru.misis.gamification.model.LevelProgress;
import ru.misis.gamification.model.entity.PointsAccount;
import ru.misis.gamification.model.entity.PointsTransaction;
import ru.misis.gamification.model.entity.RuleEntity;
import ru.misis.gamification.model.enums.TransactionStatus;
import ru.misis.gamification.model.enums.TransactionType;
import ru.misis.gamification.repository.PointsAccountRepository;
import ru.misis.gamification.repository.PointsTransactionRepository;
import ru.misis.gamification.util.PointsCalculator;
import ru.misis.gamification.util.PointsLevelCalculator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис управления очками пользователей
 * <p>
 * Предназначен для динамического расчета бонусных очков в системе геймификации LMS на основе:
 *     <ul>
 *         <li>Правил начисления {@link RuleEntity}</li>
 *         <li>Событий пользователя {@link GamificationEvent}</li>
 *         <li>Гибких формул расчета (Groovy скрипты)</li>
 *     </ul>
 * </p>
 *
 * <p>
 * Основной сервис для операций с баллами системы геймификации.
 * Обрабатывает начисления, списания, проверку баланса и расчет уровней.
 * </p>
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PointsService {

    /**
     * Стандартный срок действия начисленных очков (дней)
     */
    private static final int DEFAULT_POINTS_EXPIRATION_DAYS = 365;

    /**
     * Репозиторий для работы с балансами пользователей
     */
    private final PointsAccountRepository pointsAccountRepository;

    /**
     * Репозиторий для работы с транзакциями
     */
    private final PointsTransactionRepository transactionRepository;

    // TODO
//    private final PointsChangedProducer pointsChangedProducer;

    /**
     * Калькулятор очков
     */
    private final PointsCalculator pointsCalculator;

    /**
     * Калькулятор для расчета уровней
     */
    private final PointsLevelCalculator pointsLevelCalculator;

    /**
     * Сервис для работы с правилами начисления
     */
    private final RuleEngineService ruleEngineService;

    /**
     * Находит или создает аккаунт пользователя.
     * <p>
     * Если аккаунт не существует, создает новый с нулевым балансом.
     * Используется при первом взаимодействии пользователя с системой.
     * </p>
     *
     * @param userId уникальный идентификатор пользователя
     * @return существующий или созданный аккаунт
     * @throws IllegalArgumentException если userId {@code null} или пустой
     */
    @Transactional(readOnly = true)
    public PointsAccount getOrCreateAccount(String userId) {
        validateUserId(userId);

        return pointsAccountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PointsAccount newAccount = createNewAccount(userId);
                    log.info("Создан новый аккаунт для пользователя: {}", userId);
                    return newAccount;
                });
    }

    /**
     * Получает текущий баланс пользователя.
     *
     * @param userId уникальный идентификатор пользователя
     * @return баланс доступных очков
     * @throws AccountNotFoundException если аккаунт не найден
     * @throws IllegalArgumentException если userId {@code null} или пустой
     */
    @Transactional(readOnly = true)
    public Long getBalance(String userId) {
        validateUserId(userId);

        PointsAccount account = pointsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        return account.getAvailablePoints();
    }

    /**
     * Начисляет очки пользователю по правилу.
     * <p>
     * Основная операция начисления очков за выполнение действий.
     * Проверяет дублирование событий и применяет лимиты правил.
     * </p>
     *
     * @param userId      идентификатор пользователя
     * @param rule        правило начисления
     * @param eventId     идентификатор события (для идемпотентности)
     * @param description описание операции
     * @return созданная транзакция
     * @throws DuplicateEventException  если событие уже обработано
     * @throws PointsLimitException     если превышены лимиты правила
     * @throws IllegalArgumentException если параметры некорректны
     */
    public PointsTransaction awardPoints(String userId, RuleEntity rule,
                                         String eventId, String description) {
        validateAwardParameters(userId, rule, eventId);

        // Проверяем дублирование события
        checkDuplicateEvent(eventId);

        // Получаем или создаем аккаунт
        PointsAccount account = getOrCreateAccount(userId);

        // Проверяем лимиты правила
        checkRuleLimits(userId, rule, account);

        // Вычисляем количество очков
        Long points = calculatePointsForRule(rule);

        // Создаем транзакцию
        PointsTransaction transaction = createAwardTransaction(
                userId, rule, eventId, points, description
        );

        // Обновляем баланс
        updateAccountBalance(account, points);

        // Сохраняем изменения
        transaction = transactionRepository.save(transaction);
        pointsAccountRepository.save(account);

        log.info("Начислено {} очков пользователю {} по правилу {}",
                points, userId, rule.getRuleId());

        return transaction;
    }

    /**
     * Создает транзакцию начисления.
     */
    private PointsTransaction createAwardTransaction(String userId, RuleEntity rule,
                                                     String eventId, Long points, String description) {
        return PointsTransaction.builder()
                .userId(userId)
                .pointsDelta(points)
                .type(TransactionType.EARN)
                .status(TransactionStatus.COMPLETED)
                .eventId(eventId)
                .ruleId(rule.getRuleId())
                .source(rule.getEventType())
                .description(description != null ? description : rule.getName())
                .transactionDate(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(DEFAULT_POINTS_EXPIRATION_DAYS))
                .build();
    }

    /**
     * Создает транзакцию списания.
     */
    private PointsTransaction createSpendTransaction(String userId, Long amount, String purpose) {
        return PointsTransaction.builder()
                .userId(userId)
                .pointsDelta(-amount)
                .type(TransactionType.SPEND)
                .status(TransactionStatus.COMPLETED)
                .eventId(UUID.randomUUID().toString())
                .ruleId("MANUAL_SPEND")
                .source("USER_SPENDING")
                .description(purpose)
                .transactionDate(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(DEFAULT_POINTS_EXPIRATION_DAYS))
                .build();
    }

    /**
     * Вычисляет количество очков по правилу.
     */
    private Long calculatePointsForRule(RuleEntity rule) {
        // В реальной реализации здесь была бы логика расчета
        // по формуле или фиксированному значению
        return rule.getPointsValue().longValue();
    }

    /**
     * Проверяет лимиты правила.
     */
    private void checkRuleLimits(String userId, RuleEntity rule, PointsAccount account) {
        // Дневной лимит
        if (rule.getMaxDaily() != null) {
            Long dailyEarned = transactionRepository.calculateDailyPoints(
                    userId,
                    rule.getRuleId(),
                    LocalDateTime.now().minusDays(1)
            );
            if (dailyEarned >= rule.getMaxDaily()) {
                throw new PointsLimitException(
                        String.format("Дневной лимит правила %s исчерпан", rule.getRuleId()));
            }
        }

        // Общий лимит
        if (rule.getMaxTotal() != null) {
            Long totalEarned = transactionRepository.calculateTotalPoints(userId, rule.getRuleId());
            if (totalEarned >= rule.getMaxTotal()) {
                throw new PointsLimitException(
                        String.format("Общий лимит правила %s исчерпан", rule.getRuleId()));
            }
        }
    }

    /**
     * Проверяет дублирование события.
     */
    private void checkDuplicateEvent(String eventId) {
        transactionRepository.findByEventId(eventId)
                .ifPresent(existing -> {
                    throw new DuplicateEventException(
                            "Событие уже обработано: " + eventId);
                });
    }

    /**
     * Списывает очки с баланса пользователя.
     * <p>
     * Используется для покупки бонусов, участия в активностях
     * и других операций, требующих расходов.
     * </p>
     *
     * @param userId  идентификатор пользователя
     * @param amount  количество очков для списания
     * @param purpose назначение платежа
     * @return созданная транзакция
     * @throws InsufficientPointsException если недостаточно средств
     * @throws AccountNotFoundException    если аккаунт не найден
     * @throws IllegalArgumentException    если параметры некорректны
     */
    public PointsTransaction spendPoints(String userId, Long amount, String purpose) {
        validateSpendParameters(userId, amount, purpose);

        PointsAccount account = pointsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        // Проверяем достаточность средств
        if (account.getAvailablePoints() < amount) {
            throw new InsufficientPointsException(
                    String.format("Недостаточно очков. Доступно: %d, Требуется: %d",
                            account.getAvailablePoints(), amount)
            );
        }

        PointsTransaction transaction = PointsTransaction.builder()
                .userId(userId)
                .pointsDelta(-amount)
                .type(TransactionType.SPEND)
                .status(TransactionStatus.COMPLETED)
                .description("Spent on: " + purpose)
                .source("MANUAL_SPEND")
                .transactionDate(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(365))
                .build();

        account.setAvailablePoints(account.getAvailablePoints() - amount);
        account.setUpdatedAt(LocalDateTime.now());
        pointsAccountRepository.save(account);

        log.info("Списано {} очков у пользователя {}: {}", amount, userId, purpose);

//        publishPointsChangedEvent(account, transaction);

        log.info("User {} spent {} points on: {}", userId, amount, purpose);

        return transactionRepository.save(transaction);
    }

    /**
     * Обрабатывает событие геймификации.
     * <p>
     * Находит применимые правила для события и начисляет очки
     * по каждому подходящему правилу.
     * </p>
     *
     * @param event событие геймификации
     * @throws ValidationException      если событие некорректно
     * @throws IllegalArgumentException если event {@code null}
     */
    public void processEvent(GamificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Событие не может быть null");
        }

        log.info("Обработка события геймификации: {} для пользователя: {}",
                event.type(), event.userId());

        try {
            // Валидируем событие
            validateEvent(event);

            // Находим применимые правила
            var applicableRules = ruleEngineService.findApplicableRules(event);

            // Применяем каждое правило
            for (RuleEntity rule : applicableRules) {
                applyRuleToEvent(event, rule);
            }

            log.info("Событие успешно обработано: {}", event.eventId());

        } catch (ValidationException e) {
            log.warn("Валидация события не пройдена: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка обработки события: {}", event.eventId(), e);
            throw new RuntimeException("Не удалось обработать событие", e);
        }
    }

//    /**
//     * Применяет правило к событию.
//     */
//    private void applyRuleToEvent(GamificationEvent event, RuleEntity rule) {
//        log.debug("Применение правила {} к событию {}", rule.getRuleId(), event.eventId());
//
//        // В реальной реализации здесь была бы сложная логика
//        // сопоставления события и правил
//        awardPoints(
//                event.userId(),
//                rule,
//                event.eventId().toString(),
//                "Начисление за событие: " + event.type()
//        );
//    }

    /**
     * Применяет правило к событию и начисляет очки.
     */
    public void applyRuleToEvent(GamificationEvent event, RuleEntity rule) {
        log.debug("Применение правила {} к событию {}", rule.getRuleId(), event.eventId());

        applyRule(event, rule);
    }

    /**
     * Применяет правило к событию и начисляет очки.
     * Основной метод, который использует PointsCalculator для расчета очков.
     */
    public void applyRule(GamificationEvent event, RuleEntity rule) {
        log.debug("Applying rule {} to event {}", rule.getRuleId(), event.eventId());

        // 1. Вычисляем количество очков через PointsCalculator
        Long points = pointsCalculator.calculatePoints(rule, event);

        if (points == 0) {
            log.debug("Rule {} resulted in 0 points, skipping", rule.getRuleId());
            return;
        }

        // 2. Получаем или создаем аккаунт
        PointsAccount account = pointsAccountRepository
                .findByUserId(event.userId())
                .orElseGet(() -> createAccount(event.userId()));

        // 3. Проверяем лимиты
        checkLimits(account, rule, points, event);

        // 4. Создаем транзакцию
        PointsTransaction transaction = createTransaction(event, rule, points);

        // 5. Обновляем баланс
        updateBalance(account, points, rule);

        // 6. Сохраняем
        transaction = transactionRepository.save(transaction);
        account = pointsAccountRepository.save(account);

        // 7. Публикуем событие об изменении баллов
        publishPointsChangedEvent(account, transaction);

        // 8. Проверяем уровень
        checkLevelUp(account);

        log.info("Applied rule {}: awarded {} points to user {}",
                rule.getRuleId(), points, event.userId());
    }

    /**
     * Получает прогресс пользователя до следующего уровня.
     *
     * @param userId Идентификатор пользователя
     * @return Информация о прогрессе
     * @throws AccountNotFoundException Если аккаунт не найден
     * @throws IllegalArgumentException Если userId {@code null} или пустой
     */
    @Transactional(readOnly = true)
    public LevelProgress getLevelProgress(String userId) {
        validateUserId(userId);

        PointsAccount account = pointsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        long pointsToNextLevel = pointsLevelCalculator.getPointsToNextLevel(
                account.getTotalPoints(), account.getLevel());

        double progressPercentage = pointsLevelCalculator.getProgressToNextLevel(
                account.getTotalPoints(), account.getLevel());

        return LevelProgress.builder()
                .userId(userId)
                .currentLevel(account.getLevel())
                .currentPoints(account.getTotalPoints())
                .pointsToNextLevel(pointsToNextLevel)
                .progressPercentage(progressPercentage)
                .build();
    }

    /**
     * Обновляет уровень пользователя на основе общего количества очков.
     *
     * @param account аккаунт пользователя
     */
    private void updateUserLevel(PointsAccount account) {
        int newLevel = pointsLevelCalculator.calculateLevel(account.getTotalPoints());

        if (newLevel > account.getLevel()) {
            int oldLevel = account.getLevel();
            account.setLevel(newLevel);

            log.info("Пользователь {} повысил уровень с {} до {}",
                    account.getUserId(), oldLevel, newLevel);

            // Можно отправить событие о повышении уровня
            publishLevelUpEvent(account, oldLevel, newLevel);
        }
    }

    /**
     * Валидация входящего события.
     */
    private void validateEvent(GamificationEvent event) {
        if (event.userId() == null || event.userId().trim().isEmpty()) {
            throw new ValidationException("Идентификатор пользователя обязателен");
        }

        if (event.type() == null) {
            throw new ValidationException("Тип события обязателен");
        }

        if (event.eventId() == null) {
            throw new ValidationException("Идентификатор события обязателен");
        }
    }

    private PointsAccount createAccount(String userId) {
        PointsAccount account = PointsAccount.builder()
                .userId(userId)
                .totalPoints(0L)
                .availablePoints(0L)
                .frozenPoints(0L)
                .level(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Создан баланс очков для пользователя: {}", userId);
        return account;
    }

    private void checkLimits(PointsAccount account, RuleEntity rule, Long points, GamificationEvent event) {
        // Проверка дневного лимита
        if (rule.getMaxDaily() != null) {
            Long dailyEarned = transactionRepository.calculateDailyPoints(
                    account.getUserId(),
                    rule.getRuleId(),
                    LocalDateTime.now().minusDays(1)
            );
            if (dailyEarned + points > rule.getMaxDaily()) {
                throw new PointsLimitException(
                        String.format("Превышен дневной лимит для правила %s. Заработано очков за день: %d, Attempt: %d, Limit: %d",
                                rule.getRuleId(), dailyEarned, points, rule.getMaxDaily()));
            }
        }

        // Проверка общего лимита
        if (rule.getMaxTotal() != null) {
            Long totalEarned = transactionRepository.calculateTotalPoints(
                    account.getUserId(),
                    rule.getRuleId()
            );
            if (totalEarned + points > rule.getMaxTotal()) {
                throw new PointsLimitException(
                        String.format("Превышен общий лимит для правила %s. Заработано очков всего: %d, Attempt: %d, Limit: %d",
                                rule.getRuleId(), totalEarned, points, rule.getMaxTotal()));
            }
        }
    }

    private PointsTransaction createTransaction(GamificationEvent event, RuleEntity rule, Long points) {
        return PointsTransaction.builder()
                .userId(event.userId())
                .pointsDelta(points)
                .type(points > 0 ? TransactionType.EARN : TransactionType.SPEND)
                .status(TransactionStatus.COMPLETED)
                .eventId(event.eventId().toString())
                .ruleId(rule.getRuleId())
                .source(event.type())
                .description(rule.getName())
                .transactionDate(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(365))
                .build();
    }

    private void updateBalance(PointsAccount account, Long points, RuleEntity rule) {
        account.setTotalPoints(account.getTotalPoints() + points);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        account.setUpdatedAt(LocalDateTime.now());

        // Логика "заморозки" очков на время
//        if (rule.getExpirationDays() != null) {
//            // Можно добавить логику frozen points
//        }
    }

    private void publishPointsChangedEvent(PointsAccount account, PointsTransaction transaction) {
        try {
            PointsChangedEvent event = PointsChangedEvent.awardPoints(
                    UUID.fromString(transaction.getEventId()),
                    account.getUserId(),
                    transaction.getPointsDelta(),
                    account.getAvailablePoints(),
                    account.getTotalPoints(),
                    account.getLevel(),
                    transaction.getTransactionId(),
                    transaction.getRuleId()
            );

//            pointsChangedProducer.send(event);
            log.debug("Published points changed event for user {}", account.getUserId());

        } catch (Exception e) {
            log.error("Failed to publish points changed event: {}", e.getMessage(), e);
            // Не бросаем исключение, чтобы не откатывать транзакцию
        }
    }

    private void checkLevelUp(PointsAccount account) {
        int newLevel = pointsLevelCalculator.calculateLevel(account.getTotalPoints());

        if (newLevel > account.getLevel()) {
            int oldLevel = account.getLevel();
            account.setLevel(newLevel);

            log.info("User {} leveled up from {} to {}",
                    account.getUserId(), oldLevel, newLevel);

            // Можно отправить событие о повышении уровня
            publishLevelUpEvent(account, oldLevel, newLevel);
        }
    }

    private void publishLevelUpEvent(PointsAccount account, int oldLevel, int newLevel) {
        try {
            LevelUpEvent event = LevelUpEvent.create(
                    account.getUserId(),
                    oldLevel,
                    newLevel,
                    account.getTotalPoints()
            );

            // pointsChangedProducer.sendLevelUp(event); // если есть такой метод
            log.debug("Событие повышения уровня: пользователь {} с {} до {}",
                    account.getUserId(), oldLevel, newLevel);

        } catch (Exception e) {
            log.error("Ошибка публикации события повышения уровня: {}", e.getMessage(), e);
        }
    }

    /**
     * Валидирует идентификатор пользователя.
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }
    }

    /**
     * Валидирует параметры начисления очков.
     */
    private void validateAwardParameters(String userId, RuleEntity rule, String eventId) {
        validateUserId(userId);

        if (rule == null) {
            throw new IllegalArgumentException("Правило не может быть null");
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор события не может быть пустым");
        }
    }

    /**
     * Валидирует параметры списания очков.
     */
    private void validateSpendParameters(String userId, Long amount, String purpose) {
        validateUserId(userId);

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Количество очков для списания должно быть положительным");
        }

        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Назначение платежа не может быть пустым");
        }
    }

    /**
     * Создает новый аккаунт пользователя.
     */
    private PointsAccount createNewAccount(String userId) {
        return PointsAccount.builder()
                .userId(userId)
                .totalPoints(0L)
                .availablePoints(0L)
                .frozenPoints(0L)
                .level(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Обновляет баланс аккаунта.
     */
    private void updateAccountBalance(PointsAccount account, Long points) {
        account.setTotalPoints(account.getTotalPoints() + points);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        account.setUpdatedAt(LocalDateTime.now());

        // Обновляем уровень пользователя
        updateUserLevel(account);
    }
}
