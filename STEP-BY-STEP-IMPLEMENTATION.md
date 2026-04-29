# 📋 Guía Paso a Paso - Implementación Completa

Esta guía detalla **exactamente** qué hacer para implementar el sistema completo.

---

## 🎯 Resumen Ejecutivo

**Tiempo estimado total:** 2-3 semanas  
**Costo mensual:** $0-32 USD  
**Complejidad:** Media  
**Requisitos:** Conocimientos básicos de Java, React, SQL

---

## 📅 Plan de Implementación

### Semana 1: Infraestructura y Backend
- Días 1-2: Configurar servicios cloud
- Días 3-5: Implementar backend

### Semana 2: Frontend y Base de Datos
- Días 1-3: Implementar frontend
- Días 4-5: Configurar base de datos

### Semana 3: Testing y Deployment
- Días 1-3: Testing completo
- Días 4-5: Deployment y documentación

---

## 🚀 FASE 1: Preparación (1-2 horas)

### Paso 1.1: Crear Cuentas Necesarias

```bash
# 1. Crear cuenta en Supabase
https://supabase.com/dashboard/sign-up
- Email: tu-email@empresa.com
- Crear nuevo proyecto: "firmador-CruzRoja"
- Región: South America (São Paulo) - más cercana a CR
- Anotar: Project URL y API Keys

# 2. Crear cuenta en Render
https://dashboard.render.com/register
- Conectar con GitHub
- Verificar email

# 3. Crear cuenta en Vercel
https://vercel.com/signup
- Conectar con GitHub
- Verificar email

# 4. Preparar repositorio GitHub
https://github.com/new
- Nombre: firmador-digital
- Visibilidad: Private (recomendado)
- Inicializar con README
```

### Paso 1.2: Instalar Herramientas Locales

```bash
# En Windows (usando Chocolatey)
choco install git nodejs maven openjdk17 vscode

# En macOS (usando Homebrew)
brew install git node maven openjdk@17

# En Linux (Ubuntu/Debian)
sudo apt update
sudo apt install git nodejs npm maven openjdk-17-jdk

# Verificar instalaciones
git --version          # Debe ser 2.30+
node --version         # Debe ser 18+
mvn --version          # Debe ser 3.8+
java -version          # Debe ser 17+
```

### Paso 1.3: Clonar y Preparar Proyecto

```bash
# 1. Clonar tu repositorio actual
cd C:\Users\Andrey\source\repos
git clone https://github.com/tu-usuario/firmador-digital.git
cd firmador-digital

# 2. Verificar estructura
ls -la
# Deberías ver:
# - firmador-core/
# - firmador-gui/
# - firmador-backend/
# - pom-parent.xml
# - BACKEND-COMPLETE-CODE.md
# - FRONTEND-COMPLETE-CODE.md
# - etc.
```

---

## 🗄️ FASE 2: Configurar Base de Datos (2-3 horas)

### Paso 2.1: Crear Proyecto Supabase

```bash
# 1. Ir a Supabase Dashboard
https://supabase.com/dashboard

# 2. Crear nuevo proyecto
- Name: firmador-production
- Database Password: [generar contraseña segura y guardarla]
- Region: South America (São Paulo)
- Pricing Plan: Free (para empezar)

# 3. Esperar ~2 minutos mientras se crea el proyecto

# 4. Anotar credenciales (Settings > API)
Project URL: https://xxxxxxxxxxxxx.supabase.co
anon/public key: eyJhbGc...
service_role key: eyJhbGc... (¡MANTENER SECRETO!)
```

### Paso 2.2: Ejecutar Migrations

```sql
-- 1. Ir a SQL Editor en Supabase Dashboard
https://supabase.com/dashboard/project/[tu-project-id]/sql

-- 2. Crear nueva query y ejecutar en orden:

-- MIGRATION 1: Initial Schema
-- Copiar TODO el contenido de DATABASE-COMPLETE-SCHEMA.md
-- Sección: "MIGRATION 001: Initial Schema"
-- Click "Run" (debe tomar ~10 segundos)

-- MIGRATION 2: RLS Policies
-- Copiar contenido de "MIGRATION 002: Row Level Security Policies"
-- Click "Run"

-- MIGRATION 3: Functions
-- Copiar contenido de "MIGRATION 003: Functions and Triggers"
-- Click "Run"

-- MIGRATION 4: Seed Data
-- Copiar contenido de "MIGRATION 004: Seed Data"
-- Click "Run"

-- 3. Verificar que todo se creó correctamente
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public';

-- Deberías ver 8 tablas:
-- users, signature_workflows, workflow_steps, documents,
-- signature_history, pending_documents, notifications, audit_log
```

