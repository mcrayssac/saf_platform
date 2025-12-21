package com.acme.saf.saf_control.runtime.factory;

import com.acme.saf.saf_control.runtime.core.Actor;
import com.acme.saf.saf_control.runtime.core.ActorFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultActorFactory implements ActorFactory {

    @Override
    public Actor create(String type, Map<String, Object> params) {
        return switch (type.toUpperCase()) {
            case "CLIENT" -> new ClientActor(params);
            case "VILLE" -> new VilleActor(params);
            case "CAPTEUR" -> new CapteurActor(params);
            default -> null;
        };
    }

    @Override
    public boolean supports(String type) {
        return "CLIENT".equalsIgnoreCase(type)
                || "VILLE".equalsIgnoreCase(type)
                || "CAPTEUR".equalsIgnoreCase(type);
    }
}