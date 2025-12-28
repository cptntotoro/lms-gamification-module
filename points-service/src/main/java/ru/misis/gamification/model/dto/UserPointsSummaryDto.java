package ru.misis.gamification.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPointsSummaryDto {
    private String userId;
    private Long totalPoints;
    private Long availablePoints;
    private Integer level;
}