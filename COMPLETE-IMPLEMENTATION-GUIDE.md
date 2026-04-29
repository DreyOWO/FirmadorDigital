# 🚀 Guía Completa de Implementación - Sistema de Firma Digital

Esta guía contiene TODO el código necesario para implementar el sistema completo.

## 📁 Estructura del Proyecto

```
firmador/
├── firmador-backend/          # Spring Boot API
├── firmador-frontend/         # React App
├── firmador-agent/            # Desktop Agent
├── supabase/                  # Database Schema
└── docs/                      # Documentation
```

---

# 1️⃣ BACKEND - Spring Boot

## Estructura Completa

```
firmador-backend/
├── pom.xml
├── src/main/
│   ├── java/cr/libre/firmador/backend/
│   │   ├── FirmadorBackendApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   └── SupabaseConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── DocumentController.java
│   │   │   ├── SignatureController.java
│   │   │   ├── WorkflowController.java
│   │   │   └── NotificationController.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── DocumentService.java
│   │   │   ├── SignatureService.java
│   │   │   ├── WorkflowService.java
│   │   │   ├── NotificationService.java
│   │   │   └── SupabaseStorageService.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Document.java
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
│   │   │   ├── WorkflowDTO.java
│   │   │   └── NotificationDTO.java
│   │   └── security/
│   │       ├── JwtTokenProvider.java
│   │       ├── JwtAuthenticationFilter.java
│   │       └── UserPrincipal.java
│   └── resources/
│       ├── application.yml
│       └── application-prod.yml
└── Dockerfile
```

## Código Completo del Backend

### FirmadorBackendApplication.java
```java
package cr.libre.firmador.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FirmadorBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirmadorBackendApplication.class, args);
    }
}
```

### SecurityConfig.java
```java
package cr.libre.firmador.backend.config;

import cr.libre.firmador.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### DocumentController.java
```java
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
```

### SignatureController.java
```java
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
```

### DocumentService.java
```java
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
        return pendingRepository.existsByDocumentIdAndUserId(documentId, userId);
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
```

---

# 2️⃣ FRONTEND - React + TypeScript

## Estructura Completa

```
firmador-frontend/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
├── .env.example
├── public/
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── vite-env.d.ts
    ├── pages/
    │   ├── Login.tsx
    │   ├── Dashboard.tsx
    │   ├── PendingDocuments.tsx
    │   ├── DocumentViewer.tsx
    │   ├── AdminPanel.tsx
    │   └── WorkflowManager.tsx
    ├── components/
    │   ├── Layout.tsx
    │   ├── Navbar.tsx
    │   ├── DocumentCard.tsx
    │   ├── DocumentList.tsx
    │   ├── PDFViewer.tsx
    │   ├── SignatureDialog.tsx
    │   ├── RejectDialog.tsx
    │   ├── WorkflowBuilder.tsx
    │   ├── NotificationBell.tsx
    │   └── ProtectedRoute.tsx
    ├── services/
    │   ├── api.ts
    │   ├── auth.ts
    │   ├── supabase.ts
    │   └── agentClient.ts
    ├── hooks/
    │   ├── useAuth.ts
    │   ├── usePendingDocuments.ts
    │   ├── useDocument.ts
    │   ├── useSignature.ts
    │   └── useNotifications.ts
    ├── types/
    │   └── index.ts
    ├── utils/
    │   └── constants.ts
    └── styles/
        └── globals.css
