package ru.misis.gamification.events.domain.internal;

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
 * Событие изменения баланса очков пользователя
 *
 * <p>
 * Генерируется сервисом PointsService при успешном начислении или списании очков.
 * Содержит информацию об изменении баланса, новом уровне пользователя
 * и транзакции, которая привела к изменению.
 * </p>
 *
 * @param eventId         Уникальный идентификатор события
 * @param userId          Идентификатор пользователя
 * @param occurredAt      Время возникновения события
 * @param pointsDelta     Изменение количества очков (положительное - начисление, отрицательное - списание)
 * @param newBalance      Новый баланс на активном счете пользователя
 * @param newTotalBalance Новый общий баланс (включая исторические начисления)
 * @param newLevel        Новый уровень пользователя после изменения
 * @param transactionId   Идентификатор транзакции
 * @param ruleId          Идентификатор правила геймификации, которое вызвало изменение
 * @see GamificationEvent
 * @see EventConstants#POINTS_CHANGED
 */
public record PointsChangedEvent(
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
        @JsonProperty("pointsDelta")
        @NotNull(message = "pointsDelta не может быть null")
        Long pointsDelta,

        @JsonProperty("newBalance")
        @NotNull(message = "newBalance не может быть null")
        @Min(value = 0, message = "newBalance не может быть отрицательным")
        Long newBalance,

        @JsonProperty("newTotalBalance")
        @NotNull(message = "newTotalBalance не может быть null")
        @Min(value = 0, message = "newTotalBalance не может быть отрицательным")
        Long newTotalBalance,

        @JsonProperty("newLevel")
        @NotNull(message = "newLevel не может быть null")
        @Min(value = 1, message = "newLevel не может быть меньше 1")
        Integer newLevel,

        @JsonProperty("transactionId")
        @NotNull(message = "transactionId не может быть null")
        UUID transactionId,

        @JsonProperty("ruleId")
        @NotBlank(message = "ruleId не может быть пустым")
        @Size(min = 1, max = 50, message = "ruleId должен быть от 1 до 50 символов")
        String ruleId
) implements GamificationEvent {

    /**
     * {@inheritDoc}
     *
     * @return {@link EventConstants#POINTS_CHANGED}
     */
    @Override
    public String type() {
        return EventConstants.POINTS_CHANGED;
    }

    /**
     * Фабричный метод для создания события начисления очков
     *
     * @param eventId         Идентификатор исходного события
     * @param userId          Идентификатор пользователя
     * @param pointsDelta     Количество начисляемых очков
     * @param newBalance      Новый баланс на активном счете
     * @param newTotalBalance Новый общий баланс
     * @param newLevel        Новый уровень пользователя
     * @param transactionId   Идентификатор транзакции
     * @param ruleId          Идентификатор правила геймификации
     * @return Новое событие изменения баланса
     */
    public static PointsChangedEvent awardPoints(
            UUID eventId,
            String userId,
            long pointsDelta,
            long newBalance,
            long newTotalBalance,
            int newLevel,
            UUID transactionId,
            String ruleId
    ) {
        return new PointsChangedEvent(
                eventId,
                userId,
                LocalDateTime.now(),
                pointsDelta,
                newBalance,
                newTotalBalance,
                newLevel,
                transactionId,
                ruleId
        );
    }

    /**
     * Фабричный метод для создания события списания очков
     *
     * @param userId          Идентификатор пользователя
     * @param pointsDelta     Количество начисляемых очков
     * @param newBalance      Новый баланс на активном счете
     * @param newTotalBalance Новый общий баланс
     * @param newLevel        Новый уровень пользователя
     * @param transactionId   Идентификатор транзакции
     * @param ruleId          Идентификатор правила геймификации
     * @return Новое событие изменения баланса
     */
    public static PointsChangedEvent deductPoints(String userId, long pointsDelta,
                                                  long newBalance, long newTotalBalance,
                                                  int newLevel, UUID transactionId, String ruleId) {
        return new PointsChangedEvent(
                UUID.randomUUID(),
                userId,
                LocalDateTime.now(),
                -pointsDelta,
                newBalance,
                newTotalBalance,
                newLevel,
                transactionId,
                ruleId
        );
    }

    /**
     * Проверяет, было ли это начислением (а не списанием)
     *
     * @return true если pointsDelta &gt; 0, иначе false
     */
    public boolean isAward() {
        return pointsDelta > 0;
    }

    /**
     * Проверяет, было ли это списанием
     *
     * @return true если pointsDelta &lt; 0, иначе false
     */
    public boolean isDeduction() {
        return pointsDelta < 0;
    }

    /**
     * Возвращает абсолютное значение изменения очков
     *
     * @return положительное число, представляющее абсолютное значение изменения
     */
    public long getAbsoluteDelta() {
        return Math.abs(pointsDelta);
    }
}
