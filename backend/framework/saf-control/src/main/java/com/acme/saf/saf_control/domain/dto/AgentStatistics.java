package com.acme.saf.saf_control.domain.dto;

/**
 * Représente les statistiques globales sur les agents supervisés.
 *
 * @param totalAgents    Nombre total d'agents enregistrés.
 * @param activeAgents   Nombre d'agents considérés comme actifs (heartbeat < timeout).
 * @param inactiveAgents Nombre d'agents inactifs.
 * @param agentsByType   Nombre de types d'agents distincts.
 */

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public record AgentStatistics(
        long totalAgents,
        long activeAgents,
        long inactiveAgents,
        Map<String, Long> agentsByType
) {

    @Override
    public String toString() {
        return "AgentStatistics{" +
                "totalAgents=" + totalAgents +
                ", activeAgents=" + activeAgents +
                ", inactiveAgents=" + inactiveAgents +
                ", agentsByType=" + agentsByType +
                '}';
    }

    /**
     * Vérifie si tous les agents sont actifs.
     */
    public boolean allAgentsActive() {
        return totalAgents == activeAgents;
    }

    /**
     * Renvoie vrai si aucun agent n’est inactif.
     */
    public boolean hasNoInactiveAgents() {
        return inactiveAgents == 0;
    }
}
