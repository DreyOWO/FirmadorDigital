package cr.libre.firmador.backend.controller;

import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentControllerTest {

    @Test
    void getPendingDocumentsReturnsOk() {
        DocumentService documentService = mock(DocumentService.class);
        DocumentController controller = new DocumentController(documentService);

        UUID userId = UUID.randomUUID();
        DocumentDTO dto = new DocumentDTO();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Pending Doc");

        when(documentService.getPendingDocuments(userId)).thenReturn(List.of(dto));

        var response = controller.getPendingDocuments(new cr.libre.firmador.backend.security.UserPrincipal(userId));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Pending Doc", response.getBody().get(0).getTitle());
    }

    @Test
    void viewDocumentReturnsForbiddenWhenUserCannotView() {
        DocumentService documentService = mock(DocumentService.class);
        DocumentController controller = new DocumentController(documentService);

        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(documentService.canUserViewDocument(documentId, userId)).thenReturn(false);

        var response = controller.viewDocument(documentId, new cr.libre.firmador.backend.security.UserPrincipal(userId));

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void viewDocumentReturnsPdfWhenAuthorized() {
        DocumentService documentService = mock(DocumentService.class);
        DocumentController controller = new DocumentController(documentService);

        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] payload = "pdf".getBytes();

        when(documentService.canUserViewDocument(documentId, userId)).thenReturn(true);
        when(documentService.getDocumentContent(documentId)).thenReturn(new ByteArrayResource(payload));

        var response = controller.viewDocument(documentId, new cr.libre.firmador.backend.security.UserPrincipal(userId));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("inline; filename=\"document.pdf\"", response.getHeaders().getFirst("Content-Disposition"));
        assertInstanceOf(ByteArrayResource.class, response.getBody());
        assertArrayEquals(payload, ((ByteArrayResource) response.getBody()).getByteArray());
    }

    @Test
    void uploadDocumentReturnsOkForAdmin() {
        DocumentService documentService = mock(DocumentService.class);
        DocumentController controller = new DocumentController(documentService);

        UUID workflowId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DocumentDTO dto = new DocumentDTO();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Uploaded");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "doc.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".getBytes()
        );

        when(documentService.createDocument(any(), eq(workflowId), eq("Title"), eq("Desc"), eq(userId))).thenReturn(dto);

        var response = controller.uploadDocument(
            file,
            workflowId,
            "Title",
            "Desc",
            new cr.libre.firmador.backend.security.UserPrincipal(userId)
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Uploaded", response.getBody().getTitle());
    }
}

// Made with Bob
