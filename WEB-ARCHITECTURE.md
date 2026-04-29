# 🌐 Firmador Web Application Architecture

Complete architecture guide for building a React web application with Firmador digital signature library.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    React Frontend (Web)                      │
│  - Document upload/management                                │
│  - User interface for signing                                │
│  - Signature validation display                              │
└────────────────┬────────────────────────────────────────────┘
                 │ HTTPS/REST API
                 │
┌────────────────▼────────────────────────────────────────────┐
│              Spring Boot Backend (Java)                      │
│  - REST API endpoints                                        │
│  - Session management                                        │
│  - Document storage                                          │
│  - Business logic                                            │
└────────────────┬────────────────────────────────────────────┘
                 │ Library calls
                 │
┌────────────────▼────────────────────────────────────────────┐
│              Firmador Core Library                           │
│  - Signature generation (CAdES, PAdES, XAdES, etc.)         │
│  - Signature validation                                      │
│  - Certificate management                                    │
└────────────────┬────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│         Smart Card Access (Client-Side)                     │
│  - Browser extension OR                                      │
│  - Desktop agent (WebSocket) OR                              │
│  - Cloud HSM                                                 │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Recommended Architecture: Hybrid Approach

### **Option 1: Desktop Agent + Web App (RECOMMENDED)**

This is the **best approach** for smart card access while maintaining a web interface.

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   React      │  HTTPS  │   Spring     │  Local  │   Desktop    │
│   Frontend   ├────────►│   Boot       ├────────►│   Agent      │
│   (Browser)  │         │   Backend    │WebSocket│  (Firmador)  │
└──────────────┘         └──────────────┘         └──────┬───────┘
                                                          │
                                                          │ PKCS#11
                                                          │
                                                   ┌──────▼───────┐
                                                   │  Smart Card  │
                                                   └──────────────┘
```

**How it works:**
1. User installs lightweight desktop agent (built from firmador-core)
2. Agent runs locally, connects to smart card via PKCS#11
3. Web app communicates with agent via WebSocket (localhost)
4. Backend handles document processing and signature coordination

**Advantages:**
- ✅ Direct smart card access (no browser limitations)
- ✅ Secure (agent runs locally, no card data sent over network)
- ✅ Modern web UI
- ✅ Works with existing smart cards
- ✅ No browser extensions needed

---

## 📦 Implementation Guide

### 1. Backend: Spring Boot REST API

Create a new module: `firmador-backend`

#### **Project Structure:**
```
firmador-backend/
├── pom.xml
└── src/main/java/cr/libre/firmador/backend/
    ├── FirmadorBackendApplication.java
    ├── controller/
    │   ├── DocumentController.java
    │   ├── SignatureController.java
    │   └── ValidationController.java
    ├── service/
    │   ├── DocumentService.java
    │   ├── SignatureService.java
    │   └── ValidationService.java
    ├── model/
    │   ├── DocumentDTO.java
    │   ├── SignatureRequestDTO.java
    │   └── ValidationResultDTO.java
    └── config/
        ├── SecurityConfig.java
        └── CorsConfig.java
```

#### **Backend POM (firmador-backend/pom.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>cr.libre.firmador</groupId>
    <artifactId>firmador-backend</artifactId>
    <version>2.0.0</version>
    
    <dependencies>
        <!-- Firmador Core Library -->
        <dependency>
            <groupId>cr.libre.firmador</groupId>
            <artifactId>firmador-core</artifactId>
            <version>2.0.0</version>
        </dependency>
        
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- Database (optional) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        
        <!-- File Storage -->
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-java-sdk-s3</artifactId>
            <version>1.12.500</version>
        </dependency>
    </dependencies>
</project>
```

#### **REST API Endpoints:**