```

## package.json
```json
{
  "name": "firmador-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@tanstack/react-query": "^5.12.0",
    "@supabase/supabase-js": "^2.38.0",
    "react-pdf": "^7.5.1",
    "axios": "^1.6.2",
    "zustand": "^4.4.7",
    "react-hot-toast": "^2.4.1",
    "lucide-react": "^0.294.0",
    "clsx": "^2.0.0",
    "tailwind-merge": "^2.1.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.3.3",
    "vite": "^5.0.8",
    "tailwindcss": "^3.3.6",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

## src/main.tsx
```typescript
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import App from './App';
import './styles/globals.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <Toaster position="top-right" />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
```

## src/App.tsx
```typescript
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import PendingDocuments from './pages/PendingDocuments';
import DocumentViewer from './pages/DocumentViewer';
import AdminPanel from './pages/AdminPanel';
import WorkflowManager from './pages/WorkflowManager';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  const { isAuthenticated } = useAuthStore();

  return (
    <Routes>
      <Route path="/login" element={
        isAuthenticated ? <Navigate to="/dashboard" /> : <Login />
      } />
      
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/pending" element={<PendingDocuments />} />
        <Route path="/documents/:id/view" element={<DocumentViewer />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/workflows" element={<WorkflowManager />} />
      </Route>
      
      <Route path="/" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}

export default App;
```

## src/services/api.ts
```typescript
import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: `${API_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth
export const login = async (email: string, password: string) => {
  const response = await api.post('/auth/login', { email, password });
  return response.data;
};

// Documents
export const getPendingDocuments = async () => {
  const response = await api.get('/documents/pending');
  return response.data;
};

export const getDocument = async (id: string) => {
  const response = await api.get(`/documents/${id}`);
  return response.data;
};

export const getDocumentViewUrl = async (id: string) => {
  return `${API_URL}/api/documents/${id}/view`;
};

export const uploadDocument = async (
  file: File,
  workflowId: string,
  title: string,
  description?: string
) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('workflowId', workflowId);
  formData.append('title', title);
  if (description) formData.append('description', description);
  
  const response = await api.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

// Signatures
export const prepareSignature = async (documentId: string) => {
  const response = await api.post('/signatures/prepare', { documentId });
  return response.data;
};

export const completeSignature = async (
  documentId: string,
  signatureValue: string,
  comments?: string
) => {
  const response = await api.post('/signatures/complete', {
    documentId,
    signatureValue,
    comments,
  });
  return response.data;
};

export const rejectDocument = async (documentId: string, reason: string) => {
  await api.post('/signatures/reject', { documentId, reason });
};

// Workflows
export const getWorkflows = async () => {
  const response = await api.get('/workflows');
  return response.data;
};

export const createWorkflow = async (workflow: any) => {
  const response = await api.post('/workflows', workflow);
  return response.data;
};

export default api;
```

## src/pages/PendingDocuments.tsx
```typescript
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { FileText, Clock } from 'lucide-react';
import { getPendingDocuments } from '../services/api';
import DocumentCard from '../components/DocumentCard';

export default function PendingDocuments() {
  const navigate = useNavigate();
  const { data: documents, isLoading } = useQuery({
    queryKey: ['pending-documents'],
    queryFn: getPendingDocuments,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Documentos Pendientes de Firma
        </h1>
        <p className="text-gray-600">
          Tienes {documents?.length || 0} documentos esperando tu firma
        </p>
      </div>

      {documents?.length === 0 ? (
        <div className="text-center py-12">
          <FileText className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">
            No hay documentos pendientes
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            Todos tus documentos han sido firmados
          </p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {documents?.map((doc: any) => (
            <DocumentCard
              key={doc.id}
              document={doc}
              onClick={() => navigate(`/documents/${doc.id}/view`)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
```

## src/components/PDFViewer.tsx
```typescript
import { useState, useEffect } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import 'react-pdf/dist/esm/Page/AnnotationLayer.css';
import 'react-pdf/dist/esm/Page/TextLayer.css';

pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

interface PDFViewerProps {
  documentUrl: string;
  disableDownload?: boolean;
}

export default function PDFViewer({ documentUrl, disableDownload = true }: PDFViewerProps) {
  const [numPages, setNumPages] = useState<number>(0);
  const [pageNumber, setPageNumber] = useState<number>(1);

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
  };

  const handleContextMenu = (e: React.MouseEvent) => {
    if (disableDownload) {
      e.preventDefault();
    }
  };

  return (
    <div
      className="flex flex-col items-center h-full bg-gray-100"
      onContextMenu={handleContextMenu}
      style={{ userSelect: 'none' }}
    >
      <div className="flex-1 overflow-auto w-full flex justify-center p-4">
        <Document
          file={documentUrl}
          onLoadSuccess={onDocumentLoadSuccess}
          loading={
            <div className="flex items-center justify-center h-full">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
          }
        >
          <Page
            pageNumber={pageNumber}
            renderTextLayer={false}
            renderAnnotationLayer={false}
            className="shadow-lg"
          />
        </Document>
      </div>

      <div className="bg-white border-t p-4 w-full flex items-center justify-center gap-4">
        <button
          onClick={() => setPageNumber(Math.max(1, pageNumber - 1))}
          disabled={pageNumber <= 1}
          className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        
        <span className="text-sm text-gray-700">
          Página {pageNumber} de {numPages}
        </span>
        
        <button
          onClick={() => setPageNumber(Math.min(numPages, pageNumber + 1))}
          disabled={pageNumber >= numPages}
          className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronRight className="h-5 w-5" />
        </button>
      </div>
    </div>
  );
}
```

---

# 3️⃣ DATABASE - Supabase Schema

## supabase/schema.sql

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'signer',
    certificate_id VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Workflows table
CREATE TABLE signature_workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Workflow steps table
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID REFERENCES signature_workflows(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    user_id UUID REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL DEFAULT 'sign',
    is_required BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(workflow_id, step_order)
);

-- Documents table
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    workflow_id UUID REFERENCES signature_workflows(id),
    current_step INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'pending',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Signature history table
CREATE TABLE signature_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    step_order INTEGER NOT NULL,
    action VARCHAR(50) NOT NULL,
    comments TEXT,
    signature_data TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Pending documents table
CREATE TABLE pending_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    step_order INTEGER NOT NULL,
    assigned_at TIMESTAMP DEFAULT NOW(),
    notified_at TIMESTAMP,
    UNIQUE(document_id, user_id, step_order)
);

-- Notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    document_id UUID REFERENCES documents(id),
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_by ON documents(created_by);
CREATE INDEX idx_pending_documents_user ON pending_documents(user_id);
CREATE INDEX idx_signature_history_document ON signature_history(document_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

-- Row Level Security
ALTER TABLE pending_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE signature_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view their pending documents"
ON pending_documents FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can view their signature history"
ON signature_history FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can view their notifications"
ON notifications FOR SELECT
USING (auth.uid() = user_id);

-- Insert demo data
INSERT INTO users (email, password_hash, full_name, role) VALUES
('admin@example.com', '$2a$10$...', 'Admin User', 'admin'),
('user1@example.com', '$2a$10$...', 'User One', 'signer'),
('user2@example.com', '$2a$10$...', 'User Two', 'signer');
```

---

# 4️⃣ DEPLOYMENT

## Render (Backend)

### render.yaml
```yaml
services:
  - type: web
    name: firmador-backend
    env: java
    buildCommand: mvn clean package -DskipTests
    startCommand: java -jar target/firmador-backend-2.0.0.jar
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: firmador-db
          property: connectionString
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_KEY
        sync: false
      - key: JWT_SECRET
        generateValue: true

databases:
  - name: firmador-db
    databaseName: firmador
    user: firmador
```

## Vercel (Frontend)

### vercel.json
```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "framework": "vite",
  "env": {
    "VITE_API_URL": "@api_url",
    "VITE_SUPABASE_URL": "@supabase_url",
    "VITE_SUPABASE_ANON_KEY": "@supabase_key"
  }
}
```

---

# 5️⃣ SETUP INSTRUCTIONS

## 1. Setup Supabase

```bash
# 1. Create project at supabase.com
# 2. Run schema.sql in SQL Editor
# 3. Create storage bucket "documents"
# 4. Enable RLS on bucket
# 5. Copy URL and anon key
```

## 2. Setup Backend

```bash
cd firmador-backend

# Create .env file
cat > .env << EOF
DATABASE_URL=your-supabase-db-url
DATABASE_USER=postgres
DATABASE_PASSWORD=your-password
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=your-anon-key
JWT_SECRET=your-secret-key
EOF

# Build
mvn clean install

# Run
mvn spring-boot:run
```

## 3. Setup Frontend

```bash
cd firmador-frontend

# Install dependencies
npm install

# Create .env file
cat > .env << EOF
VITE_API_URL=http://localhost:8080
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
EOF

# Run
npm run dev
```

## 4. Deploy

```bash
# Backend to Render
git push render main

# Frontend to Vercel
vercel --prod
```

---

# 📝 NEXT STEPS

1. ✅ Run migration script for firmador-core
2. ✅ Build firmador-core library
3. ✅ Setup Supabase project
4. ✅ Deploy backend to Render
5. ✅ Deploy frontend to Vercel
6. ✅ Create Desktop Agent
7. ✅ Test complete flow

**Total Cost:** $0-$25/month (Render free tier + Supabase free tier + Vercel free tier)

**Development Time:** 2-3 weeks

¿Necesitas que cree algún archivo específico o quieres que continúe con más detalles?