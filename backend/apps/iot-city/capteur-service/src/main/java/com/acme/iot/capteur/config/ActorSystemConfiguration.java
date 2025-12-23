package com.acme.iot.capteur.config;

import com.acme.iot.capteur.actor.HttpCapteurActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfiguration {

    @Bean
    public ActorSystem actorSystem(HttpCapteurActorFactory actorFactory) {
        return new DefaultActorSystem(actorFactory);
    }
}
