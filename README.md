# ⚽ Biwenger Assistant

Asistente web para analizar una liga privada de Biwenger y ayudar a tomar decisiones sobre plantilla, mercado, jornadas y movimientos.

El objetivo del proyecto no es sustituir a Biwenger ni realizar automáticamente operaciones sobre la cuenta del usuario, sino utilizar sus datos para ofrecer una capa adicional de análisis, estadísticas y recomendaciones.

La aplicación dispone de un backend desarrollado con Spring Boot, un frontend Angular y una base de datos PostgreSQL. La versión de producción se ejecuta con Docker en una Raspberry Pi y el frontend se publica mediante Cloudflare Pages.

---

## 📌 Objetivo del proyecto

Biwenger Assistant nace como una herramienta personal para complementar el uso habitual de Biwenger.

La aplicación permite centralizar información de una liga, conservar datos históricos y utilizarlos para generar análisis que serían difíciles de obtener directamente desde la aplicación original.

Entre sus objetivos principales están:

- Consultar los jugadores de la liga.
- Analizar la plantilla del usuario.
- Consultar el mercado.
- Mantener históricos de precios.
- Mantener históricos de puntuaciones y jornadas.
- Consultar estadísticas.
- Analizar movimientos.
- Generar recomendaciones.
- Mostrar el estado de la jornada.
- Mantener los datos actualizados automáticamente.
- Informar al usuario sobre la frescura de los datos.
- Permitir varios usuarios con distintos niveles de permisos.

Biwenger Assistant es principalmente una aplicación de **lectura, análisis y recomendación**.

No pretende reconstruir todas las funcionalidades de Biwenger ni realizar automáticamente operaciones como alineaciones, compras, ventas o pujas.

---

# 🏗️ Arquitectura

La aplicación está dividida principalmente en tres componentes:

```text
┌──────────────────────────────┐
│          Frontend            │
│           Angular            │
│                              │
│      Cloudflare Pages        │
└──────────────┬───────────────┘
               │ HTTPS
               ▼
┌──────────────────────────────┐
│       Tailscale Funnel       │
│                              │
│ Terminación HTTPS / Proxy    │
└──────────────┬───────────────┘
               │
               │ 127.0.0.1:8080
               ▼
┌──────────────────────────────┐
│           Backend            │
│        Spring Boot           │
│                              │
│        Docker / RPi          │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│         PostgreSQL           │
│            Docker            │
└──────────────────────────────┘
```

El frontend y el backend se encuentran en orígenes diferentes, por lo que la aplicación está preparada para trabajar con autenticación basada en sesión, cookies cross-origin, CORS y protección CSRF.

---

# 🧰 Stack tecnológico

## Backend

- Java 21
- Spring Boot 4.1
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Maven
- Docker

## Frontend

- Angular
- TypeScript
- HTML
- SCSS
- Angular Router
- HttpClient

## Infraestructura

- Raspberry Pi 4 Model B
- Raspberry Pi OS 64-bit
- Docker
- Docker Compose
- PostgreSQL 17
- Tailscale
- Tailscale Funnel
- Cloudflare Pages

## Desarrollo

El proyecto se desarrolla principalmente desde Windows utilizando herramientas como:

- IntelliJ IDEA
- Visual Studio Code
- PowerShell
- Git
- Postman
- DBeaver

---

# 📂 Estructura general

```text
biwenger-assistant/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │
│   │   └── test/
│   │
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   ├── assets/
│   │   └── environments/
│   │
│   ├── angular.json
│   └── package.json
│
├── docker-compose.yml
└── README.md
```

---

# ⚙️ Backend

El backend constituye el núcleo de Biwenger Assistant.

Sus responsabilidades principales son:

- Comunicación con Biwenger.
- Sincronización de datos.
- Persistencia.
- Gestión de usuarios.
- Gestión de ligas.
- Seguridad.
- Estadísticas.
- Recomendaciones.
- Gestión de jornadas.
- Mercado.
- Movimientos.
- Histórico de jugadores.
- Exposición de la API REST utilizada por el frontend.

---

# 🔄 Sincronización con Biwenger

La aplicación mantiene una copia local de los datos necesarios para evitar depender de una llamada externa en cada consulta del frontend.

Las sincronizaciones actualizan diferentes conjuntos de información, entre ellos:

