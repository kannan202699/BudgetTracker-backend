package com.budgettracker.budget_app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void nonAuthPath_alwaysPassesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/transactions");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void loginRequests_withinLimit_passThrough() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilterInternal(req, res, chain);
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void loginRequests_exceedLimit_returns429() throws Exception {
        // Use unique IP to avoid interference with other tests
        String ip = "10.0.1.1";
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
            req.setRemoteAddr(ip);
            filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        // 21st request should be rate-limited
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void registerRequests_exceedLimit_returns429() throws Exception {
        String ip = "10.0.2.1";
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/register/user");
            req.setRemoteAddr(ip);
            filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/register/user");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void xForwardedForHeader_usedAsIp() throws Exception {
        String ip = "10.0.3.1";
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
            req.addHeader("X-Forwarded-For", ip + ", 192.168.1.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
        req.addHeader("X-Forwarded-For", ip + ", 192.168.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void xRealIpHeader_usedAsIp() throws Exception {
        String ip = "10.0.4.1";
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
            req.addHeader("X-Real-IP", ip);
            filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
        req.addHeader("X-Real-IP", ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void differentIps_separateCounters() throws Exception {
        // Exhaust limit for ip A
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
            req.setRemoteAddr("10.0.5.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        // ip B should still be allowed
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
        req.setRemoteAddr("10.0.5.2");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void loginGetRequest_notRateLimited() throws Exception {
        // GET to /auth/token should not trigger rate limiting
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/auth/token");
        req.setRemoteAddr("10.0.6.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
