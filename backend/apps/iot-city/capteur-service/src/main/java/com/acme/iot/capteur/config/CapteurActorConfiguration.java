package com.acme.iot.capteur.config;

import com.acme.iot.city.actors.CapteurActor;
import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import com.acme.saf.saf_runtime.InMemoryMailbox;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CapteurActorConfiguration {

    @Bean
    public ActorFactory capteurActorFactory() {
        return new ActorFactory() {
            @Override
            public Actor create(String type, Map<String, Object> params) {
                if ("CAPTEUR".equals(type)) {
                    return new CapteurActor(params);
                }
                throw new IllegalArgumentException("Unknown actor type: " + type);
            }
        };
    }

    @Bean
    public ActorSystem actorSystem(ActorFactory factory) {
        return new DefaultActorSystem(factory, new InMemoryMailbox());
    }
}
