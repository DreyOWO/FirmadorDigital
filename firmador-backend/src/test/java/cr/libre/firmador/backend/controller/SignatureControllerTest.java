package cr.libre.firmador.backend.controller;

import cr.libre.firmador.backend.dto.CompleteSignatureDTO;
import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.dto.RejectRequestDTO;
import cr.libre.firmador.backend.dto.SignatureRequestDTO;
import cr.libre.firmador.backend.security.UserPrincipal;
import cr.libre.firmador.backend.service.SignatureService;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignatureControllerTest {

    @Test
    void prepareSignatureReturnsOk() {
        SignatureService signatureService = mock(SignatureService.class);
        SignatureController controller = new SignatureController(signatureService);

        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SignatureRequestDTO request = new SignatureRequestDTO();
        request.setDocumentId(documentId);

        ToBeSignedDTO response = new ToBeSignedDTO();
        response.setBytes("payload".getBytes());

        when(signatureService.prepareSignature(documentId, userId)).thenReturn(response);

        var result = controller.prepareSignature(request, new UserPrincipal(userId));

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertArrayEquals("payload".getBytes(), result.getBody().getBytes());
    }

    @Test
    void completeSignatureReturnsOk() {
        SignatureService signatureService = mock(SignatureService.class);
        SignatureController controller = new SignatureController(signatureService);

        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CompleteSignatureDTO request = new CompleteSignatureDTO();
        request.setDocumentId(documentId);
        request.setSignatureValue("signed");
        request.setComments("done");

        DocumentDTO response = new DocumentDTO();
        response.setId(documentId);
        response.setStatus("completed");

        when(signatureService.completeSignature(documentId, "signed", "done", userId)).thenReturn(response);

        var result = controller.completeSignature(request, new UserPrincipal(userId));

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("completed", result.getBody().getStatus());
    }

    @Test
    void rejectDocumentReturnsOk() {
        SignatureService signatureService = mock(SignatureService.class);
        SignatureController controller = new SignatureController(signatureService);

        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RejectRequestDTO request = new RejectRequestDTO();
        request.setDocumentId(documentId);
        request.setReason("bad");

        doNothing().when(signatureService).rejectDocument(documentId, "bad", userId);

        var result = controller.rejectDocument(request, new UserPrincipal(userId));

        assertEquals(200, result.getStatusCode().value());
    }
}

// Made with Bob
