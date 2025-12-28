package ru.misis.gamification.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.misis.gamification.model.enums.TransactionStatus;
import ru.misis.gamification.model.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Транзакция баллов пользователя
 * <p>
 * Каждая операция начисления или списания очков создает запись
 * в этой таблице для аудита и отслеживания истории операций.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "points_transactions")
public class PointsTransaction {

    /**
     * Уникальный идентификатор транзакции (UUID).
     * <p>
     * Генерируется автоматически при создании транзакции.
     * </p>
     */
    @Id
    private UUID transactionId;

    /**
     * Идентификатор пользователя, совершившего транзакцию.
     * <p>
     * Должен соответствовать userId в системе аутентификации.
     * </p>
     */
    @Column(nullable = false, length = 50)
    private String userId;

    /**
     * Изменение количества очков.
     * <p>
     * Положительное значение - начисление очков.
     * Отрицательное значение - списание очков.
     * </p>
     */
    @Column(nullable = false)
    private Long pointsDelta;

    /**
     * Тип транзакции
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /**
     * Статус транзакции
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /**
     * Идентификатор исходного события из Kafka, которое инициировало транзакцию
     */
    @Column(nullable = false, length = 100)
    private String eventId;

    /**
     * Идентификатор правила, по которому были начислены очки.
     * <p>
     * Ссылается на ruleId в таблице правил.
     * Для ручных операций может иметь специальные значения.
     * </p>
     */
    @Column(nullable = false, length = 100)
    private String ruleId;

    /**
     * Описание транзакции
     * <p>
     * Человеко-читаемое описание операции.
     * Пример: "Завершение курса"
     * </p>
     */
    @Column(length = 500)
    private String description;

    /**
     * Источник транзакции
     * <p>
     * Тип события, которое инициировало транзакцию.
     * </p>
     */
    @Column(nullable = false, length = 50)
    private String source;

    /**
     * Дата и время совершения транзакции
     */
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Дата и время, когда очки истекают (становятся недействительными).
     * <p>
     * По умолчанию - через 365 дней от даты начисления.
     * </p>
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Подготовка сущности перед сохранением.
     * <p>
     * Устанавливает дату транзакции и срок истечения по умолчанию,
     * если они не были заданы явно.
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = transactionDate.plusDays(365);
        }
        if (transactionId == null) {
            transactionId = UUID.randomUUID();
        }
    }

    /**
     * Проверяет, является ли транзакция начислением.
     *
     * @return {@code true} если pointsDelta > 0
     */
    public boolean isAward() {
        return pointsDelta != null && pointsDelta > 0;
    }

    /**
     * Проверяет, является ли транзакция списанием.
     *
     * @return {@code true} если pointsDelta < 0
     */
    public boolean isDeduction() {
        return pointsDelta != null && pointsDelta < 0;
    }

    /**
     * Проверяет, истекли ли очки по этой транзакции.
     *
     * @return {@code true} если текущая дата позже expiresAt
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
