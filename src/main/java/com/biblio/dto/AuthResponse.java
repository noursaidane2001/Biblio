package com.biblio.dto;

import java.util.Map;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    Map<String, Object> user
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, Map<String, Object> user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
