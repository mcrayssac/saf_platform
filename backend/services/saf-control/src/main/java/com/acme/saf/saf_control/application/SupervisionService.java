package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.AgentCreateRequest;
import com.acme.saf.saf_control.domain.dto.AgentStatus;
import com.acme.saf.saf_control.domain.dto.AgentView;
import com.acme.saf.saf_control.domain.dto.Agent.SupervisionPolicy;
import org.springframework.stereotype.Service;

@Service
public class SupervisionService {

    private final ControlService controlService;

    public SupervisionService(ControlService controlService) {
        this.controlService = controlService;
    }

    /**
     * Redémarre complètement un agent :
     * 1. destruction de l'acteur existant
     * 2. re-création d'un nouvel acteur du même type et même état
     */
    public void restart(AgentView agent) {
        // 1. Destroy
        controlService.destroy(agent.id());
        // 2. Respawn
        controlService.spawn(new AgentCreateRequest(agent.type()));
    }

    /**
     * Politique RESUME : l'erreur est ignorée,
     * l'acteur reste dans son état actuel, aucune action corrective.
     */
    public void resume(AgentView agent) {
        System.out.println("Resuming agent " + agent.id());
    }

    /**
     * Politique STOP : l'acteur est arrêté définitivement
     * (détruit sans recréation)
     */
    public void stop(AgentView agent) {
        // Immobiliser totalement
        controlService.destroy(agent.id());
    }

    /**
     * Applique la politique de supervision à un agent inactif
     * (appelée lorsqu'un agent est en échec, expiré, ou en quarantaine)
     */
    public void handle(AgentView agent) {
        switch (agent.policy()) {
            case RESTART -> restart(agent);
            case RESUME  -> resume(agent);
            case STOP    -> stop(agent);
        }
    }
}
