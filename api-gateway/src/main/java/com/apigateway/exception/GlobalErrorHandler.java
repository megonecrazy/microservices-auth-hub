package com.apigateway.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/**
 * Global error handler for the API Gateway.
 *
 * IMPORTANT: Order is set to -2 (higher priority than Spring Boot's default at -1)
 * so we can control the JSON error format. However, we explicitly SKIP circuit breaker
 * exceptions so the CircuitBreaker gateway filter can handle them and forward
 * to the fallback URI instead.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        // ──────────────────────────────────────────────────────────
        // SKIP circuit breaker exceptions — let the CircuitBreaker
        // gateway filter handle them and route to the fallback URI.
        // If we handle them here, the fallback controller is bypassed.
        // ──────────────────────────────────────────────────────────
        if (isCircuitBreakerException(ex)) {
            log.debug("Passing circuit breaker exception to gateway filter chain: {}",
                    ex.getClass().getSimpleName());
            return Mono.error(ex);
        }

        // If response is already committed (e.g., partial write), don't interfere
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String errorLabel;
        String message;

        // ── JWT / Auth errors ──
        if (isJwtException(ex)) {
            status = HttpStatus.UNAUTHORIZED;
            errorLabel = "UNAUTHORIZED";
            message = "Invalid or expired token";
        } else if (isAccessDeniedException(ex)) {
            status = HttpStatus.FORBIDDEN;
            errorLabel = "FORBIDDEN";
            message = "Access denied";
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            errorLabel = status.getReasonPhrase().toUpperCase().replace(" ", "_");
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else {
            // ── Catch-all ──
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorLabel = "INTERNAL_SERVER_ERROR";
            message = "An unexpected error occurred";
            log.error("Unhandled gateway error on {}: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);
        }

        // Build response body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", errorLabel);
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("timestamp", LocalDateTime.now().toString());

        // Write response
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    // ── Exception type checks (by class name to avoid hard dependency issues) ──

    private boolean isCircuitBreakerException(Throwable ex) {
        return containsInChain(ex, "CallNotPermittedException")
                || containsInChain(ex, "TimeLimiterRuntimeException")
                || containsInChain(ex, "BulkheadFullException")
                || containsInChain(ex, "NoFallbackAvailableException");
    }

    private boolean isJwtException(Throwable ex) {
        return containsInChain(ex, "JwtException")
                || containsInChain(ex, "BadJwtException")
                || containsInChain(ex, "JwtValidationException");
    }

    private boolean isAccessDeniedException(Throwable ex) {
        return containsInChain(ex, "AccessDeniedException")
                || containsInChain(ex, "AuthenticationCredentialsNotFoundException");
    }

    /**
     * Walk the cause chain to check if any exception in the chain matches.
     * This is robust against wrapper exceptions like ReactiveException,
     * RetryExhaustedException, etc.
     */
    private boolean containsInChain(Throwable ex, String simpleName) {
        Throwable current = ex;
        int depth = 0;
        while (current != null && depth < 10) {
            if (current.getClass().getName().contains(simpleName)) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
}
