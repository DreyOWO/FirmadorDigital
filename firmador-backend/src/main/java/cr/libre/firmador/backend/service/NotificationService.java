package cr.libre.firmador.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    public void notifyPendingSignature(UUID userId, UUID documentId, String documentTitle) {
        log.info("Pending signature notification queued for user {} and document {}", userId, documentId);
    }

    public void notifyDocumentCompleted(UUID userId, UUID documentId, String documentTitle) {
        log.info("Document completed notification queued for user {} and document {}", userId, documentId);
    }

    public void notifyDocumentRejected(UUID userId, UUID documentId, String documentTitle, String reason) {
        log.info("Document rejected notification queued for user {} and document {}", userId, documentId);
    }
}

// Made with Bob
