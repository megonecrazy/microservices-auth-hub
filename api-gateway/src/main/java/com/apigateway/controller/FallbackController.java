package com.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    /**
     * Catch-all fallback for Auth Service.
     *
     * Uses @RequestMapping WITHOUT a method restriction so it handles
     * GET, POST, PUT, DELETE, PATCH — whatever the original request was.
     * The circuit breaker forwards the request preserving the original HTTP method,
     * so restricting to @PostMapping would cause a 405 for non-POST requests.
     */
    @RequestMapping(value = {"/auth", "/auth/**"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(ServerHttpRequest request) {
        log.warn("Auth fallback triggered for {} {}", request.getMethod(), request.getURI());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "SERVICE_UNAVAILABLE");
        body.put("message", "Auth Service is currently unavailable. Please try again later.");
        body.put("path", request.getPath().value());
        body.put("timestamp", LocalDateTime.now().toString());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}