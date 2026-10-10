# ⚽ Biwenger Assistant

Asistente web para analizar una liga privada de Biwenger y ayudar a tomar decisiones sobre plantilla, mercado, jornadas y movimientos.

Biwenger Assistant utiliza los datos disponibles de la liga para ofrecer una capa adicional de análisis, estadísticas y recomendaciones.

El objetivo del proyecto no es sustituir a Biwenger ni automatizar las decisiones del usuario, sino transformar los datos sincronizados en información útil para ayudarle a decidir.

La aplicación dispone de:

- Backend desarrollado con Spring Boot.
- Frontend desarrollado con Angular.
- Base de datos PostgreSQL.
- Motor propio de análisis y recomendaciones.
- Infraestructura de producción sobre Raspberry Pi.
- Frontend y proxy de API publicados mediante Cloudflare Pages.
- Acceso público al backend mediante Tailscale Funnel.

El motor de análisis y recomendaciones se encuentra actualmente en:

```text
Engine 2.1 — Stable
```

Engine 2.1 es la baseline estable del motor tras completar su implementación, pruebas, auditoría de regresión, despliegue y validación real.

---

# 📌 Objetivo del proyecto

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
- Analizar oportunidades de mercado.
- Recomendar alineaciones y formaciones.
- Analizar necesidades por posición.
- Tener en cuenta el próximo rival.
- Mostrar el estado de la jornada.
- Mantener los datos actualizados automáticamente.
- Informar sobre la frescura de los datos.
- Explicar las señales utilizadas por los algoritmos.
- Permitir varios usuarios con distintos niveles de permisos.

Biwenger Assistant es principalmente una aplicación de:

```text
lectura
   ↓
análisis
   ↓
explicación
   ↓
recomendación
```

No pretende reconstruir todas las funcionalidades de Biwenger ni realizar automáticamente operaciones como:

- Alineaciones.
- Compras.
- Ventas.
- Pujas.

La decisión final permanece siempre en manos del usuario.

---

# 🏗️ Arquitectura

La arquitectura actual de producción es:

```text
┌─────────────────────────────────────┐
│               Browser               │
│                                     │
│    biwenger-assistant.pages.dev     │
└──────────────────┬──────────────────┘
                   │
                   │ HTTPS
                   │ /api/*
                   ▼
┌─────────────────────────────────────┐
│          Cloudflare Pages           │
│                                     │
│   Angular + Pages Function proxy    │
└──────────────────┬──────────────────┘
                   │
                   │ HTTPS
                   │ servidor → servidor
                   ▼
┌─────────────────────────────────────┐
│           Tailscale Funnel          │
│                                     │
│      Entrada HTTPS Raspberry        │
└──────────────────┬──────────────────┘
                   │
                   │ 127.0.0.1:8080
                   ▼
┌─────────────────────────────────────┐
│              Backend                │
│           Spring Boot               │
│                                     │
│           Docker / RPi              │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│            PostgreSQL               │
│              Docker                 │
└─────────────────────────────────────┘
```

En producción el navegador utiliza:

```text
/api
```

como URL base de la API.

Las peticiones son recibidas por una **Cloudflare Pages Function**, que actúa como proxy hacia el backend publicado mediante Tailscale Funnel.

Desde el punto de vista del navegador:

```text
Frontend + API = same-origin
```

El navegador no necesita comunicarse directamente con el dominio de Tailscale.

Esta arquitectura evita depender de permisos de acceso a red local del navegador y mantiene la Raspberry detrás de las capas de proxy.

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

- Angular 22
- TypeScript
- HTML
- SCSS
- Angular Router
- HttpClient
- Vitest

## Infraestructura

- Raspberry Pi 4 Model B
- Raspberry Pi OS 64-bit
- Docker
- Docker Compose
- PostgreSQL 17
- Tailscale
- Tailscale Funnel
- Cloudflare Pages
- Cloudflare Pages Functions

## Desarrollo

El proyecto se desarrolla principalmente desde Windows utilizando:

- IntelliJ IDEA
- Visual Studio Code
- PowerShell
- Git
- Postman
- DBeaver
- Docker Desktop
- Tailscale

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
│   ├── functions/
│   │   └── api/
│   │       └── [[path]].js
│   │
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

