# SAF-Actor-Core

Core abstractions and interfaces for the SAF (Spring Actors Framework) actor system.

## Overview

`saf-actor-core` is a lightweight, framework-agnostic library that defines the fundamental building blocks for actor-based concurrent systems. It provides interfaces and base implementations inspired by the Actor Model, similar to Akka or Erlang's OTP.

This library is **domain-agnostic** and can be used by:
- **SAF-Runtime**: The execution engine that runs actors
- **Application-specific actor implementations**: Custom actors (e.g., `CityActor`, `SensorActor`) via plugins
- **Any Java application** that wants to implement actor-based concurrency

## Core Concepts

### Actor

The `Actor` interface represents a computational entity that:
- Processes messages **sequentially** (one at a time)
- Maintains **encapsulated state**
- Communicates **only through asynchronous messages**
- Can **create other actors**
- Can **change behavior** for the next message

```java
public interface Actor {
    void receive(Message message) throws Exception;
    
    // Lifecycle hooks
    default void preStart() throws Exception { }
    default void postStop() throws Exception { }
    default void preRestart(Throwable reason, Message message) throws Exception { }
    default void postRestart(Throwable reason) throws Exception { }
}
```

**Lifecycle hooks:**
- `preStart()`: Called before the actor starts processing messages (initialization)
- `postStop()`: Called after the actor stops (cleanup)
- `preRestart()`: Called before restarting after a failure
- `postRestart()`: Called after restarting

### Message

The `Message` interface represents data exchanged between actors:

```java
public interface Message extends Serializable {
    String getMessageId();
    Instant getTimestamp();
    String getCorrelationId();
    Object getPayload();
}
```

**Key features:**
- **Immutable**: Messages should not change after creation
- **Serializable**: Supports distributed actor systems
- **Self-describing**: Contains metadata (ID, timestamp, correlation ID)

**Implementation:**
Use `SimpleMessage` for basic messaging:

```java
Message msg = new SimpleMessage("Hello, Actor!");
Message msgWithCorrelation = new SimpleMessage(payload, "correlation-123");
```

### ActorRef

The `ActorRef` interface provides a **reference** to an actor without exposing the actor instance:

```java
public interface ActorRef {
    String getActorId();
    String getPath();
    
    // Tell pattern (fire-and-forget)
    void tell(Message message);
    void tell(Message message, ActorRef sender);
    
    // Ask pattern (request-response)
    CompletableFuture<Object> ask(Message message);
    CompletableFuture<Object> ask(Message message, long timeout, TimeUnit unit);
    
    // Forwarding
    void forward(Message message, ActorRef originalSender);
    
    boolean isActive();
    void stop();
}
```

**Communication patterns:**
- **tell**: Fire-and-forget asynchronous messaging
- **ask**: Request-response with timeout support (returns a `CompletableFuture`)
- **forward**: Relay a message preserving the original sender

### Actor Lifecycle

Actors transition through well-defined states:

```
CREATED → STARTING → RUNNING → STOPPING → STOPPED
                ↓
              FAILED → RESTARTING → RUNNING
```

**States (`ActorLifecycleState`):**
- `CREATED`: Actor instantiated but not started
- `STARTING`: Executing `preStart()`
- `RUNNING`: Normal operation, processing messages
- `RESTARTING`: Recovering from a failure
- `STOPPING`: Executing `postStop()`
- `STOPPED`: Terminal state, no longer processes messages
- `FAILED`: Awaiting supervision decision

**Lifecycle Events (`ActorLifecycleEvent`):**
- `ActorCreated`: Actor instance created
- `ActorStarted`: Actor started and ready
- `ActorStopped`: Actor stopped (with reason)
- `ActorFailed`: Actor encountered an error
- `ActorRestarted`: Actor recovered from failure

## Example: Simple Counter Actor

```java
public class CounterActor implements Actor {
    private int count = 0;
    
    @Override
    public void receive(Message message) throws Exception {
        Object payload = message.getPayload();
        
        if (payload instanceof String) {
            String command = (String) payload;
            switch (command) {
                case "increment":
                    count++;
                    System.out.println("Count: " + count);
                    break;
                case "decrement":
                    count--;
                    System.out.println("Count: " + count);
                    break;
                case "get":
                    // In a real system, you would send a reply to the sender
                    System.out.println("Current count: " + count);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        }
    }
    
    @Override
    public void preStart() throws Exception {
        System.out.println("CounterActor starting...");
        count = 0;
    }
    
    @Override
    public void postStop() throws Exception {
        System.out.println("CounterActor stopped. Final count: " + count);
    }
}
```

## Example: Echo Actor with Lifecycle

```java
public class EchoActor implements Actor {
    private String name;
    
    public EchoActor(String name) {
        this.name = name;
    }
    
    @Override
    public void receive(Message message) throws Exception {
        System.out.println(name + " received: " + message.getPayload());
        // In a real implementation, would reply to sender
    }
    
    @Override
    public void preStart() throws Exception {
        System.out.println(name + " started");
    }
    
    @Override
    public void postStop() throws Exception {
        System.out.println(name + " stopped");
    }
    
    @Override
    public void preRestart(Throwable reason, Message message) throws Exception {
        System.out.println(name + " restarting due to: " + reason.getMessage());
        super.preRestart(reason, message);
    }
    
    @Override
    public void postRestart(Throwable reason) throws Exception {
        System.out.println(name + " restarted successfully");
        super.postRestart(reason);
    }
}
```

## Integration with SAF Framework

### For SAF-Runtime
The runtime will provide concrete implementations of:
- `ActorRef`: Wraps actors with mailbox and dispatcher
- `ActorSystem`: Manages actor lifecycle and hierarchy
- `Mailbox`: Queues messages for sequential processing
- `Dispatcher`: Schedules actor message processing (threads/virtual threads)

### For Application Developers
Create custom actors by:
1. Implementing the `Actor` interface
2. Defining message types (implement `Message`)
3. Implementing an `ActorFactory` to create actor instances
4. Register the factory with SAF-Runtime

## Design Principles

1. **Separation of Concerns**: Core abstractions are independent of execution details
2. **Type Safety**: Strong typing with Java interfaces
3. **Framework Agnostic**: No Spring or runtime dependencies in core
4. **Extensible**: Default methods allow incremental implementation
5. **Observable**: Lifecycle events for monitoring and debugging

## Dependencies

None. This is a pure Java library with no external dependencies.

## Package Structure

```
com.acme.saf.actor.core/
├── Actor.java                  # Core actor interface
├── ActorRef.java              # Actor reference for messaging
├── Message.java               # Message interface
├── SimpleMessage.java         # Basic message implementation
├── ActorLifecycleState.java   # Lifecycle state enum
└── ActorLifecycleEvent.java   # Lifecycle event classes
```

## Next Steps

To build a complete actor system, you'll need to implement:

1. **ActorSystem**: Manages actor creation, supervision, and lifecycle
2. **Mailbox**: Message queue for each actor
3. **Dispatcher**: Thread pool or virtual thread executor
4. **SupervisionStrategy**: Handles actor failures (restart/resume/stop)
5. **ActorFactory**: Plugin mechanism for custom actor types
6. **Serialization**: Message serialization for distributed systems

These will be provided by the `saf-runtime` module.

## License

Apache-2.0
