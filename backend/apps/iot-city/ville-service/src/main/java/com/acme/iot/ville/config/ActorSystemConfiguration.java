package com.acme.iot.ville.config;

import com.acme.iot.ville.actor.HttpVilleActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfiguration {

    @Bean
    public ActorSystem actorSystem(HttpVilleActorFactory actorFactory) {
        return new DefaultActorSystem(actorFactory);
    }
}