### Paso 2.3: Configurar Storage

```sql
-- 1. Ir a Storage en Supabase Dashboard
https://supabase.com/dashboard/project/[tu-project-id]/storage

-- 2. Crear bucket "documents"
- Name: documents
- Public: NO (privado)
- File size limit: 50 MB
- Allowed MIME types: application/pdf, application/msword, etc.

-- 3. Crear bucket "signed-documents"
- Name: signed-documents
- Public: NO (privado)
- File size limit: 50 MB

-- 4. Aplicar políticas de storage
-- Ir a SQL Editor y ejecutar:
-- (Copiar de DATABASE-COMPLETE-SCHEMA.md sección "STORAGE CONFIGURATION")
```

### Paso 2.4: Verificar Setup

```sql
-- Ejecutar en SQL Editor para verificar
-- 1. Verificar usuarios demo
SELECT email, full_name, role FROM users;

-- 2. Verificar workflows
SELECT w.name, COUNT(ws.id) as steps
FROM signature_workflows w
LEFT JOIN workflow_steps ws ON w.id = ws.workflow_id
GROUP BY w.id, w.name;

-- 3. Test de función
SELECT get_unread_notification_count('00000000-0000-0000-0000-000000000002');
```

---

## 🔧 FASE 3: Implementar Backend (1-2 días)

### Paso 3.1: Copiar Código del Backend

```bash
# 1. Abrir BACKEND-COMPLETE-CODE.md

# 2. Crear estructura de directorios
cd firmador-backend/src/main/java/cr/libre/firmador/backend
mkdir -p model repository dto service controller config security

# 3. Copiar cada archivo del documento
# Por ejemplo, para User.java:
# - Abrir BACKEND-COMPLETE-CODE.md
# - Buscar "### User.java"
# - Copiar el código
# - Crear archivo: model/User.java
# - Pegar el código

# Repetir para TODOS los archivos listados en BACKEND-COMPLETE-CODE.md:
# Models: User, Document, Workflow, WorkflowStep, SignatureHistory, PendingDocument, Notification
# Repositories: UserRepository, DocumentRepository, etc.
# DTOs: LoginRequest, LoginResponse, DocumentDTO, etc.
# Security: JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
# Config: SecurityConfig, CorsConfig, SupabaseConfig
```

### Paso 3.2: Configurar Variables de Entorno

```bash
# 1. Crear archivo .env en firmador-backend/
cd firmador-backend
touch .env

# 2. Agregar configuración (reemplazar con tus valores)
cat > .env << EOF
# Database
DATABASE_URL=jdbc:postgresql://db.xxxxxxxxxxxxx.supabase.co:5432/postgres
DATABASE_USER=postgres
DATABASE_PASSWORD=tu-password-de-supabase

# Supabase
SUPABASE_URL=https://xxxxxxxxxxxxx.supabase.co
SUPABASE_KEY=tu-anon-key
SUPABASE_SERVICE_KEY=tu-service-key

# JWT
JWT_SECRET=tu-secreto-aleatorio-minimo-32-caracteres-muy-seguro

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# Spring Profile
SPRING_PROFILES_ACTIVE=development
EOF
```

### Paso 3.3: Compilar y Probar Backend

```bash
# 1. Compilar el proyecto
cd firmador-backend
mvn clean install

# Si hay errores de compilación:
# - Verificar que todas las clases estén creadas
# - Verificar imports
# - Verificar que pom.xml tenga todas las dependencias

# 2. Ejecutar tests
mvn test

# 3. Ejecutar aplicación
mvn spring-boot:run

# 4. Verificar que está corriendo
# Abrir navegador: http://localhost:8080/api/health
# Deberías ver: {"status":"UP"}
```

### Paso 3.4: Probar Endpoints

