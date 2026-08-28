package com.pricehunter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
/** Точка запуска Spring Boot-приложения Price Hunter и регистрации плановых задач. */
public class PriceHunterApplication {

    /** Запускает контейнер Spring и HTTP-приложение. */
    public static void main(String[] args) {
        SpringApplication.run(PriceHunterApplication.class, args);
    }
}
