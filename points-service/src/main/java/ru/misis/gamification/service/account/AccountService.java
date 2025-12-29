package ru.misis.gamification.service.account;

import ru.misis.gamification.exception.AccountNotFoundException;
import ru.misis.gamification.model.entity.PointsAccount;
import ru.misis.gamification.util.PointsLevelCalculator;

/**
 * Сервис для управления аккаунтами пользователей системы геймификации.
 * <p>
 * Отвечает за создание, получение и обновление учетных записей пользователей,
 * включая управление балансами очков и уровнями.
 * </p>
 */
public interface AccountService {

    /**
     * Находит существующий аккаунт пользователя или создает новый.
     * <p>
     * Если аккаунт с указанным идентификатором пользователя уже существует,
     * возвращает его. В противном случае создает новый аккаунт с нулевым балансом
     * и начальным уровнем 1.
     * </p>
     *
     * @param userId Идентификатор пользователя
     * @return Cуществующий или созданный аккаунт
     * @throws IllegalArgumentException если userId {@code null} или пустой
     */
    PointsAccount getOrCreateAccount(String userId);

    /**
     * Получает текущий баланс доступных очков пользователя.
     *
     * @param userId Идентификатор пользователя
     * @return Баланс доступных очков
     * @throws AccountNotFoundException если аккаунт не найден
     */
    Long getAvailableBalance(String userId);

    /**
     * Получает аккаунт пользователя по идентификатору.
     *
     * @param userId Идентификатор пользователя
     * @return Аккаунт пользователя
     * @throws AccountNotFoundException если аккаунт не найден
     * @throws IllegalArgumentException если userId является null или пустой строкой
     */
    PointsAccount getAccount(String userId);

    /**
     * Получает общее количество накопленных очков пользователя.
     *
     * @param userId Идентификатор пользователя
     * @return Общее количество очков
     * @throws AccountNotFoundException если аккаунт не найден
     */
    Long getTotalPoints(String userId);

    /**
     * Получает текущий уровень пользователя.
     *
     * @param userId Идентификатор пользователя
     * @return Текущий уровень пользователя
     * @throws AccountNotFoundException если аккаунт не найден
     */
    Integer getCurrentLevel(String userId);

    /**
     * Обновляет баланс аккаунта на указанное количество очков.
     * <p>
     * Обновляет как доступные очки, так и общее количество накопленных очков.
     * Автоматически проверяет и обновляет уровень пользователя при необходимости.
     * </p>
     *
     * @param account     Аккаунт пользователя
     * @param pointsDelta Изменение баланса (положительное для начисления, отрицательное для списания)
     * @return Обновленный аккаунт
     * @throws IllegalArgumentException если account является null
     */
    PointsAccount updateBalance(PointsAccount account, Long pointsDelta);

    /**
     * Проверяет и обновляет уровень пользователя на основе общего количества очков.
     * <p>
     * Использует {@link PointsLevelCalculator} для определения нового уровня.
     * Если уровень изменился, логирует это событие.
     * </p>
     *
     * @param account аккаунт пользователя
     */
    void updateUserLevel(PointsAccount account);

    /**
     * Проверяет, достаточно ли очков на балансе пользователя для списания.
     *
     * @param userId идентификатор пользователя
     * @param amount количество очков для списания
     * @return true если достаточно средств, false в противном случае
     * @throws AccountNotFoundException если аккаунт не найден
     */
    boolean hasSufficientPoints(String userId, Long amount);

    /**
     * Удаляет аккаунт пользователя.
     * <p>
     * Используется преимущественно в тестовых целях или для администрирования.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @throws AccountNotFoundException если аккаунт не найден
     */
    void deleteAccount(String userId);
}
