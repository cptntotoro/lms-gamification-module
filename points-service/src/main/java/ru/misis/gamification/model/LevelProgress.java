package ru.misis.gamification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель прогресса пользователя до следующего уровня.
 * <p>
 * Используется для отображения в интерфейсе пользователя
 * информации о текущем уровне и прогрессе к следующему.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelProgress {
    /**
     * Уникальный идентификатор пользователя в системе
     */
    private String userId;

    /**
     * Текущий уровень пользователя.
     * <p>
     * Минимальное значение - 1.
     * Уровень рассчитывается на основе общего количества очков.
     * </p>
     */
    private Integer currentLevel;

    /**
     * Общее количество очков, заработанных пользователем за все время.
     * <p>
     * Включает как доступные, так и потраченные очки.
     * </p>
     */
    private Long currentPoints;

    /**
     * Количество очков, необходимое для достижения следующего уровня.
     * <p>
     * Значение 0 означает, что пользователь достиг максимального уровня
     * или следующий уровень недостижим по текущей формуле.
     * </p>
     */
    private Long pointsToNextLevel;

    /**
     * Прогресс до следующего уровня в процентах.
     * <p>
     * Значение от 0.0 до 100.0.
     * 100.0 означает, что пользователь набрал все необходимые очки
     * и должен повысить уровень.
     * </p>
     */
    private Double progressPercentage;

    /**
     * Проверяет, достигнут ли максимальный уровень.
     *
     * @return {@code true} если pointsToNextLevel равно 0,
     *         что обычно означает максимальный уровень
     */
    public boolean isMaxLevel() {
        return pointsToNextLevel != null && pointsToNextLevel == 0;
    }

    /**
     * Получает форматированную строку прогресса.
     * <p>
     * Пример: "75.5% до уровня 5"
     * </p>
     *
     * @return форматированная строка с информацией о прогрессе
     */
    public String getFormattedProgress() {
        if (currentLevel == null || progressPercentage == null) {
            return "Нет данных";
        }

        int nextLevel = isMaxLevel() ? currentLevel : currentLevel + 1;
        return String.format("%.1f%% до уровня %d", progressPercentage, nextLevel);
    }
}