package ru.misis.gamification.events.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import ru.misis.gamification.events.constants.EventConstants;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Событие прохождения теста пользователем
 *
 * <p>
 * Генерируется когда пользователь завершает прохождение теста в системе.
 * Содержит информацию о пройденном тесте и результате в процентах.
 * </p>
 *
 * @param eventId    Уникальный идентификатор события
 * @param userId     Идентификатор пользователя
 * @param occurredAt Время возникновения события
 * @param testId     Идентификатор пройденного теста
 * @param percentage Процент правильных ответов (0-100)
 *
 * @see GamificationEvent
 * @see EventConstants#TEST_PASSED
 */
public record TestPassedEvent(
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
        @JsonProperty("testId")
        @NotBlank(message = "testId не может быть пустым")
        @Size(min = 1, max = 50, message = "testId должен быть от 1 до 50 символов")
        String testId,

        @JsonProperty("percentage")
        @DecimalMin(value = "0.0", message = "percentage не может быть меньше 0")
        @DecimalMax(value = "100.0", message = "percentage не может быть больше 100")
        double percentage
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#TEST_PASSED}
     */
    @Override
    public String type() {
        return EventConstants.TEST_PASSED;
    }
}
