# Sistema de Flujo de Firma Digital - Arquitectura Completa

Sistema de firma digital con flujo de aprobación secuencial usando React (Vercel) + Spring Boot + Supabase.

## 📋 Requisitos del Sistema

### Funcionalidades Principales:
1. ✅ **Visualizador centralizado** de documentos pendientes
2. ✅ **Sin descarga** - Solo visualización y decisión (aprobar/rechazar)
3. ✅ **Firma digital oficial** mediante sistemas gubernamentales (Costa Rica)
4. ✅ **Flujo secuencial** - Documento pasa al siguiente usuario automáticamente
5. ✅ **Administración de flujos** - Definir usuarios y secuencia de firma
6. ✅ **Trazabilidad completa** - Historial de todas las acciones

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                    React Frontend (Vercel)                       │
│  - Visualizador de documentos (PDF.js)                          │
│  - Lista de documentos pendientes                               │
│  - Interfaz de aprobación/rechazo                               │
│  - Panel de administración de flujos                            │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTPS/REST API
                 │
┌────────────────▼────────────────────────────────────────────────┐
│              Spring Boot Backend (Railway/Render)                │
│  - REST API para documentos y flujos                            │
│  - Integración con firmador-core                                │
│  - Lógica de flujo secuencial                                   │
│  - Gestión de estados                                           │
└────────────────┬────────────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
┌───────▼──────┐  ┌──────▼────────┐
│   Supabase   │  │  Desktop      │
│   Database   │  │  Agent        │
│   + Storage  │  │  (Firmador)   │
└──────────────┘  └──────┬────────┘
                         │
                  ┌──────▼────────┐
                  │  Smart Card   │
                  │  (PKCS#11)    │
                  └───────────────┘
```

---

## 🗄️ Modelo de Datos (Supabase)

### **Tablas Principales:**

```sql
-- Usuarios
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'admin', 'signer', 'viewer'
    certificate_id VARCHAR(255), -- ID del certificado digital
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Flujos de firma (plantillas)
CREATE TABLE signature_workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Pasos del flujo (secuencia de firmantes)
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID REFERENCES signature_workflows(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL, -- 1, 2, 3, etc.
    user_id UUID REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL, -- 'sign', 'approve', 'review'
    is_required BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(workflow_id, step_order)
);

-- Documentos
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(500) NOT NULL, -- Ruta en Supabase Storage
    file_size BIGINT,
    mime_type VARCHAR(100),
    workflow_id UUID REFERENCES signature_workflows(id),
    current_step INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'pending', -- 'pending', 'in_progress', 'completed', 'rejected'
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Historial de firmas
CREATE TABLE signature_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    step_order INTEGER NOT NULL,
    action VARCHAR(50) NOT NULL, -- 'signed', 'approved', 'rejected'
    comments TEXT,
    signature_data TEXT, -- Datos de la firma digital
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Documentos pendientes por usuario (vista materializada)
CREATE TABLE pending_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    step_order INTEGER NOT NULL,
    assigned_at TIMESTAMP DEFAULT NOW(),
    notified_at TIMESTAMP,
    UNIQUE(document_id, user_id, step_order)
);

-- Notificaciones
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    document_id UUID REFERENCES documents(id),
    type VARCHAR(50) NOT NULL, -- 'pending_signature', 'document_signed', 'document_rejected'
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### **Row Level Security (RLS) en Supabase:**

```sql
-- Los usuarios solo ven sus documentos pendientes
ALTER TABLE pending_documents ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their pending documents"
ON pending_documents FOR SELECT
USING (auth.uid() = user_id);

-- Los usuarios solo ven su historial
ALTER TABLE signature_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their signature history"
ON signature_history FOR SELECT
USING (auth.uid() = user_id);

-- Solo admins pueden crear flujos
ALTER TABLE signature_workflows ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Only admins can create workflows"
ON signature_workflows FOR INSERT
USING (
    EXISTS (
        SELECT 1 FROM users 
        WHERE id = auth.uid() AND role = 'admin'
    )
);
```

