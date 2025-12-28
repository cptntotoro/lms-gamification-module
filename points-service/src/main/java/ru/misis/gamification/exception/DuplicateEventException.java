package ru.misis.gamification.exception;

/**
 * Исключение, выбрасываемое при попытке повторной обработки события.
 * <p>
 * Гарантирует идемпотентность операций: одно событие обрабатывается
 * только один раз, даже если было отправлено несколько раз.
 * </p>
 **/
public class DuplicateEventException extends RuntimeException {

    /**
     * Создает исключение с указанием идентификатора события.
     *
     * @param eventId Идентификатор дублирующегося события
     */
    public DuplicateEventException(String eventId) {
        super(String.format("Событие уже обработано: %s", eventId));
    }
}
