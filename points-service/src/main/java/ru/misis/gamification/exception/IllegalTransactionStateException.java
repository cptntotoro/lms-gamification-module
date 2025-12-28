package ru.misis.gamification.exception;

class IllegalTransactionStateException extends RuntimeException {
    public IllegalTransactionStateException(String message) {
        super(message);
    }
}
