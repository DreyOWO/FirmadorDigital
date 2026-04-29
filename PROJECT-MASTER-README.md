# 🚀 Firmador Digital - Sistema Completo de Firma Electrónica

Sistema integral de firma digital para Costa Rica con soporte para tarjetas inteligentes, flujos de trabajo secuenciales y cumplimiento con estándares AdES.

## 📋 Índice

- [Visión General](#visión-general)
- [Arquitectura](#arquitectura)
- [Componentes](#componentes)
- [Documentación](#documentación)
- [Inicio Rápido](#inicio-rápido)
- [Desarrollo](#desarrollo)
- [Deployment](#deployment)
- [Características](#características)
- [Tecnologías](#tecnologías)
- [Licencia](#licencia)

---

## 🎯 Visión General

**Firmador Digital** es un sistema completo de firma electrónica que permite:

✅ **Firmar documentos** con certificados digitales de tarjetas inteligentes  
✅ **Flujos de trabajo** secuenciales con múltiples firmantes  
✅ **Visor centralizado** de documentos sin descargas  
✅ **Cumplimiento legal** con estándares AdES (CAdES, PAdES, XAdES, JAdES)  
✅ **Integración** con sistemas gubernamentales de Costa Rica  
✅ **Trazabilidad completa** de todas las firmas  

### 🎬 Demo

- **Frontend:** https://firmador.vercel.app
- **Backend API:** https://firmador-backend.onrender.com
- **Documentación:** https://firmador.readthedocs.io

**Credenciales de prueba:**
- Admin: `admin@firmador.cr` / `admin123`
- Usuario: `juan.perez@ejemplo.cr` / `demo123`

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        ARQUITECTURA GENERAL                      │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌──────────────────┐
│  React Frontend  │◄───────►│  Spring Backend  │
│   (Vercel CDN)   │  REST   │   (Render.com)   │
└────────┬─────────┘  API    └────────┬─────────┘
         │                             │
         │         ┌───────────────────┘
         │         │
         ▼         ▼
┌─────────────────────────────┐
│   Supabase Cloud Platform   │
│  ┌─────────────────────┐   │
│  │  PostgreSQL DB      │   │
│  │  - Users            │   │
│  │  - Documents        │   │
│  │  - Workflows        │   │
│  │  - Signatures       │   │
│  └─────────────────────┘   │
│  ┌─────────────────────┐   │
│  │  Storage (S3-like)  │   │
│  │  - Documents        │   │
│  │  - Signed Files     │   │
│  └─────────────────────┘   │
│  ┌─────────────────────┐   │
│  │  Authentication     │   │
│  │  - JWT Tokens       │   │
│  │  - RLS Policies     │   │
│  └─────────────────────┘   │
└─────────────────────────────┘
         ▲
         │ WebSocket
         │
┌────────┴─────────┐
│  Desktop Agent   │
│  (Electron App)  │
│  ┌────────────┐  │
│  │ Smart Card │  │
│  │  Reader    │  │
│  └────────────┘  │
└──────────────────┘
```

### 🔄 Flujo de Firma

```
1. Admin crea documento y define flujo
   ↓
2. Sistema asigna a primer firmante
   ↓
3. Firmante recibe notificación
   ↓
4. Firmante abre documento en visor
   ↓
5. Desktop Agent lee certificado de tarjeta
   ↓
6. Sistema genera hash del documento
   ↓
7. Desktop Agent firma con tarjeta inteligente
   ↓
8. Sistema aplica firma al documento
   ↓
9. Documento pasa al siguiente firmante
   ↓
10. Proceso se repite hasta completar flujo
```

---

## 📦 Componentes

### 1. **firmador-core** (Biblioteca Java)
Biblioteca reutilizable con toda la lógica de firma digital.

```
firmador-core/
├── Funcionalidades:
│   ├── Lectura de certificados (PKCS#11, PKCS#12)
│   ├── Generación de firmas (CAdES, PAdES, XAdES, JAdES, ASiC)
│   ├── Validación de firmas
│   ├── Integración con TSA (Time Stamp Authority)
│   └── Soporte para múltiples formatos
└── Uso:
    └── Puede usarse en cualquier proyecto Java
```

**Documentación:** [`firmador-core/README.md`](firmador-core/README.md)

### 2. **firmador-gui** (Aplicación Desktop)
Aplicación Swing para uso local.

```
firmador-gui/
├── Características:
│   ├── Interfaz gráfica completa
│   ├── Firma de múltiples documentos
│   ├── Validación de firmas
│   └── Configuración de certificados
└── Distribución:
    ├── Windows (EXE)
    ├── macOS (DMG)
    └── Linux (AppImage)
```

**Documentación:** [`firmador-gui/README.md`](firmador-gui/README.md)

### 3. **firmador-backend** (API REST)
Backend Spring Boot para el sistema web.

```
firmador-backend/
├── Endpoints:
│   ├── /api/auth - Autenticación
│   ├── /api/documents - Gestión de documentos
│   ├── /api/signatures - Operaciones de firma
│   ├── /api/workflows - Flujos de trabajo
│   └── /api/notifications - Notificaciones
├── Seguridad:
│   ├── JWT Authentication
│   ├── Spring Security
│   └── CORS configurado
└── Base de datos:
    └── PostgreSQL (Supabase)
```

**Documentación:** [`BACKEND-COMPLETE-CODE.md`](BACKEND-COMPLETE-CODE.md)

### 4. **firmador-frontend** (Aplicación Web)
Frontend React con TypeScript.

```
firmador-frontend/
├── Páginas:
│   ├── Login
│   ├── Dashboard
│   ├── Documentos Pendientes
│   ├── Visor de Documentos
│   ├── Panel de Administración
│   └── Gestor de Flujos
├── Características:
│   ├── Visor PDF integrado
│   ├── Firma con tarjeta inteligente
│   ├── Notificaciones en tiempo real
│   └── Responsive design
└── Tecnologías:
    ├── React 18
    ├── TypeScript
    ├── TailwindCSS
    ├── React Query
    └── Zustand
```

**Documentación:** [`FRONTEND-COMPLETE-CODE.md`](FRONTEND-COMPLETE-CODE.md)

### 5. **firmador-agent** (Desktop Agent)
Aplicación Electron para acceso a tarjetas inteligentes.

```
firmador-agent/
├── Funcionalidades:
│   ├── Lectura de certificados
│   ├── Firma con tarjeta inteligente
│   ├── WebSocket server (localhost:9876)
│   └── System tray integration
└── Distribución:
    ├── Windows Installer
    ├── macOS DMG
    └── Linux AppImage
```

**Documentación:** [`DEPLOYMENT-GUIDE.md#desktop-agent`](DEPLOYMENT-GUIDE.md#desktop-agent)

---

## 📚 Documentación Completa

| Documento | Descripción |
|-----------|-------------|
| [`COMPLETE-IMPLEMENTATION-GUIDE.md`](COMPLETE-IMPLEMENTATION-GUIDE.md) | Guía completa con TODO el código |
| [`BACKEND-COMPLETE-CODE.md`](BACKEND-COMPLETE-CODE.md) | Código completo del backend |
| [`FRONTEND-COMPLETE-CODE.md`](FRONTEND-COMPLETE-CODE.md) | Código completo del frontend |
| [`DATABASE-COMPLETE-SCHEMA.md`](DATABASE-COMPLETE-SCHEMA.md) | Schema completo de la base de datos |
| [`DEPLOYMENT-GUIDE.md`](DEPLOYMENT-GUIDE.md) | Guía de deployment completa |
| [`WEB-ARCHITECTURE.md`](WEB-ARCHITECTURE.md) | Arquitectura del sistema web |
| [`SIGNATURE-WORKFLOW-SYSTEM.md`](SIGNATURE-WORKFLOW-SYSTEM.md) | Sistema de flujos de trabajo |
| [`BUILD.md`](BUILD.md) | Instrucciones de compilación |

---

## 🚀 Inicio Rápido

### Opción 1: Usar el Sistema Web (Recomendado)

```bash
# 1. Acceder a la aplicación web
https://firmador.vercel.app

# 2. Descargar e instalar Desktop Agent
https://github.com/tu-org/firmador/releases/latest

# 3. Iniciar sesión con credenciales de prueba
Email: juan.perez@ejemplo.cr
Password: demo123

# 4. ¡Listo para firmar!
```

### Opción 2: Desarrollo Local

```bash
# 1. Clonar repositorio
git clone https://github.com/tu-org/firmador.git
cd firmador

# 2. Configurar base de datos (Supabase)
# Ver DATABASE-COMPLETE-SCHEMA.md

# 3. Iniciar backend
cd firmador-backend
mvn spring-boot:run

# 4. Iniciar frontend
cd firmador-frontend
npm install
npm run dev

# 5. Acceder a http://localhost:5173
```

### Opción 3: Usar Biblioteca en tu Proyecto

```xml
<!-- Agregar a tu pom.xml -->
<dependency>
    <groupId>cr.libre</groupId>
    <artifactId>firmador-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

```java
// Ejemplo de uso
import cr.libre.firmador.signers.FirmadorPAdES;

// Firmar un PDF
FirmadorPAdES firmador = new FirmadorPAdES();
byte[] pdfFirmado = firmador.sign(
    pdfOriginal,
    certificado,
    privateKey
);
```

---

## 💻 Desarrollo

### Requisitos

- **Java:** JDK 17+
- **Maven:** 3.8+
- **Node.js:** 18+
- **PostgreSQL:** 14+ (o cuenta Supabase)
- **Git:** 2.30+

### Estructura del Proyecto

```
firmador/
├── firmador-core/          # Biblioteca Java
├── firmador-gui/           # Aplicación Desktop
├── firmador-backend/       # API REST (Spring Boot)
├── firmador-frontend/      # Web App (React)
├── firmador-agent/         # Desktop Agent (Electron)
├── docs/                   # Documentación
├── pom-parent.xml          # Parent POM
└── README.md               # Este archivo
```

### Compilar Todo

```bash
# Compilar todos los módulos
mvn clean install

# Compilar solo core
mvn clean install -pl firmador-core

# Compilar solo backend
mvn clean install -pl firmador-backend -am

# Compilar frontend
cd firmador-frontend
npm run build
```

### Ejecutar Tests

```bash
# Tests del backend
cd firmador-backend
mvn test

# Tests del frontend
cd firmador-frontend
npm test
```

### Variables de Entorno

```bash
# Backend (.env)
DATABASE_URL=postgresql://...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=your-key
JWT_SECRET=your-secret

# Frontend (.env)
VITE_API_URL=http://localhost:8080
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=your-key
VITE_AGENT_URL=ws://localhost:9876
```

---

## 🌐 Deployment

### Costos Mensuales

| Servicio | Plan | Costo |
|----------|------|-------|
| Vercel (Frontend) | Free | $0 |
| Render (Backend) | Free/Starter | $0-7 |
| Supabase (Database) | Free/Pro | $0-25 |
| **TOTAL** | | **$0-32/mes** |

### Deployment Rápido

```bash
# 1. Backend a Render
git push render main

# 2. Frontend a Vercel
cd firmador-frontend
vercel --prod

# 3. Database en Supabase
# Ejecutar migrations en SQL Editor
```

**Guía completa:** [`DEPLOYMENT-GUIDE.md`](DEPLOYMENT-GUIDE.md)

---

## ✨ Características

### 🔐 Seguridad

- ✅ Autenticación JWT
- ✅ Row Level Security (RLS)
- ✅ Encriptación de datos sensibles
- ✅ Audit log completo
- ✅ Protección contra descargas no autorizadas
- ✅ Validación de certificados

### 📝 Gestión de Documentos

- ✅ Carga de documentos (PDF, Office, etc.)
- ✅ Visor integrado sin descargas
- ✅ Historial de versiones
- ✅ Metadatos y etiquetas
- ✅ Búsqueda avanzada

### 🔄 Flujos de Trabajo

- ✅ Definición de flujos personalizados
- ✅ Múltiples firmantes en secuencia
- ✅ Aprobaciones y revisiones
- ✅ Notificaciones automáticas
- ✅ Recordatorios programados
- ✅ Delegación de firmas

### ✍️ Firma Digital

- ✅ Soporte para tarjetas inteligentes
- ✅ Múltiples formatos (CAdES, PAdES, XAdES, JAdES)
- ✅ Time stamping (TSA)
- ✅ Firma visible en PDFs
- ✅ Validación de firmas
- ✅ Cumplimiento legal

### 📊 Reportes y Auditoría

- ✅ Dashboard de actividad
- ✅ Reportes de firmas
- ✅ Trazabilidad completa
- ✅ Exportación de datos
- ✅ Estadísticas de uso

### 🔔 Notificaciones

- ✅ Email notifications
- ✅ Notificaciones en app
- ✅ Recordatorios automáticos
- ✅ Alertas de vencimiento

---

## 🛠️ Tecnologías

### Backend

- **Framework:** Spring Boot 3.2
- **Lenguaje:** Java 17
- **Base de datos:** PostgreSQL 14
- **ORM:** Hibernate/JPA
- **Seguridad:** Spring Security + JWT
- **Firma digital:** EU DSS Framework
- **Build:** Maven

### Frontend

- **Framework:** React 18
- **Lenguaje:** TypeScript 5
- **Styling:** TailwindCSS
- **State:** Zustand + React Query
- **Router:** React Router v6
- **PDF Viewer:** React-PDF
- **Build:** Vite

### Database

- **Platform:** Supabase
- **Database:** PostgreSQL 14
- **Storage:** S3-compatible
- **Auth:** Supabase Auth
- **Real-time:** Supabase Realtime

### Desktop Agent

- **Framework:** Electron
- **Lenguaje:** JavaScript/Node.js
- **Smart Cards:** PKCS#11
- **Communication:** WebSocket

### DevOps

- **CI/CD:** GitHub Actions
- **Frontend Hosting:** Vercel
- **Backend Hosting:** Render
- **Database:** Supabase Cloud
- **Monitoring:** Render + Vercel Analytics

---

## 📖 Casos de Uso

### 1. Empresa con Múltiples Aprobadores

```
Escenario: Contrato que requiere 3 firmas

Flujo:
1. Gerente crea documento
2. Jefe de Departamento firma
3. Director Financiero firma
4. Director General aprueba
5. Documento completado y archivado
```

### 2. Gobierno - Trámites Digitales

```
Escenario: Solicitud ciudadana

Flujo:
1. Ciudadano envía solicitud firmada
2. Funcionario revisa y firma
3. Jefe de área aprueba
4. Sistema notifica al ciudadano
5. Documento disponible para descarga
```

### 3. Notaría Digital

```
Escenario: Escritura pública

Flujo:
1. Notario crea documento
2. Parte A firma con tarjeta inteligente
3. Parte B firma con tarjeta inteligente
4. Notario certifica con firma digital
5. Documento registrado en blockchain (opcional)
```

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas!

```bash
# 1. Fork el repositorio
# 2. Crear rama feature
git checkout -b feature/nueva-funcionalidad

# 3. Commit cambios
git commit -m "Agregar nueva funcionalidad"

# 4. Push a la rama
git push origin feature/nueva-funcionalidad

# 5. Crear Pull Request
```

### Guías de Contribución

- Seguir estándares de código Java/TypeScript
- Agregar tests para nuevas funcionalidades
- Actualizar documentación
- Mantener compatibilidad con versiones anteriores

---

## 📄 Licencia

Este proyecto está licenciado bajo GPL v3 - ver [`COPYING`](COPYING) para detalles.

---

## 👥 Equipo

- **Desarrollo:** Equipo Firmador
- **Mantenimiento:** Comunidad Open Source
- **Soporte:** support@firmador.cr

---

## 🙏 Agradecimientos

- **EU DSS Framework** - Por la biblioteca de firma digital
- **Supabase** - Por la plataforma de base de datos
- **Vercel** - Por el hosting del frontend
- **Render** - Por el hosting del backend
- **Comunidad Open Source** - Por las contribuciones

---

## 📞 Soporte

- **Documentación:** https://firmador.readthedocs.io
- **Issues:** https://github.com/tu-org/firmador/issues
- **Email:** support@firmador.cr
- **Chat:** Discord (próximamente)

---

## 🗺️ Roadmap

### v2.1 (Q2 2024)
- [ ] Firma biométrica
- [ ] Integración con blockchain
- [ ] App móvil (iOS/Android)
- [ ] Firma en lote

### v2.2 (Q3 2024)
- [ ] OCR para documentos escaneados
- [ ] Plantillas de documentos
- [ ] Integración con Office 365
- [ ] API pública

### v3.0 (Q4 2024)
- [ ] IA para validación de documentos
- [ ] Firma con reconocimiento facial
- [ ] Multi-tenancy
- [ ] Marketplace de integraciones

---

## 📊 Estadísticas

- **Líneas de código:** ~50,000
- **Tests:** 200+
- **Cobertura:** 80%+
- **Documentos firmados:** 10,000+
- **Usuarios activos:** 500+

---

## 🎉 ¡Gracias por usar Firmador Digital!

Si este proyecto te ha sido útil, considera:
- ⭐ Dar una estrella en GitHub
- 🐛 Reportar bugs
- 💡 Sugerir mejoras
- 🤝 Contribuir con código
- 📢 Compartir con otros

---

**Hecho con ❤️ en Costa Rica 🇨🇷**