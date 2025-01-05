package com.example.microservices.direct.service;

import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.direct.StoreDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.List;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class StoreDirectServiceImpl implements StoreDirectService {
    private final SecurityContext nullSecCtx = new SecurityContextImpl();
    private final StoreDirectIntegration integration;

    @Autowired
    public StoreDirectServiceImpl(StoreDirectIntegration integration) {
        this.integration = integration;
    }

    @Override
    public Mono<Store> getStore(int storeId) {
        return integration.getStore(storeId)
                .doOnError(ex -> log.warn("getStore failed: {}", ex.toString()))
                .log(log.getName(), FINE);
    }

    @Override
    public Mono<Void> createStore(Store body) {
        try {
            log.debug("create a new store for storeId: {}", body.getStoreId());
            Store store = new Store(body.getStoreId(), body.getLocation(), body.getCapacity(), body.getUsedCapacity(), null);

            return integration.createStore(store)
                    .doOnError(ex -> log.warn("createStore failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createStore failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> updateStore(Store body) {
        try {
            log.debug("update the store for storeId: {}", body.getStoreId());
            Store store = new Store(body.getStoreId(), body.getLocation(), body.getCapacity(), body.getUsedCapacity(), null);

            return integration.updateStore(store)
                    .doOnError(ex -> log.warn("createStore failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createStore failed: {}", re.toString());
            throw re;
        }
    }

/*    @Override
    public Mono<Void> deleteStore(int storeId) {
        try {
            return integration.deleteStore(storeId)
                    .doOnError(ex -> log.warn("deleteStore failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("deleteStore failed: {}", re.toString());
            throw re;
        }
    }*/

    @Override
    public Mono<Void> deleteStore(int storeId) {
        return getLogAuthorizationInfoMono()
                .flatMap(sc -> integration.deleteStore(storeId)
                        .doOnError(ex -> log.warn("deleteStore failed: {}", ex.toString()))
                        .onErrorResume(Mono::error)
                        .then()
                );
    }


    private Mono<SecurityContext> getLogAuthorizationInfoMono() {
        return getSecurityContextMono().doOnNext(this::logAuthorizationInfo);
    }

    private Mono<SecurityContext> getSecurityContextMono() {
        return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSecCtx);
    }

    private void logAuthorizationInfo(SecurityContext sc) {
        if (sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
            Jwt jwtToken = ((JwtAuthenticationToken) sc.getAuthentication()).getToken();
            logAuthorizationInfo(jwtToken);
        } else {
            log.warn("No JWT based Authentication supplied, running tests are we?");
        }
    }

    private void logAuthorizationInfo(Jwt jwt) {
        if (jwt == null) {
            log.warn("No JWT supplied, running tests are we?");
        } else {
            if (log.isDebugEnabled()) {
                URL issuer = jwt.getIssuer();
                List<String> audience = jwt.getAudience();
                Object subject = jwt.getClaims().get("sub");
                Object scopes = jwt.getClaims().get("scope");
                Object expires = jwt.getClaims().get("exp");

                log.debug("Authorization info: Subject: {}, scopes: {}, expires {}: issuer: {}, audience: {}", subject, scopes, expires, issuer, audience);
            }
        }
    }
}
