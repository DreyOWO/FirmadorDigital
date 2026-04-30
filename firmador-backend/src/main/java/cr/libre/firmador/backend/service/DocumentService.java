package cr.libre.firmador.backend.service;

import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.model.*;
import cr.libre.firmador.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final PendingDocumentRepository pendingRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final NotificationService notificationService;
    private final SupabaseStorageService storageService;
    
    public List<DocumentDTO> getPendingDocuments(UUID userId) {
        List<PendingDocument> pending = pendingRepository
            .findByUserIdOrderByAssignedAtDesc(userId);
        
        return pending.stream()
            .map(pd -> DocumentDTO.from(pd.getDocument()))
            .collect(Collectors.toList());
    }
    
    public DocumentDTO getDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        if (!canUserViewDocument(documentId, userId)) {
            throw new RuntimeException("Access denied");
        }
        
        return DocumentDTO.from(document);
    }
    
    public boolean canUserViewDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));

        if (userId.equals(document.getCreatedBy())) {
            return true;
        }

        if (pendingRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            return true;
        }

        return getUserDocumentHistory(userId).stream()
            .anyMatch(historyDocument -> documentId.equals(historyDocument.getId()));
    }
    
    public Resource getDocumentContent(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        return storageService.getDocument(document.getFilePath());
    }
    
    @Transactional
    public DocumentDTO createDocument(
            MultipartFile file,
            UUID workflowId,
            String title,
            String description,
            UUID createdBy) {
        
        // Upload to Supabase Storage
        String filePath = storageService.uploadDocument(file);
        
        // Create document record
        Document document = new Document();
        document.setTitle(title);
        document.setDescription(description);
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setWorkflowId(workflowId);
        document.setCurrentStep(1);
        document.setStatus("pending");
        document.setCreatedBy(createdBy);
        document.setCreatedAt(LocalDateTime.now());
        
        document = documentRepository.save(document);
        
        // Assign to first signer
        assignToNextSigner(document.getId());
        
        return DocumentDTO.from(document);
    }
    
    @Transactional
    public void assignToNextSigner(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        WorkflowStep nextStep = workflowStepRepository
            .findByWorkflowIdAndStepOrder(
                document.getWorkflowId(),
                document.getCurrentStep()
            )
            .orElseThrow(() -> new RuntimeException("Workflow step not found"));
        
        PendingDocument pending = new PendingDocument();
        pending.setDocumentId(documentId);
        pending.setUserId(nextStep.getUserId());
        pending.setStepOrder(nextStep.getStepOrder());
        pending.setAssignedAt(LocalDateTime.now());
        
        pendingRepository.save(pending);
        
        // Send notification
        notificationService.notifyPendingSignature(
            nextStep.getUserId(),
            documentId,
            document.getTitle()
        );
    }
    
    @Transactional
    public void moveToNextStep(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // Remove from current user's pending
        pendingRepository.deleteByDocumentId(documentId);
        
        // Check if there's a next step
        WorkflowStep nextStep = workflowStepRepository
            .findByWorkflowIdAndStepOrder(
                document.getWorkflowId(),
                document.getCurrentStep() + 1
            )
            .orElse(null);
        
        if (nextStep == null) {
            // No more steps, mark as completed
            document.setStatus("completed");
            document.setCompletedAt(LocalDateTime.now());
            documentRepository.save(document);
            
            // Notify creator
            notificationService.notifyDocumentCompleted(
                document.getCreatedBy(),
                documentId,
                document.getTitle()
            );
        } else {
            // Move to next step
            document.setCurrentStep(document.getCurrentStep() + 1);
            documentRepository.save(document);
            
            // Assign to next signer
            assignToNextSigner(documentId);
        }
    }
    
    public List<DocumentDTO> getUserDocumentHistory(UUID userId) {
        // Get documents where user has signed
        return documentRepository.findByCreatedByOrSignedBy(userId)
            .stream()
            .map(DocumentDTO::from)
            .collect(Collectors.toList());
    }
}
