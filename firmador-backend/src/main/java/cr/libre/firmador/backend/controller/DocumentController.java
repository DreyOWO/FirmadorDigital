package cr.libre.firmador.backend.controller;

import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.model.User;
import cr.libre.firmador.backend.security.UserPrincipal;
import cr.libre.firmador.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    @GetMapping("/pending")
    public ResponseEntity<List<DocumentDTO>> getPendingDocuments(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DocumentDTO> documents = documentService
            .getPendingDocuments(userPrincipal.getId());
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        DocumentDTO document = documentService.getDocument(id, userPrincipal.getId());
        return ResponseEntity.ok(document);
    }
    
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        if (!documentService.canUserViewDocument(id, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        Resource resource = documentService.getDocumentContent(id);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"")
            .body(resource);
    }
    
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("workflowId") UUID workflowId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        DocumentDTO document = documentService.createDocument(
            file, workflowId, title, description, userPrincipal.getId()
        );
        
        return ResponseEntity.ok(document);
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<DocumentDTO>> getDocumentHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DocumentDTO> documents = documentService
            .getUserDocumentHistory(userPrincipal.getId());
        return ResponseEntity.ok(documents);
    }
}
