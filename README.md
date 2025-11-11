# SAF — Spring Actors Framework (work in progress)

> Plateforme d’**acteurs (agents)** distribuée construite avec **Spring Boot** (backend) et **React + Tailwind + shadcn/ui** (frontend).
> Objectif : offrir un **plan de contrôle** (SAF-Control) et un **plan d’exécution** (SAF-Runtime) pour créer, superviser et faire communiquer des acteurs à l’échelle.

---

## Sommaire

- [SAF — Spring Actors Framework (work in progress)](#saf--spring-actors-framework-work-in-progress)
  - [Sommaire](#sommaire)
  - [Vision](#vision)
  - [Architecture](#architecture)
    - [SAF-Control (plan de contrôle)](#saf-control-plan-de-contrôle)
    - [SAF-Runtime (plan d’exécution)](#saf-runtime-plan-dexécution)
    - [Flux type](#flux-type)
  - [Choix techniques](#choix-techniques)
  - [Arborescence du repo](#arborescence-du-repo)
  - [Frontend](#frontend)
  - [Backend](#backend)
    - [Contrats API (brouillon)](#contrats-api-brouillon)
  - [Démarrage local](#démarrage-local)
    - [Frontend](#frontend-1)
    - [Backend](#backend-1)
      - [SAF-Control](#saf-control)
      - [SAF-Runtime (à venir)](#saf-runtime-à-venir)
  - [Conventions \& qualité](#conventions--qualité)
  - [Feuille de route](#feuille-de-route)
  - [Licence](#licence)

---

## Vision

Construire une **plateforme d’acteurs** inspirée des modèles “actor/agents” (mailbox, supervision, tell/ask, timers), **scalable** et **observables**, utilisable via une **API claire** et une **UI** de pilotage.
Priorités : **simplicité d’intégration** (API REST), **résilience** (stratégies de reprise), **scalabilité horizontale** (multi-pods), **observabilité** (métriques, logs, événements temps réel).

---

## Architecture

### SAF-Control (plan de contrôle)

* **Rôle** : façade **API** et **orchestrateur** de la plateforme.
* **Responsabilités** :

  * CRUD du **cycle de vie** des acteurs : créer (*spawn*), arrêter, configurer.
  * **Registry** (annuaire) des acteurs et de leur localisation.
  * **Supervision globale** : politiques (restart/resume/stop), quarantaines.
  * **Événements temps réel** (SSE/WebSocket) pour l’UI (logs, métriques, états).
  * **Routage logique** des messages vers le(s) Runtime(s).
  * **Sécurité / quotas / audit** (plus tard).
* **Interfaces** : API publique (REST + SSE).

### SAF-Runtime (plan d’exécution)

* **Rôle** : exécuter les **acteurs** et livrer les **messages**.
* **Responsabilités** :

  * **Actor** (état/behaviour), **Mailbox**, **Dispatcher** (threads/virtual threads).
  * **Supervision locale** (restart d’un acteur en échec).
  * **Routage** (tell/ask, timeouts, corrélation).
  * **Messages inter-pods** via **broker** (Kafka/RabbitMQ).
  * **Persistance** optionnelle (snapshots / event store).
  * **Health & metrics**.
* **Interfaces** : endpoints **internes** (health, metrics). Pas d’API publique directe par défaut.

> **Relation** : Les **clients** (UI, scripts, intégrations) parlent à **SAF-Control**.
> **SAF-Control** orchestre les **SAF-Runtime**. Le Runtime **n’administre pas** Control.

### Flux type

**Créer un acteur**

1. Client → **SAF-Control** : `POST /agents` (+type, params)
2. Control publie une **commande** sur le **broker**
3. Un **SAF-Runtime** consomme, **spawn** l’acteur
4. Runtime émet un **événement** `ActorStarted`
5. Control met à jour le **registry** et **pousse** l’événement (SSE) au front

**Envoyer un message (ask)**

1. Client → **SAF-Control** : `POST /agents/{id}/message` (payload, timeout)
2. Control **route** vers le Runtime hébergeant l’acteur
3. Runtime traite (mailbox → behaviour)
4. Réponse → broker → **SAF-Control** → client (HTTP / stream)

---

## Choix techniques

* **Frontend** : **React 19 + TypeScript + Vite**, **Tailwind CSS** + **shadcn/ui**

  * Rapidité de dev, design system cohérent, composants accessibles (Radix).
* **Backend** : **Spring Boot 3.x (Java 21)**

  * Simplicité de packaging, support natif observabilité/métriques, écosystème mature.
  * **Virtual Threads** (Loom) possibles pour concu élevée & code lisible.
* **Messagerie** : **Kafka** ou **RabbitMQ**.

  * Kafka : débit/partitions/ordre; RabbitMQ : routes/facilité RPC.
* **Style d’archi (back)** : **Hexagonal / Ports & Adapters**

  * Domaine pur, ports in/out, adapters techniques remplaçables (broker, persistance).
* **Observabilité** : Micrometer → Prometheus/Grafana, logs JSON, SSE pour l’UI.

---

## Arborescence du repo

> **Pour l’instant** : frontend + backend (control & runtime). L’infra et d’autres libs arriveront au fur et à mesure.

```
SAF_PLATFORM/
├─ README.md                         # README global (vision, archi, démarrage)
├─ .gitignore                        # Ignore global

├─ backend/                          # Services Spring Boot (Control + Runtime, à venir)      
│  ├─ pom.xml                        # POM parent (modules, versions) — si Maven, à venir
│  └─ services/
│     ├─ saf-control/                # Service plan de contrôle (API publique)
│     │  ├─ pom.xml                  # Dépendances Spring Web/Actuator/JSON…
│     │  └─ src/
│     │     ├─ main/
│     │     │  ├─ java/com/acme/saf/saf_control/
│     │     │  │  ├─ web/            # Controllers REST/SSE (ports in)
│     │     │  │  ├─ application/    # Use cases (ports out → broker/registry)
│     │     │  │  ├─ domain/         # Modèles “contrôle” (léger)
│     │     │  │  └─ infrastructure/  # Adapters (broker, registry store, config)
│     │     │  └─ resources/
│     │     │     └─ application.yml  # Config/profils (dev/test)
│     │     └─ test/java/...          # Tests JUnit5 (à venir)
│     └─ saf-runtime/                # Service plan d’exécution (acteurs)
│        ├─ pom.xml
│        └─ src/
│           ├─ main/
│           │  ├─ java/com/acme/saf/saf_runtime/
│           │  │  ├─ domain/         # Actor, Mailbox, Dispatcher, Supervision
│           │  │  ├─ application/    # Timers, policies, orchestration locale
│           │  │  ├─ infrastructure/ # Messaging/persistence adapters (broker/DB)
│           │  │  └─ web/            # Health/metrics (interne)
│           │  └─ resources/
│           │     └─ application.yml
│           └─ test/java/...         # Tests de charge/chaos ciblés (à venir)

└─ frontend/
   ├─ package.json                   # Scripts dev/build, deps React/Tailwind/shadcn
   ├─ pnpm-lock.yaml                 # Lockfile pnpm (verrouille les versions)
   ├─ index.html                     # Entrée Vite (montage #root)
   ├─ vite.config.ts                 # Config Vite + alias "@"
   ├─ tailwind.config.ts             # Thème + tokens shadcn/ui
   ├─ postcss.config.js              # PostCSS (tailwind + autoprefixer)
   ├─ eslint.config.js               # Règles ESLint (TS/React)
   ├─ components.json                # Config shadcn/ui (chemins, style)
   ├─ tsconfig.json                  # TS root (paths, jsx)
   ├─ tsconfig.app.json              # TS pour le code applicatif
   ├─ tsconfig.node.json             # TS pour outils/build
   ├─ tsconfig.app.tsbuildinfo       # Cache TS (généré) — peut être ignoré
   ├─ tsconfig.tsbuildinfo           # Cache TS (généré) — peut être ignoré
   ├─ .gitignore                     # Ignore spécifiques du front (dist, env locaux)
   ├─ README.md                      # README du front (scripts, conventions UI)
   ├─ public/                        # Assets statiques servis tels quels
   ├─ dist/                          # Build de prod (généré par `pnpm build`)
   ├─ node_modules/                  # Dépendances (non commit)
   └─ src/
      ├─ App.tsx                     # Shell d’app (header/nav, routes placeholders)
      ├─ main.tsx                    # Entrée React, BrowserRouter, styles globaux
      ├─ vite-env.d.ts               # Types Vite
      ├─ styles/
      │  └─ globals.css              # Tailwind layers + variables shadcn/ui
      ├─ assets/                     # Images/icônes locales
      ├─ components/
      │  ├─ ui/                      # Composants shadcn/ui générés (atomes)
      │  └─ theme-toggle.tsx         # Bouton changement thème (clair/sombre)
      ├─ context/
      │  └─ theme-provider.tsx       # Provider thème (clair/sombre/system)
      ├─ lib/
      │  └─ utils.ts                 # Utilitaires (cn, formatters…)
      └─ app/                        # (à venir) router/layouts par pages/features
                                     # ex: agents/, messaging/, supervision/

```

---

## Frontend

* **But** : Piloter la plateforme (créer/détruire des agents, envoyer des messages, visualiser l’état/les logs/les métriques).
* **Pages** :

  * **Agents** : liste, création/suppression, état en temps réel (SSE).
  * **Messaging** : envoi **tell/ask** avec timeouts, journal des échanges.
  * **Supervision** : redémarrages, politiques, métriques.
* **Tech** : shadcn/ui + Tailwind, React Router, axios.
* **Config** : `VITE_API_BASE_URL` pour cibler **SAF-Control**.

---

## Backend

* **SAF-Control** :

  * Ports in : REST + SSE.
  * Ports out : **broker** (commandes), **registry store** (DB/cache).
  * Cas d’usage : `SpawnActor`, `DestroyActor`, `SendMessage`, `StreamEvents`.
* **SAF-Runtime** :

  * Domaine : `Actor`, `ActorRef`, `Mailbox`, `Dispatcher`, `SupervisionPolicy`.
  * Ports in : **broker** (commandes).
  * Ports out : **broker** (événements, réponses).
  * Web : `GET /health`, `GET /metrics` (interne).

### Sécurité (clé API)

Les endpoints de **SAF-Control** sont protégés par une **clé API** simple, vérifiée via un filtre Spring (`ApiKeyFilter`).

#### Configuration

La clé est définie dans `application.properties` :

```properties
saf.security.api-key=cle-api
```

#### En-tête attendu

Chaque requête doit inclure l’en-tête HTTP suivant : 

```text
X-API-KEY: cle-api
```

#### Exemple de test (PowerShell)

```powershell
Invoke-RestMethod -Uri http://localhost:8080/agents -Headers @{ "X-API-KEY" = "cle-api" }
```

#### Exemple de test (cURL)

```bash
curl -H "X-API-KEY: cle-api" http://localhost:8080/agents
```

#### Exemple côté front (fetch)

Lorsque le front communique avec le backend, il doit inclure la clé dans les en-têtes HTTP : 

```javascript
fetch("http://localhost:8080/agents", {
  method: "GET",
  headers: {
    "Content-Type": "application/json",
    "X-API-KEY": "cle-api"
  }
})
  .then(response => {
    if (!response.ok) throw new Error("Unauthorized");
    return response.json();
  })
  .then(data => console.log(data))
  .catch(error => console.error(error));
```

#### Option pour le développement

Pour simplifier les tests locaux, la vérification peut être désactivée en laissant la clé vide :

```properties
saf.security.api-key=
```

Dans ce cas, le filtre accepte toutes les requêtes sans contrôle.

### Contrats API (brouillon)

> **Scope minimal** pour amorcer l’UI ; les schémas exacts seront figés via Swagger/OpenAPI plus tard.

**SAF-Control — REST**

* `POST /agents`
  Request: `{ "type": "string", "params": { ... } }`
  Response: `{ "id": "actor-123", "status": "starting" }`
* `DELETE /agents/{id}`
  Response: `{ "id": "actor-123", "status": "stopped" }`
* `GET /agents/{id}`
  Response: `{ "id": "actor-123", "state": { ... }, "node": "runtime-1" }`
* `POST /agents/{id}/message`
  Request: `{ "mode": "tell"|"ask", "payload": { ... }, "timeoutMs": 5000 }`
  Response (ask): `{ "correlationId": "...", "result": { ... } }`
* `GET /events/stream` (SSE)
  Events: `ActorStarted`, `ActorStopped`, `MessageDelivered`, `Error`, `Metric`, …

**SAF-Runtime — interne**

* `GET /health` → `{"status": "UP"}`
* `GET /metrics` → prom exposition

---

## Démarrage local

> **Pré-requis** : Node.js ≥ 20, pnpm (ou npm), Java 21.
> **Note** : le backend est encore en chantier ; seuls les scripts frontend sont actifs.

### Frontend

```bash
cd frontend
pnpm i
pnpm dev
# http://localhost:5173
```

### Backend

#### SAF-Control

```bash
cd backend/services/saf-control
./mvnw spring-boot:run
```

**Endpoints disponibles :**

* **Santé** : `GET http://localhost:8080/actuator/health`
* **Prometheus** : `GET http://localhost:8080/actuator/prometheus`
* **OpenAPI** : `GET http://localhost:8080/swagger`
* **SSE (stub)** : `GET http://localhost:8080/events/stream`

#### SAF-Runtime (à venir)

* `saf-runtime` : health/metrics

---

## Conventions & qualité

* **Branches** : `main` (stable), `dev` (intégration), `feature/*`.
* **Commits** : Conventional Commits (`feat:`, `fix:`, `docs:`…).
* **Qualité** :
* 
  * Front : ESLint, Prettier.
  * Back : JUnit 5 — à intégrer.

---

## Feuille de route

1. **Back – SAF-Control (MVP)** : endpoints `POST/DELETE/GET /agents`, `POST /agents/{id}/message`, `GET /events/stream`.
2. **Back – SAF-Runtime (MVP)** : domaine `Actor/Mailbox/Dispatcher` minimal, `GET /health`, intégration broker simulée.
3. **Front – Agents** : liste + création/suppression + stream SSE.
4. **Broker réel** (Kafka ou RabbitMQ) + routage `ask` avec timeouts.
5. **Supervision** (policies restart/resume/stop) + métriques.
6. **Persistance** (snapshots/event store) — optionnel.

---

## Licence

Apache-2.0
