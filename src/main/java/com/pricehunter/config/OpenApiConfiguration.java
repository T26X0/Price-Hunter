package com.pricehunter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Настраивает название и назначение интерактивной OpenAPI-документации Price Hunter. */
@Configuration
public class OpenApiConfiguration {

    /** Возвращает метаданные, отображаемые в Swagger UI и OpenAPI JSON. */
    @Bean
    public OpenAPI priceHunterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Price Hunter API")
                .version("v1")
                .description("REST API каталога, предложений и истории цен Price Hunter."));
    }
}
