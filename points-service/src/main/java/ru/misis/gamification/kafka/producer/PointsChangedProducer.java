//package ru.misis.gamification.kafka.producer;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import ru.misis.gamification.model.PointsChangedEvent;
//
///**
// * Продюсер для отправки событий об изменении очков пользователя в Kafka.
// *
// * <p>Отправляет события в топик {@code points.changed} для уведомления других сервисов
// * системы (например, сервиса уведомлений, сервиса достижений) об изменении баланса очков.</p>
// *
// * <p>Ключом сообщения является userId, что обеспечивает гарантированный порядок
// * обработки событий для каждого пользователя в рамках одной партиции.</p>
// **/
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class PointsChangedProducer {
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    /**
//     * Отправляет событие об изменении очков пользователя в Kafka.
//     *
//     * <p>Метод является идемпотентным: повторная отправка того же события
//     * не приводит к дублированию начисления очков в системе-получателе.</p>
//     *
//     * @throws RuntimeException если произошла ошибка при отправке в Kafka.
//     *                          Ошибка логируется, но не прерывает выполнение основного потока,
//     *                          чтобы не откатывать транзакцию начисления очков.
//     **/
//    public void send(PointsChangedEvent event) {
//        try {
//            kafkaTemplate.send("points.changed", event.getUserId(), event);
//            log.debug("Sent points changed event for user: {}", event.getUserId());
//        } catch (Exception e) {
//            log.error("Failed to send points changed event: {}", e.getMessage(), e);
//        }
//    }
//}
