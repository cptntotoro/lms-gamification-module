package ru.misis.gamification.exception;

/**
 * Исключение при расчете.
 */
public class CalculationException extends RuntimeException {
    public CalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