---

## 🚀 Backend: Spring Boot API

### **Estructura del Proyecto:**

```
firmador-backend/
├── pom.xml
└── src/main/java/cr/libre/firmador/backend/
    ├── FirmadorBackendApplication.java
    ├── config/
    │   ├── SupabaseConfig.java
    │   ├── SecurityConfig.java
    │   └── WebSocketConfig.java
    ├── controller/
    │   ├── DocumentController.java
    │   ├── WorkflowController.java
    │   ├── SignatureController.java
    │   └── AdminController.java
    ├── service/
    │   ├── DocumentService.java
    │   ├── WorkflowService.java
    │   ├── SignatureService.java
    │   ├── NotificationService.java
    │   └── SupabaseService.java
    ├── model/
    │   ├── Document.java
    │   ├── Workflow.java
    │   ├── WorkflowStep.java
    │   ├── SignatureHistory.java
    │   └── PendingDocument.java
    └── dto/
        ├── DocumentDTO.java
        ├── SignatureRequestDTO.java
        └── WorkflowDTO.java
```

### **Dependencias (pom.xml):**

```xml
<dependencies>
    <!-- Firmador Core -->
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
    
    <!-- Supabase Client -->
    <dependency>
        <groupId>io.supabase</groupId>
        <artifactId>supabase-kt</artifactId>
        <version>1.3.2</version>
    </dependency>
    
    <!-- PostgreSQL (Supabase usa PostgreSQL) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
</dependencies>
```

### **Configuración (application.yml):**

```yaml
spring:
  application:
    name: firmador-backend
  
  datasource:
    url: ${SUPABASE_DB_URL}
    username: ${SUPABASE_DB_USER}
    password: ${SUPABASE_DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

supabase:
  url: ${SUPABASE_URL}
  key: ${SUPABASE_KEY}
  storage-bucket: documents

server:
  port: 8080
```

### **Controladores Principales:**

