package cr.libre.firmador.backend.service;

import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.model.Document;
import cr.libre.firmador.backend.repository.DocumentRepository;
import cr.libre.firmador.backend.repository.PendingDocumentRepository;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PendingDocumentRepository pendingDocumentRepository;

    @Mock
    private DocumentService documentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SignatureService signatureService;

    @Test
    void prepareSignatureReturnsBytesForAuthorizedUser() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        document.setTitle("Agreement");

        when(pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        ToBeSignedDTO result = signatureService.prepareSignature(documentId, userId);

        assertArrayEquals(
            ("SIGN:" + documentId + ":Agreement").getBytes(StandardCharsets.UTF_8),
            result.getBytes()
        );
    }

    @Test
    void prepareSignatureRejectsUnauthorizedUser() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(false);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> signatureService.prepareSignature(documentId, userId)
        );

        assertEquals("User is not allowed to sign this document", exception.getMessage());
    }

    @Test
    void completeSignatureRequiresSignatureValue() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> signatureService.completeSignature(documentId, " ", null, userId)
        );

        assertEquals("Signature value is required", exception.getMessage());
    }

    @Test
    void completeSignatureMovesWorkflowAndReturnsUpdatedDocument() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document document = new Document();
        document.setId(documentId);
        document.setStatus("pending");

        Document updatedDocument = new Document();
        updatedDocument.setId(documentId);
        updatedDocument.setStatus("completed");

        when(pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document), Optional.of(updatedDocument));

        DocumentDTO result = signatureService.completeSignature(documentId, "signed-value", "ok", userId);

        assertEquals("completed", result.getStatus());
        verify(documentService).moveToNextStep(documentId);
    }

    @Test
    void rejectDocumentMarksDocumentRejectedAndNotifiesCreator() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Document document = new Document();
        document.setId(documentId);
        document.setCreatedBy(creatorId);
        document.setTitle("Contract");

        when(pendingDocumentRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(true);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        signatureService.rejectDocument(documentId, "Invalid content", userId);

        assertEquals("rejected", document.getStatus());
        verify(pendingDocumentRepository).deleteByDocumentId(documentId);
        verify(notificationService).notifyDocumentRejected(creatorId, documentId, "Contract", "Invalid content");
    }
}

// Made with Bob
