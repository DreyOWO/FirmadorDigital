package cr.libre.firmador.backend.service;

import cr.libre.firmador.backend.dto.LoginRequest;
import cr.libre.firmador.backend.dto.LoginResponse;
import cr.libre.firmador.backend.dto.UserProfileDTO;
import cr.libre.firmador.backend.model.User;
import cr.libre.firmador.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtSupport jwtSupport;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("User is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String normalizedRole = normalizeRole(user.getRole());
        String token = jwtSupport.generateToken(user.getId(), user.getEmail(), normalizedRole);

        return new LoginResponse(
            token,
            "Bearer",
            user.getEmail(),
            user.getFullName(),
            normalizedRole
        );
    }

    public UserProfileDTO getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(normalizeRole(user.getRole()));
        return UserProfileDTO.from(user);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }

        return role.trim().toUpperCase(Locale.ROOT);
    }

    public interface JwtSupport {
        String generateToken(UUID userId, String email, String role);
    }
}

// Made with Bob