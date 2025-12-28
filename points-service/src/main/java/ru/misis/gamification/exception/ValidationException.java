package ru.misis.gamification.exception;

/**
 * Исключение, выбрасываемое при ошибках валидации данных.
 * <p>
 * Используется для валидации входящих событий, правил
 * и других объектов системы.
 * </p>
 **/
public class ValidationException extends RuntimeException {

    /**
     * Создает исключение с сообщением об ошибке валидации.
     *
     * @param message Описание ошибки валидации
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Создает исключение с сообщением и причиной.
     *
     * @param message Описание ошибки валидации
     * @param cause   Исходное исключение
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}