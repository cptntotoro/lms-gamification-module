package ru.misis.gamification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, возникающее при ошибке потребления сообщений из Kafka.
 *
 * <p>Используется для обозначения ошибок, связанных с обработкой событий из очереди Kafka.
 * Отличается от других бизнес-исключений тем, что предназначено для управления
 * механизмами retry в Spring Kafka.</p>
 *
 * <p>Особенности:
 * <ul>
 *   <li>Помечено как {@link ResponseStatus} 500 INTERNAL_SERVER_ERROR</li>
 *   <li>Автоматически перехватывается Spring Retry для повторных попыток</li>
 *   <li>Содержит информацию об оригинальном событии и причине ошибки</li>
 *   <li>Используется триггером для отправки событий в DLQ</li>
 * </ul>
 * </p>
 *
 * <p>Пример использования:
 * <pre>
 * try {
 *     processEvent(event);
 * } catch (BusinessException e) {
 *     // Бизнес-ошибки не требуют retry
 *     throw e;
 * } catch (Exception e) {
 *     // Технические ошибки требуют retry
 *     throw new KafkaConsumingException("Failed to process event", e);
 * }
 * </pre>
 * </p>
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class KafkaConsumingException extends RuntimeException {

    /**
     * Создает новое исключение с указанным сообщением.
     *
     * @param message описание ошибки потребления
     */
    public KafkaConsumingException(String message) {
        super(message);
    }

    /**
     * Создает новое исключение с указанным сообщением и причиной.
     *
     * @param message описание ошибки потребления
     * @param cause оригинальное исключение, вызвавшее ошибку
     */
    public KafkaConsumingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Создает новое исключение с указанным событием и причиной.
     *
     * @param event событие Kafka, которое не удалось обработать
     * @param cause оригинальное исключение, вызвавшее ошибку
     */
    public KafkaConsumingException(Object event, Throwable cause) {
        super(String.format("Failed to process Kafka event: %s", event), cause);
    }
}