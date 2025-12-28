//package ru.misis.gamification.kafka.service;
//
//import lombok.Builder;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//import ru.misis.gamification.events.domain.GamificationEvent;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.time.LocalDateTime;
//
///**
// * Сервис для работы с Dead Letter Queue (DLQ) - очередью "мертвых" сообщений.
// *
// * <p>DLQ используется для хранения сообщений, которые не удалось обработать после
// * исчерпания всех попыток retry. Это позволяет:</p>
// * <ul>
// *   <li>Не блокировать основную очередь проблемными сообщениями</li>
// *   <li>Сохранять все неудачные сообщения для последующего анализа</li>
// *   <li>Вручную повторно обработать сообщения после исправления проблемы</li>
// *   <li>Собирать метрики по типам ошибок обработки</li>
// * </ul>
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class DeadLetterQueueService {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    /**
//     * Отправляет событие в Dead Letter Queue для последующего анализа.
//     *
//     * <p>Метод оборачивает оригинальное событие в контейнер с информацией об ошибке
//     * и отправляет в специализированный топик DLQ.</p>
//     *
//     * @param event оригинальное событие, которое не удалось обработать
//     * @param exception исключение, вызвавшее сбой обработки
//     *
//     * @throws RuntimeException если не удалось отправить сообщение в DLQ.
//     *                          Ошибка логируется, но не прерывает основной поток.
//     */
//    public void sendToDlq(GamificationEvent event, Exception exception) {
//        try {
//            DlqMessage dlqMessage = DlqMessage.builder()
//                    .originalEvent(event)
//                    .errorMessage(exception.getMessage())
//                    .errorType(exception.getClass().getName())
//                    .stackTrace(getStackTraceAsString(exception))
//                    .failedAt(LocalDateTime.now())
//                    .retryCount(0) // Можно увеличивать при повторных отправках
//                    .build();
//
//            kafkaTemplate.send("gamification.dlq", event.getEventId().toString(), dlqMessage);
//            log.warn("Sent event {} to DLQ. Error: {}", event.getEventId(), exception.getMessage());
//
//        } catch (Exception e) {
//            log.error("Failed to send event to DLQ: {}", e.getMessage(), e);
//            // Не бросаем исключение, чтобы не маскировать оригинальную ошибку
//        }
//    }
//
//    /**
//     * Преобразует стектрейс исключения в строку.
//     */
//    private String getStackTraceAsString(Exception exception) {
//        StringWriter sw = new StringWriter();
//        PrintWriter pw = new PrintWriter(sw);
//        exception.printStackTrace(pw);
//        return sw.toString();
//    }
//
//    /**
//     * DTO для сообщений в DLQ
//     */
//    @Data
//    @Builder
//    public static class DlqMessage {
//
//        /**
//         * Оригинальное событие
//         */
//        private GamificationEvent originalEvent;
//
//        /**
//         * Описание ошибки
//         */
//        private String errorMessage;
//
//        /**
//         * Тип исключения
//         */
//        private String errorType;
//
//        /**
//         * Стектрейс
//         */
//        private String stackTrace;
//
//        /**
//         * Время возникновения ошибки
//         */
//        private LocalDateTime failedAt;
//
//        /**
//         * Количество выполненных попыток
//         */
//        private Integer retryCount;
//    }
//}