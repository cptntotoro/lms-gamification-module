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
 * Событие создания поста на форуме пользователем
 *
 * <p>
 * Генерируется когда пользователь создает новый пост в разделе форума.
 * Содержит информацию о созданном посте и связанной теме обсуждения.
 * </p>
 *
 * @param eventId    Уникальный идентификатор события
 * @param userId     Идентификатор пользователя
 * @param occurredAt Время возникновения события
 * @param postId     Идентификатор созданного поста
 * @param topicId    Идентификатор темы форума
 *
 * @see GamificationEvent
 * @see EventConstants#FORUM_POST_CREATED
 */
public record ForumPostCreatedEvent(
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
        @JsonProperty("postId")
        @NotBlank(message = "postId не может быть пустым")
        @Size(min = 1, max = 50, message = "postId должен быть от 1 до 50 символов")
        String postId,

        @JsonProperty("topicId")
        @NotBlank(message = "topicId не может быть пустым")
        @Size(min = 1, max = 50, message = "topicId должен быть от 1 до 50 символов")
        String topicId
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#FORUM_POST_CREATED}
     */
    @Override
    public String type() {
        return EventConstants.FORUM_POST_CREATED;
    }
}