```java
// DocumentController.java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private SupabaseService supabaseService;
    
    // Obtener documentos pendientes del usuario actual
    @GetMapping("/pending")
    public ResponseEntity<List<DocumentDTO>> getPendingDocuments(
        @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            documentService.getPendingDocuments(user.getId())
        );
    }
    
    // Visualizar documento (sin descarga)
    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewDocument(
        @PathVariable UUID id,
        @AuthenticationPrincipal User user) {
        
        // Verificar que el usuario tiene permiso
        if (!documentService.canUserViewDocument(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Obtener documento de Supabase Storage
        byte[] content = supabaseService.getDocument(id);
        
        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "inline") // inline = no descarga
            .body(content);
    }
    
    // Subir nuevo documento (solo admin)
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentDTO> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam("workflowId") UUID workflowId,
        @RequestParam("title") String title,
        @AuthenticationPrincipal User user) {
        
        // Subir a Supabase Storage
        String filePath = supabaseService.uploadDocument(file);
        
        // Crear registro en BD
        Document doc = documentService.createDocument(
            title, filePath, workflowId, user.getId()
        );
        
        // Asignar al primer firmante
        documentService.assignToNextSigner(doc.getId());
        
        return ResponseEntity.ok(DocumentDTO.from(doc));
    }
}

// SignatureController.java
@RestController
@RequestMapping("/api/signatures")
public class SignatureController {
    
    @Autowired
    private SignatureService signatureService;
    
    @Autowired
    private DocumentService documentService;
    
    // Aprobar y firmar documento
    @PostMapping("/approve")
    public ResponseEntity<SignatureResultDTO> approveDocument(
        @RequestBody SignatureRequestDTO request,
        @AuthenticationPrincipal User user) {
        
        // 1. Verificar que es el turno del usuario
        if (!documentService.isUserTurn(request.getDocumentId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new SignatureResultDTO("No es tu turno para firmar"));
        }
        
        // 2. Preparar documento para firma
        ToBeSignedDTO toBeSigned = signatureService.prepareSignature(
            request.getDocumentId()
        );
        
        // 3. El usuario firma con su smart card (vía Desktop Agent)
        // Este paso se hace en el frontend con WebSocket al agente
        
        return ResponseEntity.ok(new SignatureResultDTO(toBeSigned));
    }
    
    // Completar firma (después de que el usuario firmó con smart card)
    @PostMapping("/complete")
    public ResponseEntity<DocumentDTO> completeSignature(
        @RequestBody CompleteSignatureDTO request,
        @AuthenticationPrincipal User user) {
        
        // 1. Completar la firma con el valor del smart card
        Document signedDoc = signatureService.completeSignature(
            request.getDocumentId(),
            request.getSignatureValue(),
            user.getId()
        );
        
        // 2. Registrar en historial
        signatureService.recordSignature(
            request.getDocumentId(),
            user.getId(),
            "signed",
            request.getComments()
        );
        
        // 3. Avanzar al siguiente paso del flujo
        documentService.moveToNextStep(request.getDocumentId());
        
        // 4. Notificar al siguiente firmante
        documentService.notifyNextSigner(request.getDocumentId());
        
        return ResponseEntity.ok(DocumentDTO.from(signedDoc));
    }
    
    // Rechazar documento
    @PostMapping("/reject")
    public ResponseEntity<Void> rejectDocument(
        @RequestBody RejectRequestDTO request,
        @AuthenticationPrincipal User user) {
        
        // Registrar rechazo
        signatureService.recordSignature(
            request.getDocumentId(),
            user.getId(),
            "rejected",
            request.getReason()
        );
        
        // Marcar documento como rechazado
        documentService.rejectDocument(request.getDocumentId());
        
        // Notificar al creador
        documentService.notifyDocumentRejected(
            request.getDocumentId(),
            user.getFullName(),
            request.getReason()
        );
        
        return ResponseEntity.ok().build();
    }
}

// WorkflowController.java (Admin)
@RestController
@RequestMapping("/api/workflows")
@PreAuthorize("hasRole('ADMIN')")
public class WorkflowController {
    
    @Autowired
    private WorkflowService workflowService;
    
    // Crear nuevo flujo
    @PostMapping
    public ResponseEntity<WorkflowDTO> createWorkflow(
        @RequestBody CreateWorkflowDTO request,
        @AuthenticationPrincipal User user) {
        
        Workflow workflow = workflowService.createWorkflow(
            request.getName(),
            request.getDescription(),
            request.getSteps(), // Lista de usuarios en orden
            user.getId()
        );
        
        return ResponseEntity.ok(WorkflowDTO.from(workflow));
    }
    
    // Listar flujos
    @GetMapping
    public ResponseEntity<List<WorkflowDTO>> listWorkflows() {
        return ResponseEntity.ok(
            workflowService.getAllWorkflows()
        );
    }
    
    // Actualizar flujo
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDTO> updateWorkflow(
        @PathVariable UUID id,
        @RequestBody UpdateWorkflowDTO request) {
        
        Workflow updated = workflowService.updateWorkflow(id, request);
        return ResponseEntity.ok(WorkflowDTO.from(updated));
    }
}
```

### **Servicios Clave:**

