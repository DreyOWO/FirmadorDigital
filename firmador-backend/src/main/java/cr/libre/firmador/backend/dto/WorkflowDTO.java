package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class WorkflowDTO {
    private UUID id;
    private String name;
    private String description;
    private List<WorkflowStepDTO> steps;
    
    @Data
    public static class WorkflowStepDTO {
        private Integer stepOrder;
        private UUID userId;
        private String actionType;
        private Boolean isRequired;
    }
}
