package com.acme.iot.client.config;

import com.acme.iot.client.actor.HttpClientActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfiguration {

    @Bean
    public ActorSystem actorSystem(HttpClientActorFactory actorFactory) {
        return new DefaultActorSystem(actorFactory);
    }
}
