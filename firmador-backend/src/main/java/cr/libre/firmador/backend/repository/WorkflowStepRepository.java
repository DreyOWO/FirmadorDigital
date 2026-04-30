package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {
    Optional<WorkflowStep> findByWorkflowIdAndStepOrder(UUID workflowId, Integer stepOrder);
}
