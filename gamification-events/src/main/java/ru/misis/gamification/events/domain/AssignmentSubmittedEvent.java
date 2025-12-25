package ru.misis.gamification.events.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import ru.misis.gamification.events.constants.EventConstants;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Событие сдачи задания пользователем
 *
 * <p>
 * Генерируется когда пользователь отправляет выполненное задание в системе.
 * Содержит информацию о сданном задании и соблюдении сроков сдачи.
 * </p>
 *
 * @param eventId      Уникальный идентификатор события
 * @param userId       Идентификатор пользователя
 * @param occurredAt   Время возникновения события
 * @param assignmentId Идентификатор сданного задания
 * @param onTime       Флаг своевременной сдачи
 *
 * @see GamificationEvent
 * @see EventConstants#ASSIGNMENT_SUBMITTED
 */
public record AssignmentSubmittedEvent(
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
        @JsonProperty("assignmentId")
        @NotBlank(message = "assignmentId не может быть пустым")
        @Size(min = 1, max = 50, message = "assignmentId должен быть от 1 до 50 символов")
        String assignmentId,

        @JsonProperty("onTime")
        @NotNull(message = "onTime не может быть null")
        boolean onTime
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#ASSIGNMENT_SUBMITTED}
     */
    @Override
    public String type() {
        return EventConstants.ASSIGNMENT_SUBMITTED;
    }
}