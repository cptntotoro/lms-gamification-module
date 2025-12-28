package ru.misis.gamification.config;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class GroovyConfig {

    @Bean
    public GroovyShell groovyShell(CompilerConfiguration compilerConfiguration) {
        return new GroovyShell(compilerConfiguration);
    }

    @Bean
    public CompilerConfiguration groovyCompilerConfiguration() {
        CompilerConfiguration config = new CompilerConfiguration();

        // Настройки безопасности
        SecureASTCustomizer secure = getSecureASTCustomizer();

        // Импорты по умолчанию
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStaticImport("java.lang.Math", "sqrt");
        imports.addStaticImport("java.lang.Math", "log");
        imports.addStaticImport("java.lang.Math", "pow");
        imports.addStaticImport("java.lang.Math", "round");

        config.addCompilationCustomizers(secure, imports);
        return config;
    }

    private static @NonNull SecureASTCustomizer getSecureASTCustomizer() {
        SecureASTCustomizer secure = new SecureASTCustomizer();
        secure.setClosuresAllowed(true); // Разрешены замыкания
        secure.setMethodDefinitionAllowed(false); // Запрещено создание методов
        secure.setAllowedImports(Arrays.asList(
                "java.lang.Math",
                "java.util.Map",
                "java.util.List",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Double",
                "java.lang.String"
        ));

        secure.setPackageAllowed(false); // Запрещены объявления пакетов
        return secure;
    }
}