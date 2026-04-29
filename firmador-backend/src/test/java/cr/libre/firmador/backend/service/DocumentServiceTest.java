package cr.libre.firmador.backend.service;

import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.model.Document;
import cr.libre.firmador.backend.model.PendingDocument;
import cr.libre.firmador.backend.model.WorkflowStep;
import cr.libre.firmador.backend.repository.DocumentRepository;
import cr.libre.firmador.backend.repository.PendingDocumentRepository;
import cr.libre.firmador.backend.repository.WorkflowStepRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PendingDocumentRepository pendingRepository;

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SupabaseStorageService storageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void getPendingDocumentsReturnsMappedDtos() {
        UUID userId = UUID.randomUUID();
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Contract");

        PendingDocument pendingDocument = new PendingDocument();
        pendingDocument.setDocument(document);

        when(pendingRepository.findByUserIdOrderByAssignedAtDesc(userId)).thenReturn(List.of(pendingDocument));

        List<DocumentDTO> result = documentService.getPendingDocuments(userId);

        assertEquals(1, result.size());
        assertEquals(document.getId(), result.get(0).getId());
        assertEquals("Contract", result.get(0).getTitle());
    }

    @Test
    void getDocumentThrowsWhenUserCannotView() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(pendingRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> documentService.getDocument(documentId, userId));

        assertEquals("Access denied", exception.getMessage());
    }

    @Test
    void getDocumentContentDelegatesToStorageService() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        document.setFilePath("documents/test.pdf");
        ByteArrayResource resource = new ByteArrayResource("pdf".getBytes());

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(storageService.getDocument("documents/test.pdf")).thenReturn(resource);

        ByteArrayResource result = (ByteArrayResource) documentService.getDocumentContent(documentId);

        assertArrayEquals("pdf".getBytes(), result.getByteArray());
    }

    @Test
    void createDocumentUploadsAndAssignsFirstSigner() {
        UUID workflowId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(storageService.uploadDocument(multipartFile)).thenReturn("documents/generated.pdf");
        when(multipartFile.getSize()).thenReturn(123L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            saved.setId(documentId);
            return saved;
        });

        WorkflowStep firstStep = new WorkflowStep();
        firstStep.setWorkflowId(workflowId);
        firstStep.setStepOrder(1);
        firstStep.setUserId(UUID.randomUUID());

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(new Document() {{
            setId(documentId);
            setWorkflowId(workflowId);
            setCurrentStep(1);
            setTitle("My PDF");
        }}));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(workflowId, 1)).thenReturn(Optional.of(firstStep));

        DocumentDTO result = documentService.createDocument(
            multipartFile,
            workflowId,
            "My PDF",
            "Description",
            createdBy
        );

        assertNotNull(result.getId());
        assertEquals("My PDF", result.getTitle());
        verify(notificationService).notifyPendingSignature(firstStep.getUserId(), documentId, "My PDF");
    }

    @Test
    void moveToNextStepMarksDocumentCompletedWhenNoMoreSteps() {
        UUID documentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        Document document = new Document();
        document.setId(documentId);
        document.setWorkflowId(workflowId);
        document.setCurrentStep(1);
        document.setTitle("Final Doc");
        document.setCreatedBy(creatorId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(workflowId, 2)).thenReturn(Optional.empty());

        documentService.moveToNextStep(documentId);

        assertEquals("completed", document.getStatus());
        assertNotNull(document.getCompletedAt());
        verify(pendingRepository).deleteByDocumentId(documentId);
        verify(notificationService).notifyDocumentCompleted(creatorId, documentId, "Final Doc");
    }

    @Test
    void moveToNextStepAdvancesAndAssignsNextSigner() {
        UUID documentId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID nextUserId = UUID.randomUUID();

        Document document = new Document();
        document.setId(documentId);
        document.setWorkflowId(workflowId);
        document.setCurrentStep(1);
        document.setTitle("Needs Next Step");

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setWorkflowId(workflowId);
        nextStep.setStepOrder(2);
        nextStep.setUserId(nextUserId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(workflowStepRepository.findByWorkflowIdAndStepOrder(workflowId, 2)).thenReturn(Optional.of(nextStep));

        documentService.moveToNextStep(documentId);

        assertEquals(2, document.getCurrentStep());
        verify(notificationService).notifyPendingSignature(nextUserId, documentId, "Needs Next Step");
    }
}

// Made with Bob
