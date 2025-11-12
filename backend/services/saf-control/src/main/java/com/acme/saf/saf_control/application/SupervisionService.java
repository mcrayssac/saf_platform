package com.acme.saf.saf_control.application;

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
     * Applique la politique de supervision à un agent inactif
     */
    public void handle(AgentView agent) {
        SupervisionPolicy policy = agent.policy();

        if (policy == null) {
            System.out.println("⚠️ Aucune politique définie pour l'agent " + agent.id());
            return;
        }

        switch (policy) {
            case RESTART -> {
                System.out.println("🔁 Politique: RESTART → redémarrage de l'agent " + agent.id());
                controlService.destroy(agent.id());
                // En vrai projet : re-spawn avec les mêmes paramètres (à implémenter)
            }

            case STOP -> {
                System.out.println("⛔ Politique: STOP → arrêt de l'agent " + agent.id());
                controlService.destroy(agent.id());
            }

            case QUARANTINE -> {
                System.out.println("🧪 Politique: QUARANTINE → mise en quarantaine de l'agent " + agent.id());
                // À adapter : ici on ne détruit pas, on pourrait stocker l'agent ailleurs
                AgentView quarantined = new AgentView(
                        agent.id(),
                        agent.type(),
                        "quarantined",
                        agent.runtimeNode(),
                        agent.host(),
                        agent.port(),
                        AgentStatus.QUARANTINED,
                        agent.lastHeartbeat(),
                        agent.policy()
                );

                controlService.update(quarantined);
            }

            case RESUME -> {
                System.out.println("🔄 Politique: RESUME → aucune action sur l'agent " + agent.id());
            }
        }
    }
}