```java
// DocumentService.java
@Service
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private PendingDocumentRepository pendingRepository;
    
    @Autowired
    private WorkflowStepRepository workflowStepRepository;
    
    public void moveToNextStep(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow();
        
        // Obtener siguiente paso del flujo
        WorkflowStep nextStep = workflowStepRepository
            .findByWorkflowIdAndStepOrder(
                doc.getWorkflowId(),
                doc.getCurrentStep() + 1
            )
            .orElse(null);
        
        if (nextStep == null) {
            // No hay más pasos, documento completado
            doc.setStatus("completed");
            doc.setCompletedAt(LocalDateTime.now());
        } else {
            // Avanzar al siguiente paso
            doc.setCurrentStep(doc.getCurrentStep() + 1);
            
            // Asignar al siguiente usuario
            PendingDocument pending = new PendingDocument();
            pending.setDocumentId(documentId);
            pending.setUserId(nextStep.getUserId());
            pending.setStepOrder(nextStep.getStepOrder());
            pendingRepository.save(pending);
        }
        
        documentRepository.save(doc);
    }
    
    public boolean isUserTurn(UUID documentId, UUID userId) {
        return pendingRepository.existsByDocumentIdAndUserId(
            documentId, userId
        );
    }
}

// SignatureService.java
@Service
public class SignatureService {
    
    @Autowired
    private SupabaseService supabaseService;
    
    private FirmadorPAdES padesSigner = new FirmadorPAdES(null);
    
    public ToBeSignedDTO prepareSignature(UUID documentId) {
        // 1. Obtener documento de Supabase
        byte[] documentBytes = supabaseService.getDocument(documentId);
        DSSDocument document = new InMemoryDocument(documentBytes);
        
        // 2. Preparar para firma (sin firmar aún)
        // Esto genera el hash que el usuario firmará con su smart card
        CAdESSignatureParameters parameters = new CAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
        
        CAdESService service = new CAdESService(getCertificateVerifier());
        ToBeSigned dataToSign = service.getDataToSign(document, parameters);
        
        // 3. Retornar datos para que el frontend los envíe al Desktop Agent
        return new ToBeSignedDTO(dataToSign.getBytes());
    }
    
    public Document completeSignature(
        UUID documentId,
        String signatureValue,
        UUID userId) {
        
        // 1. Obtener documento original
        byte[] documentBytes = supabaseService.getDocument(documentId);
        DSSDocument document = new InMemoryDocument(documentBytes);
        
        // 2. Completar firma con el valor del smart card
        SignatureValue sigValue = new SignatureValue(
            SignatureAlgorithm.RSA_SHA256,
            Base64.getDecoder().decode(signatureValue)
        );
        
        CAdESService service = new CAdESService(getCertificateVerifier());
        DSSDocument signedDoc = service.signDocument(
            document,
            parameters,
            sigValue
        );
        
        // 3. Guardar documento firmado en Supabase
        String newPath = supabaseService.saveSignedDocument(
            documentId,
            signedDoc.openStream().readAllBytes()
        );
        
        // 4. Actualizar registro en BD
        Document doc = documentRepository.findById(documentId)
            .orElseThrow();
        doc.setFilePath(newPath);
        
        return documentRepository.save(doc);
    }
}
```

---

## 🎨 Frontend: React (Vercel)

### **Estructura del Proyecto:**

```
firmador-frontend/
├── package.json
├── vercel.json
├── public/
└── src/
    ├── App.tsx
    ├── pages/
    │   ├── Dashboard.tsx
    │   ├── PendingDocuments.tsx
    │   ├── DocumentViewer.tsx
    │   ├── AdminPanel.tsx
    │   └── WorkflowManager.tsx
    ├── components/
    │   ├── DocumentList.tsx
    │   ├── PDFViewer.tsx
    │   ├── SignatureDialog.tsx
    │   ├── WorkflowBuilder.tsx
    │   └── NotificationBell.tsx
    ├── services/
    │   ├── api.ts
    │   ├── supabase.ts
    │   ├── agentClient.ts
    │   └── auth.ts
    ├── hooks/
    │   ├── usePendingDocuments.ts
    │   ├── useSignature.ts
    │   └── useWorkflows.ts
    └── types/
        └── index.ts
```

### **Componentes Principales:**

