package org.akusher.crmfortutor.security;

import org.akusher.crmfortutor.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION = 900_000L; // 15 min
    private static final long REFRESH_EXPIRATION = 604_800_000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("generateAccessToken creates token with ACCESS tokenType")
    void generateAccessToken_HasAccessTokenType() {
        String token = jwtTokenProvider.generateAccessToken(1L, "user@example.com", Role.ROLE_TUTOR);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("ACCESS");
    }

    @Test
    @DisplayName("generateRefreshToken creates token with REFRESH tokenType")
    void generateRefreshToken_HasRefreshTokenType() {
        String token = jwtTokenProvider.generateRefreshToken("user@example.com");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("validateToken returns false for invalid token")
    void validateToken_InvalidToken_ReturnsFalse() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();
    }
}
