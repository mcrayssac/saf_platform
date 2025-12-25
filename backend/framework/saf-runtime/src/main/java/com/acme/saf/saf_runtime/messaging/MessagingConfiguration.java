package com.acme.saf.saf_runtime.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration pour le système de messaging inter-pods.
 * 
 * Charge la configuration depuis un fichier de propriétés et initialise
 * les composants nécessaires (producteur, consommateur).
 */
public class MessagingConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingConfiguration.class);
    
    private static final String CONFIG_FILE = "messaging.properties";
    private static final String DEFAULT_BROKER_TYPE = "kafka";
    
    private final Properties properties;
    
    /**
     * Crée une configuration en chargeant les propriétés depuis le fichier par défaut.
     */
    public MessagingConfiguration() {
        this(CONFIG_FILE);
    }
    
    /**
     * Crée une configuration en chargeant les propriétés depuis un fichier spécifique.
     * 
     * @param configFilePath Le chemin du fichier de configuration
     */
    public MessagingConfiguration(String configFilePath) {
        this.properties = new Properties();
        loadConfiguration(configFilePath);
    }
    
    /**
     * Crée une configuration avec les propriétés fournies.
     * 
     * @param properties Les propriétés de configuration
     */
    public MessagingConfiguration(Properties properties) {
        this.properties = properties != null ? properties : new Properties();
    }
    
    /**
     * Charge la configuration depuis un fichier de propriétés.
     * 
     * @param configFilePath Le chemin du fichier
     */
    private void loadConfiguration(String configFilePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFilePath)) {
            if (input == null) {
                logger.warn("Configuration file not found: {}. Using default configuration.", configFilePath);
                setDefaults();
            } else {
                properties.load(input);
                logger.info("Configuration loaded from: {}", configFilePath);
            }
            
        } catch (Exception e) {
            logger.warn("Error loading configuration from {}: {}. Using defaults.", configFilePath, e.getMessage());
            setDefaults();
        }
        
        // Override avec variables d'environnement
        loadEnvironmentVariables();
    }
    
    /**
     * Définit les valeurs par défaut si la configuration n'est pas chargée.
     */
    private void setDefaults() {
        if (properties.isEmpty()) {
            properties.setProperty("broker.type", DEFAULT_BROKER_TYPE);
            properties.setProperty("broker.name", "default");
            properties.setProperty("kafka.bootstrap.servers", "localhost:9092");
            properties.setProperty("rabbitmq.host", "localhost");
            properties.setProperty("rabbitmq.port", "5672");
            properties.setProperty("rabbitmq.username", "guest");
            properties.setProperty("rabbitmq.password", "guest");
            properties.setProperty("consumer.threads", "4");
            logger.info("Using default messaging configuration");
        }
    }
    
    /**
     * Charge les variables d'environnement pour overrider la configuration.
     */
    private void loadEnvironmentVariables() {
        String messagingBrokerType = System.getenv("MESSAGING_BROKER_TYPE");
        if (messagingBrokerType != null && !messagingBrokerType.isEmpty()) {
            properties.setProperty("broker.type", messagingBrokerType);
            logger.info("Set broker type from env: {}", messagingBrokerType);
        }
        
        String kafkaBrokers = System.getenv("KAFKA_BROKERS");
        if (kafkaBrokers != null && !kafkaBrokers.isEmpty()) {
            properties.setProperty("kafka.bootstrap.servers", kafkaBrokers);
            logger.info("Set Kafka brokers from env: {}", kafkaBrokers);
        }
        
        String rabbitmqHost = System.getenv("RABBITMQ_HOST");
        if (rabbitmqHost != null && !rabbitmqHost.isEmpty()) {
            properties.setProperty("rabbitmq.host", rabbitmqHost);
            logger.info("Set RabbitMQ host from env: {}", rabbitmqHost);
        }
        
        String rabbitmqPort = System.getenv("RABBITMQ_PORT");
        if (rabbitmqPort != null && !rabbitmqPort.isEmpty()) {
            properties.setProperty("rabbitmq.port", rabbitmqPort);
            logger.info("Set RabbitMQ port from env: {}", rabbitmqPort);
        }
    }
    
    /**
     * Initialise le système de messaging et retourne le gestionnaire.
     * 
     * @return L'instance configurée du gestionnaire InterPodMessaging
     * @throws Exception en cas d'erreur d'initialisation
     */
    public InterPodMessaging initializeMessaging() throws Exception {
        try {
            String brokerType = properties.getProperty("broker.type", DEFAULT_BROKER_TYPE);
            
            logger.info("Initializing messaging with broker type: {}", brokerType);
            
            // Créer le sérialiseur
            MessageSerializer serializer = new JacksonMessageSerializer();
            
            // Créer le producteur et consommateur
            MessageProducer producer = new DefaultMessageProducer(serializer);
            MessageConsumer consumer = new DefaultMessageConsumer(serializer);
            
            // Initialiser et retourner le gestionnaire
            InterPodMessaging messaging = InterPodMessaging.initialize(producer, consumer);
            logger.info("Messaging system initialized successfully with {} broker", brokerType);
            
            return messaging;
            
        } catch (Exception e) {
            logger.error("Error during messaging initialization", e);
            throw e;
        }
    }
    
    /**
     * Retourne une propriété de configuration.
     * 
     * @param key La clé
     * @param defaultValue La valeur par défaut
     * @return La valeur de la propriété
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Retourne une propriété de configuration.
     * 
     * @param key La clé
     * @return La valeur de la propriété ou null
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Retourne le type du broker configuré.
     * 
     * @return Le type du broker
     */
    public String getBrokerType() {
        return properties.getProperty("broker.type", DEFAULT_BROKER_TYPE);
    }
}
