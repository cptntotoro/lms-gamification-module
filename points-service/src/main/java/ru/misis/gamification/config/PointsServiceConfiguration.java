//package ru.misis.gamification.config;
//
//import groovy.lang.GroovyShell;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
///**
// * Конфигурация Spring бинов для Points Service.
// * Особенность для научного исследования: легко заменяемые компоненты
// * для экспериментов с разными алгоритмами.
// */
//@Configuration
//@EnableTransactionManagement
//@EnableScheduling
//public class PointsServiceConfiguration {
//
//    /**
//     * GroovyShell с безопасным sandbox.
//     * Для формул в правилах геймификации.
//     */
//    @Bean
//    @Primary
//    public GroovyShell groovyShell() {
//        // Настраиваем безопасный sandbox
//        groovy.security.SandboxTransformer sandboxTransformer =
//                new groovy.security.SandboxTransformer();
//
//        org.codehaus.groovy.control.CompilerConfiguration config =
//                new org.codehaus.groovy.control.CompilerConfiguration();
//        config.addCompilationCustomizers(sandboxTransformer);
//
//        // Разрешаем только безопасные операции
//        groovy.security.SandboxSecurityManager securityManager =
//                new groovy.security.SandboxSecurityManager();
//        securityManager.setRestrictions(new java.security.PermissionCollection() {
//            @Override
//            public void add(java.security.Permission permission) {}
//
//            @Override
//            public boolean implies(java.security.Permission permission) {
//                // Запрещаем опасные операции
//                if (permission instanceof java.lang.RuntimePermission) {
//                    return false;
//                }
//                if (permission instanceof java.io.FilePermission) {
//                    return false;
//                }
//                if (permission instanceof java.net.SocketPermission) {
//                    return false;
//                }
//                return true;
//            }
//
//            @Override
//            public java.util.Enumeration<java.security.Permission> elements() {
//                return java.util.Collections.emptyEnumeration();
//            }
//        });
//
//        GroovyShell shell = new GroovyShell(
//                Thread.currentThread().getContextClassLoader(),
//                new groovy.lang.Binding(),
//                config
//        );
//
//        return shell;
//    }
//
//    /**
//     * ObjectMapper с настройками для безопасности.
//     */
//    @Bean
//    @Primary
//    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
//        com.fasterxml.jackson.databind.ObjectMapper mapper =
//                new com.fasterxml.jackson.databind.ObjectMapper();
//
//        // Отключаем опасные фичи
//        mapper.enable(
//                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
//        );
//        mapper.disable(
//                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
//        );
//        mapper.disable(
//                com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS
//        );
//
//        // Безопасные типы
//        mapper.activateDefaultTyping(
//                mapper.getPolymorphicTypeValidator(),
//                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL
//        );
//
//        return mapper;
//    }
//
//    /**
//     * TaskScheduler для периодических задач.
//     */
//    @Bean
//    public org.springframework.scheduling.TaskScheduler taskScheduler() {
//        return new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
//    }
//
//    /**
//     * CacheManager для кэширования правил.
//     */
//    @Bean
//    public org.springframework.cache.CacheManager cacheManager() {
//        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(
//                "rules", "userBalances", "leaderboard"
//        );
//    }
//}