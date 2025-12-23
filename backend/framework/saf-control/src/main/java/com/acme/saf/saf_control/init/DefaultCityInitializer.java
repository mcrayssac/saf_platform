package com.acme.saf.saf_control.init;

import com.acme.saf.saf_control.dto.CreateActorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Initializes default cities and sensors on application startup.
 * 
 * Creates 3 cities (Paris, Lyon, Marseille) with sensors for each.
 */
@Component
public class DefaultCityInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultCityInitializer.class);
    
    private final WebClient webClient;
    
    @Value("${saf.init.enabled:true}")
    private boolean initEnabled;
    
    @Value("${saf.init.delay-seconds:5}")
    private int delaySeconds;
    
    public DefaultCityInitializer(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaultConfiguration() {
        if (!initEnabled) {
            log.info("Default initialization disabled");
            return;
        }
        
        log.info("Initializing default cities and sensors in {} seconds...", delaySeconds);
        
        // Wait for services to be fully ready
        Mono.delay(Duration.ofSeconds(delaySeconds))
            .then(Mono.defer(this::createDefaultCities))
            .subscribe(
                result -> log.info("✅ Default configuration initialized successfully"),
                error -> log.error("❌ Failed to initialize default configuration", error)
            );
    }
    
    private Mono<Void> createDefaultCities() {
        return createCity("Paris", 48.8566, 2.3522)
            .then(createCity("Lyon", 45.7640, 4.8357))
            .then(createCity("Marseille", 43.2965, 5.3698))
            .then();
    }
    
    private Mono<Void> createCity(String cityName, double latitude, double longitude) {
        log.info("Creating city: {}", cityName);
        
        return createVilleActor(cityName, latitude, longitude)
            .delayElement(Duration.ofMillis(500))
            .then(createCapteur(cityName, "temperature", "Temperature Sensor"))
            .delayElement(Duration.ofMillis(500))
            .then(createCapteur(cityName, "humidity", "Humidity Sensor"))
            .delayElement(Duration.ofMillis(500))
            .then(createCapteur(cityName, "pressure", "Pressure Sensor"))
            .then(Mono.fromRunnable(() -> log.info("✓ City {} initialized with 3 sensors", cityName)))
            .then();
    }
    
    private Mono<String> createVilleActor(String cityName, double latitude, double longitude) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", cityName);
        params.put("latitude", latitude);
        params.put("longitude", longitude);
        
        CreateActorRequest request = new CreateActorRequest();
        request.setServiceId("ville-service");
        request.setActorType("VilleActor");
        request.setParams(params);
        
        return webClient.post()
            .uri("http://localhost:8080/api/v1/actors")
            .header("X-API-KEY", "test")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                String actorId = (String) response.get("actorId");
                log.info("  → Created VilleActor: {} ({})", cityName, actorId);
                return actorId;
            })
            .onErrorResume(error -> {
                log.warn("  ⚠ Failed to create VilleActor {}: {}", cityName, error.getMessage());
                return Mono.empty();
            });
    }
    
    private Mono<String> createCapteur(String cityName, String sensorType, String sensorName) {
        Map<String, Object> params = new HashMap<>();
        params.put("villeId", cityName); // VilleActor ID
        params.put("type", sensorType);
        params.put("name", cityName + " - " + sensorName);
        
        CreateActorRequest request = new CreateActorRequest();
        request.setServiceId("capteur-service");
        request.setActorType("CapteurActor");
        request.setParams(params);
        
        return webClient.post()
            .uri("http://localhost:8080/api/v1/actors")
            .header("X-API-KEY", "test")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                String actorId = (String) response.get("actorId");
                log.info("    • Created CapteurActor: {} ({})", sensorName, actorId);
                return actorId;
            })
            .onErrorResume(error -> {
                log.warn("    ⚠ Failed to create CapteurActor {}: {}", sensorName, error.getMessage());
                return Mono.empty();
            });
    }
}
