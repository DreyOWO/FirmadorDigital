package cr.libre.firmador.backend.service;

import cr.libre.firmador.backend.dto.LoginRequest;
import cr.libre.firmador.backend.dto.LoginResponse;
import cr.libre.firmador.backend.dto.UserProfileDTO;
import cr.libre.firmador.backend.model.User;
import cr.libre.firmador.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService.JwtSupport jwtSupport;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsTokenForValidCredentials() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setRole("admin");
        user.setIsActive(true);
        user.setPasswordHash("hashed-password");

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed-password")).thenReturn(true);
        when(jwtSupport.generateToken(userId, "user@example.com", "ADMIN")).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("ADMIN", response.getRole());
        assertEquals("Test User", response.getFullName());
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setIsActive(true);
        user.setPasswordHash("hashed-password");

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authService.login(request));

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void getProfileReturnsNormalizedRole() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setFullName("Profile User");
        user.setRole("user");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileDTO profile = authService.getProfile(userId);

        assertEquals(userId, profile.getId());
        assertEquals("user@example.com", profile.getEmail());
        assertEquals("Profile User", profile.getFullName());
        assertEquals("USER", profile.getRole());
    }
}

// Made with Bob
