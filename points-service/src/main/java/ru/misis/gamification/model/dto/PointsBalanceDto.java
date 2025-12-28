package ru.misis.gamification.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PointsBalanceDto {
    private String userId;
    private Long totalPoints;
    private Long availablePoints;
    private Long frozenPoints;
    private Integer level;
    private LocalDateTime updatedAt;
}
