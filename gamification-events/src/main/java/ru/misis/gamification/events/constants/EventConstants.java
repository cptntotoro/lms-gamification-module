package ru.misis.gamification.events.constants;

/**
 * Константы для типов событий и общих значений
 */
public final class EventConstants {

    private EventConstants() {
    }

    /**
     * Тип события: завершение задачи
     */
    public static final String TASK_COMPLETED = "TASK_COMPLETED";

    /**
     * Тип события: прохождение теста
     */
    public static final String TEST_PASSED = "TEST_PASSED";

    /**
     * Тип события: запись на курс
     */
    public static final String COURSE_ENROLLED = "COURSE_ENROLLED";

    /**
     * Тип события: создание поста на форуме
     */
    public static final String FORUM_POST_CREATED = "FORUM_POST_CREATED";

    /**
     * Тип события: сдача задания
     */
    public static final String ASSIGNMENT_SUBMITTED = "ASSIGNMENT_SUBMITTED";

    /**
     * Тип события: изменение баланса очков
     */
    public static final String POINTS_CHANGED = "POINTS_CHANGED";

    /**
     * Тип события: повышение уровня
     */
    public static final String LEVEL_UP = "LEVEL_UP";

    /**
     * Тип события: получение достижения
     */
    public static final String ACHIEVEMENT_UNLOCKED = "ACHIEVEMENT_UNLOCKED";

    /**
     * Редкость достижения: обычное
     */
    public static final String ACHIEVEMENT_RARITY_COMMON = "COMMON";

    /**
     * Редкость достижения: редкое
     */
    public static final String ACHIEVEMENT_RARITY_RARE = "RARE";

    /**
     * Редкость достижения: эпическое
     */
    public static final String ACHIEVEMENT_RARITY_EPIC = "EPIC";

    /**
     * Редкость достижения: легендарное
     */
    public static final String ACHIEVEMENT_RARITY_LEGENDARY = "LEGENDARY";

    /**
     * Максимально возможный балл
     */
    public static final int MAX_SCORE = 100;

    /**
     * Минимально возможный балл
     */
    public static final int MIN_SCORE = 0;

    /**
     * Минимальный процент прохождения теста
     */
    public static final double MIN_PERCENTAGE = 0.0;

    /**
     * Максимальный процент прохождения теста
     */
    public static final double MAX_PERCENTAGE = 100.0;
}