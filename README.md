# SAF — Spring Actors Framework (work in progress)

> Plateforme d'**acteurs (agents)** distribuée construite avec **Spring Boot** (backend) et **React + Tailwind + shadcn/ui** (frontend).
> Objectif : offrir un **plan de contrôle** (SAF-Control) et des **microservices d'acteurs** pour créer, superviser et faire communiquer des acteurs à l'échelle.
>
> Le framework est **autonome** : il peut être déployé tel quel comme plateforme générique, ou **embarqué** dans une application tierce qui vient brancher ses propres acteurs via un mécanisme de **plugin (ActorFactory)**, à la manière d'Akka.

---

## Sommaire

- [SAF — Spring Actors Framework (work in progress)](#saf--spring-actors-framework-work-in-progress)
  - [Sommaire](#sommaire)
  - [Vision](#vision)
  - [Architecture](#architecture)
    - [SAF-Actor-Core (cœur du framework)](#saf-actor-core-cœur-du-framework)
    - [SAF-Control (plan de contrôle)](#saf-control-plan-de-contrôle)
    - [SAF-Runtime (bibliothèque d'exécution)](#saf-runtime-bibliothèque-dexécution)
    - [Microservices d'acteurs](#microservices-dacteurs)
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
  - [Initialisation de l'Application IoT City](#initialisation-de-lapplication-iot-city)
    - [Script d'Initialisation](#script-dinitialisation)
    - [Fonctionnement du Script](#fonctionnement-du-script)
    - [Utilisation](#utilisation)
    - [Vérification](#vérification)
    - [Idempotence](#idempotence)
  - [Démarrage local](#démarrage-local)
    - [Option 1 : Docker Compose](#option-1--docker-compose)
    - [Option 2 (dépréciée) : Développement natif](#option-2-dépréciée--développement-natif)
      - [Frontend](#frontend-1)
      - [Backend](#backend-1)
        - [SAF-Control (framework)](#saf-control-framework)
        - [Microservices IoT City](#microservices-iot-city)
  - [Conventions \& qualité](#conventions--qualité)
  - [Système de Supervision](#système-de-supervision)
    - [1. Supervision des Microservices (Infrastructure)](#1-supervision-des-microservices-infrastructure)
    - [2. Endpoints de Santé des Acteurs (Application)](#2-endpoints-de-santé-des-acteurs-application)
    - [3. Supervision Locale Automatique (Application)](#3-supervision-locale-automatique-application)
    - [Résilience Complète](#résilience-complète)
  - [Feuille de route](#feuille-de-route)
  - [Licence](#licence)

---

## Vision

Construire une **plateforme d'acteurs** inspirée des modèles "actor/agents" (mailbox, supervision, tell/ask, timers), **scalable** et **observables**, utilisable via une **API claire** et une **UI** de pilotage.  
Priorités : **simplicité d'intégration** (API REST), **résilience** (stratégies de reprise), **scalabilité horizontale** (multi-pods), **observabilité** (métriques, logs, événements temps réel).

Le framework est pensé en **mode framework réutilisable** :

- utilisable seul, comme une plateforme générique "SAF-Control + microservices d'acteurs" accessible par API,
- ou **embarqué** dans une application tierce qui fournit ses propres types d'acteurs (ex. City, Sensor…) via une **`ActorFactory`** sans que SAF ne dépende de cette application.

---

## Architecture

### SAF-Actor-Core (cœur du framework)

* **Rôle** : librairie Java générique qui définit les **abstractions d'acteurs** :
  * `Actor`, `ActorRef`, `ActorSystem`, `Mailbox`, `SupervisionPolicy`, `Message`, etc.
  * `ActorFactory` (contrat permettant de créer des acteurs à partir d'un type logique et d'un contexte).
* **Responsabilités** :
  * Modéliser le comportement d'un acteur (state + `receive(message)`).
  * Encapsuler la logique de supervision (restart / resume / stop).
  * Fournir un contrat d'**usine d'acteurs** (`ActorFactory`) que les applications tierces peuvent implémenter pour **brancher leurs propres acteurs métier**.
* **Dépendances** :
  * Ne dépend **d'aucun domaine applicatif** (pas de notion de ville, capteur, client, etc.).
  * Est utilisé à la fois par **SAF-Runtime** et par les librairies d'acteurs métiers des applications.

### SAF-Control (plan de contrôle)

* **Rôle** : façade **API** et **orchestrateur** de la plateforme.
* **Responsabilités** :

  * CRUD du **cycle de vie** des acteurs : créer (*spawn*), arrêter, configurer.
  * **Registry distribué** (annuaire) des acteurs et de leur localisation.
  * **Découverte de services** : les microservices d'acteurs s'enregistrent auprès de Control.
  * **Routage des requêtes** : distribution des demandes de création d'acteurs aux microservices appropriés.
  * **Initialisation par défaut** : création automatique de la configuration initiale (3 villes + capteurs).
  * **Événements temps réel** (WebSocket) pour l'UI (logs, métriques, états).
  * **Sécurité / quotas / audit** (clé API).
* **Interfaces** : API publique (REST + WebSocket).
* **Port** : 8080

> SAF-Control ne connaît **pas** les acteurs métiers concrets (City, Sensor, etc.) : il manipule des "types d'acteurs" et des payloads génériques, puis route les demandes vers les microservices qui savent créer ces acteurs.

### SAF-Runtime (bibliothèque d'exécution)

* **Rôle** : bibliothèque Java (Spring Boot) fournissant les **classes de base** pour exécuter les acteurs dans un microservice.
* **Responsabilités** :

  * **ActorSystem** qui orchestre :
    * **Actor** (état/behaviour), **Mailbox**, **Dispatcher** (threads/virtual threads),
    * **Supervision locale** (restart d'un acteur en échec),
    * **Timers** / messages différés.
  * **Classes de base** pour les microservices :
    * `BaseActorRuntimeController` : endpoints HTTP pour la création/gestion d'acteurs
    * `ActorSystemConfiguration` : configuration Spring pour l'ActorSystem
  * **ActorFactory** : point d'extension pour brancher des acteurs métiers.
  * **Health & metrics**.
* **Architecture** : Bibliothèque embarquée, pas un service autonome

> **Mode embedded** :
>
> - le **framework SAF** fournit `SAF-Actor-Core` (abstractions), `SAF-Runtime` (classes de base), `SAF-Control` (orchestrateur),
> - une application métier fournit un **module d'acteurs** (qui dépend de `saf-actor-core`) + une implémentation d'`ActorFactory`,
> - chaque type d'acteur vit dans son **propre microservice** qui étend SAF-Runtime.

### Microservices d'acteurs

Dans l'architecture actuelle, **chaque type d'acteur vit dans son propre microservice** :

* **Client Service** (port 8082)
  * Héberge les `ClientActor`
  * Chaque acteur représente un utilisateur/client
  * S'enregistre auprès de villes pour recevoir des rapports climatiques

* **Ville Service** (port 8083)
  * Héberge les `VilleActor`
  * Chaque acteur représente une ville (Paris, Lyon, Marseille)
  * Agrège les données des capteurs de sa ville
  * Envoie des rapports aux clients enregistrés

* **Capteur Service** (port 8084)
  * Héberge les `CapteurActor`
  * Chaque acteur représente un capteur (temperature, humidity, pressure)
  * Génère des lectures périodiques
  * Envoie les données à son VilleActor parent

> **Relation** :
>
> - Les **clients** (UI, scripts) parlent à **SAF-Control**.
> - **SAF-Control** route les demandes de création d'acteurs vers les **microservices appropriés** via HTTP.
> - Chaque **microservice** utilise SAF-Runtime comme base et fournit sa propre `ActorFactory`.
> - Les **acteurs communiquent** entre eux via HTTP en passant par SAF-Control qui maintient le registre.

### Flux type

**Créer un acteur**

1. Client → **SAF-Control** : `POST /api/v1/actors` (serviceId, type, params)
2. Control identifie le **microservice cible** via le `serviceId`
3. Control → **Microservice** : `POST /runtime/actors` (commande HTTP)
4. Microservice demande à son `ActorFactory` de créer l'acteur, puis **spawn** l'acteur
5. Microservice → **Control** : réponse avec l'UUID de l'acteur créé
6. Control enregistre l'acteur dans le **registre distribué**
7. Control → Client : réponse avec les infos de l'acteur

**Envoyer un message à un acteur**

1. Client → **SAF-Control** : `POST /api/v1/actors/{id}/tell` (payload)
2. Control consulte le **registre** pour localiser l'acteur
3. Control → **Microservice** : `POST /runtime/actors/{id}/tell` (routage HTTP)
4. Microservice traite (mailbox → behaviour de l'acteur)
5. Microservice → **Control** : confirmation
6. Control → Client : confirmation de livraison

**Communication inter-acteurs**

- Les acteurs communiquent via leurs `ActorRef`
- Les messages sont routés via **SAF-Control** qui connaît la localisation de chaque acteur
- Communication **HTTP synchrone** (pas de broker message pour le moment)

---

## Choix techniques

* **Frontend** : **React 19 + TypeScript + Vite**, **Tailwind CSS** + **shadcn/ui**

  * Rapidité de dev, design system cohérent, composants accessibles (Radix).
* **Backend** : **Spring Boot 3.x (Java 21)**

  * Simplicité de packaging, support natif observabilité/métriques, écosystème mature.
  * **Virtual Threads** (Loom) possibles pour concu élevée & code lisible.
* **Communication** : **HTTP/REST** pour la communication inter-services

  * SAF-Control comme API Gateway et registre central
  * Communication synchrone entre microservices
* **Architecture microservices** : **Un microservice par type d'acteur**

  * Isolation, scalabilité indépendante, déploiement séparé
* **Style d'archi (back)** : **Hexagonal / Ports & Adapters**

  * Domaine pur, ports in/out, adapters techniques remplaçables
* **Observabilité** : Micrometer → Prometheus/Grafana, logs JSON, WebSocket pour l'UI.

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
│  │  ├─ saf-runtime/                # Bibliothèque runtime (classes de base Spring Boot)
│  │  │  ├─ pom.xml
│  │  │  └─ src/main/java/com/acme/saf/saf_runtime/
│  │  │     ├─ DefaultActorSystem.java         # Implémentation ActorSystem
│  │  │     ├─ InMemoryMailbox.java            # Implémentation Mailbox
│  │  │     ├─ controller/
│  │  │     │  └─ BaseActorRuntimeController.java  # Contrôleur de base pour microservices
│  │  │     ├─ config/
│  │  │     │  └─ ActorSystemConfiguration.java    # Config Spring de base
│  │  │     ├─ metrics/                        # Métriques runtime
│  │  │     └─ websocket/                      # Support WebSocket
│  │  │
│  │  └─ saf-control/                # Control plane (Spring Boot)
│  │     ├─ pom.xml
│  │     └─ src/main/java/com/acme/saf/saf_control/
│  │        ├─ controller/           # Controllers REST/WebSocket
│  │        ├─ registry/             # Registre distribué d'acteurs
│  │        ├─ init/                 # Initialisation par défaut (3 villes)
│  │        ├─ security/             # Filtres de sécurité (API Key)
│  │        └─ dto/                  # DTOs pour l'API
│  │
│  └─ apps/                          # 🔶 APPLICATIONS (100% spécifiques au cas d'usage)
│     └─ iot-city/                   # Application IoT City
│        ├─ iot-city-domain/         # Acteurs métier (Client, Ville, Capteur)
│        │  ├─ pom.xml               # Dépend uniquement de saf-actor-core
│        │  └─ src/main/java/com/acme/iot/city/
│        │     ├─ actors/
│        │     │  ├─ ClientActor.java   # Acteur Client (métier)
│        │     │  ├─ VilleActor.java    # Acteur Ville (métier)
│        │     │  ├─ CapteurActor.java  # Acteur Capteur (métier)
│        │     │  └─ IotActorFactory.java  # Factory pour créer les acteurs IoT
│        │     ├─ model/             # Modèles métier (ClimateConfig, SensorReading, etc.)
│        │     └─ messages/          # Messages métier
│        │
│        ├─ client-service/          # Microservice Client (Spring Boot + SAF-Runtime)
│        │  ├─ pom.xml               # Dépend de: saf-runtime + iot-city-domain
│        │  ├─ Dockerfile
│        │  └─ src/main/java/com/acme/iot/client/
│        │     ├─ ClientServiceApplication.java   # Application Spring Boot
│        │     ├─ controller/
│        │     │  └─ ActorRuntimeController.java  # Hérite de BaseActorRuntimeController
│        │     ├─ actor/
│        │     │  ├─ HttpClientActor.java         # Adapter HTTP pour ClientActor
│        │     │  └─ HttpClientActorFactory.java  # Factory HTTP pour clients
│        │     └─ config/
│        │        ├─ ActorSystemConfiguration.java         # Config ActorSystem
│        │        └─ ServiceRegistrationInitializer.java   # Enregistrement SAF-Control
│        │
│        ├─ ville-service/           # Microservice Ville (Spring Boot + SAF-Runtime)
│        │  ├─ pom.xml               # Dépend de: saf-runtime + iot-city-domain
│        │  ├─ Dockerfile
│        │  └─ src/main/java/com/acme/iot/ville/
│        │     ├─ VilleServiceApplication.java
│        │     ├─ controller/
│        │     │  └─ ActorRuntimeController.java
│        │     ├─ actor/
│        │     │  ├─ HttpVilleActor.java
│        │     │  └─ HttpVilleActorFactory.java
│        │     └─ config/
│        │        ├─ ActorSystemConfiguration.java
│        │        └─ ServiceRegistrationInitializer.java
│        │
│        └─ capteur-service/         # Microservice Capteur (Spring Boot + SAF-Runtime)
│           ├─ pom.xml               # Dépend de: saf-runtime + iot-city-domain
│           ├─ Dockerfile
│           └─ src/main/java/com/acme/iot/capteur/
│              ├─ CapteurServiceApplication.java
│              ├─ controller/
│              │  └─ ActorRuntimeController.java
│              ├─ actor/
│              │  ├─ HttpCapteurActor.java
│              │  └─ HttpCapteurActorFactory.java
│              └─ config/
│                 ├─ ActorSystemConfiguration.java
│                 └─ ServiceRegistrationInitializer.java

└─ frontend/
   ├─ package.json                   # Scripts dev/build, deps React/Tailwind/shadcn
   ├─ pnpm-lock.yaml                 # Lockfile pnpm
   ├─ index.html                     # Entrée Vite
   ├─ vite.config.ts                 # Config Vite + alias "@"
   ├─ tailwind.config.ts             # Thème + tokens shadcn/ui
   ├─ components.json                # Config shadcn/ui
   ├─ Dockerfile                     # Docker multi-stage build
   ├─ nginx.conf                     # Configuration Nginx
   └─ src/
      ├─ App.tsx                     # Shell d'app (header/nav, routes)
      ├─ main.tsx                    # Entrée React
      ├─ components/ui/              # Composants shadcn/ui
      ├─ app/
      │  ├─ agents/                  # Dashboard agents génériques
      │  └─ iot-city/                # Dashboard IoT City
      └─ styles/globals.css          # Tailwind layers + variables
```

---

## Frontend

* **But** : Piloter la plateforme (créer/détruire des agents, envoyer des messages, visualiser l'état/les logs/les métriques).
* **Pages** :

  * **Agents** : liste, création/suppression, état en temps réel (WebSocket).
  * **IoT City** : tableau de bord des villes et capteurs avec mises à jour en temps réel.
  * **Messaging** : envoi **tell** avec routing automatique.
* **Tech** : shadcn/ui + Tailwind, React Router, fetch API.
* **Config** : Les requêtes `/api/*` sont automatiquement proxifiées vers **SAF-Control** par Nginx.

---

## Backend

* **SAF-Control** :

  * Ports in : REST + WebSocket.
  * Ports out : HTTP vers les microservices d'acteurs.
  * Cas d'usage : `CreateActor`, `DestroyActor`, `SendMessage`, `StreamEvents`.
  * Reste **agnostique métier** : ne connaît que des types d'acteurs et des payloads sérialisés.
  * **Registre distribué** : maintient la correspondance actorId → (serviceId, microservice URL)
  * **Résilience** : Mécanisme de heartbeat automatique avec re-registration des microservices en cas de redémarrage de SAF-Control

* **SAF-Runtime** :

  * Bibliothèque fournissant les **classes de base** pour créer un microservice d'acteurs.
  * Fournit : `ActorSystem`, `Mailbox`, `Dispatcher`, `SupervisionPolicy`, `BaseActorRuntimeController`.
  * Extensibilité : chaque microservice fournit une **implémentation d'`ActorFactory`** qui déclare comment instancier ses acteurs métier.

* **Microservices d'acteurs** :

  * Dépendent de `saf-runtime` (classes de base) + domaine métier.
  * Exposent des endpoints `/runtime/actors` pour la création/gestion d'acteurs.
  * S'enregistrent auprès de **SAF-Control** au démarrage.
  * Gèrent le cycle de vie de leurs acteurs localement.
  * **Auto-récupération** : Heartbeat périodique (30s par défaut) et re-registration automatique si SAF-Control redémarre

### Sécurité (clé API)

Les endpoints de **SAF-Control** sont protégés par une **clé API** simple, vérifiée via un filtre Spring (`ApiKeyFilter`).

#### Configuration

La clé est définie dans `application.properties` :

```properties
saf.security.api-key=cle-api
```

#### En-tête attendu

Chaque requête doit inclure l'en-tête HTTP suivant :

```text
X-API-KEY: cle-api
```

#### Exemple de test (PowerShell)

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/v1/actors -Headers @{ "X-API-KEY" = "cle-api" }
```

#### Exemple de test (cURL)

```bash
curl -H "X-API-KEY: cle-api" http://localhost:8080/api/v1/actors
```

#### Exemple côté front (fetch)

Lorsque le front communique avec le backend, il doit inclure la clé dans les en-têtes HTTP :

```javascript
fetch("http://localhost:8080/api/v1/actors", {
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

> **Scope minimal** pour amorcer l'UI ; les schémas exacts sont disponibles via Swagger/OpenAPI.

**SAF-Control — REST**

* `POST /api/v1/actors`
  Request: `{ "serviceId": "ville-service", "type": "VilleActor", "params": { "name": "Paris", ... } }`
  Response: `{ "actorId": "uuid", "serviceId": "ville-service", "type": "VilleActor", ... }`
* `DELETE /api/v1/actors/{id}`
  Response: `{ "actorId": "uuid", "status": "stopped" }`
* `GET /api/v1/actors`
  Response: `[ { "actorId": "uuid", "serviceId": "ville-service", ... }, ... ]`
* `POST /api/v1/actors/{id}/tell`
  Request: `{ "payload": { ... } }`
  Response: `{ "delivered": true }`
* `GET /api/v1/services`
  Response: `[ { "serviceId": "ville-service", "url": "http://ville-service:8083", ... }, ... ]`
* WebSocket: `/ws` pour les événements temps réel

**Microservices — endpoints internes**

* `GET /actuator/health` → `{"status": "UP"}`
* `GET /actuator/prometheus` → métriques Prometheus
* `POST /runtime/actors` → création d'acteur (appelé par SAF-Control)
* `POST /runtime/actors/{id}/tell` → envoi de message (appelé par SAF-Control)

---

## Initialisation de l'Application IoT City

L'application IoT City nécessite une **initialisation manuelle** après le démarrage de tous les services. Cette approche garantit une séparation claire entre le framework SAF (générique) et l'application métier (IoT City).

### Script d'Initialisation

Un script bash est fourni pour créer la configuration initiale :

```bash
./scripts/init-iot-city.sh
```

**Ce script crée automatiquement :**
* **3 Villes** : Paris, Lyon, Marseille
* **6 Clients** : 2 clients par ville
* **9 Capteurs** : 3 capteurs par ville (température, humidité, pression)

### Fonctionnement du Script

Le script effectue les opérations suivantes :

1. **Vérification de santé** : Attend que tous les services soient opérationnels
   - SAF-Control (port 8080)
   - Client Service (port 8084)
   - Ville Service (port 8085)
   - Capteur Service (port 8086)

2. **Création des villes** : Envoie des requêtes API pour créer les VilleActors
   - Paris (2.1M habitants, 105.4 km²)
   - Lyon (516K habitants, 47.87 km²)
   - Marseille (870K habitants, 240.62 km²)

3. **Création des clients** : Crée 2 ClientActors par ville
   - S'enregistrent auprès de leur ville
   - Recevront les rapports climatiques

4. **Création des capteurs** : Crée 3 CapteurActors par ville
   - Capteur de température
   - Capteur d'humidité
   - Capteur de pression

### Utilisation

**Avec Docker Compose :**

```bash
# 1. Démarrer tous les services
docker-compose up -d

# 2. Attendre que les services soient healthy (environ 60s)
docker-compose ps

# 3. Exécuter le script d'initialisation
./scripts/init-iot-city.sh
```

### Vérification

Après l'exécution du script, vous pouvez vérifier que les acteurs ont été créés :

```bash
# Lister tous les acteurs
curl -H "X-API-KEY: mock-secret" http://localhost:8080/api/v1/actors

# Vérifier les services enregistrés
curl -H "X-API-KEY: mock-secret" http://localhost:8080/api/v1/services
```

### Idempotence

Le script est **idempotent** : si les acteurs existent déjà, les requêtes de création échoueront mais le script continuera. Pour réinitialiser complètement :

```bash
# Arrêter et supprimer tous les conteneurs
docker-compose down

# Redémarrer
docker-compose up -d

# Réexécuter le script
./scripts/init-iot-city.sh
```

---

## Démarrage local

Deux options sont disponibles pour démarrer la plateforme localement :

### Option 1 : Docker Compose

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
* **SAF-Control API** : http://localhost:8080
* **Client service** : http://localhost:8082
* **Ville service** : http://localhost:8083
* **Capteur service** : http://localhost:8084
* **Swagger UI** : http://localhost:8080/swagger
* **Health Check** : http://localhost:8080/actuator/health

Pour plus de détails (architecture, commandes, dépannage, sécurité), voir **[DOCKER.md](./DOCKER.md)**.


### Option 2 (dépréciée) : Développement natif

> **Pré-requis** : Node.js ≥ 20, pnpm (ou npm), Java 21.

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
* **API** : `GET http://localhost:8080/api/v1/actors` (avec header `X-API-KEY`)

##### Microservices IoT City

Démarrer les 3 microservices (dans des terminaux séparés) :

```bash
# Client service (port 8082)
cd backend/apps/iot-city/client-service
mvn spring-boot:run
```

```bash
# Ville service (port 8083)
cd backend/apps/iot-city/ville-service
mvn spring-boot:run
```

```bash
# Capteur service (port 8084)
cd backend/apps/iot-city/capteur-service
mvn spring-boot:run
```

**Vérification :**

```bash
# Vérifier que les services sont enregistrés
curl -H "X-API-KEY: test" http://localhost:8080/api/v1/services

# Vérifier les acteurs créés automatiquement
curl -H "X-API-KEY: test" http://localhost:8080/api/v1/actors
```


---

## Conventions & qualité

* **Branches** : `main` (stable), `dev` (intégration), `feature/*`.
* **Commits** : Conventional Commits (`feat:`, `fix:`, `docs:`…).
* **Qualité** :

  * Front : ESLint, Prettier.
  * Back : JUnit 5, tests d'intégration.

---

## Système de Supervision

Le framework SAF implémente un **système de supervision à 3 niveaux** pour garantir la résilience et la disponibilité :

### 1. Supervision des Microservices (Infrastructure)

**`ServiceHealthMonitor`** - Surveillance active des microservices
- **Health checks HTTP** toutes les 10 secondes vers `/actuator/health`
- **Détection automatique** des services down/recovered
- **Marquage des acteurs** comme unavailable/available en cas de panne
- **Événements** : `ServiceDownEvent`, `ServiceRecoveredEvent` pour monitoring

**Flux de supervision :**
```
Service DOWN détecté
  → service.setActive(false)
  → actorRegistry.markActorsUnavailable(serviceId)
  → Événement ServiceDownEvent émis

Service RECOVERED détecté
  → service.setActive(true)
  → actorRegistry.markActorsAvailable(serviceId)
  → Événement ServiceRecoveredEvent émis
```

### 2. Endpoints de Santé des Acteurs (Application)

Chaque microservice expose des **endpoints de santé** pour superviser ses acteurs :
- Consultation du statut de santé d'un acteur (`GET /runtime/actors/{id}/health`)
- Redémarrage manuel d'un acteur en cas de besoin (`POST /runtime/actors/{id}/restart`)

### 3. Supervision Locale Automatique (Application)

Application automatique des stratégies de supervision dans chaque microservice :
- **Restart automatique** des acteurs en cas d'exception
- **Stratégies disponibles :**
  - **`OneForOneStrategy`** (défaut) : Redémarre uniquement l'acteur en échec
  - **`AllForOneStrategy`** : Redémarre tous les acteurs supervisés
- **Recovery intelligent** : resume, stop ou escalade selon la gravité

### Résilience Complète
Le système de supervision offre :
- Détection automatique des services down en 10 secondes
- Marquage des acteurs orphelins comme unavailable
- Recovery automatique des services
- Restart automatique des acteurs en cas d'exception
- Logs de supervision détaillés pour debugging

---

## Feuille de route

1. [x] **Architecture microservices** : SAF-Control + microservices par type d'acteur
2. [x] **Initialisation par défaut** : 3 villes + 9 capteurs créés automatiquement
3. [x] **Communication HTTP** : routage via SAF-Control entre microservices
4. [x] **WebSocket** : mises à jour en temps réel pour le frontend
5. [x] **Supervision** : système complet à 3 niveaux (infrastructure, acteurs, stratégies)
6. [ ] **Métriques avancées** : observabilité complète avec Prometheus/Grafana
7. [ ] **Persistance** : snapshots d'état et event store (optionnel)
8. [ ] **Broker message** : intégration Kafka/RabbitMQ pour communication async (optionnel)
9. [ ] **Scalabilité horizontale** : déploiement multi-instances avec load balancing

---

## Licence

Apache-2.0
