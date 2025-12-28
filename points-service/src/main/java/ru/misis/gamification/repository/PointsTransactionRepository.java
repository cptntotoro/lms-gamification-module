package ru.misis.gamification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.misis.gamification.model.entity.PointsTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с транзакциями.
 * Ключевой компонент для научного исследования:
 * позволяет анализировать временные паттерны активности студентов.
 */
@Repository
public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, UUID> {

    /**
     * Транзакции пользователя с пагинацией.
     */
    Page<PointsTransaction> findByUserId(String userId, Pageable pageable);

    /**
     * Транзакции за период.
     * Для исследования: активность по дням недели, времени суток.
     */
    @Query("SELECT pt FROM PointsTransaction pt WHERE pt.userId = :userId " +
            "AND pt.transactionDate BETWEEN :startDate AND :endDate " +
            "AND pt.status = 'COMPLETED' " +
            "ORDER BY pt.transactionDate DESC")
    Page<PointsTransaction> findByUserIdAndPeriod(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Сумма очков по правилу за день.
     * Для контроля лимитов и исследования "дофаминовых" паттернов.
     */
    @Query("SELECT COALESCE(SUM(pt.pointsDelta), 0) FROM PointsTransaction pt " +
            "WHERE pt.userId = :userId AND pt.ruleId = :ruleId " +
            "AND pt.transactionDate >= :startDate " +
            "AND pt.pointsDelta > 0 " +
            "AND pt.status = 'COMPLETED'")
    Long calculateDailyPoints(
            @Param("userId") String userId,
            @Param("ruleId") String ruleId,
            @Param("startDate") LocalDateTime startDate);

    /**
     * Общая сумма по правилу.
     */
    @Query("SELECT COALESCE(SUM(pt.pointsDelta), 0) FROM PointsTransaction pt " +
            "WHERE pt.userId = :userId AND pt.ruleId = :ruleId " +
            "AND pt.pointsDelta > 0 " +
            "AND pt.status = 'COMPLETED'")
    Long calculateTotalPoints(
            @Param("userId") String userId,
            @Param("ruleId") String ruleId);

    /**
     * Поиск по eventId для идемпотентности.
     * Критически важно: предотвращает дублирующие начисления.
     */
    Optional<PointsTransaction> findByEventId(String eventId);

    /**
     * Последние N транзакций.
     */
    @Query(value = "SELECT * FROM points_transactions pt " +
            "WHERE pt.user_id = :userId " +
            "ORDER BY pt.transaction_date DESC " +
            "LIMIT :count", nativeQuery = true)
    List<PointsTransaction> findLatestByUserId(
            @Param("userId") String userId,
            @Param("count") int count);

    /**
     * Аналитика: транзакции по типам.
     * Для исследования: какие действия студентов наиболее популярны.
     */
    @Query("SELECT pt.type, COUNT(pt), SUM(pt.pointsDelta) " +
            "FROM PointsTransaction pt " +
            "WHERE pt.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY pt.type")
    List<Object[]> getTransactionStatsByType(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Находит транзакции для отката (компенсирующие).
     * Для реализации SAGA pattern.
     */
    @Query("SELECT pt FROM PointsTransaction pt " +
            "WHERE pt.userId = :userId " +
            "AND pt.status = 'COMPLETED' " +
            "AND pt.transactionDate >= :sinceDate " +
            "AND pt.pointsDelta > 0 " +
            "ORDER BY pt.transactionDate DESC")
    List<PointsTransaction> findEarnedTransactionsForRollback(
            @Param("userId") String userId,
            @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Агрегация по часам дня.
     * Для исследования: когда студенты наиболее активны.
     */
    @Query("SELECT " +
            "EXTRACT(HOUR FROM pt.transactionDate) as hour, " +
            "COUNT(pt) as transactionCount, " +
            "SUM(pt.pointsDelta) as totalPoints " +
            "FROM PointsTransaction pt " +
            "WHERE pt.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY EXTRACT(HOUR FROM pt.transactionDate) " +
            "ORDER BY hour")
    List<Object[]> getHourlyActivity(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);


    List<Object[]> getRuleUsageStatistics(LocalDateTime localDateTime, LocalDateTime now);

    Page<PointsTransaction> findByUserIdAndFilters(String userId, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
}