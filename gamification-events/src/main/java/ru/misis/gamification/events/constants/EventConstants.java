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

    // Другие полезные константы

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