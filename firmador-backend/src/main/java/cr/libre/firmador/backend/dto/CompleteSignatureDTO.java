package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CompleteSignatureDTO {
    private UUID documentId;
    private String signatureValue;
    private String comments;
}
