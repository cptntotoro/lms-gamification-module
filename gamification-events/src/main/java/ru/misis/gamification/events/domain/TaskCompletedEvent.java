package ru.misis.gamification.events.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import ru.misis.gamification.events.constants.EventConstants;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Событие завершения задачи пользователем
 *
 * <p>
 * Генерируется когда пользователь успешно завершает задание в системе LMS.
 * Содержит информацию о выполненной задаче и набранных баллах.
 * </p>
 *
 * @param eventId    Уникальный идентификатор события
 * @param userId     Идентификатор пользователя
 * @param occurredAt Время возникновения события
 * @param taskId     Идентификатор выполненной задачи
 * @param score      Количество набранных баллов
 *
 * @see GamificationEvent
 * @see EventConstants#TASK_COMPLETED
 */
public record TaskCompletedEvent(
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
        @JsonProperty("taskId")
        @NotBlank(message = "taskId не может быть пустым")
        @Size(min = 1, max = 50, message = "taskId должен быть от 1 до 50 символов")
        String taskId,

        @JsonProperty("score")
        @Min(value = EventConstants.MIN_SCORE, message = "score не может быть меньше {value}")
        @Max(value = EventConstants.MAX_SCORE, message = "score не может быть больше {value}")
        int score
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#TASK_COMPLETED}
     */
    @Override
    public String type() {
        return EventConstants.TASK_COMPLETED;
    }
}
