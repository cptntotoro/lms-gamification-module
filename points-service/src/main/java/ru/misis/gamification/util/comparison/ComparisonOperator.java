package ru.misis.gamification.util.comparison;

import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Базовые операторы сравнения.
 */
public enum ComparisonOperator {

    EQUALS("=") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            if (actual == null) return false;
            return actual.toString().equals(expected);
        }
    },

    NOT_EQUALS("!=") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            if (actual == null) return true;
            return !actual.toString().equals(expected);
        }
    },

    GREATER_THAN(">") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            return compareNumbers(actual, expected) > 0;
        }
    },

    LESS_THAN("<") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            return compareNumbers(actual, expected) < 0;
        }
    },

    GREATER_THAN_OR_EQUALS(">=") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            return compareNumbers(actual, expected) >= 0;
        }
    },

    LESS_THAN_OR_EQUALS("<=") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            return compareNumbers(actual, expected) <= 0;
        }
    },

    CONTAINS("~=") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            if (actual == null) return false;
            return actual.toString().contains(expected);
        }
    },

    REGEX("~") {
        @Override
        public boolean evaluate(Object actual, String expected) {
            if (actual == null) return false;
            try {
                return Pattern.compile(expected).matcher(actual.toString()).find();
            } catch (Exception e) {
                return false;
            }
        }
    };

    @Getter
    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Сравнивает как числа если возможно, иначе как строки.
     */
    protected int compareNumbers(Object actual, String expected) {
        try {
            double actualNum = Double.parseDouble(actual.toString());
            double expectedNum = Double.parseDouble(expected);
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            return actual.toString().compareTo(expected);
        }
    }

    /**
     * Абстрактный метод для оценки условия.
     */
    public abstract boolean evaluate(Object actual, String expected);

    /**
     * Находит оператор по символу.
     */
    public static ComparisonOperator fromSymbol(String condition) {
        for (ComparisonOperator op : values()) {
            if (condition.startsWith(op.symbol)) {
                return op;
            }
        }
        return EQUALS; // По умолчанию
    }

    /**
     * Извлекает значение из условия.
     */
    public static String extractValue(String condition, ComparisonOperator operator) {
        return condition.startsWith(operator.symbol)
                ? condition.substring(operator.symbol.length()).trim()
                : condition;
    }
}