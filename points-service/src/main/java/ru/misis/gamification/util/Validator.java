package ru.misis.gamification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.misis.gamification.model.entity.RuleEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Валидатор для бизнес-правил и событий.
 * Критически важен для научного исследования:
 * 1. Обеспечивает корректность данных для анализа
 * 2. Предотвращает ошибки в экспериментальных правилах
 * 3. Валидирует события от разных LMS
 */
@Component
@Slf4j
public class Validator {

    // Регулярки для валидации
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    private static final Pattern EVENT_ID_PATTERN =
            Pattern.compile("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");
    private static final Pattern RULE_ID_PATTERN =
            Pattern.compile("^[a-z0-9-]{3,100}$");

    /**
     * Валидация события от LMS.
     */
    public ValidationResult validateEvent(Map<String, Object> event) {
        ValidationResult result = new ValidationResult();

        // Обязательные поля
        if (!event.containsKey("eventId")) {
            result.addError("eventId", "Required field");
        } else if (!isValidEventId(event.get("eventId").toString())) {
            result.addError("eventId", "Invalid format");
        }

        if (!event.containsKey("userId")) {
            result.addError("userId", "Required field");
        } else if (!isValidUserId(event.get("userId").toString())) {
            result.addError("userId", "Invalid format");
        }

        if (!event.containsKey("eventType")) {
            result.addError("eventType", "Required field");
        }

        if (!event.containsKey("occurredAt")) {
            result.addError("occurredAt", "Required field");
        } else {
            try {
                LocalDateTime.parse(event.get("occurredAt").toString());
            } catch (Exception e) {
                result.addError("occurredAt", "Invalid date format");
            }
        }

        // payload может быть пустым, но если есть - должен быть объектом
        if (event.containsKey("payload") && !(event.get("payload") instanceof Map)) {
            result.addError("payload", "Must be an object");
        }

        return result;
    }

    /**
     * Валидация правила геймификации.
     */
    public ValidationResult validateRule(RuleEntity rule) {
        ValidationResult result = new ValidationResult();

        // ruleId
        if (rule.getRuleId() == null || rule.getRuleId().isBlank()) {
            result.addError("ruleId", "Required field");
        } else if (!RULE_ID_PATTERN.matcher(rule.getRuleId()).matches()) {
            result.addError("ruleId", "Invalid format. Use lowercase letters, numbers and hyphens");
        }

        // eventType
        if (rule.getEventType() == null || rule.getEventType().isBlank()) {
            result.addError("eventType", "Required field");
        }

        // name
        if (rule.getName() == null || rule.getName().isBlank()) {
            result.addError("name", "Required field");
        } else if (rule.getName().length() > 200) {
            result.addError("name", "Max length is 200 characters");
        }

        // pointsValue
        if (rule.getPointsValue() == null) {
            result.addError("pointsValue", "Required field");
        } else if (rule.getPointsValue() < -10000 || rule.getPointsValue() > 10000) {
            result.addError("pointsValue", "Must be between -10000 and 10000");
        }

        // formula и pointsValue взаимоисключающи
        if (rule.getFormula() != null && !rule.getFormula().isBlank() && rule.getPointsValue() != 0) {
            result.addWarning("formula", "Formula overrides pointsValue");
        }

        // priority
        if (rule.getPriority() == null) {
            result.addError("priority", "Required field");
        } else if (rule.getPriority() < 1 || rule.getPriority() > 100) {
            result.addError("priority", "Must be between 1 and 100");
        }

        // validFrom
        if (rule.getValidFrom() == null) {
            result.addError("validFrom", "Required field");
        } else if (rule.getValidFrom().isBefore(LocalDateTime.now().minusYears(1))) {
            result.addWarning("validFrom", "Rule starts in the past");
        }

        // validTo если указан
        if (rule.getValidTo() != null && rule.getValidTo().isBefore(rule.getValidFrom())) {
            result.addError("validTo", "Must be after validFrom");
        }

        // maxDaily и maxTotal
        if (rule.getMaxDaily() != null && rule.getMaxDaily() < 1) {
            result.addError("maxDaily", "Must be positive");
        }

        if (rule.getMaxTotal() != null && rule.getMaxTotal() < 1) {
            result.addError("maxTotal", "Must be positive");
        }

        // conditions
        if (rule.getConditions() != null) {
            for (Map.Entry<String, String> condition : rule.getConditions().entrySet()) {
                if (condition.getKey().isBlank()) {
                    result.addError("conditions", "Condition key cannot be empty");
                }
                if (condition.getValue().isBlank()) {
                    result.addError("conditions", "Condition value cannot be empty");
                }
            }
        }

        return result;
    }

    /**
     * Валидация запроса на списание очков.
     */
    public ValidationResult validateSpendRequest(String userId, Long amount, String purpose) {
        ValidationResult result = new ValidationResult();

        if (!isValidUserId(userId)) {
            result.addError("userId", "Invalid user ID");
        }

        if (amount == null || amount <= 0) {
            result.addError("amount", "Must be positive");
        } else if (amount > 10000) {
            result.addError("amount", "Cannot spend more than 10000 points at once");
        }

        if (purpose == null || purpose.isBlank()) {
            result.addError("purpose", "Required field");
        } else if (purpose.length() > 500) {
            result.addError("purpose", "Max length is 500 characters");
        }

        return result;
    }

    /**
     * Проверка формата userId.
     */
    private boolean isValidUserId(String userId) {
        return userId != null && USER_ID_PATTERN.matcher(userId).matches();
    }

    /**
     * Проверка формата eventId.
     */
    private boolean isValidEventId(String eventId) {
        return eventId != null && EVENT_ID_PATTERN.matcher(eventId).matches();
    }

    /**
     * Проверка корректности payload события.
     */
    public boolean isValidPayload(Map<String, Object> payload) {
        if (payload == null) {
            return true; // payload может быть пустым
        }

        // Проверяем глубину вложенности (ограничение для упрощения)
        if (getMaxDepth(payload) > 5) {
            log.warn("Payload too deep: {}", getMaxDepth(payload));
            return false;
        }

        // Проверяем размер (примерно)
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(payload);
            if (json.length() > 10000) { // 10KB максимум
                log.warn("Payload too large: {} bytes", json.length());
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Вычисление максимальной глубины объекта.
     */
    private int getMaxDepth(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return 1;
        }

        int maxDepth = 1;
        for (Object value : map.values()) {
            if (value instanceof Map) {
                int depth = 1 + getMaxDepth((Map<String, Object>) value);
                if (depth > maxDepth) {
                    maxDepth = depth;
                }
            }
        }

        return maxDepth;
    }

    /**
     * Валидация для научного исследования.
     * Проверяет, что событие содержит все необходимые для анализа поля.
     */
    public ValidationResult validateForResearch(Map<String, Object> event) {
        ValidationResult result = new ValidationResult();

        // Для исследования важно знать контекст
        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
        if (payload != null) {
            // Должен быть идентификатор курса
            if (!payload.containsKey("courseId")) {
                result.addWarning("payload.courseId", "Missing for research analysis");
            }

            // Должен быть тип активности
            if (!payload.containsKey("activityType")) {
                result.addWarning("payload.activityType", "Missing for categorization");
            }

            // Желательно время выполнения
            if (!payload.containsKey("durationMinutes")) {
                result.addWarning("payload.durationMinutes", "Missing for engagement analysis");
            }
        }

        return result;
    }
}
