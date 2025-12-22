# SAF — Spring Actors Framework (work in progress)

> Plateforme d’**acteurs (agents)** distribuée construite avec **Spring Boot** (backend) et **React + Tailwind + shadcn/ui** (frontend).
> Objectif : offrir un **plan de contrôle** (SAF-Control) et un **plan d’exécution** (SAF-Runtime) pour créer, superviser et faire communiquer des acteurs à l’échelle.
>
> Le framework est **autonome** : il peut être déployé tel quel comme plateforme générique, ou **embarqué** dans une application tierce qui vient brancher ses propres acteurs via un mécanisme de **plugin (ActorFactory)**, à la manière d’Akka.

---

## Sommaire

- [SAF — Spring Actors Framework (work in progress)](#saf--spring-actors-framework-work-in-progress)
  - [Sommaire](#sommaire)
  - [Vision](#vision)
  - [Architecture](#architecture)
    - [SAF-Actor-Core (cœur du framework)](#saf-actor-core-cœur-du-framework)
    - [SAF-Control (plan de contrôle)](#saf-control-plan-de-contrôle)
    - [SAF-Runtime (plan d’exécution)](#saf-runtime-plan-dexécution)
    - [Flux type](#flux-type)
  - [Choix techniques](#choix-techniques)
  - [Arborescence du repo](#arborescence-du-repo)
  - [Frontend](#frontend)
  - [Backend](#backend)
    - [Sécurité (clé API)](#sécurité-clé-api)
      - [Configuration](#configuration)
      - [En-tête attendu](#en-tête-attendu)
      - [Exemple de test (PowerShell)](#exemple-de-test-powershell)
      - [Exemple de test (cURL)](#exemple-de-test-curl)
      - [Exemple côté front (fetch)](#exemple-côté-front-fetch)
      - [Option pour le développement](#option-pour-le-développement)
    - [Contrats API (brouillon)](#contrats-api-brouillon)
  - [Démarrage local](#démarrage-local)
    - [Option 1 : Développement natif](#option-1--développement-natif)
      - [Frontend](#frontend-1)
      - [Backend](#backend-1)
        - [SAF-Control (framework)](#saf-control-framework)
        - [SAF-Runtime (framework)](#saf-runtime-framework)
        - [Microservices IoT City](#microservices-iot-city)
    - [Option 2 : Docker Compose](#option-2--docker-compose)
  - [Conventions \& qualité](#conventions--qualité)
  - [Feuille de route](#feuille-de-route)
  - [Licence](#licence)

---

## Vision

Construire une **plateforme d’acteurs** inspirée des modèles “actor/agents” (mailbox, supervision, tell/ask, timers), **scalable** et **observables**, utilisable via une **API claire** et une **UI** de pilotage.  
Priorités : **simplicité d’intégration** (API REST), **résilience** (stratégies de reprise), **scalabilité horizontale** (multi-pods), **observabilité** (métriques, logs, événements temps réel).

Le framework est pensé en **mode framework réutilisable** :

- utilisable seul, comme une plateforme générique “SAF-Control + SAF-Runtime” accessible par API,
- ou **embarqué** dans une application tierce qui fournit ses propres types d’acteurs (ex. City, Sensor…) via une **`ActorFactory`** sans que SAF ne dépende de cette application.

---

## Architecture

### SAF-Actor-Core (cœur du framework)

* **Rôle** : librairie Java générique qui définit les **abstractions d’acteurs** :
  * `Actor`, `ActorRef`, `ActorSystem`, `Mailbox`, `SupervisionPolicy`, `Message`, etc.
  * `ActorFactory` (contrat permettant de créer des acteurs à partir d’un type logique et d’un contexte).
* **Responsabilités** :
  * Modéliser le comportement d’un acteur (state + `receive(message)`).
  * Encapsuler la logique de supervision (restart / resume / stop).
  * Fournir un contrat d’**usine d’acteurs** (`ActorFactory`) que les applications tierces peuvent implémenter pour **brancher leurs propres acteurs métier**.
* **Dépendances** :
  * Ne dépend **d’aucun domaine applicatif** (pas de notion de ville, capteur, client, etc.).
  * Est utilisé à la fois par **SAF-Runtime** et par les librairies d’acteurs métiers des applications.

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

> SAF-Control ne connaît **pas** les acteurs métiers concrets (City, Sensor, etc.) : il manipule des “types d’acteurs” et des payloads génériques. Ce sont les **plugins d’application** (via ActorFactory dans le Runtime) qui donnent du sens à ces types.

### SAF-Runtime (plan d’exécution)

* **Rôle** : exécuter les **acteurs** et livrer les **messages**, de façon générique, à la manière d’un moteur Akka.
* **Responsabilités** :

  * **ActorSystem** qui orchestre :
    * **Actor** (état/behaviour), **Mailbox**, **Dispatcher** (threads/virtual threads),
    * **Supervision locale** (restart d’un acteur en échec),
    * **Timers** / messages différés.
  * **ActorFactory** : point d’extension pour brancher des acteurs métiers.
    * Le Runtime ne connaît que l’**interface** `ActorFactory`.
    * Une application tierce peut fournir une implémentation (plugin) qui dit :
      * `"City"` → `new CityActor(...)`
      * `"Sensor"` → `new SensorActor(...)`
  * **Routage** (tell/ask, timeouts, corrélation).
  * **Messages inter-pods** via **broker** (Kafka/RabbitMQ).
  * **Persistance** optionnelle (snapshots / event store).
  * **Health & metrics**.
* **Interfaces** : endpoints **internes** (health, metrics). Pas d’API publique directe par défaut.

> Dans ce mode **embedded / plugin** :
>
> - le **framework SAF** fournit `SAF-Actor-Core`, `SAF-Runtime`, `SAF-Control`,
> - une application métier fournit un **module d’acteurs** (qui dépend de `saf-actor-core`) + une implémentation d’`ActorFactory` injectée dans le Runtime,
> - le runtime reste **générique** et ne dépend jamais du code métier.

> **Relation** :
>
> - Les **clients** (UI, scripts, intégrations, microservices métier) parlent à **SAF-Control**.
> - **SAF-Control** orchestre les **SAF-Runtime** (création, routage).
> - **SAF-Runtime** instancie les acteurs concrets via une **ActorFactory fournie par l’application** (plugin).
> - Le Runtime **n’administre pas** Control, et Control ne dépend pas des acteurs métiers.

### Flux type

**Créer un acteur**

1. Client → **SAF-Control** : `POST /agents` (+type, params)
2. Control publie une **commande** sur le **broker**
3. Un **SAF-Runtime** consomme, demande à son `ActorFactory` de créer un acteur du type demandé (`"City"`, `"Sensor"`, etc. si l’application a fourni ces types), puis **spawn** l’acteur
4. Runtime émet un **événement** `ActorStarted`
5. Control met à jour le **registry** et **pousse** l’événement (SSE) au front

**Envoyer un message (ask)**

1. Client → **SAF-Control** : `POST /agents/{id}/message` (payload, timeout)
2. Control **route** vers le Runtime hébergeant l’acteur
3. Runtime traite (mailbox → behaviour), en s’appuyant sur les classes d’acteurs fournies par le plugin d’application
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

> **Architecture Framework/Application** : Le projet est maintenant structuré pour séparer clairement le **framework SAF** (100% générique et réutilisable) des **applications** qui l'utilisent (comme IoT City).

```text
SAF_PLATFORM/
├─ README.md                         # README global (vision, archi, démarrage)
├─ .gitignore                        # Ignore global
├─ docker-compose.yml                # Orchestration des services
├─ DOCKER.md                         # Guide de déploiement Docker
├─ monitoring/                       # Config Prometheus

├─ backend/
│  ├─ framework/                     # 🔷 FRAMEWORK SAF (100% générique, réutilisable)
│  │  ├─ pom.xml                     # POM parent du framework
│  │  ├─ saf-actor-core/             # Librairie Java d'acteurs (pas de Spring)
│  │  │  ├─ pom.xml
│  │  │  └─ src/main/java/com/acme/saf/actor/core/
│  │  │     ├─ Actor.java            # Interface Actor
│  │  │     ├─ ActorRef.java         # Référence d'acteur
│  │  │     ├─ ActorSystem.java      # Interface du système d'acteurs
│  │  │     ├─ ActorFactory.java     # Interface pour plugin d'acteurs métier
│  │  │     ├─ Message.java          # Abstraction des messages
│  │  │     ├─ Mailbox.java          # Boîte aux lettres
│  │  │     ├─ Dispatcher.java       # Dispatch des messages
│  │  │     └─ SupervisionStrategy.java  # Stratégies de supervision
│  │  │
│  │  ├─ saf-runtime/                # Engine runtime générique (Spring Boot)
│  │  │  ├─ pom.xml
│  │  │  └─ src/main/java/com/acme/saf/saf_runtime/
│  │  │     ├─ DefaultActorSystem.java   # Implémentation ActorSystem
│  │  │     ├─ InMemoryMailbox.java      # Implémentation Mailbox
│  │  │     └─ metrics/                  # Métriques runtime
│  │  │
│  │  └─ saf-control/                # Control plane générique (Spring Boot)
│  │     ├─ pom.xml
│  │     └─ src/main/java/com/acme/saf/saf_control/
│  │        ├─ web/                  # Controllers REST/SSE
│  │        ├─ application/          # Services de contrôle
│  │        ├─ domain/               # Modèles de contrôle
│  │        ├─ security/             # Filtres de sécurité (API Key)
│  │        └─ infrastructure/       # Adapters (events, routing)
│  │
│  └─ apps/                          # 🔶 APPLICATIONS (100% spécifiques au cas d'usage)
│     └─ iot-city/                   # Application IoT City
│        ├─ iot-city-domain/         # Acteurs métier (Client, Ville, Capteur)
│        │  ├─ pom.xml               # Dépend uniquement de saf-actor-core
│        │  └─ src/main/java/com/acme/iot/city/actors/
│        │     ├─ ClientActor.java   # Acteur Client (métier)
│        │     ├─ VilleActor.java    # Acteur Ville (métier)
│        │     ├─ CapteurActor.java  # Acteur Capteur (métier)
│        │     └─ IotActorFactory.java  # Factory pour créer les acteurs IoT
│        │
│        ├─ client-service/          # Microservice Client (Spring Boot)
│        ├─ ville-service/           # Microservice Ville (Spring Boot)
│        ├─ capteur-service/         # Microservice Capteur (Spring Boot)
│        └─ iot-runtime/             # Runtime applicatif (SAF + IoT Domain)
│           ├─ pom.xml               # Dépend de: saf-actor-core + iot-city-domain
│           ├─ Dockerfile
│           └─ src/
│              ├─ main/java/com/acme/iot/runtime/
│              │  ├─ IotRuntimeApplication.java  # Application Spring Boot
│              │  └─ config/
│              │     └─ ActorConfiguration.java  # Wire IotActorFactory
│              └─ resources/
│                 └─ application.yml

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
  * Reste **agnostique métier** : ne connaît que des types d’acteurs et des payloads sérialisés.

* **SAF-Runtime** :

  * Domaine : `Actor`, `ActorRef`, `Mailbox`, `Dispatcher`, `SupervisionPolicy` (via SAF-Actor-Core).
  * Ports in : **broker** (commandes).
  * Ports out : **broker** (événements, réponses).
  * Web : `GET /health`, `GET /metrics` (interne).
  * Extensibilité : une application qui veut fonctionner en mode embedded fournit une **implémentation d’`ActorFactory`** (plugin) qui déclare comment instancier ses acteurs métier par type. Le runtime reste générique.

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

* `GET /actuator/health` → `{"status": "UP"}`
* `GET /actuator/prometheus` → prom exposition

---

## Démarrage local

Deux options sont disponibles pour démarrer la plateforme localement :

### Option 1 : Développement natif

> **Pré-requis** : Node.js ≥ 20, pnpm (ou npm), Java 21.
> **Note** : le backend est encore en chantier ; seuls les endpoints/stubs principaux sont disponibles.

#### Frontend

```bash
cd frontend
pnpm i
pnpm dev
# http://localhost:5173
```

#### Backend

##### SAF-Control (framework)

```bash
cd backend/framework/saf-control
./mvnw spring-boot:run
```

**Endpoints disponibles :**

* **Santé** : `GET http://localhost:8080/actuator/health`
* **OpenAPI** : `GET http://localhost:8080/swagger`
* **SSE (stub)** : `GET http://localhost:8080/events/stream`

##### SAF-Runtime (framework)

```bash
cd backend/framework/saf-runtime
./mvnw spring-boot:run
```

**Endpoints disponibles :**

* **Santé** : `GET http://localhost:8081/actuator/health`
* **Prometheus** : `GET http://localhost:8081/actuator/prometheus`

##### Microservices IoT City

```bash
cd backend/apps/iot-city/client-service
mvn spring-boot:run
```

```bash
cd backend/apps/iot-city/ville-service
mvn spring-boot:run
```

```bash
cd backend/apps/iot-city/capteur-service
mvn spring-boot:run
```

**Ports par défaut :**

* **Client** : `http://localhost:8082/actuator/health`
* **Ville** : `http://localhost:8083/actuator/health`
* **Capteur** : `http://localhost:8084/actuator/health`

### Option 2 : Docker Compose

> **Pré-requis** : Docker Engine 20.10+, Docker Compose V2+, au moins 2 Go de RAM disponible.

Pour déployer la plateforme avec Docker Compose (recommandé pour les tests et le déploiement), consultez le guide complet : **[DOCKER.md](./DOCKER.md)**

**Démarrage rapide :**

```bash
# 1. Configurer les variables d'environnement
cp .env.example .env

# 2. Démarrer tous les services
docker-compose up -d

# 3. Vérifier les services
docker-compose ps
```

**Accès :**
* **Frontend** : http://localhost
* **Backend API** : http://localhost:8080
* **Client service** : http://localhost:8082
* **Ville service** : http://localhost:8083
* **Capteur service** : http://localhost:8084
* **Runtime API** : http://localhost:8081 (si SAF-Runtime est lancé en local)
* **Swagger UI** : http://localhost:8080/swagger
* **Health Check** : http://localhost:8080/actuator/health
* **Runtime Health Check** : http://localhost:8081/actuator/health (si SAF-Runtime est lancé en local)

Pour plus de détails (architecture, commandes, dépannage, sécurité), voir **[DOCKER.md](./DOCKER.md)**.

---

## Conventions & qualité

* **Branches** : `main` (stable), `dev` (intégration), `feature/*`.
* **Commits** : Conventional Commits (`feat:`, `fix:`, `docs:`…).
* **Qualité** :

  * Front : ESLint, Prettier.
  * Back : JUnit 5 — à intégrer.

---

## Feuille de route

1. **Back – SAF-Control (MVP)** : endpoints `POST/DELETE/GET /agents`, `POST /agents/{id}/message`, `GET /events/stream`.
2. **Back – SAF-Runtime (MVP)** : domaine `Actor/Mailbox/Dispatcher` minimal, `GET /health`, intégration broker simulée, introduction d’une **`ActorFactory`** pour permettre le mode plugin.
3. **Front – Agents** : liste + création/suppression + stream SSE.
4. **Back – SAF-Actor-Core** : stabilisation des interfaces `Actor`, `ActorRef`, `ActorSystem`, `ActorFactory` pour publication en tant que lib.
5. **Broker réel** (Kafka ou RabbitMQ) + routage `ask` avec timeouts.
6. **Supervision** (policies restart/resume/stop) + métriques.
7. **Persistance** (snapshots/event store) — optionnel.

---

## Licence

Apache-2.0
