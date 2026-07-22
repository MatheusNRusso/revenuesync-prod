package com.mtnrs.revenuesync.infra.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private RateLimitConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String clientIP = getClientIP(request);

        // Skip rate limiting for excluded paths (actuator, static, docs)
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitType rateLimitType = determineRateLimitType(requestURI);
        boolean allowed = rateLimiter.isAllowed(clientIP, rateLimitType);

        if (!allowed) {
            sendRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            return xForwardedForHeader.split(",")[0].trim();
        }

        String xRealIPHeader = request.getHeader("X-Real-IP");
        if (xRealIPHeader != null && !xRealIPHeader.isEmpty()) {
            return xRealIPHeader;
        }

        return request.getRemoteAddr();
    }

    private RateLimitType determineRateLimitType(String uri) {
        if (uri.startsWith("/auth/") ||
                uri.startsWith("/api/auth/") ||
                uri.contains("/login") ||
                uri.contains("/register")) {
            return RateLimitType.AUTH;
        }
        return RateLimitType.PUBLIC;
    }

    private boolean isExcludedPath(String uri) {
        return uri.startsWith("/actuator") ||
                uri.startsWith("/assets/") ||
                uri.startsWith("/static/") ||
                uri.equals("/favicon.ico") ||
                uri.startsWith("/api-docs") ||
                uri.startsWith("/swagger") ||
                uri.startsWith("/v3/api-docs");
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Rate limit exceeded. Please try again later.");
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}