```typescript
// pages/PendingDocuments.tsx
export const PendingDocuments: React.FC = () => {
    const { data: documents, isLoading } = usePendingDocuments();
    const navigate = useNavigate();
    
    return (
        <div className="container mx-auto p-6">
            <h1 className="text-2xl font-bold mb-6">
                Documentos Pendientes de Firma
            </h1>
            
            {isLoading ? (
                <LoadingSpinner />
            ) : (
                <div className="grid gap-4">
                    {documents?.map(doc => (
                        <DocumentCard
                            key={doc.id}
                            document={doc}
                            onView={() => navigate(`/documents/${doc.id}/view`)}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

// pages/DocumentViewer.tsx
export const DocumentViewer: React.FC = () => {
    const { id } = useParams();
    const [showSignDialog, setShowSignDialog] = useState(false);
    const { data: document } = useDocument(id);
    
    return (
        <div className="h-screen flex flex-col">
            {/* Header con acciones */}
            <div className="bg-white border-b p-4 flex justify-between">
                <h2 className="text-xl font-semibold">{document?.title}</h2>
                <div className="space-x-2">
                    <button
                        onClick={() => setShowSignDialog(true)}
                        className="bg-green-600 text-white px-4 py-2 rounded"
                    >
                        Aprobar y Firmar
                    </button>
                    <button
                        onClick={() => handleReject()}
                        className="bg-red-600 text-white px-4 py-2 rounded"
                    >
                        Rechazar
                    </button>
                </div>
            </div>
            
            {/* Visor PDF (sin opción de descarga) */}
            <div className="flex-1">
                <PDFViewer
                    documentId={id}
                    disableDownload={true}
                    disablePrint={true}
                />
            </div>
            
            {/* Dialog de firma */}
            {showSignDialog && (
                <SignatureDialog
                    documentId={id}
                    onClose={() => setShowSignDialog(false)}
                    onSuccess={() => navigate('/dashboard')}
                />
            )}
        </div>
    );
};

// components/PDFViewer.tsx
import { Document, Page, pdfjs } from 'react-pdf';

pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

export const PDFViewer: React.FC<PDFViewerProps> = ({
    documentId,
    disableDownload,
    disablePrint
}) => {
    const [numPages, setNumPages] = useState<number>(0);
    const [pageNumber, setPageNumber] = useState<number>(1);
    const [pdfUrl, setPdfUrl] = useState<string>('');
    
    useEffect(() => {
        // Obtener URL temporal del documento (sin permitir descarga)
        api.getDocumentViewUrl(documentId).then(url => {
            setPdfUrl(url);
        });
    }, [documentId]);
    
    // Deshabilitar menú contextual (click derecho)
    const handleContextMenu = (e: React.MouseEvent) => {
        if (disableDownload) {
            e.preventDefault();
        }
    };
    
    return (
        <div
            className="pdf-viewer"
            onContextMenu={handleContextMenu}
            style={{ userSelect: 'none' }} // Evitar selección de texto
        >
            <Document
                file={pdfUrl}
                onLoadSuccess={({ numPages }) => setNumPages(numPages)}
                options={{
                    cMapUrl: 'cmaps/',
                    cMapPacked: true,
                }}
            >
                <Page
                    pageNumber={pageNumber}
                    renderTextLayer={false} // Deshabilitar capa de texto
                    renderAnnotationLayer={false}
                />
            </Document>
            
            {/* Controles de navegación */}
            <div className="controls">
                <button
                    disabled={pageNumber <= 1}
                    onClick={() => setPageNumber(pageNumber - 1)}
                >
                    Anterior
                </button>
                <span>
                    Página {pageNumber} de {numPages}
                </span>
                <button
                    disabled={pageNumber >= numPages}
                    onClick={() => setPageNumber(pageNumber + 1)}
                >
                    Siguiente
                </button>
            </div>
        </div>
    );
};

// components/SignatureDialog.tsx
export const SignatureDialog: React.FC<SignatureDialogProps> = ({
    documentId,
    onClose,
    onSuccess
}) => {
    const [cards, setCards] = useState<CardInfo[]>([]);
    const [selectedCard, setSelectedCard] = useState<string>('');
    const [pin, setPin] = useState<string>('');
    const [comments, setComments] = useState<string>('');
    const [signing, setSigning] = useState(false);
    
    const agentClient = new FirmadorAgentClient();
    const api = new FirmadorAPI();
    
    useEffect(() => {
        // Conectar con Desktop Agent y listar tarjetas
        agentClient.listCards().then(setCards);
    }, []);
    
    const handleSign = async () => {
        setSigning(true);
        
        try {
            // 1. Preparar firma en backend
            const toBeSigned = await api.prepareSignature(documentId);
            
            // 2. Firmar con smart card (Desktop Agent)
            const signature = await agentClient.sign(
                selectedCard,
                pin,
                toBeSigned.bytes
            );
            
            // 3. Completar firma en backend
            await api.completeSignature({
                documentId,
                signatureValue: signature,
                comments
            });
            
            toast.success('Documento firmado exitosamente');
            onSuccess();
        } catch (error) {
            toast.error('Error al firmar: ' + error.message);
        } finally {
            setSigning(false);
        }
    };
    
    return (
        <Dialog open onClose={onClose}>
            <DialogTitle>Firmar Documento</DialogTitle>
            <DialogContent>
                <div className="space-y-4">
                    <Select
                        label="Certificado Digital"
                        value={selectedCard}
                        onChange={e => setSelectedCard(e.target.value)}
                    >
                        {cards.map(card => (
                            <option key={card.id} value={card.id}>
                                {card.name} - {card.identification}
                            </option>
                        ))}
                    </Select>
                    
                    <TextField
                        type="password"
                        label="PIN"
                        value={pin}
                        onChange={e => setPin(e.target.value)}
                    />
                    
                    <TextField
                        label="Comentarios (opcional)"
                        multiline
                        rows={3}
                        value={comments}
                        onChange={e => setComments(e.target.value)}
                    />
                </div>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancelar</Button>
                <Button
                    onClick={handleSign}
                    disabled={!selectedCard || !pin || signing}
                    variant="contained"
                >
                    {signing ? 'Firmando...' : 'Firmar'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

// pages/AdminPanel.tsx (Administración de flujos)
export const AdminPanel: React.FC = () => {
    const [workflows, setWorkflows] = useState<Workflow[]>([]);
    const [users, setUsers] = useState<User[]>([]);
    const [showCreateDialog, setShowCreateDialog] = useState(false);
    
    return (
        <div className="container mx-auto p-6">
            <h1 className="text-2xl font-bold mb-6">
                Administración de Flujos de Firma
            </h1>
            
            <button
                onClick={() => setShowCreateDialog(true)}
                className="bg-blue-600 text-white px-4 py-2 rounded mb-4"
            >
                Crear Nuevo Flujo
            </button>
            
            <div className="grid gap-4">
                {workflows.map(workflow => (
                    <WorkflowCard
                        key={workflow.id}
                        workflow={workflow}
                        onEdit={() => handleEdit(workflow.id)}
                        onDelete={() => handleDelete(workflow.id)}
                    />
                ))}
            </div>
            
            {showCreateDialog && (
                <WorkflowBuilder
                    users={users}
                    onClose={() => setShowCreateDialog(false)}
                    onSave={handleCreateWorkflow}
                />
            )}
        </div>
    );
};
```

