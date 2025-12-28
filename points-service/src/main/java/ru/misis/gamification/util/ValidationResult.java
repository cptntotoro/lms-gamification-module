package ru.misis.gamification.util;

import lombok.Data;

import java.util.Map;

/**
 * Результат валидации с поддержкой warnings.
 */
@Data
class ValidationResult {
    private boolean valid = true;
    private Map<String, String> errors;
    private Map<String, String> warnings;

    public void addError(String field, String message) {
        valid = false;
        errors.put(field, message);
    }

    public void addWarning(String field, String message) {
        warnings.put(field, message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public String getErrorMessage() {
        return errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(java.util.stream.Collectors.joining("; "));
    }
}
