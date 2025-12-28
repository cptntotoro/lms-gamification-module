package ru.misis.gamification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Калькулятор для расчета уровней пользователя на основе накопленных очков
 * <p>
 * Реализует несколько алгоритмов расчета уровня для исследования
 * различных моделей прогрессии в системе геймификации.
 * </p>
 */
@Component
@Slf4j
public class PointsLevelCalculator {

    /**
     * Базовое количество очков, необходимое для первого уровня
     */
    private static final long BASE_POINTS = 1000L;

    /**
     * Вычисляет уровень пользователя по экспоненциальной прогрессии.
     * <p>
     * Каждый следующий уровень требует в N раз больше очков, чем предыдущий:
     * Уровень 1: 0-1000 очков
     * Уровень 2: 1001-3000 очков (2000 очков разницы)
     * Уровень 3: 3001-6000 очков (3000 очков разницы)
     * Уровень N: предыдущий + N*1000
     * </p>
     *
     * @param totalPoints Общее количество очков пользователя
     * @return текущий уровень пользователя (минимальное значение - 1)
     * @throws IllegalArgumentException если totalPoints {@code null}
     */
    public int calculateLevelExponential(Long totalPoints) {
        validateTotalPoints(totalPoints);

        if (totalPoints <= 0) return 1;

        int level = 1;
        long required = BASE_POINTS;
        long accumulated = 0L;

        while (accumulated + required <= totalPoints) {
            accumulated += required;
            level++;
            required += level * BASE_POINTS;
        }

        return level;
    }

    /**
     * Вычисляет уровень пользователя по логарифмической прогрессии.
     * <p>
     * Уровень растет медленно с увеличением очков, что мотивирует
     * пользователей продолжать активность даже на высоких уровнях.
     * </p>
     *
     * <p>
     * Формула: level = floor(log10(totalPoints + 1)) + 1
     * </p>
     *
     * @param totalPoints Общее количество очков пользователя
     * @return Текущий уровень пользователя
     * @throws IllegalArgumentException Если totalPoints {@code null}
     */
    public int calculateLevelLogarithmic(Long totalPoints) {
        validateTotalPoints(totalPoints);

        if (totalPoints <= 0) return 1;

        // Формула: level = floor(log10(totalPoints + 1)) + 1
        double level = Math.log10(totalPoints + 1);
        return (int) Math.floor(level) + 1;
    }

    /**
     * Вычисляет уровень пользователя по линейной прогрессии.
     * <p>
     * Простейшая модель: каждые 1000 очков = +1 уровень.
     * Легко понимается пользователями.
     * </p>
     *
     * @param totalPoints Общее количество очков пользователя
     * @return Текущий уровень пользователя
     * @throws IllegalArgumentException Если totalPoints {@code null}
     */
    public int calculateLevelLinear(Long totalPoints) {
        validateTotalPoints(totalPoints);

        if (totalPoints <= 0) return 1;

        return (int) (totalPoints / BASE_POINTS) + 1;
    }

    /**
     * Гибридный расчет уровня (по умолчанию).
     * <p>
     * Использует разные алгоритмы в зависимости от количества очков:
     * <ul>
     *     <li>0-1000 очков: линейная прогрессия (быстрый старт)</li>
     *     <li>1001-10000 очков: экспоненциальная (средние уровни)</li>
     *     <li>>10000 очков: логарифмическая (плавный рост на высоких уровнях)</li>
     * </ul>
     * </p>
     *
     * @param totalPoints Общее количество очков пользователя
     * @return Текущий уровень пользователя
     * @throws IllegalArgumentException Если totalPoints {@code null}
     */
    public int calculateLevel(Long totalPoints) {
        validateTotalPoints(totalPoints);

        if (totalPoints <= BASE_POINTS) {
            return calculateLevelLinear(totalPoints);
        } else if (totalPoints <= 10000) {
            return calculateLevelExponential(totalPoints);
        } else {
            return calculateLevelLogarithmic(totalPoints);
        }
    }

    /**
     * Вычисляет количество очков, необходимых для достижения следующего уровня.
     *
     * @param totalPoints Общее количество очков пользователя
     * @param currentLevel Текущий уровень пользователя
     * @return Количество очков до следующего уровня (0 если уже достигнут максимум)
     * @throws IllegalArgumentException Если любой из параметров {@code null}
     */
    public long getPointsToNextLevel(Long totalPoints, Integer currentLevel) {
        validateTotalPoints(totalPoints);

        if (currentLevel == null) {
            throw new IllegalArgumentException("currentLevel не может быть null");
        }

        if (currentLevel <= 0) {
            throw new IllegalArgumentException("currentLevel должен быть положительным");
        }

        // Вычисляем очки, необходимые для достижения текущего уровня
        long pointsForCurrentLevel = calculatePointsForLevel(currentLevel - 1);

        // Вычисляем очки, необходимые для достижения следующего уровня
        long pointsForNextLevel = calculatePointsForLevel(currentLevel);

        // Очки, уже набранные на текущем уровне
        long pointsInCurrentLevel = totalPoints - pointsForCurrentLevel;

        // Очки, необходимые для перехода на следующий уровень
        long pointsNeededForLevelUp = pointsForNextLevel - pointsForCurrentLevel;

        return Math.max(0, pointsNeededForLevelUp - pointsInCurrentLevel);
    }

    /**
     * Вычисляет прогресс до следующего уровня в процентах.
     *
     * @param totalPoints Общее количество очков пользователя
     * @param currentLevel Текущий уровень пользователя
     * @return Процент прогресса (0.0 - 100.0)
     * @throws IllegalArgumentException Если любой из параметров {@code null}
     */
    public double getProgressToNextLevel(Long totalPoints, Integer currentLevel) {
        validateTotalPoints(totalPoints);

        if (currentLevel == null) {
            throw new IllegalArgumentException("currentLevel не может быть null");
        }

        if (currentLevel <= 0) {
            throw new IllegalArgumentException("currentLevel должен быть положительным");
        }

        // Очки, необходимые для достижения текущего уровня
        long pointsForCurrentLevel = calculatePointsForLevel(currentLevel - 1);

        // Очки, необходимые для достижения следующего уровня
        long pointsForNextLevel = calculatePointsForLevel(currentLevel);

        // Очки, уже набранные на текущем уровне
        long pointsInCurrentLevel = totalPoints - pointsForCurrentLevel;

        // Очки, необходимые для перехода на следующий уровень
        long pointsNeededForLevelUp = pointsForNextLevel - pointsForCurrentLevel;

        if (pointsNeededForLevelUp == 0) {
            return 100.0;
        }

        double progress = ((double) pointsInCurrentLevel / pointsNeededForLevelUp) * 100.0;
        return Math.min(progress, 100.0); // Ограничиваем 100%
    }

    /**
     * Вычисляет количество очков, необходимое для достижения указанного уровня.
     *
     * @param level целевой уровень
     * @return общее количество очков для достижения уровня
     * @throws IllegalArgumentException если level меньше 1
     */
    public long calculatePointsForLevel(int level) {
        if (level <= 0) {
            return 0L;
        }

        // Сумма арифметической прогрессии: 1000 + 2000 + 3000 + ... + level*1000
        return (level * (level + 1L) / 2L) * BASE_POINTS;
    }

    /**
     * Валидирует входные параметры.
     */
    private void validateTotalPoints(Long totalPoints) {
        if (totalPoints == null) {
            throw new IllegalArgumentException("totalPoints не может быть null");
        }

        if (totalPoints < 0) {
            throw new IllegalArgumentException("totalPoints не может быть отрицательным");
        }
    }
}