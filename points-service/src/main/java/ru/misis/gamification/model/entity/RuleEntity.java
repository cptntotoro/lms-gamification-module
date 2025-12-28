package ru.misis.gamification.model.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Правило начисления очков
 * Определяет условия и параметры начисления баллов за различные действия пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "points_rules")
public class RuleEntity {

    /**
     * Уникальный идентификатор правила.
     * Формат: [тип-события]-[название]-[версия]
     * Пример: task-completed-basic-v1
     */
    @Id
    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    /**
     * Тип события Kafka, к которому применяется правило.
     * Должен соответствовать eventType в GamificationEvent.
     * Пример: TASK_COMPLETED, TEST_PASSED, FORUM_POST_CREATED
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * Человеко-читаемое название правила.
     * Отображается в интерфейсе администратора.
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Подробное описание правила и условий его применения.
     * Может содержать Markdown для форматирования.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Фиксированное количество очков для начисления.
     * Используется, если formula не задана или пуста.
     * Может быть отрицательным для штрафов.
     */
    @Column(name = "points_value", nullable = false)
    private Integer pointsValue;

    /**
     * Формула для динамического расчета очков на языке Groovy.
     * Поддерживаемые переменные: payload.* (данные из события)
     * Примеры:
     * - "payload.score * 10" (за каждый балл 10 очков)
     * - "payload.difficulty == 'HARD' ? 100 : 50" (условное начисление)
     * - "Math.min(payload.score * 5, 200)" (с ограничением)
     * - "score * 10 + (difficulty == "HARD" ? 50 : 0)"
     */
    @Column(name = "formula", length = 500)
    private String formula;

    /**
     * Максимальное количество очков, которое можно начислить
     * по этому правилу за день для одного пользователя.
     * Если null - лимит отсутствует.
     */
    @Column(name = "max_daily")
    private Integer maxDaily;

    /**
     * Максимальное количество очков, которое можно начислить
     * по этому правилу всего для одного пользователя.
     * Если null - лимит отсутствует.
     */
    @Column(name = "max_total")
    private Integer maxTotal;

    /**
     * Приоритет правила (меньше = выше приоритет).
     * Используется, когда к одному событию применимо несколько правил.
     * Правила применяются в порядке возрастания приоритета.
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    /**
     * Флаг активности правила.
     * Неактивные правила игнорируются движком правил.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Дата и время, с которого правило начинает действовать.
     * События, произошедшие до этой даты, не обрабатываются.
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    /**
     * Дата и время, до которого правило действует.
     * Если null - правило бессрочное.
     * События, произошедшие после этой даты, не обрабатываются.
     */
    @Column(name = "valid_to")
    private LocalDateTime validTo;

    /**
     * Дополнительные условия применения правила в формате ключ-значение.
     * Ключ - путь к полю в payload события (dot notation)
     * Значение - ожидаемое значение или выражение сравнения
     *
     * Примеры:
     * - {"payload.courseId": "java-basics"} (только для курса Java)
     * - {"payload.score": ">=70"} (только при score >= 70)
     * - {"payload.difficulty": "HARD|EXPERT"} (только для сложных заданий)
     *
     * Все условия должны выполниться (логическое И).
     */
    @ElementCollection
    @CollectionTable(
            name = "rule_conditions",
            joinColumns = @JoinColumn(name = "rule_id"),
            foreignKey = @ForeignKey(name = "fk_rule_conditions_rule")
    )
    @MapKeyColumn(name = "condition_key", length = 100)
    @Column(name = "condition_value", length = 200)
    private Map<String, String> conditions;

    /**
     * Проверяет, активно ли правило в указанное время.
     * Учитывает isActive, validFrom и validTo.
     *
     * @param checkTime время для проверки
     * @return true если правило активно в указанное время, иначе false
     */
    public boolean isActiveAt(LocalDateTime checkTime) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }

        if (checkTime.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && checkTime.isAfter(validTo)) {
            return false;
        }

        return true;
    }

    /**
     * Проверяет, истекло ли правило.
     * Правило считается истекшим, если validTo в прошлом.
     *
     * @return true если правило истекло, иначе false
     */
    public boolean isExpired() {
        return validTo != null && validTo.isBefore(LocalDateTime.now());
    }

    /**
     * Проверяет, является ли правило динамическим
     * (имеет формулу для расчета очков).
     *
     * @return true если правило динамическое, иначе false
     */
    public boolean isDynamic() {
        return formula != null && !formula.trim().isEmpty();
    }

    /**
     * Проверяет, имеет ли правило условия применения.
     *
     * @return true если есть условия, иначе false
     */
    public boolean hasConditions() {
        return conditions != null && !conditions.isEmpty();
    }

    /**
     * Создает копию правила с новым идентификатором.
     * Используется для создания вариантов правил (A/B тестирование).
     *
     * @param newRuleId новый идентификатор правила
     * @param suffix суффикс для названия (например, "-variant-a")
     * @return копия правила
     */
    public RuleEntity copyWithNewId(String newRuleId, String suffix) {
        return RuleEntity.builder()
                .ruleId(newRuleId)
                .eventType(this.eventType)
                .name(this.name + suffix)
                .description(this.description)
                .pointsValue(this.pointsValue)
                .formula(this.formula)
                .maxDaily(this.maxDaily)
                .maxTotal(this.maxTotal)
                .priority(this.priority)
                .isActive(this.isActive)
                .validFrom(this.validFrom)
                .validTo(this.validTo)
                .conditions(this.conditions != null ? Map.copyOf(this.conditions) : null)
                .build();
    }
}
