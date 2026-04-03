package com.nexpath.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter to prevent API abuse.
 * Production: replace with Redis-based bucket4j or resilience4j.
 *
 * Public endpoints: max 30 requests/minute per IP
 * Auth endpoints:   max 10 requests/minute per IP (brute-force guard)
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int GLOBAL_LIMIT   = 120; // per minute per IP
    private static final int AUTH_LIMIT     = 10;  // stricter for /api/auth/**
    private static final int PUBLIC_AI_LIMIT = 20; // for /api/chat/public

    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ip  = getClientIp(req);
        String uri = req.getRequestURI();

        int limit = GLOBAL_LIMIT;
        if (uri.startsWith("/api/auth/")) {
            limit = AUTH_LIMIT;
        } else if (uri.startsWith("/api/chat/public")) {
            limit = PUBLIC_AI_LIMIT;
        }

        RequestCounter counter = counters.computeIfAbsent(ip + "|" + limit, k -> new RequestCounter());

        if (counter.increment(limit)) {
            resp.setStatus(429);
            resp.setContentType("application/json");
            resp.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please slow down.\",\"status\":429}"
            );
            return;
        }

        // Security Headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resp.setHeader("Permissions-Policy", "geolocation=(), microphone=()");

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    // ── Inner class: sliding-window counter per minute ──
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean increment(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart > TimeUnit.MINUTES.toMillis(1)) {
                windowStart = now;
                count.set(1);
                return false;
            }
            return count.incrementAndGet() > limit;
        }
    }
}
