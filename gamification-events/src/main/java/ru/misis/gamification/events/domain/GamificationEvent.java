package ru.misis.gamification.events.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ru.misis.gamification.events.constants.EventConstants;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Базовый интерфейс для всех событий геймификации
 *
 * <p>
 * Определяет общий контракт для всех событий в системе геймификации.
 * </p>
 *
 * @see TaskCompletedEvent
 * @see TestPassedEvent
 * @see CourseEnrolledEvent
 * @see ForumPostCreatedEvent
 * @see AssignmentSubmittedEvent
 * @see EventConstants
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TaskCompletedEvent.class, name = EventConstants.TASK_COMPLETED),
        @JsonSubTypes.Type(value = TestPassedEvent.class, name = EventConstants.TEST_PASSED),
        @JsonSubTypes.Type(value = CourseEnrolledEvent.class, name = EventConstants.COURSE_ENROLLED),
        @JsonSubTypes.Type(value = ForumPostCreatedEvent.class, name = EventConstants.FORUM_POST_CREATED),
        @JsonSubTypes.Type(value = AssignmentSubmittedEvent.class, name = EventConstants.ASSIGNMENT_SUBMITTED)
})
@JsonSerialize
@JsonDeserialize
public sealed interface GamificationEvent
        permits TaskCompletedEvent, TestPassedEvent,
        CourseEnrolledEvent, ForumPostCreatedEvent,
        AssignmentSubmittedEvent {

    /**
     * Возвращает уникальный идентификатор события (UUID)
     *
     * @return UUID события
     */
    @JsonProperty("eventId")
    UUID eventId();

    /**
     * Возвращает тип события.
     *
     * <p>
     * Определяет, какие правила начисления очков должны быть применены.
     * </p>
     *
     * <p>
     * Значение должно соответствовать одной из констант в {@link EventConstants}.
     * </p>
     *
     * @return тип события, не может быть {@code null} или пустой строкой
     * @see EventConstants
     */
    @JsonProperty("type")
    String type();

    /**
     * Возвращает идентификатор пользователя, совершившего действие
     *
     * <p>
     * Соответствует userId в системе аутентификации LMS.
     * Используется для идентификации пользователя при начислении очков
     * </p>
     *
     * @return идентификатор пользователя, не может быть {@code null}
     */
    @JsonProperty("userId")
    String userId();

    /**
     * Возвращает дату и время возникновения события в системе-источнике
     * <p>
     * Время фиксируется в момент создания события в системе-источнике.
     * Используется для временных ограничений в правилах начисления.
     * </p>
     *
     * @return метка времени события, не может быть {@code null}
     */
    @JsonProperty("occurredAt")
    LocalDateTime occurredAt();

    /**
     * Создает событие завершения задачи
     *
     * <p>
     * Фабричный метод для удобного создания событий {@link TaskCompletedEvent}.
     * Автоматически генерирует {@code eventId} и {@code occurredAt}.
     * </p>
     *
     * @param userId Идентификатор пользователя, не может быть {@code null} или пустым
     * @param taskId Идентификатор задачи, не может быть {@code null} или пустым
     * @param score  Количество баллов, должно быть в диапазоне 0-100
     * @return Созданное событие завершения задачи
     * @throws IllegalArgumentException Если параметры невалидны
     */
    static TaskCompletedEvent taskCompleted(String userId, String taskId, int score) {
        return new TaskCompletedEvent(
                UUID.randomUUID(),
                userId,
                LocalDateTime.now(),
                taskId,
                score
        );
    }

    /**
     * Создает событие прохождения теста
     *
     * <p>
     * Фабричный метод для удобного создания событий {@link TestPassedEvent}.
     * Автоматически генерирует {@code eventId} и {@code occurredAt}.
     * </p>
     *
     * @param userId     Идентификатор пользователя, не может быть {@code null} или пустым
     * @param testId     Идентификатор теста, не может быть {@code null} или пустым
     * @param percentage Процент прохождения, должен быть в диапазоне 0-100
     * @return Созданное событие прохождения теста
     * @throws IllegalArgumentException Если параметры невалидны
     */
    static TestPassedEvent testPassed(String userId, String testId, double percentage) {
        return new TestPassedEvent(
                UUID.randomUUID(),
                userId,
                LocalDateTime.now(),
                testId,
                percentage
        );
    }

    /**
     * Проверяет, является ли событие заданного типа
     *
     * <p>
     * Удобный метод для проверки типа события без необходимости использования
     * оператора {@code instanceof} или сравнения строк.
     * </p>
     *
     * @param eventType Тип события для проверки, не может быть {@code null}
     * @return {@code true} Если тип события совпадает с переданным
     * @see EventConstants
     */
    default boolean isType(String eventType) {
        return type().equals(eventType);
    }
}