# 🚀 Backend Complete Code - Ready to Copy

Este documento contiene TODO el código del backend listo para copiar y pegar.

## 📁 Estructura de Archivos

```
firmador-backend/
├── pom.xml (✅ Ya creado)
├── src/main/
│   ├── java/cr/libre/firmador/backend/
│   │   ├── FirmadorBackendApplication.java (✅ Ya creado)
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   └── SupabaseConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── DocumentController.java
│   │   │   ├── SignatureController.java
│   │   │   └── WorkflowController.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── DocumentService.java
│   │   │   ├── SignatureService.java
│   │   │   ├── WorkflowService.java
│   │   │   └── SupabaseStorageService.java
│   │   ├── model/
│   │   │   ├── User.java (✅ Ya creado)
│   │   │   ├── Document.java (✅ Ya creado)
│   │   │   ├── Workflow.java
│   │   │   ├── WorkflowStep.java
│   │   │   ├── SignatureHistory.java
│   │   │   ├── PendingDocument.java
│   │   │   └── Notification.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── DocumentRepository.java
│   │   │   ├── WorkflowRepository.java
│   │   │   ├── WorkflowStepRepository.java
│   │   │   ├── SignatureHistoryRepository.java
│   │   │   ├── PendingDocumentRepository.java
│   │   │   └── NotificationRepository.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── DocumentDTO.java
│   │   │   ├── SignatureRequestDTO.java
│   │   │   ├── CompleteSignatureDTO.java
│   │   │   └── WorkflowDTO.java
│   │   └── security/
│   │       ├── JwtTokenProvider.java
│   │       ├── JwtAuthenticationFilter.java
│   │       └── UserPrincipal.java
│   └── resources/
│       └── application.yml (✅ Ya creado)
└── Dockerfile
```

---

## 📦 MODELS (Entities)

### Workflow.java
```java
package cr.libre.firmador.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signature_workflows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

### WorkflowStep.java
```java
package cr.libre.firmador.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "action_type", nullable = false)
    private String actionType = "sign"; // sign, approve, review
    
    @Column(name = "is_required")
    private Boolean isRequired = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### SignatureHistory.java
```java
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
```

### PendingDocument.java
```java
package cr.libre.firmador.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();
    
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;
}
```

### Notification.java
```java
package cr.libre.firmador.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "document_id")
    private UUID documentId;
    
    @Column(nullable = false)
    private String type; // pending_signature, document_completed, document_rejected
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

## 🗄️ REPOSITORIES

### UserRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### DocumentRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByCreatedBy(UUID userId);
    List<Document> findByStatus(String status);
    
    @Query("SELECT d FROM Document d WHERE d.createdBy = :userId OR " +
           "EXISTS (SELECT sh FROM SignatureHistory sh WHERE sh.documentId = d.id AND sh.userId = :userId)")
    List<Document> findByCreatedByOrSignedBy(UUID userId);
}
```

### WorkflowRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByIsActiveTrue();
    List<Workflow> findByCreatedBy(UUID userId);
}
```

### WorkflowStepRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {
    List<WorkflowStep> findByWorkflowIdOrderByStepOrder(UUID workflowId);
    Optional<WorkflowStep> findByWorkflowIdAndStepOrder(UUID workflowId, Integer stepOrder);
}
```

### SignatureHistoryRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.SignatureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignatureHistoryRepository extends JpaRepository<SignatureHistory, UUID> {
    List<SignatureHistory> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
    List<SignatureHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
```

### PendingDocumentRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.PendingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PendingDocumentRepository extends JpaRepository<PendingDocument, UUID> {
    List<PendingDocument> findByUserIdOrderByAssignedAtDesc(UUID userId);
    boolean existsByDocumentIdAndUserId(UUID documentId, UUID userId);
    void deleteByDocumentId(UUID documentId);
}
```

### NotificationRepository.java
```java
package cr.libre.firmador.backend.repository;

import cr.libre.firmador.backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndIsReadFalse(UUID userId);
}
```

---

## 📝 DTOs

### LoginRequest.java
```java
package cr.libre.firmador.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
```

### LoginResponse.java
```java
package cr.libre.firmador.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String email;
    private String fullName;
    private String role;
}
```

