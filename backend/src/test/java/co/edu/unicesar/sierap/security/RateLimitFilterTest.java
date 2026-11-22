package co.edu.unicesar.sierap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import co.edu.unicesar.sierap.config.RateLimitProperties;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Pruebas unitarias del límite de intentos por IP (ADR-021), con un reloj controlado.
 */
class RateLimitFilterTest {

    private MutableClock clock;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-22T10:00:00Z"));
        filter = filterWith(true);
    }

    private RateLimitFilter filterWith(boolean enabled) {
        RateLimitProperties properties = new RateLimitProperties(enabled, 60, 3, 2, 2, 2);
        SecurityErrorWriter writer = new SecurityErrorWriter(JsonMapper.builder().findAndAddModules().build(), clock);
        return new RateLimitFilter(properties, writer, clock);
    }

    private MockHttpServletResponse call(RateLimitFilter target, String method, String uri, String ip)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        target.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private int post(String uri, String ip) throws Exception {
        return call(filter, "POST", uri, ip).getStatus();
    }

    @Test
    void letsRequestsPassUpToTheLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(post("/api/auth/login", "1.1.1.1")).isEqualTo(HttpServletResponse.SC_OK);
        }
    }

    @Test
    void rejectsWith429AndRetryAfterWhenTheLimitIsExceeded() throws Exception {
        for (int i = 0; i < 3; i++) {
            post("/api/auth/login", "1.1.1.1");
        }
        clock.advance(Duration.ofSeconds(20));
        MockHttpServletResponse response = call(filter, "POST", "/api/auth/login", "1.1.1.1");
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("40");
        assertThat(response.getContentAsString())
                .contains("\"status\":429")
                .contains("Demasiados intentos. Espera un momento e inténtalo de nuevo.")
                .contains("/api/auth/login");
    }

    @Test
    void resetsWhenTheWindowPasses() throws Exception {
        for (int i = 0; i < 4; i++) {
            post("/api/auth/login", "1.1.1.1");
        }
        assertThat(post("/api/auth/login", "1.1.1.1")).isEqualTo(429);
        clock.advance(Duration.ofSeconds(60));
        assertThat(post("/api/auth/login", "1.1.1.1")).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void countsEachIpSeparately() throws Exception {
        for (int i = 0; i < 4; i++) {
            post("/api/auth/login", "1.1.1.1");
        }
        assertThat(post("/api/auth/login", "1.1.1.1")).isEqualTo(429);
        assertThat(post("/api/auth/login", "2.2.2.2")).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void countsEachRouteSeparatelyWithItsOwnLimit() throws Exception {
        assertThat(post("/api/auth/register", "1.1.1.1")).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(post("/api/auth/register", "1.1.1.1")).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(post("/api/auth/register", "1.1.1.1")).isEqualTo(429);
        assertThat(post("/api/auth/login", "1.1.1.1")).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void doesNotAffectOtherRoutesOrMethods() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(post("/api/groups", "1.1.1.1")).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(call(filter, "GET", "/api/auth/login", "1.1.1.1").getStatus())
                    .isEqualTo(HttpServletResponse.SC_OK);
        }
    }

    @Test
    void doesNothingWhenDisabled() throws Exception {
        RateLimitFilter disabled = filterWith(false);
        for (int i = 0; i < 20; i++) {
            assertThat(call(disabled, "POST", "/api/auth/login", "1.1.1.1").getStatus())
                    .isEqualTo(HttpServletResponse.SC_OK);
        }
    }

    /** Reloj que solo avanza cuando la prueba lo indica. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
