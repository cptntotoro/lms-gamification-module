package ru.misis.gamification.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.misis.gamification.util.PointsLevelCalculator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Баланс очков пользователя
 * <p>
 * Хранит информацию о текущем состоянии очков пользователя:
 * общее количество, доступное, замороженное и уровень
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "points_accounts",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id", unique = true),
                @Index(name = "idx_total_points", columnList = "total_points DESC"),
                @Index(name = "idx_level", columnList = "level DESC")
        })
public class PointsAccount {
    /**
     * Идентификатор записи
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID uuid;

    /**
     * Идентификатор пользователя в системе (LMS)
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    /**
     * Общее количество очков, заработанных пользователем за все время.
     * <p>
     * Включает как доступные, так и замороженные и потраченные очки.
     * Используется для расчета уровня и рейтингов.
     * </p>
     */
    @Column(name = "total_points", nullable = false)
    private Long totalPoints;

    /**
     * Количество доступных очков (траты, переводов, обмена на бонусы).
     * <p>
     * Не может быть отрицательным.
     * </p>
     */
    @Column(name = "available_points", nullable = false)
    private Long availablePoints;

    /**
     * Количество замороженных (недоступных) очков
     * <p>
     *     Примеры использования:
     *     <ul>
     *         <li>Очки за задание, которое еще проверяется преподавателем</li>
     *         <li>Временная блокировка при подозрении на мошенничество</li>
     *         <li>Ожидание подтверждения операции</li>
     *     </ul>
     * </p>
     */
    @Column(name = "frozen_points", nullable = false)
    private Long frozenPoints;

    /**
     * Текущий уровень пользователя, рассчитанный на основе {@link PointsAccount#totalPoints}.
     *
     * Уровень повышается нелинейно: для каждого следующего уровня
     * требуется больше очков, чем для предыдущего.
     * Формула по умолчанию: level = floor(sqrt(totalPoints / 1000)) + 1
     */
    @Column(name = "level", nullable = false)
    private Integer level;

    /**
     * Версия записи для оптимистичной блокировки (optimistic locking).
     * Автоматически увеличивается при каждом обновлении.
     * Предотвращает lost updates при конкурентном доступе.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Дата и время создания записи об аккаунте.
     * Устанавливается автоматически при создании.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления баланса.
     * Обновляется при каждой операции с очками.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Предсоздает объект PointsAccount с дефолтными значениями.
     * Используется при первом начислении очков пользователю.
     *
     * @param userId идентификатор пользователя
     * @return PointsAccount с инициализированными значениями
     */
    public static PointsAccount createDefault(String userId) {
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
     * Проверяет, достаточно ли доступных очков для списания.
     *
     * @param amount Количество очков для списания
     * @return true если достаточно очков, иначе false
     * @throws IllegalArgumentException если amount <= 0
     */
    public boolean hasSufficientPoints(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Количество очков для списания должно быть положительным");
        }
        return availablePoints >= amount;
    }

    /**
     * Замораживает указанное количество очков.
     * Очки переходят из available в frozen.
     *
     * @param amount количество очков для заморозки
     * @throws IllegalArgumentException если amount <= 0 или недостаточно очков
     */
    public void freezePoints(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!hasSufficientPoints(amount)) {
            throw new IllegalArgumentException("Insufficient available points");
        }

        availablePoints -= amount;
        frozenPoints += amount;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Размораживает указанное количество очков.
     * Очки переходят из frozen в available.
     *
     * @param amount количество очков для разморозки
     * @throws IllegalArgumentException если amount <= 0 или недостаточно замороженных очков
     */
    public void unfreezePoints(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (frozenPoints < amount) {
            throw new IllegalArgumentException("Insufficient frozen points");
        }

        frozenPoints -= amount;
        availablePoints += amount;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Начисляет очки на баланс.
     * Увеличивает totalPoints и availablePoints.
     * Автоматически проверяет повышение уровня.
     *
     * @param amount          количество очков для начисления
     * @param levelCalculator калькулятор уровней
     * @throws IllegalArgumentException если amount <= 0
     */
    public void addPoints(Long amount, PointsLevelCalculator levelCalculator) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        totalPoints += amount;
        availablePoints += amount;
        updatedAt = LocalDateTime.now();

        // Проверяем повышение уровня
        int newLevel = levelCalculator.calculateLevel(totalPoints);
        if (newLevel > level) {
            level = newLevel;
        }
    }

    /**
     * Списывает очки с баланса.
     * Уменьшает только availablePoints.
     *
     * @param amount количество очков для списания
     * @throws IllegalArgumentException если amount <= 0 или недостаточно очков
     */
    public void spendPoints(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!hasSufficientPoints(amount)) {
            throw new IllegalArgumentException("Insufficient available points");
        }

        availablePoints -= amount;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Возвращает общее количество "живых" очков
     * (available + frozen).
     *
     * @return сумма доступных и замороженных очков
     */
    public Long getActivePoints() {
        return availablePoints + frozenPoints;
    }
}
