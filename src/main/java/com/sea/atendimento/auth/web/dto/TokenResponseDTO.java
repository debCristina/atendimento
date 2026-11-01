package com.sea.atendimento.auth.web.dto;

import java.time.Duration;

public record TokenResponseDTO(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponseDTO bearer(String token, Duration expiration) {
        return new TokenResponseDTO(token, "Bearer", expiration.toSeconds());
    }
}