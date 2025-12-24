#!/bin/bash

# Script pour vérifier l'installation du système de messaging inter-pods du SAF Framework

echo "=========================================="
echo "SAF Framework Inter-Pod Messaging Verification"
echo "=========================================="
echo ""

MESSAGING_DIR="backend/framework/saf-runtime/src/main/java/com/acme/saf/saf_runtime/messaging"
BROKERS_DIR="$MESSAGING_DIR/brokers"
EXAMPLES_DIR="$MESSAGING_DIR/examples"

echo "Checking core messaging files..."
FILES=(
    "$MESSAGING_DIR/BrokerMessage.java"
    "$MESSAGING_DIR/BrokerException.java"
    "$MESSAGING_DIR/MessageHandler.java"
    "$MESSAGING_DIR/MessageSerializer.java"
    "$MESSAGING_DIR/JacksonMessageSerializer.java"
    "$MESSAGING_DIR/MessageBroker.java"
    "$MESSAGING_DIR/MessageProducer.java"
    "$MESSAGING_DIR/DefaultMessageProducer.java"
    "$MESSAGING_DIR/MessageConsumer.java"
    "$MESSAGING_DIR/DefaultMessageConsumer.java"
    "$MESSAGING_DIR/MessageMetadata.java"
    "$MESSAGING_DIR/ProducerCallback.java"
    "$MESSAGING_DIR/InterPodMessaging.java"
    "$MESSAGING_DIR/MessagingConfiguration.java"
    "$MESSAGING_DIR/MessagingIntegrationTest.java"
    "$BROKERS_DIR/AbstractMessageBroker.java"
    "$BROKERS_DIR/KafkaBroker.java"
    "$BROKERS_DIR/RabbitMQBroker.java"
    "$BROKERS_DIR/MessageBrokerFactory.java"
    "$EXAMPLES_DIR/InterPodMessagingExample.java"
    "$EXAMPLES_DIR/IotActorIntegrationExample.java"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (MISSING)"
    fi
done

echo ""
echo "Checking configuration files..."
CONFIG_FILES=(
    "backend/framework/saf-runtime/src/main/resources/messaging.properties"
    "backend/framework/saf-runtime/MESSAGING_GUIDE.md"
    "backend/framework/saf-runtime/ARCHITECTURE_MESSAGING.md"
    "$MESSAGING_DIR/README.md"
)

for file in "${CONFIG_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (MISSING)"
    fi
done

echo ""
echo "=========================================="
echo "Verification Complete"
echo "=========================================="
