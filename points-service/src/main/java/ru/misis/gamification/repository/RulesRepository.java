package ru.misis.gamification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.misis.gamification.model.entity.RuleEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для правил начисления.
 * Особенность для научной работы: правила могут динамически меняться
 * в ходе эксперимента для измерения их эффективности.
 */
@Repository
public interface RulesRepository extends JpaRepository<RuleEntity, String> {

    /**
     * Активные правила для типа события.
     * Поддерживает A/B тестирование: разные правила для разных групп студентов.
     */
    @Query("SELECT r FROM RuleEntity r WHERE r.eventType = :eventType " +
            "AND r.isActive = true " +
            "AND r.validFrom <= :checkTime " +
            "AND (r.validTo IS NULL OR r.validTo >= :checkTime) " +
            "ORDER BY r.priority ASC")
    List<RuleEntity> findActiveRulesForEventType(
            @Param("eventType") String eventType,
            @Param("checkTime") LocalDateTime checkTime);

    /**
     * Поиск правила по ID.
     */
    Optional<RuleEntity> findByRuleId(String ruleId);

    /**
     * Все активные правила.
     */
    List<RuleEntity> findByIsActiveTrue();

    /**
     * Правила, требующие пересчета (например, после изменения формулы).
     * Для исследования: как изменения правил влияют на мотивацию.
     */
    @Query("SELECT r FROM RuleEntity r WHERE " +
            "(r.formula IS NOT NULL AND r.validTo IS NULL) OR " +
            "r.validTo >= CURRENT_TIMESTAMP")
    List<RuleEntity> findRulesRequiringRecalculation();

    /**
     * Правила по приоритету.
     */
    List<RuleEntity> findByEventTypeOrderByPriorityAsc(String eventType);

    /**
     * Деактивация устаревших правил.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE RuleEntity r SET r.isActive = false " +
            "WHERE r.validTo < CURRENT_TIMESTAMP AND r.isActive = true")
    int deactivateExpiredRules();

    /**
     * Правила с экспериментальным флагом.
     * Для A/B тестирования разных механик геймификации.
     */
    @Query("SELECT r FROM RuleEntity r WHERE " +
            "EXISTS (SELECT 1 FROM r.conditions c WHERE c.key = 'experimentGroup')")
    List<RuleEntity> findExperimentalRules();

    /**
     * Статистика использования правил.
     * Для исследования: какие правила наиболее эффективны.
     */
    @Query("SELECT r.ruleId, r.name, COUNT(t), SUM(t.pointsDelta) " +
            "FROM RuleEntity r LEFT JOIN PointsTransaction t ON r.ruleId = t.ruleId " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY r.ruleId, r.name")
    List<Object[]> getRuleUsageStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
