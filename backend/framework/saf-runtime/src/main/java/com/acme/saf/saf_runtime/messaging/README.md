# SAF Runtime - Messaging Module

Communication inter-pods via Kafka ou RabbitMQ.

## Quick Start

```java
// Initialize
MessagingConfiguration config = new MessagingConfiguration();
InterPodMessaging messaging = config.initializeMessaging();

// Send
messaging.getProducer().send(message, "topic-name");

// Receive
messaging.getConsumer().subscribe(
    "com.example.MyMessage",
    MyMessage.class,
    msg -> handleMessage(msg)
);
messaging.getConsumer().listen("topic-name");
```

## Configuration

Edit `messaging.properties`:
```properties
broker.type=kafka
kafka.bootstrap.servers=localhost:9092
```

Or with RabbitMQ:
```properties
broker.type=rabbitmq
rabbitmq.host=localhost
rabbitmq.port=5672
```

## API

### InterPodMessaging (Singleton)
- `getInstance()` - Get instance
- `getProducer()` - Get producer
- `getConsumer()` - Get consumer
- `isConnected()` - Check connection

### MessageProducer
- `send(message, topic)` - Send synchronously
- `sendAsync(message, topic, callback)` - Send asynchronously

### MessageConsumer
- `subscribe(type, class, listener)` - Register listener
- `listen(topic)` - Start listening
- `stopListening(topic)` - Stop listening

## Files

- `BrokerMessage.java` - Transport message
- `MessageSerializer.java` + `JacksonMessageSerializer.java` - Serialization
- `MessageBroker.java` + `KafkaBroker.java` + `RabbitMQBroker.java` - Brokers
- `MessageProducer.java` + `DefaultMessageProducer.java` - Producer
- `MessageConsumer.java` + `DefaultMessageConsumer.java` - Consumer
- `InterPodMessaging.java` - Singleton manager
- `MessagingConfiguration.java` - Configuration
- `messaging.properties` - Default config
