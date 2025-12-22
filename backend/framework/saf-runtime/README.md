# SAF-Runtime - Implémentation de Mailbox

Ce module fournit une implémentation concrète de `Mailbox` en mémoire avec gestion FIFO et de la concurrence.

## InMemoryMailbox - Implémentation principale

**Base technologique** : `ConcurrentLinkedQueue` + `ReentrantLock`

### Caractéristiques
- ✅ **Stockage en mémoire** (pas de persistance)
- ✅ **FIFO garanti** (First In, First Out)
- ✅ **Thread-safe** avec `ConcurrentLinkedQueue`
- ✅ **Métriques intégrées** (compteurs enqueue/dequeue)
- ✅ **Verrouillage explicite** pour opérations atomiques

### Usage basique
```java
// Créer une mailbox
InMemoryMailbox mailbox = new InMemoryMailbox();

// Ajouter des messages
mailbox.enqueue(new MyMessage("Hello"));
mailbox.enqueue(new MyMessage("World"));

// Traiter les messages (FIFO)
Message msg;
while ((msg = mailbox.dequeue()) != null) {
    System.out.println(msg.getPayload());
}
```

### Méthodes disponibles
```java
// Opérations de base
mailbox.enqueue(message);     // Ajouter un message
Message msg = mailbox.dequeue(); // Retirer un message
boolean empty = mailbox.isEmpty(); // Vérifier si vide
int size = mailbox.size();    // Nombre de messages
mailbox.clear();             // Vider la mailbox

// Métriques
int enqueued = mailbox.getEnqueueCount();  // Total ajoutés
int dequeued = mailbox.getDequeueCount();  // Total retirés
int pending = mailbox.getPendingCount();   // En attente

// Verrouillage
boolean locked = mailbox.isLocked();       // État du verrou
mailbox.withLock(() -> {
    // Opération atomique
});
```

## Gestion de la Concurrence

### Mécanismes utilisés

1. **ConcurrentLinkedQueue**
   - Thread-safe sans verrous explicites
   - Performance élevée pour enqueue/dequeue
   - Garantit l'ordre FIFO

2. **ReentrantLock**
   - Verrouillage explicite pour `clear()`
   - Évite les conditions de course
   - Fairness FIFO pour les threads en attente

3. **AtomicInteger**
   - Compteurs thread-safe
   - Métriques sans synchronisation
   - Incrémentation atomique

### Thread-Safety garantie
- ✅ Lectures/écritures concurrentes
- ✅ Pas de corruption de données
- ✅ FIFO respecté même sous charge
- ✅ Métriques atomiques
- ✅ Opérations `clear()` sûres

## Architecture

```
Mailbox (interface de saf-actor-core)
└── InMemoryMailbox (implémentation concrète)
    ├── ConcurrentLinkedQueue (file d'attente)
    ├── ReentrantLock (verrouillage)
    └── AtomicInteger (métriques)
```

## Compilation et utilisation

### Compiler le module
```bash
cd backend/framework/saf-runtime
mvn compile
```

### Utiliser dans votre code
```java
import com.acme.saf.saf_runtime.InMemoryMailbox;
import com.acme.saf.actor.core.Mailbox;

// Créer et utiliser
Mailbox mailbox = new InMemoryMailbox();
```

## Dépendances

- `saf-actor-core` : Interface `Mailbox` et `Message`

Cette implémentation fournit une mailbox robuste, performante et thread-safe pour votre système d'acteurs SAF.