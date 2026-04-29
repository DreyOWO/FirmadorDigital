# 🚀 Deployment Guide - Complete Setup

Esta guía contiene todas las configuraciones necesarias para desplegar el sistema completo.

## 📋 Tabla de Contenidos

1. [Arquitectura de Deployment](#arquitectura)
2. [Backend - Render](#backend-render)
3. [Frontend - Vercel](#frontend-vercel)
4. [Database - Supabase](#database-supabase)
5. [Desktop Agent](#desktop-agent)
6. [CI/CD Pipeline](#cicd)
7. [Monitoring & Logs](#monitoring)

---

## 🏗️ Arquitectura de Deployment

```
┌─────────────────────────────────────────────────────────────┐
│                         INTERNET                             │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                │                           │
        ┌───────▼────────┐         ┌───────▼────────┐
        │   Vercel CDN   │         │  Render.com    │
        │   (Frontend)   │         │   (Backend)    │
        │  React + Vite  │         │  Spring Boot   │
        └───────┬────────┘         └───────┬────────┘
                │                           │
                │         ┌─────────────────┘
                │         │
        ┌───────▼─────────▼────────┐
        │   Supabase Cloud         │
        │  - PostgreSQL Database   │
        │  - Storage (S3-like)     │
        │  - Authentication        │
        │  - Real-time             │
        └──────────────────────────┘
                │
        ┌───────▼────────┐
        │  User's PC     │
        │ Desktop Agent  │
        │ (Smart Card)   │
        └────────────────┘
```

**Costos Estimados:**
- Vercel: $0/mes (Free tier)
- Render: $0-7/mes (Free tier o Starter)
- Supabase: $0-25/mes (Free tier o Pro)
- **Total: $0-32/mes**

---

## 🔧 Backend - Render

### render.yaml

```yaml
services:
  # Spring Boot Backend
  - type: web
    name: firmador-backend
    env: java
    region: oregon
    plan: free  # or starter ($7/month)
    buildCommand: |
      cd firmador-backend
      mvn clean package -DskipTests
    startCommand: |
      java -Dserver.port=$PORT \
           -Xmx512m \
           -jar target/firmador-backend-2.0.0.jar
    healthCheckPath: /api/health
    envVars:
      - key: JAVA_OPTS
        value: -Xmx512m -Xms256m
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: DATABASE_URL
        fromDatabase:
          name: firmador-db
          property: connectionString
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_KEY
        sync: false
      - key: SUPABASE_SERVICE_KEY
        sync: false
      - key: JWT_SECRET
        generateValue: true
      - key: CORS_ALLOWED_ORIGINS
        value: https://firmador.vercel.app,https://firmador-*.vercel.app

databases:
  - name: firmador-db
    databaseName: firmador
    user: firmador
    plan: free  # 256MB RAM, 1GB storage
```

### Dockerfile (Opcional - para deployment manual)

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM
COPY pom-parent.xml pom.xml

# Copy modules
COPY firmador-core firmador-core/
COPY firmador-backend firmador-backend/

# Build
RUN mvn clean package -DskipTests -pl firmador-backend -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy JAR
COPY --from=build /app/firmador-backend/target/firmador-backend-*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Run
ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Xmx512m", \
            "-Xms256m", \
            "-jar", \
            "app.jar"]
```

### application-production.yml

```yaml
spring:
  application:
    name: firmador-backend
  
  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

server:
  port: ${PORT:8080}
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  error:
    include-message: always
    include-stacktrace: never

supabase:
  url: ${SUPABASE_URL}
  key: ${SUPABASE_KEY}
  service-key: ${SUPABASE_SERVICE_KEY}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}

logging:
  level:
    root: INFO
    cr.libre.firmador: INFO
    org.springframework.web: INFO
    org.hibernate: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### Deploy Steps

```bash
# 1. Create Render account at render.com

# 2. Connect GitHub repository

# 3. Create new Web Service
#    - Name: firmador-backend
#    - Environment: Java
#    - Build Command: cd firmador-backend && mvn clean package -DskipTests
#    - Start Command: java -jar target/firmador-backend-2.0.0.jar

# 4. Add environment variables:
SPRING_PROFILES_ACTIVE=production
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_KEY=your-anon-key
SUPABASE_SERVICE_KEY=your-service-key
JWT_SECRET=your-random-secret-key-min-32-chars
CORS_ALLOWED_ORIGINS=https://firmador.vercel.app

# 5. Deploy!
```

---

## 🎨 Frontend - Vercel

### vercel.json

```json
{
  "version": 2,
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "framework": "vite",
  "installCommand": "npm install",
  "devCommand": "npm run dev",
  "env": {
    "VITE_API_URL": "@api_url",
    "VITE_SUPABASE_URL": "@supabase_url",
    "VITE_SUPABASE_ANON_KEY": "@supabase_key",
    "VITE_AGENT_URL": "ws://localhost:9876"
  },
  "build": {
    "env": {
      "VITE_API_URL": "@api_url",
      "VITE_SUPABASE_URL": "@supabase_url",
      "VITE_SUPABASE_ANON_KEY": "@supabase_key"
    }
  },
  "routes": [
    {
      "src": "/api/(.*)",
      "dest": "$VITE_API_URL/api/$1"
    },
    {
      "src": "/(.*)",
      "dest": "/index.html"
    }
  ],
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "X-Content-Type-Options",
          "value": "nosniff"
        },
        {
          "key": "X-Frame-Options",
          "value": "DENY"
        },
        {
          "key": "X-XSS-Protection",
          "value": "1; mode=block"
        },
        {
          "key": "Referrer-Policy",
          "value": "strict-origin-when-cross-origin"
        }
      ]
    }
  ]
}
```

### .env.production

```env
VITE_API_URL=https://firmador-backend.onrender.com
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
VITE_AGENT_URL=ws://localhost:9876
```

### Deploy Steps

```bash
# 1. Install Vercel CLI
npm install -g vercel

# 2. Login to Vercel
vercel login

# 3. Navigate to frontend directory
cd firmador-frontend

# 4. Deploy to production
vercel --prod

# 5. Set environment variables in Vercel dashboard:
#    - VITE_API_URL
#    - VITE_SUPABASE_URL
#    - VITE_SUPABASE_ANON_KEY

# 6. Configure custom domain (optional)
vercel domains add firmador.tu-dominio.com
```

### GitHub Actions for Vercel

```yaml
# .github/workflows/deploy-frontend.yml
name: Deploy Frontend to Vercel

on:
  push:
    branches: [main]
    paths:
      - 'firmador-frontend/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install Vercel CLI
        run: npm install -g vercel
      
      - name: Deploy to Vercel
        working-directory: ./firmador-frontend
        run: |
          vercel --token ${{ secrets.VERCEL_TOKEN }} --prod --yes
        env:
          VERCEL_ORG_ID: ${{ secrets.VERCEL_ORG_ID }}
          VERCEL_PROJECT_ID: ${{ secrets.VERCEL_PROJECT_ID }}
```

---

## 🗄️ Database - Supabase

### Setup Steps

```bash
# 1. Create Supabase project at supabase.com

# 2. Run migrations in SQL Editor (in order):
#    - 001_initial_schema.sql
#    - 002_rls_policies.sql
#    - 003_functions.sql
#    - 004_seed_data.sql
#    - storage/buckets.sql

# 3. Configure Storage:
#    - Create 'documents' bucket (private)
#    - Create 'signed-documents' bucket (private)
#    - Apply storage policies

# 4. Configure Authentication:
#    - Enable Email authentication
#    - Set up email templates
#    - Configure SMTP (optional)

# 5. Get credentials:
#    - Project URL: https://xxx.supabase.co
#    - Anon Key: (from Settings > API)
#    - Service Key: (from Settings > API)
```

### Backup Configuration

```bash
# Install Supabase CLI
npm install -g supabase

# Login
supabase login

# Link project
supabase link --project-ref your-project-ref

# Create backup
supabase db dump -f backup.sql

# Restore backup
supabase db reset
psql -h db.xxx.supabase.co -U postgres -d postgres -f backup.sql
```

---

## 💻 Desktop Agent

### Electron App Structure

```
firmador-agent/
├── package.json
├── main.js
├── preload.js
├── renderer/
│   ├── index.html
│   └── app.js
└── build/
    ├── icon.ico
    └── icon.png
```

### package.json

```json
{
  "name": "firmador-agent",
  "version": "1.0.0",
  "main": "main.js",
  "scripts": {
    "start": "electron .",
    "build": "electron-builder",
    "build:win": "electron-builder --win",
    "build:mac": "electron-builder --mac",
    "build:linux": "electron-builder --linux"
  },
  "dependencies": {
    "ws": "^8.14.2",
    "node-forge": "^1.3.1"
  },
  "devDependencies": {
    "electron": "^27.0.0",
    "electron-builder": "^24.6.4"
  },
  "build": {
    "appId": "cr.libre.firmador.agent",
    "productName": "Firmador Agent",
    "directories": {
      "output": "dist"
    },
    "files": [
      "main.js",
      "preload.js",
      "renderer/**/*"
    ],
    "win": {
      "target": "nsis",
      "icon": "build/icon.ico"
    },
    "mac": {
      "target": "dmg",
      "icon": "build/icon.png"
    },
    "linux": {
      "target": "AppImage",
      "icon": "build/icon.png"
    }
  }
}
```

### main.js

```javascript
const { app, BrowserWindow, Tray, Menu } = require('electron');
const WebSocket = require('ws');
const path = require('path');

let mainWindow;
let tray;
let wss;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 400,
    height: 300,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  mainWindow.loadFile('renderer/index.html');
  
  // Hide instead of close
  mainWindow.on('close', (event) => {
    if (!app.isQuitting) {
      event.preventDefault();
      mainWindow.hide();
    }
  });
}

function createTray() {
  tray = new Tray(path.join(__dirname, 'build/icon.png'));
  
  const contextMenu = Menu.buildFromTemplate([
    { label: 'Abrir', click: () => mainWindow.show() },
    { label: 'Estado: Conectado', enabled: false },
    { type: 'separator' },
    { label: 'Salir', click: () => {
      app.isQuitting = true;
      app.quit();
    }}
  ]);
  
  tray.setToolTip('Firmador Agent');
  tray.setContextMenu(contextMenu);
  
  tray.on('click', () => {
    mainWindow.show();
  });
}

function startWebSocketServer() {
  wss = new WebSocket.Server({ port: 9876 });
  
  wss.on('connection', (ws) => {
    console.log('Client connected');
    
    ws.on('message', async (message) => {
      try {
        const request = JSON.parse(message);
        const response = await handleCommand(request);
        ws.send(JSON.stringify(response));
      } catch (error) {
        ws.send(JSON.stringify({
          id: request.id,
          error: error.message
        }));
      }
    });
    
    ws.on('close', () => {
      console.log('Client disconnected');
    });
  });
  
  console.log('WebSocket server started on port 9876');
}

async function handleCommand(request) {
  const { id, command, params } = request;
  
  switch (command) {
    case 'list_certificates':
      return {
        id,
        result: await listCertificates()
      };
    
    case 'sign_data':
      return {
        id,
        result: await signData(params.certificateId, params.dataToSign)
      };
    
    default:
      throw new Error(`Unknown command: ${command}`);
  }
}

async function listCertificates() {
  // TODO: Implement PKCS#11 certificate listing
  // This would use the firmador-core library
  return [
    {
      id: 'cert-1',
      subject: 'CN=Juan Pérez',
      issuer: 'CN=CA SINPE',
      validFrom: '2024-01-01',
      validTo: '2026-01-01'
    }
  ];
}

async function signData(certificateId, dataToSign) {
  // TODO: Implement PKCS#11 signing
  // This would use the firmador-core library
  return 'base64-encoded-signature';
}

app.whenReady().then(() => {
  createWindow();
  createTray();
  startWebSocketServer();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

app.on('before-quit', () => {
  if (wss) {
    wss.close();
  }
});
```

### Build & Distribute

```bash
# Build for Windows
npm run build:win

# Build for macOS
npm run build:mac

# Build for Linux
npm run build:linux

# Output will be in dist/ folder
```

---

## 🔄 CI/CD Pipeline

### .github/workflows/deploy-all.yml

```yaml
name: Deploy All Services

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Test Backend
        run: |
          cd firmador-backend
          mvn test
  
  deploy-backend:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Render
        run: |
          curl -X POST ${{ secrets.RENDER_DEPLOY_HOOK }}
  
  deploy-frontend:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Deploy to Vercel
        working-directory: ./firmador-frontend
        run: |
          npm install -g vercel
          vercel --token ${{ secrets.VERCEL_TOKEN }} --prod --yes
        env:
          VERCEL_ORG_ID: ${{ secrets.VERCEL_ORG_ID }}
          VERCEL_PROJECT_ID: ${{ secrets.VERCEL_PROJECT_ID }}
```

---

## 📊 Monitoring & Logs

### Render Monitoring

```bash
# View logs
render logs -s firmador-backend

# View metrics
render metrics -s firmador-backend
```

### Vercel Monitoring

```bash
# View deployment logs
vercel logs firmador-frontend

# View analytics
vercel analytics
```

### Supabase Monitoring

- Go to Supabase Dashboard
- Navigate to Database > Logs
- Check API logs, Database logs, and Storage logs

---

## ✅ Deployment Checklist

### Pre-Deployment

- [ ] Run all tests locally
- [ ] Update environment variables
- [ ] Review security settings
- [ ] Backup database
- [ ] Test in staging environment

### Backend Deployment

- [ ] Create Render account
- [ ] Connect GitHub repository
- [ ] Configure environment variables
- [ ] Deploy backend service
- [ ] Verify health check endpoint
- [ ] Test API endpoints

### Frontend Deployment

- [ ] Create Vercel account
- [ ] Connect GitHub repository
- [ ] Configure environment variables
- [ ] Deploy frontend
- [ ] Test all pages
- [ ] Verify API integration

### Database Setup

- [ ] Create Supabase project
- [ ] Run all migrations
- [ ] Configure storage buckets
- [ ] Set up RLS policies
- [ ] Test authentication
- [ ] Verify data access

### Desktop Agent

- [ ] Build agent for target platforms
- [ ] Test smart card integration
- [ ] Create installer
- [ ] Distribute to users

### Post-Deployment

- [ ] Monitor logs for errors
- [ ] Test complete workflow
- [ ] Verify email notifications
- [ ] Check performance metrics
- [ ] Update documentation

---

## 🆘 Troubleshooting

### Backend Issues

```bash
# Check logs
render logs -s firmador-backend --tail

# Restart service
render restart -s firmador-backend

# Check environment variables
render env list -s firmador-backend
```

### Frontend Issues

```bash
# Check build logs
vercel logs

# Redeploy
vercel --prod

# Clear cache
vercel --prod --force
```

### Database Issues

```sql
-- Check connections
SELECT count(*) FROM pg_stat_activity;

-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

## 📞 Support

- **Documentation:** See README files in each module
- **Issues:** GitHub Issues
- **Email:** support@firmador.cr

---

## 🎉 Success!

Your complete digital signature system is now deployed and ready to use!

**URLs:**
- Frontend: https://firmador.vercel.app
- Backend API: https://firmador-backend.onrender.com
- Database: https://xxx.supabase.co

**Next Steps:**
1. Share the frontend URL with users
2. Distribute the Desktop Agent
3. Monitor usage and performance
4. Gather user feedback
5. Iterate and improve!