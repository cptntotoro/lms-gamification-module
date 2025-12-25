package ru.misis.gamification.events.domain.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.misis.gamification.events.constants.EventConstants;
import ru.misis.gamification.events.domain.GamificationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Событие получения достижения пользователем
 *
 * <p>
 * Генерируется системой геймификации при выполнении пользователем условий,
 * необходимых для получения определенного достижения (achievement).
 * </p>
 *
 * @param eventId           Уникальный идентификатор события
 * @param userId            Идентификатор пользователя
 * @param occurredAt        Время получения достижения
 * @param achievementId     Идентификатор достижения
 * @param achievementName   Название достижения для отображения пользователю
 * @param description       Подробное описание достижения и условий его получения
 * @param pointsReward      Количество очков, начисляемых за получение достижения
 * @param rarity            Редкость достижения (например, "COMMON", "RARE", "EPIC", "LEGENDARY")
 * @param iconUrl           URL иконки достижения для отображения в интерфейсе
 *
 * @see GamificationEvent
 * @see EventConstants#ACHIEVEMENT_UNLOCKED
 */
public record AchievementEvent(
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
        @JsonProperty("achievementId")
        @NotBlank(message = "achievementId не может быть пустым")
        @Size(min = 1, max = 50, message = "achievementId должен быть от 1 до 50 символов")
        String achievementId,

        @JsonProperty("achievementName")
        @NotBlank(message = "achievementName не может быть пустым")
        @Size(min = 1, max = 100, message = "achievementName должен быть от 1 до 100 символов")
        String achievementName,

        @JsonProperty("description")
        @NotBlank(message = "description не может быть пустым")
        @Size(min = 1, max = 500, message = "description должен быть от 1 до 500 символов")
        String description,

        @JsonProperty("pointsReward")
        @NotNull(message = "pointsReward не может быть null")
        @Min(value = 0, message = "pointsReward не может быть отрицательным")
        Long pointsReward,

        @JsonProperty("rarity")
        @Pattern(
                regexp = EventConstants.ACHIEVEMENT_RARITY_COMMON + "|" +
                        EventConstants.ACHIEVEMENT_RARITY_RARE + "|" +
                        EventConstants.ACHIEVEMENT_RARITY_EPIC + "|" +
                        EventConstants.ACHIEVEMENT_RARITY_LEGENDARY,
                message = "rarity должен быть одним из: " +
                        EventConstants.ACHIEVEMENT_RARITY_COMMON + ", " +
                        EventConstants.ACHIEVEMENT_RARITY_RARE + ", " +
                        EventConstants.ACHIEVEMENT_RARITY_EPIC + ", " +
                        EventConstants.ACHIEVEMENT_RARITY_LEGENDARY
        )
        String rarity,

        @JsonProperty("iconUrl")
        @Size(max = 500, message = "iconUrl должен быть не длиннее 500 символов")
        String iconUrl
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#ACHIEVEMENT_UNLOCKED}
     */
    @Override
    public String type() {
        return EventConstants.ACHIEVEMENT_UNLOCKED;
    }

    /**
     * Фабричный метод для создания события получения достижения
     *
     * @param userId          Идентификатор пользователя
     * @param achievementId   Идентификатор достижения
     * @param achievementName Название достижения
     * @param description     Описание достижения
     * @param pointsReward    Количество очков за достижение
     * @param rarity          Редкость достижения
     * @param iconUrl         URL иконки достижения (может быть {@code null})
     * @return Созданное событие получения достижения
     * @throws IllegalArgumentException если обязательные параметры некорректны
     */
    public static AchievementEvent create(
            String userId,
            String achievementId,
            String achievementName,
            String description,
            Long pointsReward,
            String rarity,
            String iconUrl
    ) {
        return new AchievementEvent(
                UUID.randomUUID(),
                userId,
                LocalDateTime.now(),
                achievementId,
                achievementName,
                description,
                pointsReward,
                rarity,
                iconUrl
        );
    }

    /**
     * Проверяет, является ли достижение редким или выше
     *
     * @return {@code true} Если редкость "RARE", "EPIC" или "LEGENDARY"
     */
    public boolean isRareOrAbove() {
        return switch (rarity.toUpperCase()) {
            case EventConstants.ACHIEVEMENT_RARITY_RARE,
                 EventConstants.ACHIEVEMENT_RARITY_EPIC,
                 EventConstants.ACHIEVEMENT_RARITY_LEGENDARY -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, является ли достижение эпическим или легендарным
     *
     * @return {@code true} Если редкость "EPIC" или "LEGENDARY"
     */
    public boolean isEpicOrLegendary() {
        return switch (rarity.toUpperCase()) {
            case EventConstants.ACHIEVEMENT_RARITY_EPIC,
                 EventConstants.ACHIEVEMENT_RARITY_LEGENDARY -> true;
            default -> false;
        };
    }

    /**
     * Возвращает краткое описание достижения для уведомлений
     *
     * @return Форматированная строка с информацией о достижении
     */
    public String getNotificationMessage() {
        return String.format(
                "Поздравляем! Вы получили достижение \"%s\" (%s). Начислено %d очков.",
                achievementName,
                rarity,
                pointsReward
        );
    }

    /**
     * Проверяет, начисляются ли очки за это достижение
     *
     * @return {@code true} если pointsReward больше 0
     */
    public boolean hasPointsReward() {
        return pointsReward > 0;
    }
}
