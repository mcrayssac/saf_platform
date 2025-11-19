package com.acme.saf.actor.core;


public interface SupervisionStrategy {
    SupervisionDirective handleFailure(Actor actor, Throwable cause, Message message);
}