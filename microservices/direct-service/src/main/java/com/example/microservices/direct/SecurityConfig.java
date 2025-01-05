package com.example.microservices.direct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorizeExchangeSpec) ->
                        authorizeExchangeSpec
                                .pathMatchers("/openapi/**").permitAll()
                                .pathMatchers("/webjars/**").permitAll()
                                .pathMatchers("/actuator/**").permitAll()
                                .pathMatchers(POST, "/api/**").hasAuthority("SCOPE_general:write")
                                .pathMatchers(DELETE, "/api/**").hasAuthority("SCOPE_general:write")
                                .pathMatchers(GET, "/api/**").hasAuthority("SCOPE_general:read")
                                .anyExchange().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