```bash
# Usar Postman, Insomnia o curl

# 1. Test de login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@firmador.cr",
    "password": "admin123"
  }'

# Deberías recibir:
# {
#   "accessToken": "eyJhbGc...",
#   "tokenType": "Bearer",
#   "email": "admin@firmador.cr",
#   "fullName": "Administrador del Sistema",
#   "role": "admin"
# }

# 2. Guardar el token y probar endpoint protegido
TOKEN="tu-token-aqui"

curl -X GET http://localhost:8080/api/documents/pending \
  -H "Authorization: Bearer $TOKEN"

# Deberías recibir: []
```

---

## 🎨 FASE 4: Implementar Frontend (1-2 días)

### Paso 4.1: Crear Proyecto Frontend

```bash
# 1. Crear proyecto con Vite
cd ..  # Volver a raíz del proyecto
npm create vite@latest firmador-frontend -- --template react-ts

# 2. Entrar al directorio
cd firmador-frontend

# 3. Instalar dependencias base
npm install

# 4. Instalar dependencias adicionales
npm install react-router-dom @tanstack/react-query @supabase/supabase-js
npm install react-pdf axios zustand react-hot-toast lucide-react
npm install clsx tailwind-merge date-fns

# 5. Instalar dependencias de desarrollo
npm install -D tailwindcss autoprefixer postcss
npm install -D @types/react @types/react-dom

# 6. Inicializar Tailwind
npx tailwindcss init -p
```

### Paso 4.2: Copiar Configuración

```bash
# 1. Copiar archivos de configuración de FRONTEND-COMPLETE-CODE.md

# package.json - reemplazar el generado con el del documento
# tsconfig.json - reemplazar
# vite.config.ts - reemplazar
# tailwind.config.js - reemplazar
# postcss.config.js - crear nuevo
# index.html - reemplazar

# 2. Crear archivo .env
cat > .env << EOF
VITE_API_URL=http://localhost:8080
VITE_SUPABASE_URL=https://xxxxxxxxxxxxx.supabase.co
VITE_SUPABASE_ANON_KEY=tu-anon-key
VITE_AGENT_URL=ws://localhost:9876
EOF
```

### Paso 4.3: Copiar Código del Frontend

```bash
# 1. Crear estructura de directorios
mkdir -p src/{pages,components,services,stores,hooks,types,utils,styles}

# 2. Copiar cada archivo de FRONTEND-COMPLETE-CODE.md

# Types
# - Copiar src/types/index.ts

# Services
# - Copiar src/services/api.ts
# - Copiar src/services/agentClient.ts

# Stores
# - Copiar src/stores/authStore.ts

# Hooks
# - Copiar src/hooks/usePendingDocuments.ts
# - Copiar src/hooks/useDocument.ts
# - Copiar src/hooks/useSignature.ts

# Pages
# - Copiar src/pages/Login.tsx
# - Copiar src/pages/PendingDocuments.tsx
# - Copiar src/pages/DocumentViewer.tsx (ver COMPLETE-IMPLEMENTATION-GUIDE.md)
# - Copiar src/pages/Dashboard.tsx (ver COMPLETE-IMPLEMENTATION-GUIDE.md)

# Components
# - Copiar src/components/PDFViewer.tsx
# - Copiar src/components/Layout.tsx (ver COMPLETE-IMPLEMENTATION-GUIDE.md)
# - Copiar src/components/ProtectedRoute.tsx (ver COMPLETE-IMPLEMENTATION-GUIDE.md)
# - Copiar src/components/DocumentCard.tsx (ver COMPLETE-IMPLEMENTATION-GUIDE.md)

# Styles
# - Copiar src/styles/globals.css

# Main files
# - Copiar src/main.tsx
# - Copiar src/App.tsx
```

### Paso 4.4: Ejecutar Frontend

```bash
# 1. Instalar dependencias (si no lo hiciste)
npm install

# 2. Ejecutar en modo desarrollo
npm run dev

# 3. Abrir navegador
http://localhost:5173

# 4. Probar login
# Email: admin@firmador.cr
# Password: admin123

# Deberías poder iniciar sesión y ver el dashboard
```

---

## 🧪 FASE 5: Testing Local (1 día)

### Paso 5.1: Test de Flujo Completo

