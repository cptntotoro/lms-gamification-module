package ru.misis.gamification.service.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misis.gamification.exception.AccountNotFoundException;
import ru.misis.gamification.model.entity.PointsAccount;
import ru.misis.gamification.repository.AccountRepository;
import ru.misis.gamification.util.PointsLevelCalculator;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    /**
     * Репозиторий для работы с балансами пользователей
     */
    private final AccountRepository accountRepository;

    /**
     * алькулятор для расчета уровней пользователя на основе накопленных очков
     */
    private final PointsLevelCalculator levelCalculator;

    @Transactional(readOnly = true)
    @Override
    public PointsAccount getOrCreateAccount(String userId) {
        PointsAccount account;

        try {
            account = getAccount(userId);
        } catch (AccountNotFoundException e) {
            account = createNewAccount(userId);
            log.info("Создан новый аккаунт для пользователя: {}", userId);

        }
        return account;
    }

    @Transactional(readOnly = true)
    @Override
    public Long getAvailableBalance(String userId) {
        PointsAccount account = getAccount(userId);
        return account.getAvailablePoints();
    }

    @Transactional(readOnly = true)
    @Override
    public PointsAccount getAccount(String userId) {
        validateUserId(userId);

        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    @Override
    public Long getTotalPoints(String userId) {
        PointsAccount account = getAccount(userId);
        return account.getTotalPoints();
    }

    @Transactional(readOnly = true)
    @Override
    public Integer getCurrentLevel(String userId) {
        PointsAccount account = getAccount(userId);
        return account.getLevel();
    }

    @Override
    public PointsAccount updateBalance(PointsAccount account, Long pointsDelta) {
        if (account == null) {
            throw new IllegalArgumentException("Аккаунт не может быть null");
        }

        log.debug("Обновление баланса пользователя {} на {} очков",
                account.getUserId(), pointsDelta);

        account.setTotalPoints(account.getTotalPoints() + pointsDelta);
        account.setAvailablePoints(account.getAvailablePoints() + pointsDelta);
        account.setUpdatedAt(LocalDateTime.now());

        updateUserLevel(account);

        PointsAccount savedAccount = accountRepository.save(account);
        log.debug("Баланс пользователя {} обновлен. Доступно: {}, Всего: {}, Уровень: {}",
                savedAccount.getUserId(),
                savedAccount.getAvailablePoints(),
                savedAccount.getTotalPoints(),
                savedAccount.getLevel());

        return savedAccount;
    }

    @Override
    public void updateUserLevel(PointsAccount account) {
        int newLevel = levelCalculator.calculateLevel(account.getTotalPoints());

        if (newLevel > account.getLevel()) {
            int oldLevel = account.getLevel();
            account.setLevel(newLevel);

            log.info("Пользователь {} повысил уровень с {} до {} (всего очков: {})",
                    account.getUserId(), oldLevel, newLevel, account.getTotalPoints());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasSufficientPoints(String userId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Количество очков должно быть положительным числом");
        }

        PointsAccount account = getAccount(userId);
        return account.getAvailablePoints() >= amount;
    }

    @Override
    public void deleteAccount(String userId) {
        PointsAccount account = getAccount(userId);
        accountRepository.delete(account);
        log.info("Аккаунт пользователя {} удален", userId);
    }

    /**
     * Сбрасывает баланс аккаунта до нуля.
     * <p>
     * Используется для тестирования или сброса состояния пользователя.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return обновленный аккаунт
     * @throws AccountNotFoundException если аккаунт не найден
     */
    public PointsAccount resetAccount(String userId) {
        PointsAccount account = getAccount(userId);

        account.setTotalPoints(0L);
        account.setAvailablePoints(0L);
        account.setFrozenPoints(0L);
        account.setLevel(1);
        account.setUpdatedAt(LocalDateTime.now());

        PointsAccount savedAccount = accountRepository.save(account);
        log.info("Баланс пользователя {} сброшен до нуля", userId);

        return savedAccount;
    }

    /**
     * Создает новый аккаунт пользователя с нулевым балансом.
     *
     * @param userId идентификатор пользователя
     * @return созданный аккаунт
     * @throws IllegalArgumentException если userId является null или пустой строкой
     */
    private PointsAccount createNewAccount(String userId) {
        validateUserId(userId);

        PointsAccount account = PointsAccount.builder()
                .userId(userId)
                .totalPoints(0L)
                .availablePoints(0L)
                .frozenPoints(0L)
                .level(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    /**
     * Валидирует идентификатор пользователя.
     *
     * @param userId идентификатор пользователя для валидации
     * @throws IllegalArgumentException если userId является null или пустой строкой
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }
    }
}
