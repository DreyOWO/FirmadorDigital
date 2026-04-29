package cr.libre.firmador.backend.controller;

import cr.libre.firmador.backend.dto.CompleteSignatureDTO;
import cr.libre.firmador.backend.dto.DocumentDTO;
import cr.libre.firmador.backend.dto.RejectRequestDTO;
import cr.libre.firmador.backend.dto.SignatureRequestDTO;
import cr.libre.firmador.backend.security.UserPrincipal;
import cr.libre.firmador.backend.service.SignatureService;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {
    
    private final SignatureService signatureService;
    
    @PostMapping("/prepare")
    public ResponseEntity<ToBeSignedDTO> prepareSignature(
            @RequestBody SignatureRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        ToBeSignedDTO toBeSigned = signatureService.prepareSignature(
            request.getDocumentId(),
            userPrincipal.getId()
        );
        
        return ResponseEntity.ok(toBeSigned);
    }
    
    @PostMapping("/complete")
    public ResponseEntity<DocumentDTO> completeSignature(
            @RequestBody CompleteSignatureDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        DocumentDTO signedDoc = signatureService.completeSignature(
            request.getDocumentId(),
            request.getSignatureValue(),
            request.getComments(),
            userPrincipal.getId()
        );
        
        return ResponseEntity.ok(signedDoc);
    }
    
    @PostMapping("/reject")
    public ResponseEntity<Void> rejectDocument(
            @RequestBody RejectRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        signatureService.rejectDocument(
            request.getDocumentId(),
            request.getReason(),
            userPrincipal.getId()
        );
        
        return ResponseEntity.ok().build();
    }
}
