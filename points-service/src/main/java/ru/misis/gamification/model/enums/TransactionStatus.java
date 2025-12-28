package ru.misis.gamification.model.enums;

/**
 * Статус транзакции
 */
public enum TransactionStatus {
    PENDING,      // В обработке
    COMPLETED,    // Завершена успешно
    FAILED,       // Ошибка
    ROLLED_BACK   // Откат
}