### **Servicios:**

```typescript
// services/api.ts
export class FirmadorAPI {
    private baseURL = import.meta.env.VITE_API_URL;
    
    async getPendingDocuments(): Promise<Document[]> {
        const response = await fetch(`${this.baseURL}/api/documents/pending`, {
            headers: this.getAuthHeaders()
        });
        return response.json();
    }
    
    async getDocumentViewUrl(documentId: string): Promise<string> {
        // Obtener URL temporal para visualización (no descarga)
        const response = await fetch(
            `${this.baseURL}/api/documents/${documentId}/view-url`,
            { headers: this.getAuthHeaders() }
        );
        const data = await response.json();
        return data.url;
    }
    
    async prepareSignature(documentId: string): Promise<ToBeSignedDTO> {
        const response = await fetch(
            `${this.baseURL}/api/signatures/prepare`,
            {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ documentId })
            }
        );
        return response.json();
    }
    
    async completeSignature(request: CompleteSignatureRequest): Promise<void> {
        await fetch(`${this.baseURL}/api/signatures/complete`, {
            method: 'POST',
            headers: this.getAuthHeaders(),
            body: JSON.stringify(request)
        });
    }
    
    async rejectDocument(documentId: string, reason: string): Promise<void> {
        await fetch(`${this.baseURL}/api/signatures/reject`, {
            method: 'POST',
            headers: this.getAuthHeaders(),
            body: JSON.stringify({ documentId, reason })
        });
    }
    
    private getAuthHeaders() {
        const token = localStorage.getItem('access_token');
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        };
    }
}

// services/supabase.ts
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

export const supabase = createClient(supabaseUrl, supabaseKey);

// Autenticación
export const signIn = async (email: string, password: string) => {
    const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password
    });
    return { data, error };
};

// Suscripción a cambios en tiempo real
export const subscribeToNotifications = (
    userId: string,
    callback: (notification: Notification) => void
) => {
    return supabase
        .channel('notifications')
        .on(
            'postgres_changes',
            {
                event: 'INSERT',
                schema: 'public',
                table: 'notifications',
                filter: `user_id=eq.${userId}`
            },
            payload => callback(payload.new as Notification)
        )
        .subscribe();
};
```

