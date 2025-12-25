package ru.misis.gamification.events.domain.extenal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import ru.misis.gamification.events.constants.EventConstants;
import ru.misis.gamification.events.domain.GamificationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Событие записи пользователя на курс
 *
 * <p>
 * Генерируется когда пользователь успешно записывается на курс в системе.
 * Содержит информацию о выбранном курсе и времени записи.
 * </p>
 *
 * @param eventId    Уникальный идентификатор события
 * @param userId     Идентификатор пользователя
 * @param occurredAt Время возникновения события
 * @param courseId   Идентификатор курса
 * @see GamificationEvent
 * @see EventConstants#COURSE_ENROLLED
 */
public record CourseEnrolledEvent(
        @JsonProperty("eventId")
        @NotNull(message = "eventId не может быть null")
        UUID eventId,

        @JsonProperty("userId")
        @NotBlank(message = "userId не может быть пустым")
        @Size(min = 1, max = 100, message = "userId должен быть от 1 до 100 символов")
        String userId,

        @JsonProperty("occurredAt")
        @NotNull(message = "occurredAt не может быть null")
        @PastOrPresent(message = "occurredAt не может быть в будущем")
        LocalDateTime occurredAt,

        // Специфичные поля
        @JsonProperty("courseId")
        @NotBlank(message = "courseId не может быть пустым")
        @Size(min = 1, max = 50, message = "courseId должен быть от 1 до 50 символов")
        String courseId
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#COURSE_ENROLLED}
     */
    @Override
    public String type() {
        return EventConstants.COURSE_ENROLLED;
    }
}