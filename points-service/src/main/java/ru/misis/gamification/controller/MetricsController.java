//package ru.misis.gamification.controller;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import ru.misis.gamification.metrics.GamificationMetrics;
//import ru.misis.gamification.service.MetricsService;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/metrics")
//@Tag(name = "Metrics API", description = "API для получения метрик геймификации")
//@Slf4j
//@RequiredArgsConstructor
//public class MetricsController {
//
//    private final MetricsService metricsService;
//
////    @Operation(summary = "Получить общие метрики системы")
//    @GetMapping("/system")
//    public ResponseEntity<GamificationMetrics.SystemMetrics> getSystemMetrics(
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime from,
//
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime to) {
//
//        GamificationMetrics.SystemMetrics metrics = metricsService
//                .getSystemMetrics(from, to);
//        return ResponseEntity.ok(metrics);
//    }
//
//    @Operation(summary = "Получить метрики по правилам")
//    @GetMapping("/rules")
//    public ResponseEntity<List<GamificationMetrics.RuleMetrics>> getRuleMetrics(
//            @RequestParam(required = false) String ruleId,
//            @RequestParam(required = false) String eventType,
//            @RequestParam(defaultValue = "10") int limit) {
//
//        List<GamificationMetrics.RuleMetrics> metrics = metricsService
//                .getRuleMetrics(ruleId, eventType, limit);
//        return ResponseEntity.ok(metrics);
//    }
//
//    @Operation(summary = "Топ пользователей по очкам")
//    @GetMapping("/leaderboard")
//    public ResponseEntity<List<GamificationMetrics.UserMetrics>> getLeaderboard(
//            @RequestParam(defaultValue = "total") String sortBy,
//            @RequestParam(defaultValue = "100") int limit) {
//
//        List<GamificationMetrics.UserMetrics> leaderboard = metricsService
//                .getTopUsers(sortBy, limit);
//        return ResponseEntity.ok(leaderboard);
//    }
//
//    @Operation(summary = "Анализ эффективности геймификации")
//    @GetMapping("/effectiveness")
//    public ResponseEntity<GamificationMetrics.EffectivenessMetrics> getEffectivenessMetrics(
//            @RequestParam String courseId,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime semesterStart,
//
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime semesterEnd) {
//
//        GamificationMetrics.EffectivenessMetrics metrics = metricsService
//                .getEffectivenessMetrics(courseId, semesterStart, semesterEnd);
//        return ResponseEntity.ok(metrics);
//    }
//
//    @Operation(summary = "Экспорт метрик в разных форматах")
//    @GetMapping("/export")
//    public ResponseEntity<byte[]> exportMetrics(
//            @RequestParam MetricsExportFormat format,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime from,
//
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime to) {
//
//        byte[] exportData = metricsService.exportMetrics(format, from, to);
//
//        return ResponseEntity.ok()
//                .header("Content-Type", format.getContentType())
//                .header("Content-Disposition",
//                        "attachment; filename=\"metrics." + format.getFileExtension() + "\"")
//                .body(exportData);
//    }
//
//    @Operation(summary = "Анализ временных паттернов")
//    @GetMapping("/temporal")
//    public ResponseEntity<GamificationMetrics.TemporalMetrics> getTemporalMetrics(
//            @RequestParam
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime from,
//
//            @RequestParam
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime to,
//
//            @RequestParam(defaultValue = "DAY") AggregationPeriod period) {
//
//        GamificationMetrics.TemporalMetrics metrics = metricsService
//                .getTemporalMetrics(from, to, period);
//        return ResponseEntity.ok(metrics);
//    }
//}