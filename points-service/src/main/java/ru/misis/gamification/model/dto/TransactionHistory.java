package ru.misis.gamification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.misis.gamification.model.entity.PointsTransaction;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для истории транзакций.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionHistory {
    private String userId;
    private List<PointsTransaction> transactions;
    private Long totalEarned;
    private Long totalSpent;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
}