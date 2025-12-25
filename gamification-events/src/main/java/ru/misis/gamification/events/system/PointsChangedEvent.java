package ru.misis.gamification.events.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

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
 * @param userId          Идентификатор пользователя
 * @param pointsDelta     Изменение количества очков (положительное - начисление, отрицательное - списание)
 * @param newBalance      Новый баланс на активном счете пользователя
 * @param newTotalBalance Новый общий баланс (включая исторические начисления)
 * @param newLevel        Новый уровень пользователя после изменения
 * @param transactionId   Идентификатор транзакции
 * @param eventId         Уникальный идентификатор события
 * @param ruleId          Идентификатор правила геймификации, которое вызвало изменение
 * @param occurredAt      Время возникновения события
 */
public record PointsChangedEvent(
        @JsonProperty("userId")
        @NotBlank(message = "userId не может быть пустым")
        @Size(min = 1, max = 100, message = "userId должен быть от 1 до 100 символов")
        String userId,

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

        @JsonProperty("eventId")
        @NotNull(message = "eventId не может быть null")
        UUID eventId,

        @JsonProperty("ruleId")
        @NotBlank(message = "ruleId не может быть пустым")
        @Size(min = 1, max = 50, message = "ruleId должен быть от 1 до 50 символов")
        String ruleId,

        @JsonProperty("occurredAt")
        @NotNull(message = "occurredAt не может быть null")
        @PastOrPresent(message = "occurredAt не может быть в будущем")
        LocalDateTime occurredAt
) {

    /**
     * Фабричный метод для создания события начисления очков
     *
     * @param userId          Идентификатор пользователя
     * @param points          Количество начисляемых очков
     * @param newBalance      Новый баланс на активном счете
     * @param newTotalBalance Новый общий баланс
     * @param newLevel        Новый уровень пользователя
     * @param transactionId   Идентификатор транзакции
     * @param sourceEventId   Идентификатор исходного события
     * @param ruleId          Идентификатор правила геймификации
     * @return Новое событие изменения баланса
     */
    public static PointsChangedEvent awardPoints(
            String userId,
            long points,
            long newBalance,
            long newTotalBalance,
            int newLevel,
            UUID transactionId,
            UUID sourceEventId,
            String ruleId
    ) {
        return new PointsChangedEvent(
                userId,
                points,
                newBalance,
                newTotalBalance,
                newLevel,
                transactionId,
                sourceEventId,
                ruleId,
                LocalDateTime.now()
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
}
