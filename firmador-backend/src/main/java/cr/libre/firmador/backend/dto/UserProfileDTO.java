package cr.libre.firmador.backend.dto;

import cr.libre.firmador.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserProfileDTO {
    private UUID id;
    private String email;
    private String fullName;
    private String role;

    public static UserProfileDTO from(User user) {
        return new UserProfileDTO(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );
    }
}

// Made with Bob