```java
// DocumentController.java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> uploadDocument(
        @RequestParam("file") MultipartFile file) {
        // Store document, return ID
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable String id) {
        // Retrieve document info
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String id) {
        // Download original or signed document
    }
}

// SignatureController.java
@RestController
@RequestMapping("/api/signatures")
public class SignatureController {
    
    @PostMapping("/prepare")
    public ResponseEntity<ToBeSignedDTO> prepareSignature(
        @RequestBody SignatureRequestDTO request) {
        // Prepare document for signing
        // Returns data to be signed by desktop agent
    }
    
    @PostMapping("/complete")
    public ResponseEntity<DocumentDTO> completeSignature(
        @RequestBody SignatureValueDTO signatureValue) {
        // Complete signature with value from desktop agent
        // Returns signed document
    }
}

// ValidationController.java
@RestController
@RequestMapping("/api/validation")
public class ValidationController {
    
    @PostMapping("/validate")
    public ResponseEntity<ValidationResultDTO> validateSignature(
        @RequestParam("file") MultipartFile file) {
        // Validate signature using GeneralValidator
    }
}
```

---

### 2. Desktop Agent: WebSocket Server

Create a new module: `firmador-agent`

#### **Agent Structure:**
```
firmador-agent/
├── pom.xml
└── src/main/java/cr/libre/firmador/agent/
    ├── FirmadorAgentApplication.java
    ├── websocket/
    │   ├── AgentWebSocketServer.java
    │   └── SigningHandler.java
    └── service/
        └── SmartCardService.java
```

#### **Agent Implementation:**

```java
// FirmadorAgentApplication.java
public class FirmadorAgentApplication {
    public static void main(String[] args) {
        // Start WebSocket server on localhost:8765
        AgentWebSocketServer server = new AgentWebSocketServer(8765);
        server.start();
        
        // Add system tray icon
        SystemTray.getSystemTray().add(createTrayIcon());
    }
}

// AgentWebSocketServer.java
@ServerEndpoint("/sign")
public class AgentWebSocketServer {
    private SmartCardDetector detector;
    private BasicSigner signer;
    
    @OnMessage
    public String onMessage(String message, Session session) {
        JsonObject request = JsonParser.parseString(message).getAsJsonObject();
        String action = request.get("action").getAsString();
        
        switch (action) {
            case "list_cards":
                return listAvailableCards();
            case "sign":
                return signData(request);
            default:
                return error("Unknown action");
        }
    }
    
    private String signData(JsonObject request) {
        // Get card, PIN, and data to sign
        // Use BasicSigner to sign
        // Return signature value
    }
}
```

---

### 3. Frontend: React Application

#### **Project Structure:**
```
firmador-frontend/
├── package.json
├── public/
└── src/
    ├── App.tsx
    ├── components/
    │   ├── DocumentUpload.tsx
    │   ├── SignaturePanel.tsx
    │   ├── ValidationPanel.tsx
    │   └── CardSelector.tsx
    ├── services/
    │   ├── api.ts              # Backend API calls
    │   ├── agentClient.ts      # Desktop agent WebSocket
    │   └── documentService.ts
    ├── hooks/
    │   ├── useSmartCard.ts
    │   └── useSignature.ts
    └── types/
        └── index.ts
```

#### **Key Components:**

