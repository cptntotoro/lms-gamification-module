package ru.misis.gamification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.misis.gamification.model.dto.PointsBalanceDto;
import ru.misis.gamification.model.dto.SpendPointsRequest;
import ru.misis.gamification.model.dto.TransactionDto;
import ru.misis.gamification.model.dto.UserPointsSummaryDto;
import ru.misis.gamification.model.entity.PointsAccount;
import ru.misis.gamification.model.entity.PointsTransaction;
import ru.misis.gamification.repository.PointsAccountRepository;
import ru.misis.gamification.repository.PointsTransactionRepository;
import ru.misis.gamification.service.PointsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/points")
@Tag(name = "Points API", description = "API для управления очками")
@Slf4j
@RequiredArgsConstructor
public class PointsController {

    /**
     * Сервис управления очками пользователей
     */
    private final PointsService pointsService;
    private final PointsAccountRepository pointsAccountRepository;
    private final PointsTransactionRepository transactionRepository;

    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<PointsBalanceDto> getBalance(@PathVariable String userId) {
        PointsAccount account = pointsService.getOrCreateAccount(userId);

        PointsBalanceDto dto = PointsBalanceDto.builder()
                .userId(account.getUserId())
                .totalPoints(account.getTotalPoints())
                .availablePoints(account.getAvailablePoints())
                .frozenPoints(account.getFrozenPoints())
                .level(account.getLevel())
                .updatedAt(account.getUpdatedAt())
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<PointsTransaction> transactions = transactionRepository
                .findByUserIdAndFilters(userId, fromDate, toDate, pageable);

        List<TransactionDto> dtos = transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserPointsSummaryDto>> getLeaderboard(
            @RequestParam(defaultValue = "total") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {

        List<PointsAccount> topAccounts = switch (sortBy) {
            case "total" -> pointsAccountRepository.findTopByTotalPoints(limit);
            case "available" -> pointsAccountRepository.findTopByAvailablePoints(limit);
            case "level" -> pointsAccountRepository.findTopByLevel(limit);
            default -> pointsAccountRepository.findTopByTotalPoints(limit);
        };

        List<UserPointsSummaryDto> leaderboard = topAccounts.stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(leaderboard);
    }

    private UserPointsSummaryDto convertToSummaryDto(PointsAccount pointsAccount) {
        return UserPointsSummaryDto.builder()
                .userId(pointsAccount.getUserId())
                .availablePoints(pointsAccount.getAvailablePoints())
                .level(pointsAccount.getLevel())
                .totalPoints(pointsAccount.getTotalPoints())
                .build();
    }

    @PostMapping("/users/{userId}/spend")
    public ResponseEntity<TransactionDto> spendPoints(
            @PathVariable String userId,
            @RequestBody SpendPointsRequest request) {

        PointsTransaction transaction = pointsService.spendPoints(
                userId,
                request.getAmount(),
                request.getPurpose()
        );

        return ResponseEntity.ok(convertToDto(transaction));
    }

    private TransactionDto convertToDto(PointsTransaction transaction) {
        return TransactionDto.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .pointsDelta(transaction.getPointsDelta())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }
}
