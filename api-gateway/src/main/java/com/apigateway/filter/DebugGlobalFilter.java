package com.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Profile("dev")
public class DebugGlobalFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(DebugGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("Gateway request: {} {}", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI());
        return chain.filter(exchange);
    }
}