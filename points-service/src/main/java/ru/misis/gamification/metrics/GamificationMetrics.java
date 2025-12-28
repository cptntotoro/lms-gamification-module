package ru.misis.gamification.metrics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Модель метрик геймификации для научного исследования.
 * Собирает данные для анализа эффективности системы.
 */
@Data
@Builder
public class GamificationMetrics {

    /**
     * Общие метрики по системе
     */
    @Data
    @Builder
    public static class SystemMetrics {
        /** Общее количество пользователей */
        private Long totalUsers;

        /** Активные пользователи за период */
        private Long activeUsers;

        /** Средний уровень пользователей */
        private Double averageLevel;

        /** Общее количество очков в системе */
        private Long totalPointsInSystem;
    }

    /**
     * Метрики по правилам
     */
    @Data
    @Builder
    public static class RuleMetrics {
        /** ID правила */
        private String ruleId;

        /** Название правила */
        private String ruleName;

        /** Количество применений */
        private Long applicationCount;

        /** Сумма начисленных очков */
        private Long totalPointsAwarded;

        /** Среднее количество очков за применение */
        private Double averagePointsPerApplication;

        /** Коэффициент конверсии (события -> начисления) */
        private Double conversionRate;
    }

    /**
     * Метрики по пользователям
     */
    @Data
    @Builder
    public static class UserMetrics {
        /** ID пользователя */
        private String userId;

        /** Общее количество очков */
        private Long totalPoints;

        /** Текущий уровень */
        private Integer level;

        /** Количество дней активности */
        private Integer activeDays;

        /** Среднее количество очков в день */
        private Double averagePointsPerDay;

        /** Любимый тип активности (по очкам) */
        private String favoriteActivityType;
    }

    /**
     * Временные метрики
     */
    @Data
    @Builder
    public static class TemporalMetrics {
        /** Активность по часам дня (час -> количество событий) */
        private Map<Integer, Long> hourlyActivity;

        /** Активность по дням недели */
        private Map<String, Long> dailyActivity;

        /** Тренд начисления очков (дата -> сумма) */
        private Map<LocalDateTime, Long> pointsTrend;
    }

    /**
     * Метрики эффективности
     */
    @Data
    @Builder
    public static class EffectivenessMetrics {
        /** Retention rate: % пользователей, вернувшихся после начисления */
        private Map<Integer, Double> retentionByDays; // дни после начисления -> %

        /** Корреляция с академической успеваемостью */
        private Map<String, Double> correlationWithGrades; // курс -> коэффициент корреляции

        /** Влияние на completion rate */
        private Double courseCompletionImprovement;

        /** Удовлетворенность пользователей (по опросам) */
        private Double userSatisfactionScore;
    }

    // Основные поля
    private SystemMetrics systemMetrics;
    private Map<String, RuleMetrics> ruleMetrics;
    private Map<String, UserMetrics> topUsers;
    private TemporalMetrics temporalMetrics;
    private EffectivenessMetrics effectivenessMetrics;

    /** Период сбора метрик */
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    /** Время генерации отчета */
    private LocalDateTime generatedAt;
}