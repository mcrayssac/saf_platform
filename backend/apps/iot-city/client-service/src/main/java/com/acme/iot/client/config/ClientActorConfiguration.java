package com.acme.iot.client.config;

import com.acme.iot.city.actors.ClientActor;
import com.acme.saf.actor.core.Actor;
import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import com.acme.saf.saf_runtime.InMemoryMailbox;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ClientActorConfiguration {

    @Bean
    public ActorFactory clientActorFactory() {
        return new ActorFactory() {
            @Override
            public Actor create(String type, Map<String, Object> params) {
                if ("CLIENT".equals(type)) {
                    return new ClientActor(params);
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
