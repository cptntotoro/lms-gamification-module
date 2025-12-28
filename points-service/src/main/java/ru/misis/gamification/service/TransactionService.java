//package ru.misis.gamification.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.IllegalTransactionStateException;
//import org.springframework.transaction.annotation.Transactional;
//import ru.misis.gamification.exception.DuplicateTransactionException;
//import ru.misis.gamification.exception.InsufficientPointsException;
//import ru.misis.gamification.exception.TransactionNotFoundException;
//import ru.misis.gamification.model.dto.TransactionHistory;
//import ru.misis.gamification.model.entity.PointsAccount;
//import ru.misis.gamification.model.entity.PointsTransaction;
//import ru.misis.gamification.model.enums.TransactionStatus;
//import ru.misis.gamification.model.enums.TransactionType;
//import ru.misis.gamification.repository.PointsAccountRepository;
//import ru.misis.gamification.repository.PointsTransactionRepository;
//
//import javax.security.auth.login.AccountNotFoundException;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Сервис управления транзакциями.
// * Реализует паттерн SAGA для отката распределенных транзакций.
// * Для научного исследования: позволяет изучать паттерны отказов и восстановления.
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class TransactionService {
//
//    private final PointsAccountRepository accountRepository;
//    private final PointsTransactionRepository transactionRepository;
//
//    /**
//     * Создание транзакции начисления.
//     * Гарантирует идемпотентность через eventId.
//     */
//    @Transactional
//    public PointsTransaction createEarnTransaction(
//            String userId,
//            Long points,
//            String ruleId,
//            String eventId,
//            String source,
//            String description) {
//
//        // Проверка дубликата
//        transactionRepository.findByEventId(eventId)
//                .ifPresent(existing -> {
//                    throw new DuplicateTransactionException(
//                            "Transaction already exists for event: " + eventId);
//                });
//
//        // Получаем или создаем аккаунт
//        PointsAccount account = accountRepository.findByUserId(userId)
//                .orElseGet(() -> createNewAccount(userId));
//
//        // Создаем транзакцию
//        PointsTransaction transaction = PointsTransaction.builder()
//                .userId(userId)
//                .pointsDelta(points)
//                .type(TransactionType.EARN)
//                .status(TransactionStatus.PENDING)
//                .ruleId(ruleId)
//                .eventId(eventId)
//                .source(source)
//                .description(description)
//                .transactionDate(LocalDateTime.now())
//                .expiresAt(LocalDateTime.now().plusDays(365))
//                .build();
//
//        // Сохраняем транзакцию (пока без обновления баланса)
//        transaction = transactionRepository.save(transaction);
//
//        log.info("Created earn transaction {} for user {}",
//                transaction.getTransactionId(), userId);
//
//        return transaction;
//    }
//
//    /**
//     * Подтверждение транзакции (commit).
//     * Обновляет баланс пользователя.
//     */
//    @Transactional
//    public void commitTransaction(UUID transactionId) {
//        PointsTransaction transaction = transactionRepository.findById(transactionId)
//                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
//
//        if (transaction.getStatus() != TransactionStatus.PENDING) {
//            throw new IllegalTransactionStateException(
//                    "Transaction is not in PENDING state: " + transaction.getStatus());
//        }
//
//        // Обновляем баланс
//        PointsAccount account = accountRepository.findByUserId(transaction.getUserId())
//                .orElseThrow(() -> new AccountNotFoundException(transaction.getUserId()));
//
//        account.setTotalPoints(account.getTotalPoints() + transaction.getPointsDelta());
//        account.setAvailablePoints(account.getAvailablePoints() + transaction.getPointsDelta());
//        account.setUpdatedAt(LocalDateTime.now());
//
//        // Обновляем статус транзакции
//        transaction.setStatus(TransactionStatus.COMPLETED);
//
//        accountRepository.save(account);
//        transactionRepository.save(transaction);
//
//        log.info("Committed transaction {} for user {}",
//                transactionId, transaction.getUserId());
//
//        // Триггерим события для других сервисов
//        publishTransactionCommittedEvent(transaction);
//    }
//
//    /**
//     * Откат транзакции (rollback).
//     * Компенсирующая транзакция в SAGA.
//     */
//    @Transactional
//    public void rollbackTransaction(UUID transactionId, String reason) {
//        PointsTransaction transaction = transactionRepository.findById(transactionId)
//                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
//
//        // Если транзакция уже завершена, создаем компенсирующую
//        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
//            createCompensationTransaction(transaction, reason);
//        }
//
//        // Помечаем как откаченную
//        transaction.setStatus(TransactionStatus.ROLLED_BACK);
//        transactionRepository.save(transaction);
//
//        log.warn("Rolled back transaction {}: {}", transactionId, reason);
//    }
//
//    /**
//     * Создание компенсирующей транзакции.
//     */
//    private void createCompensationTransaction(
//            PointsTransaction original,
//            String reason) {
//
//        PointsTransaction compensation = PointsTransaction.builder()
//                .userId(original.getUserId())
//                .pointsDelta(-original.getPointsDelta()) // Обратный знак
//                .type(TransactionType.CORRECTION)
//                .status(TransactionStatus.COMPLETED)
//                .ruleId(original.getRuleId())
//                .eventId("compensation-" + original.getEventId())
//                .source("COMPENSATION")
//                .description("Compensation for: " + original.getDescription() + ". Reason: " + reason)
//                .transactionDate(LocalDateTime.now())
//                .expiresAt(LocalDateTime.now().plusDays(365))
//                .build();
//
//        // Обновляем баланс
//        PointsAccount account = accountRepository.findByUserId(original.getUserId())
//                .orElseThrow(() -> new AccountNotFoundException(original.getUserId()));
//
//        account.setAvailablePoints(account.getAvailablePoints() - original.getPointsDelta());
//        account.setUpdatedAt(LocalDateTime.now());
//
//        accountRepository.save(account);
//        transactionRepository.save(compensation);
//
//        log.info("Created compensation transaction {} for original {}",
//                compensation.getTransactionId(), original.getTransactionId());
//    }
//
//    /**
//     * Пакетная обработка транзакций.
//     * Для производительности при высокой нагрузке.
//     */
//    @Transactional
//    public void processBatchTransactions(List<PointsTransaction> transactions) {
//        for (PointsTransaction transaction : transactions) {
//            try {
//                if (transaction.getPointsDelta() > 0) {
//                    commitTransaction(transaction.getTransactionId());
//                } else {
//                    // Для списаний нужна дополнительная проверка баланса
//                    processSpendTransaction(transaction);
//                }
//            } catch (Exception e) {
//                log.error("Failed to process transaction {}: {}",
//                        transaction.getTransactionId(), e.getMessage());
//                transaction.setStatus(TransactionStatus.FAILED);
//                transactionRepository.save(transaction);
//            }
//        }
//    }
//
//    /**
//     * Обработка транзакции списания.
//     */
//    @Transactional
//    public void processSpendTransaction(PointsTransaction transaction) {
//        PointsAccount account = accountRepository.findByUserId(transaction.getUserId())
//                .orElseThrow(() -> new AccountNotFoundException(transaction.getUserId()));
//
//        // Проверяем достаточность средств
//        if (account.getAvailablePoints() < Math.abs(transaction.getPointsDelta())) {
//            throw new InsufficientPointsException(
//                    String.format("User %s has insufficient points. Available: %d, Required: %d",
//                            transaction.getUserId(),
//                            account.getAvailablePoints(),
//                            Math.abs(transaction.getPointsDelta())));
//        }
//
//        // Обновляем баланс
//        account.setAvailablePoints(account.getAvailablePoints() + transaction.getPointsDelta()); // pointsDelta отрицательный
//        account.setUpdatedAt(LocalDateTime.now());
//
//        transaction.setStatus(TransactionStatus.COMPLETED);
//
//        accountRepository.save(account);
//        transactionRepository.save(transaction);
//
//        log.info("Processed spend transaction {} for user {}",
//                transaction.getTransactionId(), transaction.getUserId());
//    }
//
//    /**
//     * Получение истории транзакций с аналитикой.
//     * Для научного исследования: анализ паттернов активности.
//     */
//    public TransactionHistory getTransactionHistory(String userId,
//                                                    LocalDateTime from,
//                                                    LocalDateTime to) {
//        List<PointsTransaction> transactions = transactionRepository
//                .findByUserIdAndPeriod(userId, from, to,
//                        org.springframework.data.domain.Pageable.unpaged())
//                .getContent();
//
//        long totalEarned = transactions.stream()
//                .filter(t -> t.getPointsDelta() > 0)
//                .mapToLong(PointsTransaction::getPointsDelta)
//                .sum();
//
//        long totalSpent = transactions.stream()
//                .filter(t -> t.getPointsDelta() < 0)
//                .mapToLong(t -> Math.abs(t.getPointsDelta()))
//                .sum();
//
//        return TransactionHistory.builder()
//                .userId(userId)
//                .transactions(transactions)
//                .totalEarned(totalEarned)
//                .totalSpent(totalSpent)
//                .periodFrom(from)
//                .periodTo(to)
//                .build();
//    }
//
//    /**
//     * Создание нового аккаунта.
//     */
//    private PointsAccount createNewAccount(String userId) {
//        PointsAccount account = PointsAccount.builder()
//                .userId(userId)
//                .totalPoints(0L)
//                .availablePoints(0L)
//                .frozenPoints(0L)
//                .level(1)
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//
//        return accountRepository.save(account);
//    }
//
//    /**
//     * Публикация события о завершении транзакции.
//     */
//    private void publishTransactionCommittedEvent(PointsTransaction transaction) {
//        // Отправка в Kafka для других сервисов (achievements, leaderboard, notifications)
//        // Реализация зависит от инфраструктуры
//    }
//}