package com.pricehunter.config;

import com.pricehunter.shared.ApiRoutes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Закрывает административные маршруты bearer-токеном в обычном VPS-развёртывании.
 * Пустой токен оставляет локальную разработку и существующие тесты без авторизации;
 * production compose-файл, напротив, не запустится без заданного секрета.
 */
@Component
public class AdminApiTokenFilter extends OncePerRequestFilter {

    private final byte[] expectedToken;

    /** Загружает секрет только из переменной окружения, не из исходного кода или базы данных. */
    public AdminApiTokenFilter(@Value("${pricehunter.admin.api-token:}") String adminApiToken) {
        this.expectedToken = adminApiToken.getBytes(StandardCharsets.UTF_8);
    }

    /** Проверяет только точный административный префикс и пропускает остальные публичные API. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals(ApiRoutes.Admin.ROOT) || path.startsWith(ApiRoutes.Admin.ROOT + "/"));
    }

    /** Отклоняет административный запрос без корректного токена до вызова контроллера. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (expectedToken.length == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        byte[] suppliedToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()).getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        if (!MessageDigest.isEqual(expectedToken, suppliedToken)) {
            response.setHeader("WWW-Authenticate", "Bearer");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Admin bearer token is required");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
