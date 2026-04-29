package cr.libre.firmador.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "1234567890123456789012345678901212345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 60000L);
    }

    @Test
    void generateAndValidateToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.generateToken(userId, "user@example.com", "ADMIN");

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }
}

// Made with Bob
