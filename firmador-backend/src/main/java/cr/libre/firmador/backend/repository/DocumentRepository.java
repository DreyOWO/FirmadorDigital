package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("SELECT DISTINCT d FROM Document d WHERE d.createdBy = :userId " +
           "OR d.id IN (SELECT sh.documentId FROM SignatureHistory sh WHERE sh.userId = :userId)")
    List<Document> findByCreatedByOrSignedBy(@Param("userId") UUID userId);
}
