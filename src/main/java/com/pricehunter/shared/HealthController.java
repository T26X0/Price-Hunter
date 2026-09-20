package com.pricehunter.shared;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Предоставляет лёгкую проверку готовности процесса без обращения к базе данных. */
@RestController
public class HealthController {

    /** Подтверждает, что Spring-приложение запущено и принимает HTTP-запросы. */
    @GetMapping("/healthz")
    public ResponseEntity<Void> health() {
        return ResponseEntity.noContent().build();
    }
}
