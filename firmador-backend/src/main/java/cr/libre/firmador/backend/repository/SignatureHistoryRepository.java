package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.SignatureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignatureHistoryRepository extends JpaRepository<SignatureHistory, UUID> {
    List<SignatureHistory> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    List<SignatureHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
