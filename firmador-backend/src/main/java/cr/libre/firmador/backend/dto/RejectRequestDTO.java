package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class RejectRequestDTO {
    private UUID documentId;
    private String reason;
}
