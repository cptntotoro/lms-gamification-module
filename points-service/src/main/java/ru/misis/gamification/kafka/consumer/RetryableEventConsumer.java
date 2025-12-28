//package ru.misis.gamification.kafka.consumer;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.retry.annotation.Retryable;
//import org.springframework.stereotype.Component;
//import ru.misis.gamification.events.domain.GamificationEvent;
//import ru.misis.gamification.exception.KafkaConsumingException;
//
///**
// * Консьюмер с поддержкой повторных попыток обработки событий.
// *
// * <p>Обрабатывает события из топика {@code gamification.events.retry}, куда попадают
// * события, которые не удалось обработать с первой попытки. Реализует паттерн retry
// * с экспоненциальной задержкой между попытками.</p>
// *
// * <p>Особенности:
// * <ul>
// *   <li>Экспоненциальная backoff стратегия: 1s, 2s, 4s, 8s, 16s</li>
// *   <li>Максимум 5 попыток обработки</li>
// *   <li>После исчерпания попыток событие отправляется в DLQ</li>
// *   <li>Перехватывает только {@link KafkaConsumingException} для retry</li>
// * </ul>
// * </p>
// *
// * <p>Используется Spring Retry для декларативного управления повторными попытками.</p>
// */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class RetryableEventConsumer {
//
//    private final PointsEventConsumer pointsEventConsumer;
//
//    /**
//     * Обрабатывает событие с поддержкой повторных попыток.
//     *
//     * <p>Метод пытается обработать событие до 5 раз с экспоненциальной задержкой.
//     * Если после всех попыток обработка не удалась, событие считается окончательно
//     * неудачным и дальнейшая обработка прекращается.</p>
//     *
//     * @param event событие для обработки с повторными попытками
//     * @throws KafkaConsumingException если событие не удалось обработать после всех попыток
//     * @see org.springframework.retry.annotation.Retryable
//     * @see org.springframework.retry.annotation.Backoff
//     */
//    @Retryable(
//            retryFor = KafkaConsumingException.class,
//            maxAttempts = 5,
//            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 16000)
//    )
//    @KafkaListener(
//            topics = "${kafka.topics.events.retry}",
//            groupId = "${kafka.consumer.retry-group-id}",
//            concurrency = "1"
//    )
//    public void handleEventWithRetry(GamificationEvent event) {
//        log.info("Processing event with retry: {}", event.getEventId());
//
//        try {
//            // Делегируем обработку основному консьюмеру
//            pointsEventConsumer.handleEvent(event);
//            log.info("Successfully processed retry event: {}", event.getEventId());
//
//        } catch (Exception e) {
//            log.error("Failed to process retry event {}: {}", event.getEventId(), e.getMessage());
//            throw new KafkaConsumingException("Retry processing failed", e);
//        }
//    }
//}