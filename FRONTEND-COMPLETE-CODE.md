# 🎨 Frontend Complete Code - React + TypeScript

Este documento contiene TODO el código del frontend listo para copiar y pegar.

## 📁 Estructura del Proyecto

```
firmador-frontend/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
├── .env.example
├── tailwind.config.js
├── postcss.config.js
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
    ├── stores/
    │   └── authStore.ts
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

---

## 📦 CONFIGURATION FILES

### package.json
```json
{
  "name": "firmador-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0"
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
    "tailwind-merge": "^2.1.0",
    "date-fns": "^3.0.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.3.3",
    "vite": "^5.0.8",
    "tailwindcss": "^3.3.6",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32",
    "@typescript-eslint/eslint-plugin": "^6.14.0",
    "@typescript-eslint/parser": "^6.14.0",
    "eslint": "^8.55.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "eslint-plugin-react-refresh": "^0.4.5"
  }
}
```

### tsconfig.json
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

### vite.config.ts
```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### tailwind.config.js
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
      },
    },
  },
  plugins: [],
}
```

### postcss.config.js
```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

### index.html
```html
<!doctype html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Firmador Digital - Sistema de Firma Electrónica</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

### .env.example
```env
VITE_API_URL=http://localhost:8080
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
VITE_AGENT_URL=ws://localhost:9876
```

---

## 🎯 TYPES

### src/types/index.ts
```typescript
export interface User {
  id: string;
  email: string;
  fullName: string;
  role: 'admin' | 'signer' | 'viewer';
  certificateId?: string;
  isActive: boolean;
  createdAt: string;
}

export interface Document {
  id: string;
  title: string;
  description?: string;
  filePath: string;
  fileSize: number;
  mimeType: string;
  workflowId: string;
  currentStep: number;
  status: 'pending' | 'in_progress' | 'completed' | 'rejected';
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  steps: WorkflowStep[];
  isActive: boolean;
  createdBy: string;
  createdAt: string;
}

export interface WorkflowStep {
  id: string;
  workflowId: string;
  stepOrder: number;
  userId: string;
  actionType: 'sign' | 'approve' | 'review';
  isRequired: boolean;
}

export interface SignatureHistory {
  id: string;
  documentId: string;
  userId: string;
  stepOrder: number;
  action: 'signed' | 'rejected' | 'approved';
  comments?: string;
  signatureData?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

export interface Notification {
  id: string;
  userId: string;
  documentId?: string;
  type: 'pending_signature' | 'document_completed' | 'document_rejected';
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  email: string;
  fullName: string;
  role: string;
}

export interface SignatureRequest {
  documentId: string;
}

export interface CompleteSignature {
  documentId: string;
  signatureValue: string;
  comments?: string;
}

export interface RejectRequest {
  documentId: string;
  reason: string;
}
```

---

## 🔐 SERVICES

### src/services/api.ts
```typescript
import axios from 'axios';
import type {
  LoginRequest,
  LoginResponse,
  Document,
  Workflow,
  Notification,
  CompleteSignature,
  RejectRequest,
} from '../types';

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

// Handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth
export const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
  const response = await api.post('/auth/login', credentials);
  return response.data;
};

export const logout = async (): Promise<void> => {
  await api.post('/auth/logout');
  localStorage.removeItem('access_token');
};

// Documents
export const getPendingDocuments = async (): Promise<Document[]> => {
  const response = await api.get('/documents/pending');
  return response.data;
};

export const getDocument = async (id: string): Promise<Document> => {
  const response = await api.get(`/documents/${id}`);
  return response.data;
};

export const getDocumentViewUrl = (id: string): string => {
  const token = localStorage.getItem('access_token');
  return `${API_URL}/api/documents/${id}/view?token=${token}`;
};