- Liga.
- Jugadores.
- Plantillas.
- Mercado.
- Precios.
- Informes de jugadores.
- Puntuaciones.
- Jornadas.
- Movimientos.
- Ofertas.
- Información adicional necesaria para estadísticas y recomendaciones.

Existe una sincronización automática periódica.

La configuración actual utiliza por defecto un intervalo de:

```text
15 minutos
```

entre ejecuciones, salvo que se configure otro valor mediante las propiedades correspondientes.

---

## Estados de sincronización

Una sincronización puede encontrarse en los siguientes estados:

```text
IDLE
RUNNING
SUCCESS
PARTIAL
FAILED
```

### SUCCESS

Todas las fases relevantes de la sincronización han terminado correctamente.

### PARTIAL

La sincronización ha podido actualizar datos, pero alguna de sus fases no se ha completado totalmente.

Esto permite evitar que una actualización incompleta sea presentada falsamente como correcta.

### FAILED

Se ha producido un error que impide completar la sincronización.

### RUNNING

Existe actualmente una sincronización en ejecución.

---

# 🟢 Frescura de los datos

El frontend muestra un indicador global sobre el estado de los datos.

Puede informar, entre otros estados, de:

- Datos actualizados.
- Tiempo transcurrido desde la última actualización.
- Sincronización en curso.
- Sincronización parcial.
- Datos incompletos.
- Limitación temporal de Biwenger.
- Error de sincronización.

El frontend consulta periódicamente el estado para mantener este indicador actualizado.

---

# 🗓️ Jornadas

Biwenger Assistant conserva información histórica sobre jornadas y puntuaciones de los jugadores.

La implementación contempla que una misma jornada lógica pueda aparecer en Biwenger mediante diferentes identificadores internos.

Esto es especialmente importante cuando existen:

- Partidos aplazados.
- Jornadas divididas en varios tramos.
- Partidos disputados semanas después de la jornada original.

Los informes de jugadores se identifican utilizando el jugador y el identificador del partido de Biwenger, evitando tratar todos los partidos de una misma jornada lógica como si fueran el mismo registro.

Para representar una jornada lógica se puede seleccionar posteriormente el segmento más reciente disponible para esa jornada.

---

# 🧮 Sistema de puntuación

Biwenger Assistant trabaja con el sistema de puntuación configurado para la liga.

La fórmula utilizada actualmente incluye componentes procedentes de las puntuaciones base y diferentes eventos del partido.

De forma simplificada:

```text
(score2 * 0.5)
+ (score3 * 0.5)
+ (savedPenalties * 3)
+ (penaltyMissed * -1)
+ (ownGoals * -1)
+ bonificaciones por asistencias según posición
+ bonificaciones por minutos jugados según posición
+ bonificaciones defensivas según posición
+ bonificaciones específicas relacionadas con goles
```

La aplicación dispone además de una sección **Algoritmos** en el frontend donde se documentan de forma comprensible los criterios utilizados por los diferentes análisis y recomendaciones.

---

# 📊 Estadísticas y recomendaciones

Uno de los principales objetivos de Biwenger Assistant es transformar los datos sincronizados en información útil.

La aplicación dispone de funcionalidades relacionadas con:

- Estadísticas de jugadores.
- Evolución de precios.
- Rendimiento.
- Plantilla.
- Mercado.
- Movimientos.
- Jornada.
- Recomendaciones.

El motor de recomendaciones utiliza información disponible en la base de datos para generar análisis sin modificar directamente la cuenta de Biwenger.

---

# 👥 Usuarios y roles

La aplicación diferencia actualmente dos roles principales:

```text
ADMIN
USER
```

## ADMIN

Dispone de permisos administrativos y puede realizar operaciones restringidas como determinadas modificaciones y sincronizaciones manuales.

## USER

Puede utilizar las funcionalidades normales de consulta y análisis de la aplicación, pero no modificar recursos administrativos.

---

# 🔐 Aislamiento de ligas

El acceso a recursos pertenecientes a una liga se valida de forma centralizada.

Los usuarios normales únicamente pueden acceder a los recursos de las ligas para las que tienen autorización.

La comprobación se realiza mediante componentes específicos de acceso a liga antes de permitir el acceso a rutas del tipo:

```text
/api/leagues/{leagueId}/...
```

