package com.acme.saf.saf_control.infrastructure.events;

/**
 * Événement simple provenant du Runtime (mock pour l'instant).
 * Permet de transporter :
 * - le type d'événement (ActorStarted, ActorFailed...)
 * - l'identifiant de l'agent concerné
 */
public record RuntimeEvent(
        String type,
        String agentId
) {}