```bash
# 1. Asegurarse que backend está corriendo
cd firmador-backend
mvn spring-boot:run

# 2. En otra terminal, asegurarse que frontend está corriendo
cd firmador-frontend
npm run dev

# 3. Abrir navegador en http://localhost:5173

# 4. Probar flujo completo:
# - Login como admin
# - Crear workflow (si tienes la página implementada)
# - Subir documento (si tienes la página implementada)
# - Login como usuario
# - Ver documentos pendientes
# - Abrir documento en visor
# - Firmar documento (requiere Desktop Agent)
```

### Paso 5.2: Verificar Base de Datos

```sql
-- En Supabase SQL Editor

-- 1. Verificar documentos creados
SELECT * FROM documents ORDER BY created_at DESC;

-- 2. Verificar documentos pendientes
SELECT 
    pd.*,
    d.title,
    u.full_name
FROM pending_documents pd
JOIN documents d ON pd.document_id = d.id
JOIN users u ON pd.user_id = u.id;

-- 3. Verificar historial de firmas
SELECT 
    sh.*,
    d.title,
    u.full_name
FROM signature_history sh
JOIN documents d ON sh.document_id = d.id
JOIN users u ON sh.user_id = u.id
ORDER BY sh.created_at DESC;
```

---

## 🚀 FASE 6: Deployment (1-2 días)

### Paso 6.1: Deploy Backend a Render

```bash
# 1. Commit y push tu código a GitHub
git add .
git commit -m "Backend implementation complete"
git push origin main

# 2. Ir a Render Dashboard
https://dashboard.render.com

# 3. Crear nuevo Web Service
- Click "New +"
- Select "Web Service"
- Connect GitHub repository
- Select "firmador-digital" repo

# 4. Configurar servicio
Name: firmador-backend
Region: Oregon (US West)
Branch: main
Root Directory: firmador-backend
Runtime: Java
Build Command: mvn clean package -DskipTests
Start Command: java -jar target/firmador-backend-2.0.0.jar

# 5. Agregar variables de entorno
DATABASE_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
DATABASE_USER=postgres
DATABASE_PASSWORD=[tu-password]
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_KEY=[tu-anon-key]
SUPABASE_SERVICE_KEY=[tu-service-key]
JWT_SECRET=[tu-secreto]
SPRING_PROFILES_ACTIVE=production
CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app

# 6. Click "Create Web Service"
# Esperar ~5-10 minutos para el primer deploy

# 7. Verificar que está funcionando
https://firmador-backend.onrender.com/api/health
```

### Paso 6.2: Deploy Frontend a Vercel

```bash
# 1. Instalar Vercel CLI
npm install -g vercel

# 2. Login a Vercel
vercel login

# 3. Ir al directorio del frontend
cd firmador-frontend

# 4. Crear archivo vercel.json (copiar de DEPLOYMENT-GUIDE.md)

# 5. Deploy
vercel

# Seguir prompts:
# - Set up and deploy? Yes
# - Which scope? [tu-cuenta]
# - Link to existing project? No
# - Project name? firmador-frontend
# - Directory? ./
# - Override settings? No

# 6. Deploy a producción
vercel --prod

# 7. Configurar variables de entorno en Vercel Dashboard
https://vercel.com/[tu-usuario]/firmador-frontend/settings/environment-variables

VITE_API_URL=https://firmador-backend.onrender.com
VITE_SUPABASE_URL=https://xxxxx.supabase.co
VITE_SUPABASE_ANON_KEY=[tu-anon-key]
VITE_AGENT_URL=ws://localhost:9876

# 8. Redeploy para aplicar variables
vercel --prod

# 9. Tu app estará disponible en:
https://firmador-frontend.vercel.app
```

### Paso 6.3: Configurar Dominio Personalizado (Opcional)

```bash
# En Vercel Dashboard
# 1. Ir a Settings > Domains
# 2. Agregar dominio: firmador.tu-empresa.com
# 3. Configurar DNS según instrucciones de Vercel
# 4. Esperar propagación DNS (~24 horas)

# En Render Dashboard
# 1. Ir a Settings > Custom Domain
# 2. Agregar dominio: api.firmador.tu-empresa.com
# 3. Configurar DNS según instrucciones de Render
```

---

## 💻 FASE 7: Desktop Agent (Opcional - 2-3 días)

### Paso 7.1: Crear Proyecto Electron

