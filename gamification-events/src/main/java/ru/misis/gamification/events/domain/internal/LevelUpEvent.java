package ru.misis.gamification.events.domain.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import ru.misis.gamification.events.constants.EventConstants;
import ru.misis.gamification.events.domain.GamificationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Событие повышения уровня пользователя
 *
 * <p>
 * Генерируется сервисом PointsService при повышении уровня пользователя.
 * Содержит информацию о предыдущем и новом уровне, общем количестве очков.
 * </p>
 *
 * @param eventId     Уникальный идентификатор события
 * @param userId      Идентификатор пользователя
 * @param occurredAt  Время возникновения события
 * @param oldLevel    Предыдущий уровень пользователя
 * @param newLevel    Новый уровень пользователя
 * @param totalPoints Общее количество очков пользователя
 *
 * @see GamificationEvent
 * @see EventConstants#POINTS_CHANGED
 **/
public record LevelUpEvent(
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
        @JsonProperty("oldLevel")
        @NotNull(message = "oldLevel не может быть null")
        @Min(value = 1, message = "oldLevel должен быть не меньше 1")
        Integer oldLevel,

        @JsonProperty("newLevel")
        @NotNull(message = "newLevel не может быть null")
        @Min(value = 1, message = "newLevel должен быть не меньше 1")
        Integer newLevel,

        @JsonProperty("totalPoints")
        @NotNull(message = "totalPoints не может быть null")
        @Min(value = 0, message = "totalPoints не может быть отрицательным")
        Long totalPoints
) implements GamificationEvent {

    @JsonCreator
    public LevelUpEvent {
        if (newLevel <= oldLevel) {
            throw new IllegalArgumentException("Новый уровень должен быть выше предыдущего");
        }
    }

    /**
     * Фабричный метод для создания события повышения уровня
     *
     * @param userId      Идентификатор пользователя
     * @param oldLevel    Предыдущий уровень
     * @param newLevel    Новый уровень
     * @param totalPoints Общее количество очков
     * @return Созданное событие повышения уровня
     */
    public static LevelUpEvent create(String userId, Integer oldLevel,
                                      Integer newLevel, Long totalPoints) {
        return new LevelUpEvent(
                UUID.randomUUID(),
                userId,
                LocalDateTime.now(),
                oldLevel,
                newLevel,
                totalPoints
        );
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#LEVEL_UP}
     */
    @Override
    public String type() {
        return EventConstants.LEVEL_UP;
    }

    /**
     * Возвращает разницу между уровнями
     */
    public int getLevelDifference() {
        return newLevel - oldLevel;
    }
}