```typescript
// services/agentClient.ts
export class FirmadorAgentClient {
    private ws: WebSocket;
    
    constructor() {
        this.ws = new WebSocket('ws://localhost:8765/sign');
    }
    
    async listCards(): Promise<CardInfo[]> {
        return this.sendRequest({ action: 'list_cards' });
    }
    
    async sign(cardId: string, pin: string, dataToSign: string): Promise<string> {
        return this.sendRequest({
            action: 'sign',
            cardId,
            pin,
            data: dataToSign
        });
    }
}

// services/api.ts
export class FirmadorAPI {
    private baseURL = 'https://api.yourapp.com';
    
    async uploadDocument(file: File): Promise<DocumentDTO> {
        const formData = new FormData();
        formData.append('file', file);
        return fetch(`${this.baseURL}/api/documents/upload`, {
            method: 'POST',
            body: formData
        }).then(r => r.json());
    }
    
    async prepareSignature(documentId: string): Promise<ToBeSignedDTO> {
        return fetch(`${this.baseURL}/api/signatures/prepare`, {
            method: 'POST',
            body: JSON.stringify({ documentId }),
            headers: { 'Content-Type': 'application/json' }
        }).then(r => r.json());
    }
    
    async completeSignature(signatureValue: string): Promise<DocumentDTO> {
        return fetch(`${this.baseURL}/api/signatures/complete`, {
            method: 'POST',
            body: JSON.stringify({ signatureValue }),
            headers: { 'Content-Type': 'application/json' }
        }).then(r => r.json());
    }
}

// components/SignaturePanel.tsx
export const SignaturePanel: React.FC = () => {
    const [cards, setCards] = useState<CardInfo[]>([]);
    const [selectedCard, setSelectedCard] = useState<string>('');
    const [pin, setPin] = useState<string>('');
    const agentClient = new FirmadorAgentClient();
    const api = new FirmadorAPI();
    
    useEffect(() => {
        agentClient.listCards().then(setCards);
    }, []);
    
    const handleSign = async (documentId: string) => {
        // 1. Prepare signature on backend
        const toBeSigned = await api.prepareSignature(documentId);
        
        // 2. Sign with desktop agent (smart card)
        const signature = await agentClient.sign(
            selectedCard,
            pin,
            toBeSigned.bytes
        );
        
        // 3. Complete signature on backend
        const signedDoc = await api.completeSignature(signature);
        
        // 4. Download signed document
        window.location.href = `/api/documents/${signedDoc.id}/download`;
    };
    
    return (
        <div>
            <select onChange={e => setSelectedCard(e.target.value)}>
                {cards.map(card => (
                    <option key={card.id} value={card.id}>
                        {card.name}
                    </option>
                ))}
            </select>
            <input
                type="password"
                placeholder="PIN"
                value={pin}
                onChange={e => setPin(e.target.value)}
            />
            <button onClick={() => handleSign(documentId)}>
                Sign Document
            </button>
        </div>
    );
};
```

---

## 🔐 Security Considerations

### 1. **Desktop Agent Security**
```java
// Only accept connections from localhost
@ServerEndpoint(value = "/sign", configurator = LocalhostOnlyConfigurator.class)
public class AgentWebSocketServer {
    // Implementation
}

public class LocalhostOnlyConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return originHeaderValue.contains("localhost") || 
               originHeaderValue.contains("127.0.0.1");
    }
}
```

### 2. **Backend Security**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Use JWT instead
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        return http.build();
    }
}
```

### 3. **CORS Configuration**
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("https://yourapp.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

---

## 🚀 Deployment Architecture

### **Production Setup:**

```
┌─────────────────────────────────────────────────────────────┐
│                    CloudFlare / CDN                          │
│                  (Static React App)                          │
└────────────────┬────────────────────────────────────────────┘
                 │ HTTPS
                 │
┌────────────────▼────────────────────────────────────────────┐
│              AWS Application Load Balancer                   │
└────────────────┬────────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
┌───────▼──────┐  ┌──────▼────────┐
│  Spring Boot │  │  Spring Boot  │
│  Instance 1  │  │  Instance 2   │
│  (ECS/K8s)   │  │  (ECS/K8s)    │
└───────┬──────┘  └──────┬────────┘
        │                │
        └────────┬────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│              AWS RDS PostgreSQL                              │
