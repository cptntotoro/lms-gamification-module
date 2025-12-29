package ru.misis.gamification.service.transaction;

import ru.misis.gamification.exception.DuplicateEventException;
import ru.misis.gamification.exception.PointsLimitException;
import ru.misis.gamification.model.entity.PointsTransaction;
import ru.misis.gamification.model.entity.RuleEntity;
import ru.misis.gamification.model.enums.TransactionStatus;
import ru.misis.gamification.model.enums.TransactionType;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления транзакциями системы геймификации.
 * <p>
 * Отвечает за создание, валидацию и проверку транзакций начисления и списания очков.
 * Обеспечивает идемпотентность операций через проверку дублирования событий.
 * </p>
 */
public interface TransactionService {

    /**
     * Создает транзакцию начисления очков по правилу.
     * <p>
     * Генерирует новую транзакцию с типом {@link TransactionType#EARN} и статусом
     * {@link TransactionStatus#COMPLETED}. Устанавливает срок действия очков.
     * </p>
     *
     * @param userId      Идентификатор пользователя
     * @param rule        Правило начисления очков
     * @param eventId     Идентификатор события
     * @param points      Количество начисляемых очков
     * @param description Описание операции
     * @return Созданная транзакция начисления
     * @throws IllegalArgumentException если параметры некорректны
     */
    PointsTransaction createAwardTransaction(
            String userId,
            RuleEntity rule,
            String eventId,
            Long points,
            String description);

    /**
     * Создает транзакцию списания очков.
     * <p>
     * Генерирует новую транзакцию с типом {@link TransactionType#SPEND} и статусом
     * {@link TransactionStatus#COMPLETED}
     * </p>
     *
     * @param userId  Идентификатор пользователя
     * @param eventId Идентификатор события
     * @param amount  Количество списываемых очков
     * @param purpose Назначение списания
     * @return Созданная транзакция списания
     * @throws IllegalArgumentException если параметры некорректны
     */
    PointsTransaction createSpendTransaction(
            String userId,
            String eventId,
            Long amount,
            String purpose);

    /**
     * Проверяет дублирование события по идентификатору.
     * <p>
     * Если транзакция с указанным eventId уже существует, выбрасывает исключение
     * {@link DuplicateEventException} для обеспечения идемпотентности операций.
     * </p>
     *
     * @param eventId Уникальный идентификатор события
     * @throws DuplicateEventException если событие уже обработано
     */
    void checkDuplicateEvent(String eventId);

    /**
     * Проверяет лимиты правила для пользователя.
     * <p>
     * Проверяет дневной и общий лимиты правила, если они установлены.
     * Выбрасывает исключение {@link PointsLimitException} при превышении лимитов.
     * </p>
     *
     * @param userId Идентификатор пользователя
     * @param rule   Правило с лимитами
     * @throws PointsLimitException если превышен дневной или общий лимит
     */
    void checkRuleLimits(String userId, RuleEntity rule);

    /**
     * Получает историю транзакций пользователя.
     *
     * @param userId Идентификатор пользователя
     * @param limit  Максимальное количество возвращаемых транзакций
     * @return Список транзакций пользователя
     * @throws IllegalArgumentException если userId является null или пустой строкой
     */
    List<PointsTransaction> getUserTransactions(String userId, int limit);

    /**
     * Рассчитывает общую сумму начисленных очков пользователя по правилу.
     *
     * @param userId Идентификатор пользователя
     * @param ruleId Идентификатор правила
     * @return общее количество очков, начисленных по правилу
     */
    Long calculateTotalPointsByRule(String userId, String ruleId);

    /**
     * Рассчитывает сумму очков, начисленных по правилу за последние 24 часа.
     *
     * @param userId идентификатор пользователя
     * @param ruleId идентификатор правила
     * @return количество очков, начисленных за последние 24 часа
     */
    Long calculateDailyPointsByRule(String userId, String ruleId);

    /**
     * Находит транзакцию по идентификатору события.
     *
     * @param eventId Идентификатор события
     * @return Транзакция
     */
    PointsTransaction findByEventId(String eventId);

    /**
     * Отменяет транзакцию, меняя ее статус на {@link TransactionStatus#ROLLED_BACK}.
     * <p>
     * Используется для отката ошибочных операций. Не удаляет транзакцию из базы данных.
     * </p>
     *
     * @param transactionId Идентификатор транзакции для отмены
     * @return Отмененная транзакция
     * @throws IllegalArgumentException если транзакция не найдена
     */
    PointsTransaction cancelTransaction(UUID transactionId);

    /**
     * Получает статистику транзакций пользователя.
     *
     * @param userId Идентификатор пользователя
     * @return Статистика транзакций
     */
    TransactionServiceImpl.TransactionStats getTransactionStats(String userId);
}