`README.md` constituye la documentación principal y única del repositorio.

La documentación funcional detallada del motor se encuentra además disponible dentro de la propia aplicación mediante la sección **Algoritmos**.

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
- Histórico de precios.
- Análisis deportivo.
- Análisis económico.
- Análisis de plantilla.
- Construcción de alineaciones.
- Cálculo de confianza.
- Explicabilidad.
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
- Información necesaria para estadísticas y recomendaciones.

Existe una sincronización automática periódica.

En el despliegue actual de producción se ejecuta cada:

```text
5 minutos
```

salvo que se modifique la configuración correspondiente.

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

Esto evita presentar una actualización incompleta como completamente correcta.

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

La aplicación puede seleccionar el segmento adecuado para representar una jornada lógica.

El motor contempla además jornadas con bloqueo progresivo.

En configuraciones como:

```text
onlyNoPlayed
rollingLockout
```

los jugadores cuyo partido todavía no ha comenzado pueden continuar siendo modificables, mientras que aquellos cuyo partido ya ha empezado quedan bloqueados.

---

# 🧮 Sistema de puntuación

Biwenger Assistant trabaja con el sistema de puntuación configurado para la liga.

La fórmula utilizada incluye componentes procedentes de las puntuaciones base y diferentes eventos del partido.

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

La sección **Algoritmos** de la aplicación documenta de forma comprensible los criterios, escalas, fórmulas y casos especiales utilizados por los distintos análisis y recomendaciones.

---

# 🧠 Engine 2.1

**Engine 2.1** es la versión estable actual del motor de análisis y recomendaciones.

Su objetivo es enriquecer la lógica existente utilizando mejor la evidencia histórica disponible sin convertir el sistema en una caja negra.

Sus principios principales son:

- No confundir ausencia de datos con mal rendimiento.
- Utilizar señales únicamente cuando existe evidencia suficiente.
- Mantener fallbacks neutrales cuando faltan datos.
- Separar señales deportivas y económicas.
- Limitar el impacto de las correcciones adicionales.
- Mantener compatibilidad con la lógica base.
- Explicar las señales relevantes.
- Reducir la confianza cuando existe poca evidencia.
- Evitar precisión artificial basada en datos insuficientes.

---

## 📈 Forma reciente

La forma reciente analiza los partidos más recientes disponibles dando mayor importancia a la información actual.

Para considerar válida una señal reciente se exige una muestra mínima de partidos participados y puntuados.

Una ausencia, un cambio de temporada o determinadas discontinuidades pueden romper la secuencia considerada reciente.

Esto evita utilizar como forma actual partidos antiguos separados por largos periodos sin participación.

---

## 📚 Rendimiento histórico

El rendimiento histórico utiliza una ventana mayor de partidos puntuados.

Su objetivo es proporcionar una referencia más estable sobre el nivel habitual del jugador.

El motor diferencia deliberadamente:

```text
forma reciente
        +
rendimiento histórico
```

porque representan señales distintas.

La forma reciente responde mejor a cambios inmediatos.

El histórico proporciona estabilidad.

---

## ⭐ Rating deportivo

El rating deportivo combina la evidencia reciente e histórica disponible.

Cuando ambas muestras son válidas, la forma reciente tiene mayor peso que el histórico.

Cuando únicamente una de ellas dispone de evidencia suficiente, el motor utiliza la información disponible.

Cuando ninguna dispone todavía de evidencia suficiente, se utiliza un fallback neutral en lugar de inventar rendimiento.

Posteriormente pueden intervenir otros factores como:

- Disponibilidad.
- Contexto del próximo rival.
- Dinámica deportiva.

---

# ⚔️ Dificultad del próximo rival

El rating puede incorporar el contexto del siguiente partido.

La dificultad del rival tiene en cuenta diferentes indicadores deportivos del equipo contrario y el contexto local/visitante.

Cuando falta información válida para alguno de los componentes se utilizan valores neutrales.

El efecto de la dificultad del rival está limitado para evitar que el contexto de un único partido domine completamente la valoración del jugador.

---

# 📈 Dinámica deportiva 2.1

Engine 2.1 compara la forma reciente con la referencia histórica del jugador.

De forma conceptual:

```text
variación deportiva =
forma reciente
-
rendimiento histórico
```

Esto permite detectar si el jugador parece estar:

```text
mejorando
estable
empeorando
```

La fuerza de la señal depende de la evidencia disponible.

Una diferencia observada sobre una muestra pequeña tiene menos peso que una diferencia respaldada por un histórico suficiente.

La corrección adicional está acotada para impedir que esta señal sustituya al resto del análisis.

---

# 💹 Dinámica económica 2.1

Engine 2.1 amplía el análisis del valor de mercado utilizando el histórico real de precios.

La dinámica económica estudia principalmente tres conceptos:

## Momentum

Mide el ritmo reciente de subida o bajada del valor del jugador.

## Aceleración

Compara el ritmo más reciente con periodos anteriores para detectar si la tendencia está ganando o perdiendo fuerza.

## Consistencia

Analiza hasta qué punto el histórico mantiene una dirección económica coherente.

Estas señales refinan la tendencia económica existente.

No la sustituyen.

Cuando el histórico no contiene evidencia suficiente, el motor conserva la lógica económica base como fallback.

---

# 💰 Análisis de mercado

El motor combina distintas señales para valorar oportunidades del mercado.

Entre ellas pueden intervenir:

- Precio.
- Valor de mercado.
- Tendencia económica.
- Dinámica económica.
- Forma reciente.
- Rendimiento histórico.
- Dinámica deportiva.
- Necesidades de plantilla.
- Estado del jugador.
- Presupuesto.
- Contexto deportivo.

El resultado puede utilizarse para generar:

- Recomendaciones.
- Prioridades.
- Pujas máximas.
- Explicaciones.

El objetivo no es predecir el futuro con certeza, sino ordenar oportunidades utilizando de forma coherente la información disponible.

---

# 🧩 Necesidades de plantilla

El motor analiza las necesidades de la plantilla por posición:

```text
PT
DF
MC
DL
```

Para ello tiene en cuenta factores como:

- Profundidad efectiva.
- Disponibilidad de los jugadores.
- Capacidad de completar formaciones válidas.
- Jugadores multiposición.

Los jugadores con disponibilidad reducida pueden contar parcialmente o no contar para la profundidad efectiva.

Los jugadores multiposición pueden contribuir a diferentes posiciones compatibles sin ser contabilizados simultáneamente varias veces dentro de una misma asignación.

---

# 🧱 Alineaciones y formaciones

El motor puede estudiar distintas formaciones admitidas y buscar combinaciones válidas de jugadores.

Los jugadores multiposición pueden ocupar cualquiera de sus posiciones compatibles.

El algoritmo evita utilizar el mismo jugador simultáneamente en varias posiciones.

Una formación alternativa debe aportar una mejora suficiente antes de convertirse en una recomendación.

Esto evita generar cambios tácticos irrelevantes por diferencias mínimas.

---

# 🎯 Confianza basada en evidencia

Engine 2.1 diferencia entre:

```text
calidad estimada de una recomendación
```

y:

```text
cantidad de evidencia que la respalda
```

Una recomendación puede parecer favorable y, sin embargo, disponer de una confianza menor si existen pocos datos.

La confianza tiene en cuenta la cobertura de evidencia disponible.

Conceptualmente:

```text
reciente + histórico suficientes
→ evidencia alta

solo una muestra suficiente
→ evidencia parcial

datos existentes pero insuficientes
→ evidencia baja

sin datos
→ ausencia de evidencia
```

La confianza es un **índice heurístico**.

No debe interpretarse como una probabilidad matemática de acierto.

---

# 🔎 Explicabilidad

Uno de los objetivos principales de Engine 2.1 es que las recomendaciones puedan explicarse.

El motor puede asociar motivos relacionados con:

- Precio.
- Tendencia económica.
- Dinámica económica.
- Forma reciente.
- Dinámica deportiva.
- Rendimiento histórico.
- Necesidad de una posición.
- Estado del jugador.
- Presupuesto.
- Contexto deportivo.

Las señales nuevas no aparecen simplemente porque exista un dato.

Solo deben formar parte de la explicación cuando intervienen realmente en el análisis correspondiente.

La filosofía es mantener alineados:

```text
datos
  ↓
cálculo
  ↓
señal
  ↓
explicación
  ↓
recomendación
```

---

# 🧯 Datos insuficientes y fallbacks

Engine 2.1 aplica un principio conservador:

> Es preferible no utilizar una métrica que convertir datos incompletos en una señal aparentemente precisa.

Por ello, cuando una señal no dispone de evidencia suficiente:

- No se inventa rendimiento.
- No se inventa tendencia.
- No se interpreta ausencia de datos como mal rendimiento.
- Se utiliza un fallback neutral o la lógica base disponible.
- La confianza puede reducirse.

Algunas señales potencialmente interesantes pueden permanecer fuera del motor mientras la cobertura de datos no sea suficientemente fiable.

---

# 📊 Estadísticas y recomendaciones

Biwenger Assistant transforma los datos sincronizados en información útil para la toma de decisiones.

La aplicación dispone de funcionalidades relacionadas con:

- Estadísticas de jugadores.
- Evolución de precios.
- Rendimiento.
- Plantilla.
- Mercado.
- Movimientos.
- Jornada.
- Recomendaciones.
- Necesidades por posición.
- Alineaciones.
- Formaciones.
- Dificultad del rival.
- Pujas máximas.
- Confianza.
- Explicabilidad.

El motor genera análisis.

No modifica directamente la cuenta de Biwenger.

---

# 📖 Página Algoritmos

La aplicación incluye una página específica de **Algoritmos**.

Su objetivo es documentar de forma comprensible el funcionamiento de las principales métricas y recomendaciones.

El README explica la arquitectura y filosofía general del motor.

La página Algoritmos constituye la referencia funcional detallada sobre:

- Fórmulas.
- Pesos.
- Umbrales.
- Escalas.
- Fallbacks.
- Casos especiales.
- Interpretación de las recomendaciones.

Esto evita duplicar toda la documentación matemática dentro del repositorio y facilita consultar cómo funciona el motor desde la propia aplicación.

---

# 👥 Usuarios y roles

La aplicación diferencia dos roles principales:

```text
ADMIN
USER
```

## ADMIN

Dispone de permisos administrativos y puede realizar operaciones restringidas, como determinadas modificaciones y sincronizaciones manuales.

## USER

Puede utilizar las funcionalidades normales de consulta y análisis, pero no realizar operaciones administrativas.

El rol:

```text
ADMIN de Biwenger Assistant
```

es independiente de cualquier rol de administración que un usuario pueda tener dentro de Biwenger.

---

# 🔐 Aislamiento de ligas

El acceso a recursos pertenecientes a una liga se valida de forma centralizada.

Los usuarios normales únicamente pueden acceder a los recursos de las ligas para las que tienen autorización.

La comprobación se realiza antes de permitir el acceso a rutas del tipo:

```text
/api/leagues/{leagueId}/...
```

Los administradores pueden disponer de acceso adicional cuando sea necesario.

Esto evita que un usuario pueda modificar manualmente un identificador de liga en una petición y consultar información perteneciente a otra liga.

---

# 🔒 Seguridad

La aplicación utiliza Spring Security y varias capas de protección.

## Autenticación

La autenticación se basa en sesión.

Las contraseñas se almacenan utilizando BCrypt.

Después del login, Spring Security mantiene la sesión mediante la cookie correspondiente.

---

## Proxy same-origin

En producción el navegador no llama directamente al dominio público de la Raspberry.

Las peticiones se realizan contra:

```text
https://biwenger-assistant.pages.dev/api/*
```

Cloudflare Pages Functions actúa como proxy hacia el backend.

Por tanto:

```text
Browser
   ↓
Cloudflare Pages
   ↓
/api/*
```

es una comunicación same-origin.

Posteriormente:

```text
Cloudflare
   ↓
Tailscale Funnel
   ↓
Spring Boot
```

se realiza entre servidores.

---

## CORS

El backend conserva una política CORS restrictiva.

La arquitectura actual evita, sin embargo, que el navegador dependa de una petición cross-origin para consumir la API en producción gracias al proxy `/api`.

---

## CSRF

Las operaciones que modifican información están protegidas mediante tokens CSRF.

El frontend obtiene el token mediante:

```text
GET /api/auth/csrf
```

y lo envía cuando corresponde mediante:

```text
X-XSRF-TOKEN
```

El login constituye una excepción específica.

---

## Cookies

La autenticación utiliza cookies seguras.

Entre ellas se encuentran:

```text
JSESSIONID
XSRF-TOKEN
```

La sesión continúa siendo gestionada por Spring Security aunque las peticiones atraviesen el proxy de Cloudflare.

---

# 🛡️ Protección del login

El endpoint público de autenticación dispone de protección frente a intentos repetidos de acceso.

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

Un inicio de sesión correcto elimina los intentos fallidos asociados a esa IP.

El limitador se mantiene actualmente en memoria, por lo que se reinicia al reiniciar el backend.

Está diseñado para el despliegue actual de una única instancia.

---

# 🌐 Ruta de una petición en producción

La petición atraviesa:

```text
Browser
   ↓
Cloudflare Pages
   ↓
Cloudflare Pages Function
   ↓
Tailscale Funnel
   ↓
Spring Boot
   ↓
PostgreSQL
```

El backend no necesita estar publicado directamente en Internet mediante su puerto interno.

---

# 🔑 Protección de credenciales

Las credenciales sensibles no deben almacenarse directamente en el repositorio.

Los secretos y variables privadas deben mantenerse fuera de Git.

Nunca deben versionarse archivos como:

```text
.env
```

ni otros ficheros que contengan:

- Contraseñas.
- Tokens.
- Claves de cifrado.
- Credenciales reales.
- Backups de secretos.

El repositorio puede contener archivos de ejemplo como:

```text
.env.example
```

siempre que no incluyan secretos reales.

---

# 🗄️ Base de datos

La aplicación utiliza:

```text
PostgreSQL 17
```

En producción PostgreSQL se ejecuta mediante Docker en la Raspberry Pi.

El puerto PostgreSQL del host está enlazado únicamente a:

```text
127.0.0.1:5432
```

por lo que la base de datos no se publica directamente hacia Internet.

La persistencia se gestiona principalmente mediante:

- Spring Data JPA.
- Hibernate.
- PostgreSQL.
- Flyway.

Los datos persistentes se almacenan en un volumen Docker.

En producción no debe utilizarse:

```text
docker compose down -v
```

salvo que exista una intención explícita y controlada de eliminar los datos persistentes.

---

# 🔐 Acceso a PostgreSQL con DBeaver

La base de datos de producción puede consultarse desde un equipo autorizado mediante:

```text
DBeaver
   ↓
SSH
   ↓
Tailscale
   ↓
Raspberry Pi
   ↓
127.0.0.1:5432
   ↓
PostgreSQL
```

La conexión de consulta habitual utiliza:

```text
biwenger_readonly
```

un usuario específico de PostgreSQL con permisos de solo lectura.

Este usuario puede consultar la información necesaria sin disponer de permisos normales de modificación.

PostgreSQL continúa sin estar publicado directamente en Internet.

---

# 🛫 Migraciones

Las migraciones se encuentran en los recursos del backend y se ejecutan automáticamente al iniciar la aplicación.

Flyway:

1. Comprueba la versión actual de la base de datos.
2. Valida las migraciones existentes.
3. Ejecuta las migraciones pendientes.
4. Detecta determinadas inconsistencias del esquema.

No deben modificarse migraciones que ya hayan sido aplicadas en producción.

Los cambios posteriores de estructura deben introducirse mediante una nueva migración.

La evolución:

```text
V1 → Engine 2.1
```

no necesitó cambios de esquema.

El delta final fue:

```text
DB schema delta V1 → Engine 2.1 = 0
```

Por tanto, Engine 2.1 no necesitó nuevas migraciones para su despliegue.

---

# 🐳 Docker

En producción se utilizan al menos:

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

El entorno Docker del PC de desarrollo es independiente de producción:

```text
PC Windows
└── desarrollo local

Raspberry Pi
└── producción
```

Docker Desktop puede permanecer cerrado en el PC cuando no se está desarrollando.

Esto no afecta a producción.

---

# 🚀 Despliegue del backend

El backend se ejecuta en una Raspberry Pi.

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

El backend se expone en el host de la Raspberry mediante:

```text
127.0.0.1:8080
```

Tailscale Funnel proporciona la entrada HTTPS y realiza proxy hacia:

```text
http://127.0.0.1:8080
```

La configuración puede comprobarse mediante:

```bash
tailscale serve status
```

La entrada utilizada actualmente es:

```text
https://raspdiego.tailcdc3f7.ts.net
```

Este dominio forma parte de la infraestructura servidor-servidor.

El navegador no necesita acceder directamente a él.

---

# ☁️ Frontend en producción

El frontend se publica mediante Cloudflare Pages.

URL:

```text
https://biwenger-assistant.pages.dev
```

Cloudflare Pages construye y publica Angular a partir de la rama configurada para producción.

Actualmente se utiliza:

```text
feat/league-management
```

La API de producción se configura como:

```text
/api
```

Las peticiones son gestionadas por:

```text
frontend/functions/api/[[path]].js
```

que actúa como proxy hacia Tailscale Funnel.

---

# 💻 Desarrollo local

## Requisitos

Para trabajar con el proyecto se necesita como mínimo:

- Java 21.
- Node.js / npm.
- PostgreSQL o Docker.
- Git.

---

## Backend

Desde la raíz:

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Alternativamente:

```powershell
cd backend
..\mvnw.cmd spring-boot:run
```

La API queda disponible normalmente en:

```text
http://localhost:8080
```

---

## Docker local

El entorno local puede levantarse mediante:

```powershell
docker compose up -d
```

y detenerse mediante:

```powershell
docker compose down
```

Esto conserva los volúmenes.

No utilizar:

```powershell
docker compose down -v
```

si se desea conservar la base de datos local.

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

o:

```powershell
ng serve
```

El servidor de desarrollo de Angular queda disponible normalmente en:

```text
http://localhost:4200
```

---

# 🧪 Tests

## Backend

Desde la raíz:

```powershell
.\mvnw.cmd -f backend\pom.xml test
```

o desde `backend`:

```powershell
..\mvnw.cmd test
```

La certificación de Engine 2.1 finalizó con:

```text
446 / 446 tests GREEN
```

---

## Frontend

Desde `frontend`:

```powershell
npm test -- --watch=false
```

La certificación actual finalizó con:

```text
37 / 37 tests GREEN
```

---

# 🏗️ Build del frontend

Desde `frontend`:

```powershell
npm run build
```

El build debe finalizar sin errores antes de considerar preparado para producción un cambio que afecte al frontend.

La certificación de Engine 2.1 incluyó:

```text
Frontend build GREEN
```

---

# 🧪 Flujo recomendado antes de desplegar

Para cambios relevantes:

```text
1. Implementar
2. Ejecutar tests backend
3. Ejecutar tests frontend si procede
4. Ejecutar build frontend si procede
5. Revisar git diff
6. Ejecutar git diff --check
7. Revisar git status
8. Commit
9. Push
10. Desplegar cuando corresponda
11. Smoke test en producción
```

Comandos útiles:

```bash
git diff
git diff --check
git status --short
git log --oneline -5
```

No todos los cambios requieren desplegar todos los componentes.

```text
Cambio backend
→ despliegue Raspberry

Cambio frontend
→ Cloudflare Pages

Cambio Pages Function
→ Cloudflare Pages

Cambio docker-compose de producción
→ aplicar de forma controlada en Raspberry

README / documentación
→ no requiere despliegue funcional
```

---

# 🌿 Git

La rama utilizada actualmente para desarrollo y despliegue es:

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

Nunca deben añadirse accidentalmente:

- Secretos.
- `.env`.
- Backups de credenciales.
- Tokens.
- Claves privadas.
- Archivos temporales.

---

# 🧪 Smoke tests

Después de un despliegue importante conviene comprobar al menos:

## ADMIN

- Login.
- Dashboard.
- Navegación.
- Estado de sincronización.
- Plantilla.
- Mercado.
- Jornada.
- Estadísticas.
- Recomendaciones.
- Algoritmos.
- Operaciones administrativas necesarias.
- Logout.

## USER

- Login.
- Dashboard.
- Acceso a su propia liga.
- Plantilla.
- Mercado.
- Jornada.
- Estadísticas.
- Recomendaciones.
- Algoritmos.
- Estado de sincronización.
- Imposibilidad de acceder a recursos administrativos.
- Logout.

