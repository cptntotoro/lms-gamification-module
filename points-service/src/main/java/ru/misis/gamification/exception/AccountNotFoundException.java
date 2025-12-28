package ru.misis.gamification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое когда аккаунт пользователя не найден.
 * <p>
 * Используется в операциях, требующих существования аккаунта:
 * получение баланса, списание очков и т.д.
 * </p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {

    /**
     * Создает исключение с указанием идентификатора пользователя.
     *
     * @param userId Идентификатор пользователя, аккаунт которого не найден
     */
    public AccountNotFoundException(String userId) {
        super(String.format("Аккаунт пользователя '%s' не найден", userId));
    }

    /**
     * Создает исключение с дополнительной информацией.
     *
     * @param userId  Идентификатор пользователя
     * @param message Дополнительное сообщение
     */
    public AccountNotFoundException(String userId, String message) {
        super(String.format("Аккаунт пользователя '%s' не найден: %s", userId, message));
    }
}
