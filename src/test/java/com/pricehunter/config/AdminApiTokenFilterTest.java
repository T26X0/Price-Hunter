package com.pricehunter.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверяет, что VPS-защита закрывает только административные маршруты. */
class AdminApiTokenFilterTest {

    /** Отклоняет административный запрос без заголовка с секретом. */
    @Test
    void rejectsAdminRequestWithoutBearerToken() throws Exception {
        AdminApiTokenFilter filter = new AdminApiTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/parser-sources");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, untouchedChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    /** Пропускает административный запрос с корректным bearer-токеном. */
    @Test
    void acceptsAdminRequestWithCorrectBearerToken() throws Exception {
        AdminApiTokenFilter filter = new AdminApiTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/parser-sources");
        request.addHeader("Authorization", "Bearer secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedApplication = new AtomicBoolean();

        filter.doFilter(request, response, reachedChain(reachedApplication));

        assertThat(reachedApplication).isTrue();
    }

    /** Не требует production-секрет для публичного каталожного API. */
    @Test
    void acceptsPublicRequestWithoutBearerToken() throws Exception {
        AdminApiTokenFilter filter = new AdminApiTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cities");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedApplication = new AtomicBoolean();

        filter.doFilter(request, response, reachedChain(reachedApplication));

        assertThat(reachedApplication).isTrue();
    }

    /** Создаёт цепочку, которая сигнализирует, что фильтр передал запрос приложению. */
    private static FilterChain reachedChain(AtomicBoolean reachedApplication) {
        return (request, response) -> reachedApplication.set(true);
    }

    /** Создаёт цепочку, которая упадёт, если неавторизованный запрос был ошибочно пропущен. */
    private static FilterChain untouchedChain() {
        return (request, response) -> {
            throw new AssertionError("Unauthorized request reached the application");
        };
    }
}
