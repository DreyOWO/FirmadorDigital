package cr.libre.firmador.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signature_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(nullable = false)
    private String action; // signed, rejected, approved
    
    @Column(columnDefinition = "TEXT")
    private String comments;
    
    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
