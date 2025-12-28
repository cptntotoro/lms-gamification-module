package ru.misis.gamification.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.misis.gamification.model.entity.PointsAccount;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с балансами пользователей.
 * Использует оптимистичные блокировки для предотвращения гонок при конкурентном обновлении.
 * Важно для научного исследования: позволяет изучать паттерны активности студентов.
 */
@Repository
public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {

    /**
     * Находит аккаунт пользователя с оптимистичной блокировкой.
     * Для исследования: позволяет отслеживать количество оптимистичных коллизий
     * как метрику пиковой нагрузки.
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<PointsAccount> findByUserId(String userId);

    /**
     * Топ-N пользователей по общему количеству очков.
     * Используется для рейтингов - важный элемент геймификации.
     */
    @Query("SELECT pa FROM PointsAccount pa ORDER BY pa.totalPoints DESC")
    List<PointsAccount> findTopByTotalPoints(@Param("limit") int limit);

    /**
     * Топ-N пользователей по доступным очкам.
     * Для исследования: сравнение "накопителей" и "тратящих".
     */
    @Query("SELECT pa FROM PointsAccount pa ORDER BY pa.availablePoints DESC")
    List<PointsAccount> findTopByAvailablePoints(@Param("limit") int limit);

    /**
     * Топ-N пользователей по уровню.
     * Уровень - производная от totalPoints, но с нелинейной прогрессией.
     */
    @Query("SELECT pa FROM PointsAccount pa ORDER BY pa.level DESC")
    List<PointsAccount> findTopByLevel(@Param("limit") int limit);

    /**
     * Проверяет существование аккаунта.
     */
    boolean existsByUserId(String userId);

    /**
     * Атомарное обновление баланса.
     * Для исследования: позволяет избегать гонок при конкурентных событиях.
     */
    @Modifying
    @Query("UPDATE PointsAccount pa SET " +
            "pa.totalPoints = pa.totalPoints + :pointsDelta, " +
            "pa.availablePoints = pa.availablePoints + :pointsDelta, " +
            "pa.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE pa.userId = :userId")
    int updateBalanceAtomically(
            @Param("userId") String userId,
            @Param("pointsDelta") Long pointsDelta);

    /**
     * Получает статистику по группам пользователей.
     * Для научного исследования: анализ распределения активности.
     */
    @Query("SELECT " +
            "FLOOR(pa.totalPoints / 1000) * 1000 as pointsRange, " +
            "COUNT(pa) as userCount " +
            "FROM PointsAccount pa " +
            "GROUP BY FLOOR(pa.totalPoints / 1000) " +
            "ORDER BY pointsRange")
    List<Object[]> getPointsDistribution();

    /**
     * Находит аккаунты с истекающими очками.
     * Важно для исследования: как студенты используют "срочные" бонусы.
     */
    @Query("SELECT pa FROM PointsAccount pa WHERE pa.availablePoints > 0 " +
            "AND EXISTS (SELECT 1 FROM PointsTransaction pt " +
            "WHERE pt.userId = pa.userId " +
            "AND pt.expiresAt BETWEEN CURRENT_TIMESTAMP AND :threshold)")
    List<PointsAccount> findAccountsWithExpiringPoints(
            @Param("threshold") java.time.LocalDateTime threshold);
}
