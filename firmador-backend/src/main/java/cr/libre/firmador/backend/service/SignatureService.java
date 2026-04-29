package cr.libre.firmador.backend.service;

import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.model.Document;
import cr.libre.firmador.backend.repository.DocumentRepository;
import cr.libre.firmador.backend.repository.PendingDocumentRepository;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final DocumentRepository documentRepository;
    private final PendingDocumentRepository pendingDocumentRepository;
    private final DocumentService documentService;
    private final NotificationService notificationService;

    public ToBeSignedDTO prepareSignature(UUID documentId, UUID userId) {
        if (!pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            throw new IllegalStateException("User is not allowed to sign this document");
        }

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalStateException("Document not found"));

        ToBeSignedDTO dto = new ToBeSignedDTO();
        dto.setBytes(("SIGN:" + document.getId() + ":" + document.getTitle()).getBytes(StandardCharsets.UTF_8));
        return dto;
    }

    @Transactional
    public DocumentDTO completeSignature(UUID documentId, String signatureValue, String comments, UUID userId) {
        if (!pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            throw new IllegalStateException("User is not allowed to sign this document");
        }

        if (signatureValue == null || signatureValue.isBlank()) {
            throw new IllegalArgumentException("Signature value is required");
        }

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalStateException("Document not found"));

        document.setStatus("in_progress");
        documentRepository.save(document);
        documentService.moveToNextStep(documentId);

        return DocumentDTO.from(documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalStateException("Document not found after signature completion")));
    }

    @Transactional
    public void rejectDocument(UUID documentId, String reason, UUID userId) {
        if (!pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            throw new IllegalStateException("User is not allowed to reject this document");
        }

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalStateException("Document not found"));

        document.setStatus("rejected");
        documentRepository.save(document);
        pendingDocumentRepository.deleteByDocumentId(documentId);

        notificationService.notifyDocumentRejected(
            document.getCreatedBy(),
            documentId,
            document.getTitle(),
            reason
        );
    }
}

// Made with Bob
