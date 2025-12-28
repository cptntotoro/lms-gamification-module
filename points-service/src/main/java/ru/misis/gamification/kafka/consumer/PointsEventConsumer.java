//package ru.misis.gamification.kafka.consumer;
//
//import jakarta.validation.ValidationException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import ru.misis.gamification.events.domain.GamificationEvent;
//import ru.misis.gamification.kafka.service.DeadLetterQueueService;
//import ru.misis.gamification.model.entity.RuleEntity;
//import ru.misis.gamification.service.PointsService;
//import ru.misis.gamification.service.RuleEngineService;
//import ru.misis.gamification.util.Validator;
//
//import java.util.List;
//
///**
// * Основной консьюмер для обработки событий геймификации из Kafka.
// *
// * <p>Потребляет события из топика {@code gamification.events}, которые поступают от различных
// * источников (LMS, внешние системы, UI). Каждое событие представляет собой действие пользователя,
// * за которое могут быть начислены очки согласно бизнес-правилам.</p>
// *
// * <p>Процесс обработки события:
// * <ol>
// *   <li>Валидация обязательных полей события</li>
// *   <li>Поиск применимых правил начисления через {@link RuleEngineService}</li>
// *   <li>Применение каждого правила через {@link PointsService}</li>
// *   <li>Обработка ошибок и отправка проблемных событий в DLQ</li>
// * </ol>
// * </p>
// *
// * <p>Особенности:
// * <ul>
// *   <li>Поддерживает конкурентную обработку через Spring Kafka Listener Container</li>
// *   <li>Обеспечивает идемпотентность через проверку дубликатов событий</li>
// *   <li>Имеет механизм отказоустойчивости через DLQ</li>
// *   <li>Логирует все этапы обработки для аудита и отладки</li>
// * </ul>
// * </p>
// */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class PointsEventConsumer {
//
//    /**
//     * Сервис управления очками
//     */
//    private final PointsService pointsService;
//
//    /**
//     * Движок правил
//     */
//    private final RuleEngineService ruleEngine;
//    private final DeadLetterQueueService dlqService;
//    private final Validator validator;
//
//    /**
//     * Обрабатывает событие геймификации, полученное из Kafka.
//     *
//     * <p>Метод вызывается автоматически Spring Kafka при поступлении нового сообщения
//     * в топик {@code gamification.events}.</p>
//     *
//     * @param event событие геймификации, содержащее информацию о действии пользователя.
//     *             Должно содержать:
//     *             <ul>
//     *             <li>eventId - уникальный идентификатор события (UUID)</li>
//     *             <li>userId - идентификатор пользователя</li>
//     *             <li>type - тип события (например, TASK_COMPLETED, TEST_PASSED)</li>
//     *             <li>payload - дополнительные данные события</li>
//     *             <li>occurredAt - время возникновения события</li>
//     *             </ul>
//     *
//     * @throws ValidationException если событие не проходит базовую валидацию
//     * @throws KafkaConsumingException если произошла ошибка при обработке события
//     *
//     * @see GamificationEvent
//     * @see RuleEngineService#findApplicableRules(GamificationEvent)
//     * @see PointsService#applyRule(GamificationEvent, RuleEntity)
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.events}",
//            groupId = "${kafka.consumer.group-id}",
//            concurrency = "${kafka.consumer.concurrency:3}"
//    )
//    public void handleEvent(GamificationEvent event) {
//        try {
//            log.info("Processing event: {} for user: {}", event.getType(), event.getUserId());
//
//            // 1. Валидация события
//            validateEvent(event);
//
//            // 2. Поиск применимых правил
//            List<RuleEntity> applicableRules = ruleEngine.findApplicableRules(event);
//
//            // 3. Применение каждого правила
//            for (RuleEntity rule : applicableRules) {
//                pointsService.applyRule(event, rule);
//            }
//
//            log.info("Successfully processed event: {}", event.getEventId());
//
//        } catch (ValidationException e) {
//            log.warn("Event validation failed: {}", e.getMessage());
//            // Игнорируем некорректные события, но логируем
//
//        } catch (Exception e) {
//            log.error("Failed to process event: {}", event.getEventId(), e);
//            // Отправляем в DLQ для последующего анализа
//            dlqService.sendToDlq(event, e);
//
//            // Пробрасываем исключение для retry механизма Kafka
//            throw new KafkaConsumingException("Failed to process event", e);
//        }
//    }
//
//    /**
//     * Выполняет базовую валидацию события геймификации.
//     *
//     * @param event событие для валидации
//     * @throws ValidationException если событие не содержит обязательных полей
//     */
//    private void validateEvent(GamificationEvent event) {
//        if (event.getUserId() == null || event.getUserId().isBlank()) {
//            throw new ValidationException("User ID is required");
//        }
//        if (event.getType() == null) {
//            throw new ValidationException("Event type is required");
//        }
//        if (event.getEventId() == null) {
//            throw new ValidationException("Event ID is required");
//        }
//    }
//}
