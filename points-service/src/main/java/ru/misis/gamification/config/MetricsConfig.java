//package ru.misis.gamification.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Bean;
//import io.micrometer.core.instrument.binder.MeterBinder;
//import ru.misis.gamification.repository.PointsAccountRepository;
//import ru.misis.gamification.repository.PointsTransactionRepository;
//
//@Configuration
//public class MetricsConfig {
//
//    @Bean
//    public MeterBinder gamificationMetrics(
//            PointsAccountRepository accountRepository,
//            PointsTransactionRepository transactionRepository) {
//
//        return registry -> {
//            // Количество пользователей
//            registry.gauge("gamification.users.total",
//                    accountRepository,
//                    PointsAccountRepository::count);
//
//            // Средний уровень
//            registry.gauge("gamification.level.average",
//                    accountRepository,
//                    repo -> repo.findAll()
//                            .stream()
//                            .mapToInt(acc -> acc.getLevel())
//                            .average()
//                            .orElse(0.0));
//
//            // Количество транзакций в минуту
//            registry.more().counter("gamification.transactions.rate",
//                    "type", "all");
//        };
//    }
//}