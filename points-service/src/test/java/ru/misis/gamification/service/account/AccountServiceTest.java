package ru.misis.gamification.service.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.misis.gamification.exception.AccountNotFoundException;
import ru.misis.gamification.model.entity.PointsAccount;
import ru.misis.gamification.repository.AccountRepository;
import ru.misis.gamification.util.PointsLevelCalculator;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для сервиса управления аккаунтами пользователей.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PointsLevelCalculator levelCalculator;

    @InjectMocks
    private AccountServiceImpl accountService;

    private final String testUserId = "student_001";
    private PointsAccount existingAccount;

    @BeforeEach
    void setUp() {
        existingAccount = PointsAccount.builder()
                .userId(testUserId)
                .availablePoints(1000L)
                .totalPoints(1500L)
                .frozenPoints(500L)
                .level(3)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Получение существующего аккаунта пользователя")
    void getAccount_whenAccountExists_shouldReturnAccount() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        PointsAccount result = accountService.getAccount(testUserId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getAvailablePoints()).isEqualTo(1000L);
        assertThat(result.getLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("Получение аккаунта несуществующего пользователя должно вызывать исключение")
    void getAccount_whenAccountNotExists_shouldThrowException() {
        String nonExistingUserId = "non_existing_user";
        when(accountRepository.findByUserId(nonExistingUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(nonExistingUserId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(nonExistingUserId);
    }

    @Test
    @DisplayName("Создание нового аккаунта при первом обращении")
    void getOrCreateAccount_whenAccountNotExists_shouldCreateNewAccount() {
        String newUserId = "new_user_001";
        when(accountRepository.findByUserId(newUserId))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(PointsAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAccount result = accountService.getOrCreateAccount(newUserId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(newUserId);
        assertThat(result.getAvailablePoints()).isEqualTo(0L);
        assertThat(result.getTotalPoints()).isEqualTo(0L);
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getFrozenPoints()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Получение существующего аккаунта без создания нового")
    void getOrCreateAccount_whenAccountExists_shouldReturnExistingAccount() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        PointsAccount result = accountService.getOrCreateAccount(testUserId);

        assertThat(result).isSameAs(existingAccount);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление баланса с положительным значением должно увеличивать баланс")
    void updateBalance_withPositivePoints_shouldIncreaseBalance() {
        when(levelCalculator.calculateLevel(2500L)).thenReturn(4);
        when(accountRepository.save(any(PointsAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAccount result = accountService.updateBalance(existingAccount, 1000L);

        assertThat(result.getAvailablePoints()).isEqualTo(2000L);
        assertThat(result.getTotalPoints()).isEqualTo(2500L);
        assertThat(result.getLevel()).isEqualTo(4);
    }

    @Test
    @DisplayName("Обновление баланса с отрицательным значением должно уменьшать баланс, но не понижать уровень")
    void updateBalance_withNegativePoints_shouldDecreaseBalanceButNotLevel() {
        when(levelCalculator.calculateLevel(500L)).thenReturn(2);
        when(accountRepository.save(any(PointsAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAccount result = accountService.updateBalance(existingAccount, -1000L);

        assertThat(result.getAvailablePoints()).isEqualTo(0L);
        assertThat(result.getTotalPoints()).isEqualTo(500L);

        assertThat(result.getLevel()).isEqualTo(3);

        verify(levelCalculator).calculateLevel(500L);
    }

    @Test
    @DisplayName("Проверка достаточности средств при достаточном балансе")
    void hasSufficientPoints_whenBalanceIsSufficient_shouldReturnTrue() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        boolean result = accountService.hasSufficientPoints(testUserId, 500L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Проверка достаточности средств при недостаточном балансе")
    void hasSufficientPoints_whenBalanceIsInsufficient_shouldReturnFalse() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        boolean result = accountService.hasSufficientPoints(testUserId, 2000L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Получение текущего баланса пользователя")
    void getAvailableBalance_shouldReturnCorrectBalance() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        Long balance = accountService.getAvailableBalance(testUserId);

        assertThat(balance).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Получение общего количества очков пользователя")
    void getTotalPoints_shouldReturnCorrectTotalPoints() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        Long totalPoints = accountService.getTotalPoints(testUserId);

        assertThat(totalPoints).isEqualTo(1500L);
    }

    @Test
    @DisplayName("Получение текущего уровня пользователя")
    void getCurrentLevel_shouldReturnCorrectLevel() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        Integer level = accountService.getCurrentLevel(testUserId);

        assertThat(level).isEqualTo(3);
    }

    @Test
    @DisplayName("Обновление уровня пользователя при достижении нового уровня")
    void updateUserLevel_whenNewLevelIsHigher_shouldUpdateLevel() {
        existingAccount.setTotalPoints(10000L);
        when(levelCalculator.calculateLevel(10000L)).thenReturn(5);

        accountService.updateUserLevel(existingAccount);

        assertThat(existingAccount.getLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("Обновление уровня пользователя при том же уровне не должно изменять уровень")
    void updateUserLevel_whenNewLevelIsSame_shouldNotUpdateLevel() {
        existingAccount.setTotalPoints(1500L);
        when(levelCalculator.calculateLevel(1500L)).thenReturn(3);

        accountService.updateUserLevel(existingAccount);

        assertThat(existingAccount.getLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("Удаление аккаунта пользователя")
    void deleteAccount_shouldDeleteAccountFromRepository() {
        when(accountRepository.findByUserId(testUserId))
                .thenReturn(Optional.of(existingAccount));

        accountService.deleteAccount(testUserId);

        verify(accountRepository).delete(existingAccount);
    }

    @Test
    @DisplayName("Валидация пустого идентификатора пользователя должно вызывать исключение")
    void validateUserId_withEmptyUserId_shouldThrowException() {
        // Тест пустого userId
        assertThatThrownBy(() -> accountService.getOrCreateAccount(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");

        // Тест userId с пробелами
        assertThatThrownBy(() -> accountService.getOrCreateAccount("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");

        // Тест null userId
        assertThatThrownBy(() -> accountService.getOrCreateAccount(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");
    }

    @Test
    @DisplayName("Обновление баланса с нулевым изменением должно обновить только время")
    void updateBalance_withZeroPoints_shouldUpdateTimestampOnly() {
        LocalDateTime initialUpdateTime = existingAccount.getUpdatedAt();

        when(levelCalculator.calculateLevel(1500L)).thenReturn(3);
        when(accountRepository.save(any(PointsAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAccount result = accountService.updateBalance(existingAccount, 0L);

        assertThat(result.getAvailablePoints()).isEqualTo(1000L); // Не изменилось
        assertThat(result.getTotalPoints()).isEqualTo(1500L); // Не изменилось
        assertThat(result.getUpdatedAt()).isAfter(initialUpdateTime); // Время обновилось
    }

    @Test
    @DisplayName("Проверка достаточности средств с нулевой суммой должно вызывать исключение")
    void hasSufficientPoints_withZeroAmount_shouldThrowException() {
        assertThatThrownBy(() -> accountService.hasSufficientPoints(testUserId, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("положительным числом");
    }

    @Test
    @DisplayName("Проверка достаточности средств с отрицательной суммой должно вызывать исключение")
    void hasSufficientPoints_withNegativeAmount_shouldThrowException() {
        assertThatThrownBy(() -> accountService.hasSufficientPoints(testUserId, -100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("положительным числом");
    }

    @Test
    @DisplayName("Создание аккаунта с специальными символами в userId должно работать корректно")
    void getOrCreateAccount_withSpecialCharacters_shouldWorkCorrectly() {
        String userIdWithSpecialChars = "user-001@example.com";
        when(accountRepository.findByUserId(userIdWithSpecialChars))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(PointsAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsAccount result = accountService.getOrCreateAccount(userIdWithSpecialChars);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userIdWithSpecialChars);
    }

    @Test
    @DisplayName("Обновление уровня при понижении количества очков не должно изменять уровень в меньшую сторону")
    void updateUserLevel_whenPointsDecrease_shouldNotDecreaseLevel() {
        // Начальный уровень 3 при 1500 очках
        existingAccount.setTotalPoints(1500L);
        existingAccount.setLevel(3);

        // После уменьшения до 500 очков, уровень должен остаться 3
        // (система не понижает уровни)
        existingAccount.setTotalPoints(500L);
        when(levelCalculator.calculateLevel(500L)).thenReturn(2);

        accountService.updateUserLevel(existingAccount);

        // Уровень должен остаться 3, даже если по формуле должен быть 2
        assertThat(existingAccount.getLevel()).isEqualTo(3);
    }
}