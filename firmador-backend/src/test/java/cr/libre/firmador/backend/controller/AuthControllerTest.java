package cr.libre.firmador.backend.controller;

import cr.libre.firmador.backend.dto.LoginRequest;
import cr.libre.firmador.backend.dto.LoginResponse;
import cr.libre.firmador.backend.dto.UserProfileDTO;
import cr.libre.firmador.backend.security.UserPrincipal;
import cr.libre.firmador.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    void loginReturnsOkResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret");

        LoginResponse expected = new LoginResponse(
            "jwt-token",
            "Bearer",
            "user@example.com",
            "Test User",
            "USER"
        );

        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void meReturnsCurrentUserProfile() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "ADMIN");
        UserProfileDTO expected = new UserProfileDTO(
            userId,
            "admin@example.com",
            "Admin User",
            "ADMIN"
        );

        when(authService.getProfile(userId)).thenReturn(expected);

        ResponseEntity<UserProfileDTO> response = authController.me(principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }
}

// Made with Bob