---

## 🚀 Despliegue

### **1. Backend (Railway o Render):**

```bash
# Railway
railway login
railway init
railway add
railway up

# O Render
# Conectar repositorio de GitHub
# Render detecta automáticamente Spring Boot
```

**Variables de entorno:**
```
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=eyJxxx...
SUPABASE_DB_URL=postgresql://postgres:xxx@db.xxx.supabase.co:5432/postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=xxx
JWT_SECRET=your-secret-key
```

### **2. Frontend (Vercel):**

```bash
# Instalar Vercel CLI
npm i -g vercel

# Desplegar
cd firmador-frontend
vercel

# O conectar repositorio de GitHub en vercel.com
```

**Variables de entorno (.env.production):**
```
VITE_API_URL=https://your-backend.railway.app
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=eyJxxx...
```

### **3. Supabase:**

1. Crear proyecto en supabase.com
2. Ejecutar SQL para crear tablas
3. Configurar Storage bucket "documents"
4. Habilitar Row Level Security
5. Configurar autenticación (email/password)

---

## 💰 Costos Estimados

**Supabase:**
- Free tier: 500 MB storage, 2 GB bandwidth
- Pro: $25/mes (8 GB storage, 50 GB bandwidth)

**Railway/Render:**
- Free tier: 500 horas/mes
- Hobby: $5/mes
- Pro: $20/mes

**Vercel:**
- Free tier: 100 GB bandwidth
- Pro: $20/mes

**Total mensual:** $0-$65 dependiendo del uso

---

## ✅ Resumen

**SÍ, es totalmente posible** implementar este sistema con:
- ✅ React (Vercel) - Frontend
- ✅ Spring Boot - Backend con firmador-core
- ✅ Supabase - Base de datos + Storage
- ✅ Desktop Agent - Acceso a smart card

**Ventajas:**
- Arquitectura moderna y escalable
- Costos bajos ($0-$65/mes)
- Despliegue automático
- Base de datos en tiempo real
- Seguridad robusta

¿Quieres que cree el código completo del backend o frontend?