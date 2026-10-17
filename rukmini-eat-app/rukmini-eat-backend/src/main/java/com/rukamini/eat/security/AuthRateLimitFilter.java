package com.rukamini.eat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects excessive login/registration attempts from one client address with 429. */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/register");

    private final AuthRateLimiter limiter;

    public AuthRateLimitFilter(AuthRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !limiter.isEnabled() || !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(path(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long retryAfter = limiter.acquire(path(request) + "|" + request.getRemoteAddr());
        if (retryAfter == 0) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Too many attempts. Try again later.\"}");
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