También debe comprobarse que refrescar la página mantiene correctamente la sesión.

---

# ✅ Certificación de Engine 2.1

Engine 2.1 ha completado su ciclo de:

```text
implementación
      ↓
tests
      ↓
auditoría de regresión
      ↓
build
      ↓
despliegue
      ↓
validación real
```

Resultado final:

```text
Backend tests        446 / 446 GREEN
Frontend tests        37 / 37 GREEN
Frontend build        GREEN
Regresión V1 → 2.1    GREEN
Validación real       GREEN
Producción RPi        GREEN
```

También se verificó:

```text
DB schema delta V1 → Engine 2.1 = 0
```

Por tanto:

```text
Engine 2.1 = COMPLETADO
```

Engine 2.1 constituye actualmente la baseline estable del motor.

No existe una fase pendiente de Engine 2.1.

---

# 🚦 Estado actual del proyecto

Biwenger Assistant dispone actualmente de una versión funcional desplegada en producción sobre infraestructura propia.

El estado actual incluye:

- Frontend Angular en Cloudflare Pages.
- Proxy `/api` mediante Cloudflare Pages Functions.
- Backend Spring Boot en Raspberry Pi.
- PostgreSQL 17 en Docker.
- Tailscale Funnel como entrada HTTPS al backend.
- Sincronización automática cada 5 minutos.
- Acceso privado a PostgreSQL mediante SSH + Tailscale.
- Usuario de consulta de PostgreSQL de solo lectura.
- Autenticación basada en sesión.
- Protección CSRF.
- Rate limiting del login.
- Roles ADMIN / USER.
- Aislamiento de ligas.
- Jornadas divididas y aplazadas.
- Indicador de frescura.
- Motor de recomendaciones Engine 2.1.
- Dinámica deportiva.
- Dinámica económica.
- Confianza basada en evidencia.
- Explicabilidad.
- Página de Algoritmos.
- Tests automatizados de backend y frontend.

Engine 2.1 no necesita continuar desarrollándose para considerarse terminado.

Las futuras mejoras del motor deben tratarse como una nueva evolución y no como trabajo pendiente de 2.1.

---

# 📝 Limitaciones y posibles mejoras

Existen aspectos que no bloquean el funcionamiento actual pero pueden evolucionar en futuras iteraciones.

## Soporte multi-liga

Pueden existir puntos de la aplicación todavía orientados principalmente a la liga actual.

Si el proyecto amplía su uso multi-liga, deberá revisarse cualquier dependencia residual de identificadores o supuestos específicos de una liga.

---

## Sincronización manual

La sincronización manual puede seguir evolucionando para compartir exactamente la misma semántica y tratamiento de resultados que la sincronización automática.

---

## Rate limiting

El limitador del login:

- Funciona en memoria.
- Se reinicia al reiniciar el backend.
- Está diseñado para una única instancia.
- No constituye un sistema distribuido.

Si la infraestructura evoluciona a varias instancias debería sustituirse o complementarse con almacenamiento compartido.

---

## Cobertura de datos

Algunas señales potencialmente interesantes pueden permanecer fuera del motor porque la información persistida todavía no ofrece una cobertura suficientemente fiable.

El principio general es:

```text
datos insuficientes
        ≠
señal negativa
```

y también:

```text
datos insuficientes
        ≠
permiso para inventar precisión
```

---

## Fechas y zonas horarias

La normalización de fechas y zonas horarias puede reforzarse para garantizar comportamiento uniforme en escenarios más complejos.

---

## API Biwenger

La superficie directa de Biwenger está restringida según su finalidad.

Los endpoints técnicos y de sincronización sensibles están reservados al rol correspondiente de Biwenger Assistant.

El rol administrativo de Biwenger Assistant es independiente de cualquier rol administrativo que un usuario pueda tener dentro de una liga de Biwenger.

---

# 🗺️ Roadmap

## Base funcional

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
- [x] Página de Algoritmos.
- [x] Despliegue Raspberry Pi.
- [x] Tailscale Funnel.
- [x] Frontend Cloudflare Pages.
- [x] Proxy API mediante Cloudflare Pages Functions.
- [x] Acceso privado a PostgreSQL mediante Tailscale.
- [x] Certificación técnica.

