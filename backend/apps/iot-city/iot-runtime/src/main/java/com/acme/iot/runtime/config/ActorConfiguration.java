package com.acme.iot.runtime.config;

import com.acme.iot.city.actors.IotActorFactory;
import com.acme.saf.actor.core.ActorFactory;
import com.acme.saf.actor.core.ActorSystem;
import com.acme.saf.saf_runtime.DefaultActorSystem;
import com.acme.saf.saf_runtime.InMemoryMailbox;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorConfiguration {

    @Bean
    public ActorFactory actorFactory() {
        return new IotActorFactory();
    }

    @Bean
    public ActorSystem actorSystem(ActorFactory factory) {
        return new DefaultActorSystem(factory, new InMemoryMailbox());
    }
}