```bash
# 1. Crear directorio
mkdir firmador-agent
cd firmador-agent

# 2. Inicializar proyecto
npm init -y

# 3. Instalar dependencias
npm install electron ws node-forge
npm install --save-dev electron-builder

# 4. Copiar código de DEPLOYMENT-GUIDE.md
# - main.js
# - preload.js
# - renderer/index.html
# - package.json (actualizar)

# 5. Crear iconos
# - build/icon.ico (Windows)
# - build/icon.png (macOS/Linux)
```

### Paso 7.2: Integrar con firmador-core

```bash
# El Desktop Agent necesita usar firmador-core para:
# - Leer certificados de tarjetas inteligentes
# - Firmar datos con PKCS#11

# Opciones:
# 1. Usar JNI (Java Native Interface) desde Node.js
# 2. Crear un servicio Java separado que el Agent llame
# 3. Usar node-java para llamar código Java directamente

# Recomendación: Crear servicio Java separado
# Ver COMPLETE-IMPLEMENTATION-GUIDE.md para detalles
```

### Paso 7.3: Build y Distribución

```bash
# Build para Windows
npm run build:win

# Build para macOS
npm run build:mac

# Build para Linux
npm run build:linux

# Los instaladores estarán en dist/
# Distribuir a usuarios finales
```

---

## ✅ FASE 8: Verificación Final

### Checklist Completo

```bash
# Backend
[ ] Backend compila sin errores
[ ] Todos los tests pasan
[ ] API responde en /api/health
[ ] Login funciona correctamente
[ ] Endpoints protegidos requieren token
[ ] Backend deployado en Render
[ ] Variables de entorno configuradas

# Frontend
[ ] Frontend compila sin errores
[ ] Login funciona
[ ] Dashboard se muestra correctamente
[ ] Documentos pendientes se listan
[ ] Visor PDF funciona
[ ] Frontend deployado en Vercel
[ ] Variables de entorno configuradas

# Base de Datos
[ ] Todas las tablas creadas
[ ] RLS policies aplicadas
[ ] Functions y triggers funcionando
[ ] Storage buckets creados
[ ] Usuarios demo existen
[ ] Workflows demo existen

# Integración
[ ] Frontend se conecta a backend
[ ] Backend se conecta a Supabase
[ ] Autenticación funciona end-to-end
[ ] Flujo completo de firma funciona
[ ] Notificaciones se envían

# Deployment
[ ] Backend accesible públicamente
[ ] Frontend accesible públicamente
[ ] HTTPS configurado
[ ] CORS configurado correctamente
[ ] Logs accesibles
[ ] Monitoring configurado
```

---

## 📞 Soporte y Troubleshooting

### Problemas Comunes

#### Backend no compila
```bash
# Verificar versión de Java
java -version  # Debe ser 17+

# Limpiar y recompilar
mvn clean install -U

# Verificar dependencias
mvn dependency:tree
```

#### Frontend no inicia
```bash
# Limpiar node_modules
rm -rf node_modules package-lock.json
npm install

# Verificar versión de Node
node --version  # Debe ser 18+
```

#### Error de conexión a base de datos
```bash
# Verificar credenciales en .env
# Verificar que Supabase está activo
# Verificar firewall/network
```

#### CORS errors
```bash
# Verificar CORS_ALLOWED_ORIGINS en backend
# Debe incluir la URL del frontend
# Ejemplo: https://firmador-frontend.vercel.app
```

---

## 📊 Métricas de Éxito

Al finalizar, deberías tener:

✅ **Backend funcionando** en Render  
✅ **Frontend funcionando** en Vercel  
✅ **Base de datos** configurada en Supabase  
✅ **Usuarios demo** pueden iniciar sesión  
✅ **Flujo de firma** funciona end-to-end  
✅ **Documentación** completa  
✅ **Costo mensual** de $0-32 USD  

---

## 🎉 ¡Felicitaciones!

Has implementado exitosamente el sistema completo de firma digital.

**Próximos pasos:**
1. Invitar usuarios reales
2. Crear workflows personalizados
3. Subir documentos reales
4. Monitorear uso y performance
5. Iterar basado en feedback

**¿Necesitas ayuda?**
- Revisar documentación en los archivos MD
- Buscar en logs de Render/Vercel
- Verificar Supabase Dashboard
- Contactar soporte

---

**Tiempo total estimado:** 2-3 semanas  
**Dificultad:** Media  
**Resultado:** Sistema completo de firma digital funcionando en producción

¡Éxito! 🚀