Los administradores pueden disponer de acceso adicional cuando sea necesario.

Esto evita que un usuario pueda cambiar manualmente un identificador de liga en una petición y consultar información perteneciente a otra liga.

---

# 🔒 Seguridad

La aplicación utiliza Spring Security y varias capas de protección.

## Autenticación

La autenticación se basa en sesión.

Las contraseñas de los usuarios se almacenan utilizando BCrypt.

Después de iniciar sesión, el navegador mantiene la sesión mediante la cookie correspondiente.

---

## CORS

Frontend y backend utilizan dominios diferentes en producción.

Por ello el backend dispone de configuración CORS para permitir únicamente los orígenes autorizados y trabajar con credenciales.

---

## CSRF

Las operaciones que modifican información están protegidas mediante tokens CSRF.

El frontend obtiene un token mediante:

```text
GET /api/auth/csrf
```

y lo envía posteriormente en las peticiones que requieren protección.

El login constituye una excepción específica, ya que todavía no existe una sesión autenticada cuando se realiza.

---

## Cookies

La configuración de producción permite el funcionamiento cross-origin mediante cookies seguras.

Entre otras propiedades se utilizan:

```text
Secure
SameSite=None
```

---

# 🛡️ Protección del login

El endpoint público de autenticación dispone de protección básica frente a intentos repetidos de acceso.

La configuración actual permite:

```text
5 intentos fallidos
```

dentro de una ventana de:

```text
5 minutos
```

Después del quinto fallo, la IP queda temporalmente bloqueada durante:

```text
15 minutos
```

Los siguientes intentos reciben:

```text
HTTP 429 Too Many Requests
```

junto con:

```text
Retry-After
```

indicando cuánto tiempo queda aproximadamente hasta que expire el bloqueo.

Un inicio de sesión correcto elimina los intentos fallidos asociados a esa IP.

El limitador se mantiene actualmente en memoria, por lo que se reinicia al reiniciar el backend.

Esta solución está diseñada para el despliegue actual de una única instancia.

---

# 🌐 Obtención de la IP del cliente

En producción el backend no se publica directamente en Internet.

Docker expone el servicio únicamente mediante:

```text
127.0.0.1:8080
```

Tailscale Funnel actúa como proxy público delante del backend.

La aplicación utiliza la información reenviada por el proxy para identificar la IP del cliente utilizada por el rate limiter.

---

# 🔑 Protección de credenciales

Las credenciales sensibles utilizadas para acceder a servicios externos no deben almacenarse directamente en texto plano dentro del repositorio.

La aplicación dispone de mecanismos para proteger las credenciales almacenadas utilizando cifrado autenticado.

Los secretos y variables privadas deben mantenerse fuera de Git.

Nunca deben versionarse archivos como:

```text
.env
```

ni otros ficheros que contengan credenciales reales.

El repositorio puede contener archivos de ejemplo como:

```text
.env.example
```

sin secretos reales.

---

# 🗄️ Base de datos

La aplicación utiliza PostgreSQL.

En producción PostgreSQL se ejecuta mediante Docker y no se publica directamente hacia Internet.

La persistencia se gestiona principalmente mediante:

- Spring Data JPA
- Hibernate

Los cambios de estructura se gestionan mediante:

- Flyway

---

# 🛫 Migraciones

Las migraciones se encuentran en los recursos del backend y se ejecutan automáticamente al iniciar la aplicación.

Flyway:

1. Comprueba la versión actual de la base de datos.
2. Valida las migraciones existentes.
3. Ejecuta las migraciones pendientes.
4. Impide continuar silenciosamente ante determinadas inconsistencias de esquema.

No se deben modificar migraciones que ya hayan sido aplicadas en producción.

Los cambios posteriores de estructura deben introducirse mediante una nueva migración.

---

# 🐳 Docker

En producción se utilizan al menos los servicios:

```text
backend
postgres
```

Para comprobar su estado:

```bash
docker compose ps
```

Para consultar los logs del backend:

```bash
docker compose logs --tail=100 backend
```

Para seguirlos en tiempo real:

```bash
docker compose logs -f backend
```

---

# 🚀 Despliegue del backend

El backend se ejecuta actualmente en una Raspberry Pi.

Repositorio:

```bash
~/biwenger-assistant
```

Para actualizar el código:

