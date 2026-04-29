# Firmador Frontend — Desarrollo local

Pasos para ejecutar el frontend localmente en Windows (y comprobar el build en CI).

1) Instalar Node.js (recomendado LTS 18.x)

- Descarga el instalador para Windows desde https://nodejs.org/ y ejecuta el MSI.
- Verifica en PowerShell:

```powershell
node -v
npm -v
```

2) Ejecutar la app localmente

```powershell
cd firmador-frontend
npm install
npm run dev
```

3) Variables de entorno (opcional)

Crear un archivo `.env` en `firmador-frontend` con:

```
VITE_API_URL=http://localhost:8080
VITE_SUPABASE_URL=https://your-supabase-url
VITE_SUPABASE_ANON_KEY=your-anon-key
```

4) CI/CD

He añadido un workflow GitHub Actions en `.github/workflows/frontend-ci.yml` que ejecuta `npm ci` y `npm run build` en pushes/PRs que afecten `firmador-frontend`. Si empujas los cambios, verás el resultado del build en Actions.

Notas:
- Si no quieres instalar Node localmente ahora, empuja la rama y revisa el resultado del workflow en GitHub Actions.
- Si necesitas que yo haga cambios adicionales para que el build pase en CI, indícalo y los aplicaré.
