package ru.misis.gamification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.misis.gamification.metrics.GamificationMetrics;
import ru.misis.gamification.model.entity.PointsAccount;
import ru.misis.gamification.model.entity.PointsTransaction;
import ru.misis.gamification.repository.PointsAccountRepository;
import ru.misis.gamification.repository.PointsTransactionRepository;
import ru.misis.gamification.repository.RulesRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис метрик
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    /**
     * Сервис управления очками
     */
    private final PointsAccountRepository accountRepository;

    /**
     * Репозиторий для работы с транзакциями.
     */
    private final PointsTransactionRepository transactionRepository;

    /**
     * Репозиторий для правил начисления
     */
    private final RulesRepository rulesRepository;

    // Кэш для часто запрашиваемых метрик
    private GamificationMetrics cachedMetrics;
    private LocalDateTime cacheTime;

    /**
     * Получение общих метрик системы.
     */
    public GamificationMetrics.SystemMetrics getSystemMetrics(
            LocalDateTime from, LocalDateTime to) {

        long totalUsers = accountRepository.count();

        long activeUsers = transactionRepository
                .findByUserIdAndPeriod(null, from, to, null)
                .stream()
                .map(PointsTransaction::getUserId)
                .distinct()
                .count();

        Double averageLevel = accountRepository.findAll()
                .stream()
                .mapToInt(PointsAccount::getLevel)
                .average()
                .orElse(0.0);

        Long totalPoints = accountRepository.findAll()
                .stream()
                .mapToLong(PointsAccount::getTotalPoints)
                .sum();

        return GamificationMetrics.SystemMetrics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .averageLevel(averageLevel)
                .totalPointsInSystem(totalPoints)
                .build();
    }

    /**
     * Метрики по правилам.
     */
    public List<GamificationMetrics.RuleMetrics> getRuleMetrics(
            String ruleId, String eventType, int limit) {

        // Получаем статистику из транзакций
        List<Object[]> stats = transactionRepository.getRuleUsageStatistics(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        );

        return stats.stream()
                .filter(stat -> ruleId == null || stat[0].equals(ruleId))
                .map(stat -> GamificationMetrics.RuleMetrics.builder()
                        .ruleId((String) stat[0])
                        .ruleName((String) stat[1])
                        .applicationCount(((Number) stat[2]).longValue())
                        .totalPointsAwarded(((Number) stat[3]).longValue())
                        .averagePointsPerApplication(
                                ((Number) stat[3]).doubleValue() /
                                        ((Number) stat[2]).doubleValue())
                        .conversionRate(calculateConversionRate((String) stat[0]))
                        .build())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Расчет конверсии для правила.
     */
    private Double calculateConversionRate(String ruleId) {
        // Здесь должна быть сложная логика:
        // 1. Количество событий типа X
        // 2. Количество применений правила для этих событий
        // 3. conversion = (2) / (1)

        // Упрощенная версия:
        return Math.random(); // Заглушка
    }

    /**
     * Получение полного отчета.
     */
//    public GamificationMetrics getFullMetricsReport(
//            LocalDateTime from, LocalDateTime to) {
//
//        if (cachedMetrics != null &&
//                cacheTime != null &&
//                cacheTime.isAfter(LocalDateTime.now().minusMinutes(5))) {
//            return cachedMetrics; // Возвращаем из кэша
//        }
//
//        GamificationMetrics metrics = GamificationMetrics.builder()
//                .systemMetrics(getSystemMetrics(from, to))
//                .ruleMetrics(getRuleMetrics(null, null, 100)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                GamificationMetrics.RuleMetrics::getRuleId,
//                                rm -> rm)))
//                .topUsers(getTopUsers("total", 100)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                GamificationMetrics.UserMetrics::getUserId,
//                                um -> um)))
//                .temporalMetrics(getTemporalMetrics(from, to, AggregationPeriod.DAY))
//                .effectivenessMetrics(getEffectivenessMetrics(null, from, to))
//                .periodStart(from)
//                .periodEnd(to)
//                .generatedAt(LocalDateTime.now())
//                .build();
//
//        cachedMetrics = metrics;
//        cacheTime = LocalDateTime.now();
//
//        return metrics;
//    }
//
//    /**
//     * Экспорт метрик в разных форматах.
//     */
//    public byte[] exportMetrics(MetricsExportFormat format,
//                                LocalDateTime from,
//                                LocalDateTime to) {
//
//        GamificationMetrics metrics = getFullMetricsReport(from, to);
//
//        return switch (format) {
//            case JSON -> exportToJson(metrics);
//            case CSV -> exportToCsv(metrics);
//            case EXCEL -> exportToExcel(metrics);
//            case PDF -> exportToPdf(metrics);
//        };
//    }
//
//    private byte[] exportToJson(GamificationMetrics metrics) {
//        // Используем Jackson
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.registerModule(new JavaTimeModule());
//            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//            return mapper.writeValueAsBytes(metrics);
//        } catch (Exception e) {
//            throw new MetricsExportException("Failed to export to JSON", e);
//        }
//    }
    private byte[] exportToCsv(GamificationMetrics metrics) {
        StringBuilder csv = new StringBuilder();

        // Заголовок
        csv.append("Metric,Value\n");

        // Данные
        csv.append(String.format("Total Users,%d\n",
                metrics.getSystemMetrics().getTotalUsers()));
        csv.append(String.format("Average Level,%.2f\n",
                metrics.getSystemMetrics().getAverageLevel()));
        // ... остальные поля

        return csv.toString().getBytes();
    }

    /**
     * Периодическое обновление метрик.
     */
//    @Scheduled(fixedDelay = 300000) // Каждые 5 минут
//    public void updateCachedMetrics() {
//        log.info("Updating cached metrics");
//        cachedMetrics = null;
//        getFullMetricsReport(
//                LocalDateTime.now().minusDays(7),
//                LocalDateTime.now()
//        );
//    }
}

/**
 * Форматы экспорта.
 */
enum MetricsExportFormat {
    JSON("application/json", "json"),
    CSV("text/csv", "csv"),
    EXCEL("application/vnd.ms-excel", "xlsx"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String fileExtension;

    MetricsExportFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}

/**
 * Период агрегации.
 */
enum AggregationPeriod {
    HOUR, DAY, WEEK, MONTH
}