```bash
cd ~/biwenger-assistant

git status
git pull --ff-only
```

Antes de continuar debe comprobarse que no existen modificaciones locales inesperadas.

Después puede reconstruirse el backend:

```bash
docker compose build backend
docker compose up -d backend
```

Comprobar:

```bash
docker compose ps
```

y:

```bash
docker compose logs --tail=100 backend
```

---

# ♻️ Reiniciar únicamente el backend

Cuando no es necesario reconstruir la imagen:

```bash
docker compose restart backend
```

Esto no reinicia PostgreSQL.

---

# 🌍 Publicación del backend

El backend escucha únicamente en:

```text
127.0.0.1:8080
```

y no se publica directamente mediante el puerto 8080 de la Raspberry.

Tailscale Funnel publica el servicio HTTPS y realiza proxy hacia:

```text
http://127.0.0.1:8080
```

La configuración puede comprobarse mediante:

```bash
tailscale serve status
```

En el despliegue actual se utiliza:

```text
https://raspdiego.tailcdc3f7.ts.net
```

como entrada HTTPS del backend.

---

# ☁️ Frontend en producción

El frontend se publica mediante Cloudflare Pages.

URL:

```text
https://biwenger-assistant.pages.dev
```

Cloudflare Pages construye y publica el frontend a partir de la rama configurada para el despliegue.

Actualmente el desarrollo y despliegue de la V1 se realiza desde:

```text
feat/league-management
```

---

# 💻 Ejecución local

## Requisitos

Para trabajar con el proyecto se necesita, como mínimo:

- Java 21
- Node.js / npm
- PostgreSQL o Docker
- Git

---

## Backend

Desde la raíz del proyecto:

### Windows

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Alternativamente, desde el directorio `backend`:

```powershell
cd backend
..\mvnw.cmd spring-boot:run
```

La API queda disponible normalmente en:

```text
http://localhost:8080
```

---

## Tests backend

Desde la raíz:

```powershell
.\mvnw.cmd -f backend\pom.xml test
```

o desde `backend`:

```powershell
..\mvnw.cmd test
```

Antes de desplegar cambios importantes se recomienda ejecutar siempre la suite completa.

---

## Frontend

Entrar en:

```powershell
cd frontend
```

Instalar dependencias:

```powershell
npm install
```

Levantar el servidor de desarrollo:

```powershell
npm start
```

o, según la configuración disponible:

```powershell
ng serve
```

---

## Build del frontend

```powershell
cd frontend
npm run build
```

El build debe finalizar sin errores antes de considerar un cambio preparado para producción.

---

# 🧪 Flujo recomendado antes de desplegar

Para cambios relevantes:

```text
1. Implementar
2. Ejecutar tests backend
3. Ejecutar build frontend si procede
4. Revisar git diff
5. Ejecutar git diff --check
6. Revisar git status
7. Commit
8. Push
9. Desplegar
10. Smoke test en producción
```

Comandos útiles:

```bash
git diff
git diff --check
git status --short
git log --oneline -5
```

---

# 🌿 Git

Durante el desarrollo de la V1 se utiliza principalmente:

```text
feat/league-management
```

Antes de hacer push:

```bash
git status --short
git diff --check
```

Después:

```bash
git add ...
git commit -m "..."
git push
```

No deben añadirse accidentalmente secretos, archivos `.env`, backups de credenciales ni archivos temporales.

---

# 🧪 Smoke tests

Después de un despliegue importante conviene comprobar al menos:

### ADMIN

- Login.
- Dashboard.
- Navegación.
- Estado de sincronización.
- Plantilla.
- Mercado.
- Jornada.
- Estadísticas/recomendaciones relevantes.
- Operaciones administrativas necesarias.
- Logout.

### USER

- Login.
- Dashboard.
- Acceso a su propia liga.
- Plantilla.
- Mercado.
- Jornada.
- Estadísticas/recomendaciones.
- Estado de sincronización.
- Imposibilidad de acceder a recursos administrativos.
- Logout.

También debe comprobarse que refrescar la página mantiene correctamente la sesión.

---

# 🚦 Estado actual del proyecto

El proyecto se encuentra en fase de cierre de la primera versión utilizable.

Las funcionalidades principales están implementadas y la auditoría técnica previa a V1 ha cubierto, entre otros aspectos:

