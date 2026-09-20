package com.pricehunter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Разрешает браузерные запросы к API только от явно заданных frontend-доменов.
 * Пока список пуст, CORS выключен: локальная разработка использует proxy Vite, а production обязан
 * передать точные значения через {@code CORS_ALLOWED_ORIGINS}.
 */
@Configuration
public class CorsWebConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    /** Разбирает через запятую точные origin-адреса из конфигурации окружения. */
    public CorsWebConfiguration(@Value("${pricehunter.cors.allowed-origins:}") String rawAllowedOrigins) {
        this.allowedOrigins = Arrays.stream(rawAllowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    /** Регистрирует CORS для API, не используя небезопасный wildcard вместе с credentials. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
