package com.example.codegate.auth.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingPatientSignupStore {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final Map<String, PendingSignup> pendingSignups = new ConcurrentHashMap<>();

    public String create(Long userAccountId) {
        cleanupExpired();
        String token = UUID.randomUUID().toString();
        pendingSignups.put(token, new PendingSignup(userAccountId, Instant.now().plus(TOKEN_TTL)));
        return token;
    }

    public Optional<Long> consume(String token) {
        PendingSignup pendingSignup = pendingSignups.remove(token);
        if (pendingSignup == null || pendingSignup.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(pendingSignup.userAccountId());
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        pendingSignups.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record PendingSignup(Long userAccountId, Instant expiresAt) {
    }
}