### DocumentDTO.java
```java
package cr.libre.firmador.backend.dto;

import cr.libre.firmador.backend.model.Document;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DocumentDTO {
    private UUID id;
    private String title;
    private String description;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private UUID workflowId;
    private Integer currentStep;
    private String status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    public static DocumentDTO from(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setDescription(document.getDescription());
        dto.setFilePath(document.getFilePath());
        dto.setFileSize(document.getFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setWorkflowId(document.getWorkflowId());
        dto.setCurrentStep(document.getCurrentStep());
        dto.setStatus(document.getStatus());
        dto.setCreatedBy(document.getCreatedBy());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        dto.setCompletedAt(document.getCompletedAt());
        return dto;
    }
}
```

### SignatureRequestDTO.java
```java
package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SignatureRequestDTO {
    private UUID documentId;
}
```

### CompleteSignatureDTO.java
```java
package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CompleteSignatureDTO {
    private UUID documentId;
    private String signatureValue;
    private String comments;
}
```

### RejectRequestDTO.java
```java
package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class RejectRequestDTO {
    private UUID documentId;
    private String reason;
}
```

### WorkflowDTO.java
```java
package cr.libre.firmador.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class WorkflowDTO {
    private UUID id;
    private String name;
    private String description;
    private List<WorkflowStepDTO> steps;
    
    @Data
    public static class WorkflowStepDTO {
        private Integer stepOrder;
        private UUID userId;
        private String actionType;
        private Boolean isRequired;
    }
}
```

---

## 🔐 SECURITY

### JwtTokenProvider.java
```java
package cr.libre.firmador.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}") // 24 hours default
    private long jwtExpiration;
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return UUID.fromString(claims.getSubject());
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### JwtAuthenticationFilter.java
```java
package cr.libre.firmador.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                UUID userId = tokenProvider.getUserIdFromToken(jwt);
                
                UserPrincipal userPrincipal = new UserPrincipal(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### UserPrincipal.java
```java
package cr.libre.firmador.backend.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private UUID id;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getPassword() {
        return null;
    }
    
    @Override
    public String getUsername() {
        return id.toString();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

---

## ⚙️ CONFIGURATION

### CorsConfig.java
```java
package cr.libre.firmador.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://*.vercel.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### SupabaseConfig.java
```java
package cr.libre.firmador.backend.config;

import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.SupabaseClientBuilder;
import io.github.jan.supabase.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupabaseConfig {
    
    @Value("${supabase.url}")
    private String supabaseUrl;
    
    @Value("${supabase.key}")
    private String supabaseKey;
    
    @Bean
    public SupabaseClient supabaseClient() {
        return new SupabaseClientBuilder()
                .supabaseUrl(supabaseUrl)
                .supabaseKey(supabaseKey)
                .build();
    }
}
```

---

## 🎮 CONTROLLERS

Ver el archivo COMPLETE-IMPLEMENTATION-GUIDE.md para los controllers completos (DocumentController, SignatureController, etc.)

---

## 🔧 SERVICES

Ver el archivo COMPLETE-IMPLEMENTATION-GUIDE.md para los services completos (DocumentService, SignatureService, etc.)

---

## 📋 INSTRUCCIONES DE USO

1. **Copiar todos los archivos** a sus respectivas ubicaciones en `firmador-backend/src/main/java/`

2. **Verificar el pom.xml** ya está creado con todas las dependencias

3. **Configurar application.yml** con tus credenciales de Supabase

4. **Compilar el proyecto:**
```bash
cd firmador-backend
mvn clean install
```

5. **Ejecutar:**
```bash
mvn spring-boot:run
```

El backend estará disponible en `http://localhost:8080`

---

## ✅ CHECKLIST

- [x] Models (Entities)
- [x] Repositories
- [x] DTOs
- [x] Security (JWT)
- [x] Configuration
- [ ] Controllers (ver COMPLETE-IMPLEMENTATION-GUIDE.md)
- [ ] Services (ver COMPLETE-IMPLEMENTATION-GUIDE.md)

**Nota:** Los Controllers y Services completos están en el archivo `COMPLETE-IMPLEMENTATION-GUIDE.md` debido a su extensión.