export const uploadDocument = async (
  file: File,
  workflowId: string,
  title: string,
  description?: string
): Promise<Document> => {
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

export const getDocumentHistory = async (): Promise<Document[]> => {
  const response = await api.get('/documents/history');
  return response.data;
};

// Signatures
export const prepareSignature = async (documentId: string): Promise<any> => {
  const response = await api.post('/signatures/prepare', { documentId });
  return response.data;
};

export const completeSignature = async (data: CompleteSignature): Promise<Document> => {
  const response = await api.post('/signatures/complete', data);
  return response.data;
};

export const rejectDocument = async (data: RejectRequest): Promise<void> => {
  await api.post('/signatures/reject', data);
};

// Workflows
export const getWorkflows = async (): Promise<Workflow[]> => {
  const response = await api.get('/workflows');
  return response.data;
};

export const createWorkflow = async (workflow: Partial<Workflow>): Promise<Workflow> => {
  const response = await api.post('/workflows', workflow);
  return response.data;
};

export const updateWorkflow = async (id: string, workflow: Partial<Workflow>): Promise<Workflow> => {
  const response = await api.put(`/workflows/${id}`, workflow);
  return response.data;
};

export const deleteWorkflow = async (id: string): Promise<void> => {
  await api.delete(`/workflows/${id}`);
};

// Notifications
export const getNotifications = async (): Promise<Notification[]> => {
  const response = await api.get('/notifications');
  return response.data;
};

export const markNotificationAsRead = async (id: string): Promise<void> => {
  await api.put(`/notifications/${id}/read`);
};

export const getUnreadCount = async (): Promise<number> => {
  const response = await api.get('/notifications/unread-count');
  return response.data;
};

export default api;
```

### src/services/agentClient.ts
```typescript
/**
 * Desktop Agent Client for Smart Card Communication
 * Connects to local WebSocket server running on user's machine
 */

const AGENT_URL = import.meta.env.VITE_AGENT_URL || 'ws://localhost:9876';

export class AgentClient {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(AGENT_URL);

      this.ws.onopen = () => {
        console.log('Connected to Desktop Agent');
        this.reconnectAttempts = 0;
        resolve();
      };

      this.ws.onerror = (error) => {
        console.error('Desktop Agent connection error:', error);
        reject(new Error('Failed to connect to Desktop Agent'));
      };

      this.ws.onclose = () => {
        console.log('Disconnected from Desktop Agent');
        this.attemptReconnect();
      };
    });
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      setTimeout(() => {
        console.log(`Reconnecting to Desktop Agent (attempt ${this.reconnectAttempts})...`);
        this.connect().catch(console.error);
      }, 2000 * this.reconnectAttempts);
    }
  }

  async listCertificates(): Promise<any[]> {
    return this.sendCommand('list_certificates');
  }

  async signData(certificateId: string, dataToSign: string): Promise<string> {
    return this.sendCommand('sign_data', {
      certificateId,
      dataToSign,
    });
  }

  private sendCommand(command: string, params?: any): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
        reject(new Error('Desktop Agent not connected'));
        return;
      }

      const requestId = Math.random().toString(36).substring(7);
      const message = JSON.stringify({
        id: requestId,
        command,
        params,
      });

      const timeout = setTimeout(() => {
        reject(new Error('Request timeout'));
      }, 30000);

      const messageHandler = (event: MessageEvent) => {
        const response = JSON.parse(event.data);
        if (response.id === requestId) {
          clearTimeout(timeout);
          this.ws?.removeEventListener('message', messageHandler);

          if (response.error) {
            reject(new Error(response.error));
          } else {
            resolve(response.result);
          }
        }
      };

      this.ws.addEventListener('message', messageHandler);
      this.ws.send(message);
    });
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

export const agentClient = new AgentClient();
```

---

## 🏪 STORES

### src/stores/authStore.ts
```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, token: string) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      setAuth: (user, token) => {
        localStorage.setItem('access_token', token);
        set({ user, accessToken: token, isAuthenticated: true });
      },
      clearAuth: () => {
        localStorage.removeItem('access_token');
        set({ user: null, accessToken: null, isAuthenticated: false });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);
```

---

## 🪝 HOOKS

### src/hooks/usePendingDocuments.ts
```typescript
import { useQuery } from '@tanstack/react-query';
import { getPendingDocuments } from '../services/api';

export const usePendingDocuments = () => {
  return useQuery({
    queryKey: ['pending-documents'],
    queryFn: getPendingDocuments,
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};
```

### src/hooks/useDocument.ts
```typescript
import { useQuery } from '@tanstack/react-query';
import { getDocument } from '../services/api';

export const useDocument = (id: string) => {
  return useQuery({
    queryKey: ['document', id],
    queryFn: () => getDocument(id),
    enabled: !!id,
  });
};
```

### src/hooks/useSignature.ts
```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { completeSignature, rejectDocument } from '../services/api';
import toast from 'react-hot-toast';

export const useSignature = () => {
  const queryClient = useQueryClient();

  const signMutation = useMutation({
    mutationFn: completeSignature,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-documents'] });
      toast.success('Documento firmado exitosamente');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Error al firmar documento');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: rejectDocument,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-documents'] });
      toast.success('Documento rechazado');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Error al rechazar documento');
    },
  });

  return {
    sign: signMutation.mutate,
    reject: rejectMutation.mutate,
    isSigningLoading: signMutation.isPending,
    isRejectLoading: rejectMutation.isPending,
  };
};
```

---

## 📄 PAGES

### src/pages/Login.tsx
```typescript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { login } from '../services/api';
import { useAuthStore } from '../stores/authStore';
import toast from 'react-hot-toast';
import { FileSignature } from 'lucide-react';

