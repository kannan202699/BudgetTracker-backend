package com.budgettracker.budget_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_REQUESTS    = 10;
    private static final int MAX_REGISTER_REQUESTS = 5;
    private static final long WINDOW_MS            = 300_000L; // 5 minutes

    private final ConcurrentHashMap<String, List<Long>> loginMap    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Long>> registerMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();

        if ("/auth/token".equals(path) && "POST".equalsIgnoreCase(req.getMethod())) {
            if (isLimited(loginMap, clientIp(req), MAX_LOGIN_REQUESTS)) {
                log.warn("Rate limit exceeded for login from IP: {}", clientIp(req));
                sendRateLimitResponse(res);
                return;
            }
        } else if ("/auth/register/user".equals(path) && "POST".equalsIgnoreCase(req.getMethod())) {
            if (isLimited(registerMap, clientIp(req), MAX_REGISTER_REQUESTS)) {
                log.warn("Rate limit exceeded for registration from IP: {}", clientIp(req));
                sendRateLimitResponse(res);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private boolean isLimited(ConcurrentHashMap<String, List<Long>> map, String ip, int max) {
        long now = System.currentTimeMillis();
        final boolean[] limited = {false};

        map.compute(ip, (k, list) -> {
            if (list == null) list = new ArrayList<>();
            long windowStart = now - WINDOW_MS;
            list.removeIf(t -> t < windowStart);
            if (list.size() >= max) {
                limited[0] = true;
            } else {
                list.add(now);
            }
            return list;
        });

        return limited[0];
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse res) throws IOException {
        res.setStatus(429);
        res.setContentType("application/json");
        res.getWriter().write(
            "{\"status\":429,\"error\":\"Too Many Requests\"," +
            "\"message\":\"Too many attempts. Please wait a few minutes and try again.\"}"
        );
    }
}
