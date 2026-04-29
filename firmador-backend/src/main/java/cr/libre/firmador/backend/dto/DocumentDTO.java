package cr.libre.firmador.backend.dto;

import cr.libre.firmador.backend.model.Document;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DocumentDTO {
    private UUID id;
    private String title;
    private String description;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private UUID workflowId;
    private Integer currentStep;
    private String status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    public static DocumentDTO from(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setDescription(document.getDescription());
        dto.setFilePath(document.getFilePath());
        dto.setFileSize(document.getFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setWorkflowId(document.getWorkflowId());
        dto.setCurrentStep(document.getCurrentStep());
        dto.setStatus(document.getStatus());
        dto.setCreatedBy(document.getCreatedBy());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        dto.setCompletedAt(document.getCompletedAt());
        return dto;
    }
}