export default function Login() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setAuth(
        {
          id: '',
          email: data.email,
          fullName: data.fullName,
          role: data.role as any,
          isActive: true,
          createdAt: new Date().toISOString(),
        },
        data.accessToken
      );
      toast.success('Inicio de sesión exitoso');
      navigate('/dashboard');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Credenciales inválidas');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loginMutation.mutate({ email, password });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl p-8">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-600 rounded-full mb-4">
            <FileSignature className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Firmador Digital</h1>
          <p className="text-gray-600 mt-2">Sistema de Firma Electrónica</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
              Correo Electrónico
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="usuario@ejemplo.com"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            disabled={loginMutation.isPending}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loginMutation.isPending ? 'Iniciando sesión...' : 'Iniciar Sesión'}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-600">
          <p>¿Olvidaste tu contraseña?</p>
          <a href="#" className="text-blue-600 hover:text-blue-700 font-medium">
            Recuperar contraseña
          </a>
        </div>
      </div>
    </div>
  );
}
```

### src/pages/PendingDocuments.tsx
```typescript
import { useNavigate } from 'react-router-dom';
import { FileText, Clock } from 'lucide-react';
import { usePendingDocuments } from '../hooks/usePendingDocuments';
import DocumentCard from '../components/DocumentCard';

export default function PendingDocuments() {
  const navigate = useNavigate();
  const { data: documents, isLoading } = usePendingDocuments();

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
        <div className="text-center py-12 bg-white rounded-lg shadow">
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
          {documents?.map((doc) => (
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

---

## 🧩 COMPONENTS

### src/components/PDFViewer.tsx
```typescript
import { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut } from 'lucide-react';
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
  const [scale, setScale] = useState<number>(1.0);

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
      className="flex flex-col h-full bg-gray-100"
      onContextMenu={handleContextMenu}
      style={{ userSelect: disableDownload ? 'none' : 'auto' }}
    >
      <div className="flex-1 overflow-auto flex justify-center p-4">
        <Document
          file={documentUrl}
          onLoadSuccess={onDocumentLoadSuccess}
          loading={
            <div className="flex items-center justify-center h-full">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
          }
          error={
            <div className="flex items-center justify-center h-full">
              <p className="text-red-600">Error al cargar el documento</p>
            </div>
          }
        >
          <Page
            pageNumber={pageNumber}
            scale={scale}
            renderTextLayer={!disableDownload}
            renderAnnotationLayer={!disableDownload}
            className="shadow-lg"
          />
        </Document>
      </div>

      <div className="bg-white border-t p-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setScale(Math.max(0.5, scale - 0.1))}
            className="p-2 rounded-lg hover:bg-gray-100"
            title="Alejar"
          >
            <ZoomOut className="h-5 w-5" />
          </button>
          <span className="text-sm text-gray-700 min-w-[60px] text-center">
            {Math.round(scale * 100)}%
          </span>
          <button
            onClick={() => setScale(Math.min(2.0, scale + 0.1))}
            className="p-2 rounded-lg hover:bg-gray-100"
            title="Acercar"
          >
            <ZoomIn className="h-5 w-5" />
          </button>
        </div>

        <div className="flex items-center gap-4">
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

        <div className="w-[140px]"></div>
      </div>
    </div>
  );
}
```

---

## 🎨 STYLES

### src/styles/globals.css
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  * {
    @apply border-border;
  }
  body {
    @apply bg-background text-foreground;
    font-feature-settings: "rlig" 1, "calt" 1;
  }
}

@layer utilities {
  .no-scrollbar::-webkit-scrollbar {
    display: none;
  }
  .no-scrollbar {
    -ms-overflow-style: none;
    scrollbar-width: none;
  }
}

/* Prevent text selection and context menu on PDF viewer */
.pdf-viewer-protected {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.pdf-viewer-protected * {
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
}
```

---

## 🚀 MAIN FILES

### src/main.tsx
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
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              duration: 3000,
              iconTheme: {
                primary: '#10b981',
                secondary: '#fff',
              },
            },
            error: {
              duration: 4000,
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
```

### src/App.tsx
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
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" /> : <Login />}
      />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/pending" element={<PendingDocuments />} />
        <Route path="/documents/:id/view" element={<DocumentViewer />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/workflows" element={<WorkflowManager />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" />} />
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}

export default App;
```

---

## 📋 SETUP INSTRUCTIONS

1. **Crear el proyecto:**
```bash
npm create vite@latest firmador-frontend -- --template react-ts
cd firmador-frontend
```

2. **Instalar dependencias:**
```bash
npm install
```

3. **Copiar archivos de configuración** (package.json, tsconfig.json, etc.)

4. **Crear archivo .env:**
```bash
cp .env.example .env
# Editar .env con tus credenciales
```

5. **Ejecutar en desarrollo:**
```bash
npm run dev
```

6. **Build para producción:**
```bash
npm run build
```

---

## ✅ CHECKLIST

- [x] Configuration files
- [x] Types
- [x] Services (API, Agent)
- [x] Stores (Zustand)
- [x] Hooks
- [x] Pages (Login, PendingDocuments)
- [x] Components (PDFViewer)
- [x] Styles
- [x] Main files

**Nota:** Los componentes adicionales (Layout, Navbar, DocumentCard, etc.) están disponibles en el archivo `COMPLETE-IMPLEMENTATION-GUIDE.md`.