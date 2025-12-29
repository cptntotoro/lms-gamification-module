package ru.misis.gamification.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misis.gamification.exception.DuplicateEventException;
import ru.misis.gamification.exception.PointsLimitException;
import ru.misis.gamification.exception.TransactionNotFoundException;
import ru.misis.gamification.model.entity.PointsTransaction;
import ru.misis.gamification.model.entity.RuleEntity;
import ru.misis.gamification.model.enums.TransactionStatus;
import ru.misis.gamification.model.enums.TransactionType;
import ru.misis.gamification.repository.PointsTransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    /**
     * Репозиторий для работы с транзакциями
     */
    private final PointsTransactionRepository transactionRepository;

    /**
     * Стандартный срок действия начисленных очков в днях.
     */
    private static final int DEFAULT_POINTS_EXPIRATION_DAYS = 365;

    @Override
    public PointsTransaction createAwardTransaction(
            String userId,
            RuleEntity rule,
            String eventId,
            Long points,
            String description) {

        validateTransactionParameters(userId, points, eventId);

        if (rule == null) {
            throw new IllegalArgumentException("Правило не может быть null");
        }

        PointsTransaction transaction = PointsTransaction.builder()
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

        PointsTransaction savedTransaction = transactionRepository.save(transaction);
        log.debug("Создана транзакция начисления: {} очков пользователю {} по правилу {}",
                points, userId, rule.getRuleId());

        return savedTransaction;
    }

    @Override
    public PointsTransaction createSpendTransaction(
            String userId,
            String eventId,
            Long amount,
            String purpose) {

        validateTransactionParameters(userId, amount, "MANUAL_SPEND_" + UUID.randomUUID());

        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Назначение платежа не может быть пустым");
        }

        PointsTransaction transaction = PointsTransaction.builder()
                .userId(userId)
                .pointsDelta(-amount)
                .type(TransactionType.SPEND)
                .status(TransactionStatus.COMPLETED)
                .eventId(eventId)
                .ruleId("MANUAL_SPEND")
                .source("USER_SPENDING")
                .description("Списание: " + purpose)
                .transactionDate(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(DEFAULT_POINTS_EXPIRATION_DAYS))
                .build();

        PointsTransaction savedTransaction = transactionRepository.save(transaction);
        log.info("Создана транзакция списания: {} очков пользователю {} - {}",
                amount, userId, purpose);

        return savedTransaction;
    }

    @Override
    public void checkDuplicateEvent(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор события не может быть пустым");
        }

        transactionRepository.findByEventId(eventId)
                .ifPresent(existing -> {
                    String errorMessage = String.format("Событие уже обработано: %s", eventId);
                    log.warn(errorMessage);
                    throw new DuplicateEventException(errorMessage);
                });
    }

    @Override
    public void checkRuleLimits(String userId, RuleEntity rule) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }

        if (rule == null) {
            throw new IllegalArgumentException("Правило не может быть null");
        }

        // Проверка дневного лимита
        if (rule.getMaxDaily() != null) {
            Long dailyEarned = transactionRepository.calculateDailyPoints(
                    userId,
                    rule.getRuleId(),
                    LocalDateTime.now().minusDays(1)
            );

            if (dailyEarned >= rule.getMaxDaily()) {
                String errorMessage = String.format(
                        "Дневной лимит правила %s исчерпан (лимит: %d, заработано: %d)",
                        rule.getRuleId(), rule.getMaxDaily(), dailyEarned);
                log.warn(errorMessage);
                throw new PointsLimitException(errorMessage);
            }
        }

        // Проверка общего лимита
        if (rule.getMaxTotal() != null) {
            Long totalEarned = transactionRepository.calculateTotalPoints(userId, rule.getRuleId());

            if (totalEarned >= rule.getMaxTotal()) {
                String errorMessage = String.format(
                        "Общий лимит правила %s исчерпан (лимит: %d, заработано: %d)",
                        rule.getRuleId(), rule.getMaxTotal(), totalEarned);
                log.warn(errorMessage);
                throw new PointsLimitException(errorMessage);
            }
        }

        log.debug("Лимиты правила {} для пользователя {} проверены успешно",
                rule.getRuleId(), userId);
    }


    @Transactional(readOnly = true)
    @Override
    public List<PointsTransaction> getUserTransactions(String userId, int limit) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }

        if (limit <= 0) {
            limit = 50; // Значение по умолчанию
        }

        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Long calculateTotalPointsByRule(String userId, String ruleId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }

        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор правила не может быть пустым");
        }

        return transactionRepository.calculateTotalPoints(userId, ruleId);
    }

    @Transactional(readOnly = true)
    @Override
    public Long calculateDailyPointsByRule(String userId, String ruleId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }

        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор правила не может быть пустым");
        }

        return transactionRepository.calculateDailyPoints(
                userId,
                ruleId,
                LocalDateTime.now().minusDays(1)
        );
    }

    /**
     * Находит транзакцию по идентификатору события.
     *
     * @param eventId Идентификатор события
     * @return Транзакция
     */
    @Transactional(readOnly = true)
    @Override
    public PointsTransaction findByEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор события не может быть пустым");
        }

        return transactionRepository.findByEventId(eventId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена по идентификатору события: " + eventId));
    }

    /**
     * Валидирует параметры транзакции.
     *
     * @param userId  идентификатор пользователя
     * @param points  количество очков
     * @param eventId идентификатор события
     * @throws IllegalArgumentException если любой из параметров некорректен
     */
    private void validateTransactionParameters(String userId, Long points, String eventId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }

        if (points == null || points <= 0) {
            throw new IllegalArgumentException("Количество очков должно быть положительным числом");
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор события не может быть пустым");
        }
    }

    @Override
    public PointsTransaction cancelTransaction(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Идентификатор транзакции не может быть null");
        }

        PointsTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Транзакция с ID %s не найдена", transactionId)));

        if (transaction.getStatus() == TransactionStatus.ROLLED_BACK) {
            throw new IllegalStateException("Транзакция уже отменена");
        }

        transaction.setStatus(TransactionStatus.ROLLED_BACK);
        transaction.setUpdatedAt(LocalDateTime.now());

        PointsTransaction cancelledTransaction = transactionRepository.save(transaction);
        log.info("Транзакция {} отменена", transactionId);

        return cancelledTransaction;
    }

    @Transactional(readOnly = true)
    @Override
    public TransactionStats getTransactionStats(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }

        Long totalEarned = transactionRepository.sumPositivePointsByUser(userId);
        Long totalSpent = transactionRepository.sumNegativePointsByUser(userId);
        Long transactionCount = transactionRepository.countByUserId(userId);

        return new TransactionStats(totalEarned, totalSpent, transactionCount);
    }

    /**
     * Статистика транзакций пользователя.
     */
    public record TransactionStats(Long totalEarned, Long totalSpent, Long transactionCount) {
        /**
         * Рассчитывает чистый баланс (заработано минус потрачено).
         */
        public Long getNetBalance() {
            return (totalEarned != null ? totalEarned : 0L) -
                    (totalSpent != null ? Math.abs(totalSpent) : 0L);
        }
    }
}
