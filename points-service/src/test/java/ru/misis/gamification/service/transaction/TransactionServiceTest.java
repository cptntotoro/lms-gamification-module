package ru.misis.gamification.service.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.misis.gamification.exception.DuplicateEventException;
import ru.misis.gamification.exception.PointsLimitException;
import ru.misis.gamification.model.entity.PointsTransaction;
import ru.misis.gamification.model.entity.RuleEntity;
import ru.misis.gamification.model.enums.TransactionStatus;
import ru.misis.gamification.model.enums.TransactionType;
import ru.misis.gamification.repository.PointsTransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для сервиса управления транзакциями.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private PointsTransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final String testUserId = "student_001";
    private final String testEventId = "event_123";
    private RuleEntity testRule;

    @BeforeEach
    void setUp() {
        testRule = RuleEntity.builder()
                .ruleId("task_completion_bonus")
                .name("Начисление за выполнение задания")
                .eventType("TASK_COMPLETED")
                .pointsValue(100)
                .maxDaily(500)
                .maxTotal(2000)
                .priority(1)
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("Создание транзакции начисления очков")
    void createAwardTransaction_shouldCreateEarnTransaction() {
        PointsTransaction savedTransaction = PointsTransaction.builder()
                .transactionId(UUID.randomUUID())
                .build();

        when(transactionRepository.save(any(PointsTransaction.class)))
                .thenReturn(savedTransaction);

        PointsTransaction result = transactionService.createAwardTransaction(
                testUserId,
                testRule,
                testEventId,
                150L,
                "Начисление за тестовое задание"
        );

        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isNotNull();
        verify(transactionRepository).save(any(PointsTransaction.class));
    }

    @Test
    @DisplayName("Создание транзакции списания очков")
    void createSpendTransaction_shouldCreateSpendTransaction() {
        PointsTransaction savedTransaction = PointsTransaction.builder()
                .userId(testUserId)
                .pointsDelta(-50L)
                .type(TransactionType.SPEND)
                .status(TransactionStatus.COMPLETED)
                .eventId(testEventId)
                .ruleId("MANUAL_SPEND")
                .source("USER_SPENDING")
                .description("Списание: Покупка бонуса")
                .transactionDate(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(PointsTransaction.class)))
                .thenReturn(savedTransaction);

        PointsTransaction result = transactionService.createSpendTransaction(
                testUserId,
                testEventId,
                50L,
                "Покупка бонуса"
        );

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.SPEND);
        assertThat(result.getPointsDelta()).isNegative();
        assertThat(result.getEventId()).isNotNull();
        verify(transactionRepository).save(any(PointsTransaction.class));
    }

    @Test
    @DisplayName("Проверка дублирования события при отсутствии дубликата")
    void checkDuplicateEvent_whenNoDuplicate_shouldNotThrowException() {
        when(transactionRepository.findByEventId(testEventId))
                .thenReturn(Optional.empty());

        transactionService.checkDuplicateEvent(testEventId);

        // No exception should be thrown
        verify(transactionRepository).findByEventId(testEventId);
    }

    @Test
    @DisplayName("Проверка дублирования события при наличии дубликата должно вызывать исключение")
    void checkDuplicateEvent_whenDuplicateExists_shouldThrowException() {
        PointsTransaction existingTransaction = PointsTransaction.builder()
                .eventId(testEventId)
                .build();

        when(transactionRepository.findByEventId(testEventId))
                .thenReturn(Optional.of(existingTransaction));

        assertThatThrownBy(() -> transactionService.checkDuplicateEvent(testEventId))
                .isInstanceOf(DuplicateEventException.class)
                .hasMessageContaining("Событие уже обработано");
    }

    @Test
    @DisplayName("Проверка лимитов правила при соблюдении дневного лимита")
    void checkRuleLimits_whenDailyLimitNotExceeded_shouldNotThrowException() {
        when(transactionRepository.calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(300L);

        transactionService.checkRuleLimits(testUserId, testRule);

        // No exception should be thrown
        verify(transactionRepository).calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Проверка лимитов правила при превышении дневного лимита должно вызывать исключение")
    void checkRuleLimits_whenDailyLimitExceeded_shouldThrowException() {
        when(transactionRepository.calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(600L);

        assertThatThrownBy(() -> transactionService.checkRuleLimits(testUserId, testRule))
                .isInstanceOf(PointsLimitException.class)
                .hasMessageContaining("Дневной лимит");
    }

    @Test
    @DisplayName("Проверка лимитов правила при соблюдении общего лимита")
    void checkRuleLimits_whenTotalLimitNotExceeded_shouldNotThrowException() {
        when(transactionRepository.calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(300L);
        when(transactionRepository.calculateTotalPoints(anyString(), anyString()))
                .thenReturn(1500L);

        transactionService.checkRuleLimits(testUserId, testRule);

        // No exception should be thrown
        verify(transactionRepository).calculateTotalPoints(anyString(), anyString());
    }

    @Test
    @DisplayName("Проверка лимитов правила при превышении общего лимита должно вызывать исключение")
    void checkRuleLimits_whenTotalLimitExceeded_shouldThrowException() {
        when(transactionRepository.calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(300L);
        when(transactionRepository.calculateTotalPoints(anyString(), anyString()))
                .thenReturn(2500L);

        assertThatThrownBy(() -> transactionService.checkRuleLimits(testUserId, testRule))
                .isInstanceOf(PointsLimitException.class)
                .hasMessageContaining("Общий лимит");
    }

    @Test
    @DisplayName("Получение истории транзакций пользователя")
    void getUserTransactions_shouldReturnUserTransactions() {
        List<PointsTransaction> testTransactions = List.of(
                PointsTransaction.builder()
                        .userId(testUserId)
                        .pointsDelta(100L)
                        .type(TransactionType.EARN)
                        .description("Начисление за задание")
                        .build(),
                PointsTransaction.builder()
                        .userId(testUserId)
                        .pointsDelta(-50L)
                        .type(TransactionType.SPEND)
                        .description("Покупка подсказки")
                        .build()
        );

        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(testUserId))
                .thenReturn(testTransactions);

        List<PointsTransaction> result = transactionService.getUserTransactions(testUserId, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPointsDelta()).isEqualTo(100L);
        assertThat(result.get(1).getPointsDelta()).isEqualTo(-50L);
    }

    @Test
    @DisplayName("Расчет общей суммы очков по правилу")
    void calculateTotalPointsByRule_shouldReturnCorrectTotal() {
        when(transactionRepository.calculateTotalPoints(testUserId, "test_rule_123"))
                .thenReturn(1500L);

        Long result = transactionService.calculateTotalPointsByRule(testUserId, "test_rule_123");

        assertThat(result).isEqualTo(1500L);
    }

    @Test
    @DisplayName("Расчет дневной суммы очков по правилу")
    void calculateDailyPointsByRule_shouldReturnCorrectDailyTotal() {
        when(transactionRepository.calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(300L);

        Long result = transactionService.calculateDailyPointsByRule(testUserId, "test_rule_123");

        assertThat(result).isEqualTo(300L);
    }

    @Test
    @DisplayName("Поиск транзакции по идентификатору события")
    void findByEventId_shouldReturnTransactionIfExists() {
        PointsTransaction testTransaction = PointsTransaction.builder()
                .eventId(testEventId)
                .description("Тестовая транзакция")
                .build();

        when(transactionRepository.findByEventId(testEventId))
                .thenReturn(Optional.of(testTransaction));

        PointsTransaction result = transactionService.findByEventId(testEventId);

        assertThat(result.getEventId()).isEqualTo(testEventId);
    }

    @Test
    @DisplayName("Отмена транзакции")
    void cancelTransaction_shouldUpdateTransactionStatus() {
        UUID transactionId = UUID.randomUUID();
        PointsTransaction activeTransaction = PointsTransaction.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.COMPLETED)
                .description("Начисление за тест")
                .build();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(activeTransaction));
        when(transactionRepository.save(any(PointsTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PointsTransaction result = transactionService.cancelTransaction(transactionId);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ROLLED_BACK);
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Отмена уже отмененной транзакции должно вызывать исключение")
    void cancelTransaction_whenAlreadyCancelled_shouldThrowException() {
        UUID transactionId = UUID.randomUUID();
        PointsTransaction cancelledTransaction = PointsTransaction.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.ROLLED_BACK)
                .description("Отмененная транзакция")
                .build();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(cancelledTransaction));

        assertThatThrownBy(() -> transactionService.cancelTransaction(transactionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже отменена");
    }

    @Test
    @DisplayName("Получение статистики транзакций пользователя")
    void getTransactionStats_shouldReturnCorrectStats() {
        when(transactionRepository.sumPositivePointsByUser(testUserId))
                .thenReturn(2000L);
        when(transactionRepository.sumNegativePointsByUser(testUserId))
                .thenReturn(-500L);
        when(transactionRepository.countByUserId(testUserId))
                .thenReturn(15L);

        TransactionServiceImpl.TransactionStats stats = transactionService.getTransactionStats(testUserId);

        assertThat(stats.totalEarned()).isEqualTo(2000L);
        assertThat(stats.totalSpent()).isEqualTo(-500L);
        assertThat(stats.transactionCount()).isEqualTo(15L);
        assertThat(stats.getNetBalance()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("Валидация некорректных параметров транзакции должно вызывать исключение")
    void validateTransactionParameters_withInvalidParameters_shouldThrowException() {
        // Пустой userId
        assertThatThrownBy(() ->
                transactionService.createAwardTransaction("", testRule, testEventId, 100L, "тест"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");

        // Null правило
        assertThatThrownBy(() ->
                transactionService.createAwardTransaction(testUserId, null, testEventId, 100L, "тест"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть null");

        // Пустой eventId
        assertThatThrownBy(() ->
                transactionService.createAwardTransaction(testUserId, testRule, "", 100L, "тест"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");

        // Нулевое количество очков
        assertThatThrownBy(() ->
                transactionService.createAwardTransaction(testUserId, testRule, testEventId, 0L, "тест"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("положительным числом");

        // Отрицательное количество очков
        assertThatThrownBy(() ->
                transactionService.createAwardTransaction(testUserId, testRule, testEventId, -100L, "тест"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("положительным числом");
    }

    @Test
    @DisplayName("Создание транзакции с пустым описанием должно использовать название правила")
    void createAwardTransaction_withEmptyDescription_shouldUseRuleName() {
        PointsTransaction savedTransaction = PointsTransaction.builder()
                .transactionId(UUID.randomUUID())
                .build();

        when(transactionRepository.save(any(PointsTransaction.class)))
                .thenReturn(savedTransaction);

        transactionService.createAwardTransaction(
                testUserId,
                testRule,
                testEventId,
                100L,
                null
        );

        verify(transactionRepository).save(argThat(transaction ->
                transaction.getDescription().equals(testRule.getName())));
    }

    @Test
    @DisplayName("Проверка лимитов правила без установленных лимитов должно проходить успешно")
    void checkRuleLimits_whenNoLimitsSet_shouldNotThrowException() {
        RuleEntity ruleWithoutLimits = RuleEntity.builder()
                .ruleId("rule_without_limits")
                .eventType("TEST_EVENT")
                .pointsValue(100)
                .build();

        transactionService.checkRuleLimits(testUserId, ruleWithoutLimits);

        // No exception should be thrown
        verify(transactionRepository, never()).calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class));
        verify(transactionRepository, never()).calculateTotalPoints(anyString(), anyString());
    }

    @Test
    @DisplayName("Создание транзакции списания с пустым назначением должно вызывать исключение")
    void createSpendTransaction_withEmptyPurpose_shouldThrowException() {
        assertThatThrownBy(() ->
                transactionService.createSpendTransaction(testUserId, testEventId, 100L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");

        assertThatThrownBy(() ->
                transactionService.createSpendTransaction(testUserId, testEventId, 100L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");

        assertThatThrownBy(() ->
                transactionService.createSpendTransaction(testUserId, testEventId, 100L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");
    }

    @Test
    @DisplayName("Получение истории транзакций с отрицательным лимитом должно использовать значение по умолчанию")
    void getUserTransactions_withNegativeLimit_shouldUseDefaultLimit() {
        List<PointsTransaction> testTransactions = List.of(
                PointsTransaction.builder()
                        .userId(testUserId)
                        .pointsDelta(100L)
                        .type(TransactionType.EARN)
                        .build()
        );

        when(transactionRepository.findByUserIdOrderByTransactionDateDesc(testUserId))
                .thenReturn(testTransactions);

        List<PointsTransaction> result = transactionService.getUserTransactions(testUserId, -10);

        assertThat(result).hasSize(1);
        verify(transactionRepository).findByUserIdOrderByTransactionDateDesc(testUserId);
    }

    @Test
    @DisplayName("Расчет дневных очков с нулевым результатом должно возвращать 0")
    void calculateDailyPointsByRule_whenNoPoints_shouldReturnZero() {
        when(transactionRepository.calculateDailyPoints(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(0L);

        Long result = transactionService.calculateDailyPointsByRule(testUserId, "test_rule");

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("Расчет общих очков с нулевым результатом должно возвращать 0")
    void calculateTotalPointsByRule_whenNoPoints_shouldReturnZero() {
        when(transactionRepository.calculateTotalPoints(testUserId, "test_rule"))
                .thenReturn(0L);

        Long result = transactionService.calculateTotalPointsByRule(testUserId, "test_rule");

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("Проверка дублирования события с null eventId должно вызывать исключение")
    void checkDuplicateEvent_withNullEventId_shouldThrowException() {
        assertThatThrownBy(() -> transactionService.checkDuplicateEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");
    }

    @Test
    @DisplayName("Поиск транзакции с пустым eventId должно вызывать исключение")
    void findByEventId_withEmptyEventId_shouldThrowException() {
        assertThatThrownBy(() -> transactionService.findByEventId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть пустым");
    }

    @Test
    @DisplayName("Отмена транзакции с null transactionId должно вызывать исключение")
    void cancelTransaction_withNullTransactionId_shouldThrowException() {
        assertThatThrownBy(() -> transactionService.cancelTransaction(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не может быть null");
    }

    @Test
    @DisplayName("Создание транзакции начисления с длинным описанием должно работать корректно")
    void createAwardTransaction_withLongDescription_shouldWorkCorrectly() {
        PointsTransaction savedTransaction = PointsTransaction.builder()
                .transactionId(UUID.randomUUID())
                .build();

        when(transactionRepository.save(any(PointsTransaction.class)))
                .thenReturn(savedTransaction);

        String longDescription = "Начисление баллов за выполнение комплексного задания по математике " +
                "с использованием продвинутых методов вычисления и анализа данных";

        PointsTransaction result = transactionService.createAwardTransaction(
                testUserId,
                testRule,
                testEventId,
                200L,
                longDescription
        );

        assertThat(result).isNotNull();
        verify(transactionRepository).save(any(PointsTransaction.class));
    }
}