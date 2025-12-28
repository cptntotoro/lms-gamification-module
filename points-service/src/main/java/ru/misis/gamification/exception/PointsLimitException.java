package ru.misis.gamification.exception;

/**
 * Исключение, выбрасываемое при превышении лимитов начисления очков.
 * <p>
 * Лимиты могут быть:
 * <ul>
 *     <li>Дневной лимит начисления по правилу</li>
 *     <li>Общий лимит начисления по правилу</li>
 *     <li>Лимит на пользователя в системе</li>
 * </ul>
 * </p>
 **/
public class PointsLimitException extends RuntimeException {

    /**
     * Создает исключение с описанием лимита.
     *
     * @param message описание превышенного лимита
     */
    public PointsLimitException(String message) {
        super(message);
    }

    /**
     * Создает исключение для правила с превышением дневного лимита.
     *
     * @param ruleId идентификатор правила
     * @param dailyEarned уже начислено за день
     * @param limit дневной лимит
     */
    public PointsLimitException(String ruleId, long dailyEarned, long limit) {
        super(String.format("Превышен дневной лимит правила '%s'. Начислено: %d, Лимит: %d",
                ruleId, dailyEarned, limit));
    }
}