│         (Document metadata, users, sessions)                 │
└─────────────────────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│              AWS S3                                          │
│         (Document storage)                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              User's Computer                                 │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │  Desktop Agent   │◄────►│  Smart Card      │            │
│  │  (localhost:8765)│      │  (PKCS#11)       │            │
│  └──────────────────┘      └──────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### **Infrastructure as Code (Terraform):**

```hcl
# main.tf
resource "aws_ecs_cluster" "firmador" {
  name = "firmador-cluster"
}

resource "aws_ecs_service" "backend" {
  name            = "firmador-backend"
  cluster         = aws_ecs_cluster.firmador.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 2
  
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "firmador-backend"
    container_port   = 8080
  }
}

resource "aws_db_instance" "postgres" {
  identifier        = "firmador-db"
  engine            = "postgres"
  engine_version    = "15.3"
  instance_class    = "db.t3.medium"
  allocated_storage = 100
}

resource "aws_s3_bucket" "documents" {
  bucket = "firmador-documents"
  
  lifecycle_rule {
    enabled = true
    expiration {
      days = 90  # Auto-delete after 90 days
    }
  }
}
```

---

## 📊 Alternative Architectures

### **Option 2: Cloud HSM (Enterprise)**

For organizations without smart cards:

```
React Frontend → Spring Boot Backend → AWS CloudHSM
                                      (Stores private keys)
```

**Pros:** No desktop agent needed, fully cloud-based
**Cons:** Expensive, requires key migration

### **Option 3: Browser Extension**

```
React Frontend → Browser Extension → Smart Card (via WebCrypto API)
       ↓
Spring Boot Backend (validation only)
```

**Pros:** No desktop agent
**Cons:** Limited browser support, complex development

---

## 🛠️ Development Workflow

### **1. Local Development:**
```bash
# Terminal 1: Start backend
cd firmador-backend
mvn spring-boot:run

# Terminal 2: Start desktop agent
cd firmador-agent
mvn exec:java

# Terminal 3: Start React frontend
cd firmador-frontend
npm start
```

### **2. Docker Compose:**
```yaml
version: '3.8'
services:
  backend:
    build: ./firmador-backend
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=postgresql://postgres:5432/firmador
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=firmador
      - POSTGRES_PASSWORD=secret
  
  frontend:
    build: ./firmador-frontend
    ports:
      - "3000:3000"
```

---

## 📈 Scalability Considerations

1. **Stateless Backend:** Use JWT tokens, no server sessions
2. **Document Storage:** S3 with CloudFront CDN
3. **Database:** Read replicas for validation queries
4. **Caching:** Redis for frequently accessed data
5. **Queue:** SQS for async signature processing

---

## 💰 Cost Estimation (AWS)

**Small deployment (100 users):**
- ECS Fargate (2 instances): ~$50/month
- RDS PostgreSQL (db.t3.medium): ~$100/month
- S3 Storage (100GB): ~$2/month
- ALB: ~$20/month
- **Total: ~$172/month**

**Medium deployment (1000 users):**
- ECS Fargate (4 instances): ~$100/month
- RDS PostgreSQL (db.t3.large): ~$200/month
- S3 Storage (1TB): ~$23/month
- ALB: ~$20/month
- CloudFront: ~$50/month
- **Total: ~$393/month**

---

## 🎯 Recommended Tech Stack

**Backend:**
- Spring Boot 3.2
- PostgreSQL 15
- AWS S3
- Redis (caching)

**Frontend:**
- React 18 + TypeScript
- Vite (build tool)
- TanStack Query (data fetching)
- Tailwind CSS

**Desktop Agent:**
- Java 11+ (for compatibility)
- Java-WebSocket library
- System tray integration

**DevOps:**
- Docker + Docker Compose
- GitHub Actions (CI/CD)
- Terraform (IaC)
- AWS ECS or Kubernetes

---

## 📝 Next Steps

1. Create `firmador-backend` module
2. Create `firmador-agent` module
3. Create React frontend
4. Set up development environment
5. Implement authentication
6. Deploy to staging
7. Load testing
8. Production deployment

Would you like me to create the Spring Boot backend module or the desktop agent implementation?