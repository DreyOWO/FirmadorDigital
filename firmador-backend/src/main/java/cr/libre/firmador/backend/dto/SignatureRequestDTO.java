package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SignatureRequestDTO {
    private UUID documentId;
}