---

## Engine 2.1

- [x] Forma reciente.
- [x] Rendimiento histórico.
- [x] Rating deportivo.
- [x] Contexto del próximo rival.
- [x] Necesidades por posición.
- [x] Optimización de alineaciones.
- [x] Compatibilidad con jugadores multiposición.
- [x] Jornadas con bloqueo progresivo.
- [x] Dinámica económica.
- [x] Momentum económico.
- [x] Aceleración económica.
- [x] Consistencia económica.
- [x] Dinámica deportiva.
- [x] Confianza basada en evidencia.
- [x] Fallbacks por datos insuficientes.
- [x] Explicabilidad.
- [x] Auditoría de regresión V1 → 2.1.
- [x] Suite backend completa.
- [x] Suite frontend.
- [x] Build de producción.
- [x] Despliegue Raspberry.
- [x] Validación real.

---

## Futuras evoluciones

- [ ] Recoger feedback de uso real.
- [ ] Mejorar soporte multi-liga.
- [ ] Seguir aumentando la cobertura de tests frontend.
- [ ] Mejorar normalización de fechas y zonas horarias.
- [ ] Unificar completamente sincronización manual y automática.
- [ ] Mejorar UX según feedback real.
- [ ] Revisar rate limiting si la infraestructura evoluciona a múltiples instancias.
- [ ] Evaluar backtesting del motor.
- [ ] Utilizar backtesting antes de modificar pesos o incorporar señales importantes.
- [ ] Evaluar Engine 2.2 únicamente cuando exista evidencia que justifique su evolución.
- [ ] Evaluar una aplicación móvil basada en el frontend existente.

---

# 🔭 Posible Engine 2.2

Engine 2.1 debe permanecer como baseline estable mientras no exista evidencia suficiente que justifique modificarlo.

Una futura evolución debería priorizar **backtesting**.

El objetivo sería poder responder:

```text
¿Qué habría recomendado el motor?
              ↓
¿Qué ocurrió realmente?
              ↓
¿Fue útil la recomendación?
              ↓
¿Qué señales aportaron valor?
```

Esto permitiría evaluar los algoritmos utilizando datos históricos antes de modificar pesos o introducir nuevas señales.

Posibles líneas futuras podrían incluir:

- Contexto de calendario.
- Mayor información sobre titularidad y minutos.
- Forma contextual.
- Detección de cambios de régimen económico.
- Separación más avanzada entre riesgo, potencial y confianza.

Estas posibilidades:

```text
NO forman parte de Engine 2.1
```

y no deben interpretarse como funcionalidades actualmente implementadas.

---

# 📱 Posible evolución móvil

La arquitectura actual permite plantear en el futuro una versión móvil reutilizando buena parte del frontend Angular.

Una posible vía sería:

```text
Angular
   ↓
Capacitor
   ↓
Android / iOS
```

Un orden razonable de evolución sería:

```text
Web estable
    ↓
Android / APK
    ↓
Google Play si procede
    ↓
iOS si resulta necesario
```

Esta evolución no forma parte actualmente del núcleo funcional de Biwenger Assistant.

---

# 🎯 Filosofía del proyecto

Biwenger Assistant debe aportar información útil sin intentar convertirse en una copia de Biwenger.

La filosofía general es:

```text
Sincronizar
    ↓
Persistir
    ↓
Analizar
    ↓
Contrastar evidencia
    ↓
Explicar
    ↓
Recomendar
```

El sistema debe favorecer:

```text
datos reales
    +
señales justificadas
    +
incertidumbre explícita
```

frente a recomendaciones aparentemente precisas construidas sobre información insuficiente.

Principio fundamental:

> El motor debe ayudar a tomar una decisión, no fingir que puede tomarla con certeza.

La decisión final permanece siempre en manos del usuario.

---

# 👨‍💻 Autor

Proyecto personal desarrollado por Diego.

Desarrollado inicialmente como proyecto de aprendizaje y evolucionado posteriormente hacia una aplicación funcional desplegada en infraestructura propia.

---

# 📍 Estado

```text
Biwenger Assistant