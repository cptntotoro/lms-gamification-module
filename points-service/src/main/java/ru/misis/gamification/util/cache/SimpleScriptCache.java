package ru.misis.gamification.util.cache;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Кэшер Groovy скриптов
 */
@Component
@Slf4j
public class SimpleScriptCache {

    private final CompilerConfiguration compilerConfig;
    private final ConcurrentMap<String, Script> scriptCache;

    // Статистика для отладки
    private int cacheHits = 0;
    private int cacheMisses = 0;

    public SimpleScriptCache(CompilerConfiguration compilerConfig) {
        this.compilerConfig = compilerConfig;
        this.scriptCache = new ConcurrentHashMap<>(100); // Начальный размер
    }

    /**
     * Получает скомпилированный скрипт из кэша или компилирует новый.
     */
    public Script getOrCompileScript(String formula) {
        return scriptCache.computeIfAbsent(formula, key -> {
            cacheMisses++;
            try {
                GroovyShell shell = new GroovyShell(compilerConfig);
                Script script = shell.parse(formula);
                log.debug("Compiled new script for formula hash: {}", formula.hashCode());
                return script;
            } catch (Exception e) {
                log.error("Failed to compile Groovy script: {}", e.getMessage());
                throw new RuntimeException("Failed to compile script: " + formula, e);
            }
        });
    }

    /**
     * Выполняет скрипт с переданными переменными.
     */
    public Object executeScript(String formula, Map<String, Object> variables) {
        Script script = getOrCompileScript(formula);
        cacheHits++;

        try {
            // Устанавливаем переменные
            variables.forEach(script.getBinding()::setVariable);
            return script.run();
        } catch (Exception e) {
            log.error("Failed to execute script: {}", e.getMessage());
            throw new RuntimeException("Script execution failed: " + formula, e);
        }
    }

    /**
     * Очищает кэш.
     */
    public void clearCache() {
        int size = scriptCache.size();
        scriptCache.clear();
        cacheHits = 0;
        cacheMisses = 0;
        log.info("Script cache cleared (was {} scripts)", size);
    }

    /**
     * Возвращает статистику кэша (для отладки).
     */
    public CacheStats getStats() {
        return new CacheStats(
                scriptCache.size(),
                cacheHits,
                cacheMisses,
                cacheHits + cacheMisses > 0 ?
                        (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0
        );
    }

    /**
     * Простая структура для статистики.
     */
    public record CacheStats(int size, int hits, int misses, double hitRate) {
        @Override
        public String toString() {
            return String.format(
                    "Scripts: %d, Hits: %d, Misses: %d, Hit rate: %.1f%%",
                    size, hits, misses, hitRate
            );
        }
    }
}