- Permisos de escritura.
- Separación entre ADMIN y USER.
- Aislamiento de ligas.
- Protección CSRF.
- Cookies cross-origin.
- Sincronizaciones parciales.
- Jornadas divididas/aplazadas.
- Frescura de datos.
- Rate limiting del login.
- Despliegue frontend.
- Despliegue backend.

El siguiente objetivo es realizar pruebas con usuarios reales y recoger feedback antes de considerar estable la V1.

---

# 📝 Limitaciones conocidas

Existen algunos aspectos que no bloquean la V1 pero están previstos para futuras iteraciones.

## League ID

Actualmente existen puntos de la aplicación que asumen la liga principal mediante un identificador fijo.

Esto deberá eliminarse cuando se amplíe el soporte multi-liga.

---

## Sincronización manual

La sincronización automática dispone de una gestión más completa de resultados parciales.

La sincronización manual todavía puede mejorarse para utilizar exactamente la misma semántica.

---

## Rate limiting

El limitador del login:

- funciona en memoria;
- se reinicia al reiniciar el backend;
- está diseñado para una única instancia;
- no constituye un sistema distribuido de rate limiting.

Para una infraestructura con varias instancias debería sustituirse o complementarse con una solución compartida.

---

## Frontend tests

La cobertura automatizada del backend es actualmente mucho mayor que la del frontend.

Está previsto ampliar las pruebas automatizadas del frontend en futuras iteraciones.

---

## Fechas y zonas horarias

La normalización de fechas y zonas horarias puede reforzarse para garantizar un comportamiento uniforme en escenarios con diferentes zonas horarias.

---

## API Biwenger

La superficie directa de Biwenger está restringida según su finalidad:

- `/api/biwenger/reports` forma parte de la funcionalidad de estadísticas y está disponible para usuarios autenticados.
- `/api/biwenger/test`, `/api/biwenger/league`, `/api/biwenger/competition` y `/api/biwenger/sync/*` son endpoints técnicos reservados al rol `ADMIN` de Biwenger Assistant.

El rol `ADMIN` de Biwenger Assistant es independiente de cualquier rol de administración que un usuario pueda tener dentro de una liga en Biwenger.

---

# 🗺️ Roadmap

## V1

- [x] Backend principal.
- [x] Frontend principal.
- [x] Integración con Biwenger.
- [x] Persistencia PostgreSQL.
- [x] Migraciones Flyway.
- [x] Plantilla.
- [x] Mercado.
- [x] Estadísticas.
- [x] Recomendaciones.
- [x] Jornada.
- [x] Históricos.
- [x] Autenticación.
- [x] Roles ADMIN / USER.
- [x] Aislamiento de ligas.
- [x] CSRF.
- [x] Sincronización automática.
- [x] Detección de sincronizaciones parciales.
- [x] Indicador de frescura.
- [x] Rate limiting del login.
- [x] Página de algoritmos.
- [x] Despliegue Raspberry Pi.
- [x] Tailscale Funnel.
- [x] Frontend Cloudflare Pages.
- [ ] Certificación técnica final de V1.
- [ ] Pruebas con usuarios reales.
- [ ] Corrección del feedback crítico.
- [ ] V1 estable.

## Post-V1

- [ ] Mejorar soporte multi-liga.
- [ ] Aumentar tests frontend.
- [ ] Mejorar normalización de fechas y zonas horarias.
- [ ] Unificar completamente sync manual y automática.
- [ ] Evolucionar el motor de recomendaciones.
- [ ] Mejorar UX según feedback real.
- [ ] Revisar rate limiting si la infraestructura pasa a múltiples instancias.

---

# 🎯 Filosofía del proyecto

Biwenger Assistant debe aportar información útil sin intentar convertirse en una copia de Biwenger.

La prioridad es:

```text
Sincronizar
      ↓
Persistir
      ↓
Analizar
      ↓
Explicar
      ↓
Recomendar
```

y mantener la decisión final en manos del usuario.

---

# 👨‍💻 Autor

Proyecto personal desarrollado por Diego.

Desarrollado inicialmente como proyecto de aprendizaje y evolucionado posteriormente hacia una aplicación funcional desplegada en infraestructura propia.

---

## Estado

```text
Biwenger Assistant
V1 — Testing / Release Candidate
```