package ru.misis.gamification.exception;

/**
 * Исключение, выбрасываемое когда недостаточно очков для выполнения операции.
 * <p>
 * Используется при списании очков, переводе и других расходных операциях.
 * </p>
 **/
public class InsufficientPointsException extends RuntimeException {

    /**
     * Создает исключение с информацией о балансе.
     *
     * @param available Доступное количество очков
     * @param required  Требуемое количество очков
     */
    public InsufficientPointsException(long available, long required) {
        super(String.format("Недостаточно очков. Доступно: %d, Требуется: %d", available, required));
    }

    /**
     * Создает исключение с пользовательским сообщением.
     *
     * @param message Описание ошибки
     */
    public InsufficientPointsException(String message) {
        super(message);
    }
}
