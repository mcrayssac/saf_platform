package com.acme.saf.saf_control.application;

import com.acme.saf.saf_control.domain.dto.AgentCreateRequest;
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
     * Politique RESTART : Redémarrage complet de l'agent.
     *
     * PROCESSUS :
     * 1. Détruit l'acteur existant (avec son état corrompu)
     * 2. Crée un nouvel acteur du même type
     * 3. Le nouvel agent repart avec un état vierge
     * @param agent Agent à redémarrer
     */
    public void restart(AgentView agent) {
        System.out.println("Redémarrage de l'agent " + agent.id() + " (" + agent.type() + ")");

        // 1. Destruction de l'agent défaillant
        controlService.destroy(agent.id());

        // 2. Re-création avec le même type et la même policy
        controlService.spawn(new AgentCreateRequest(
                agent.type(),
                agent.host(),
                agent.port(),
                agent.policy()  // On conserve la même politique
        ));

        System.out.println("Nouvel agent créé pour remplacer " + agent.id());
    }

    /**
     * Politique RESUME : Ignore l'erreur et laisse l'agent continuer.
     *
     * PROCESSUS :
     * - Aucune action corrective
     * - L'agent reste dans son état actuel
     * - Utile pour les erreurs transitoires qui se résolvent d'elles-mêmes
     * @param agent Agent à laisser continuer
     */
    public void resume(AgentView agent) {
        System.out.println("Reprise de l'agent " + agent.id() + " sans intervention");
        // Aucune action : l'agent continue son exécution normale
    }

    /**
     * Politique STOP : Arrêt définitif de l'agent.
     *
     * PROCESSUS :
     * - Détruit l'agent sans le recréer
     * - L'agent disparaît du système
     * - Nécessite une intervention manuelle pour le relancer
     * @param agent Agent à arrêter définitivement
     */
    public void stop(AgentView agent) {
        System.out.println("Arrêt définitif de l'agent " + agent.id());
        controlService.destroy(agent.id());
        System.out.println("L'agent ne sera pas relancé automatiquement");
    }

    /**
     * @param agent Agent à superviser
     */
    public void handle(AgentView agent) {
        System.out.println("Supervision de l'agent " + agent.id() + " avec politique " + agent.policy());

        switch (agent.policy()) {
            case RESTART -> restart(agent);
            case RESUME  -> resume(agent);
            case STOP    -> stop(agent);
            default -> {
                System.err.println("Politique inconnue : " + agent.policy());
                // Par défaut, on ne fait rien (équivalent à RESUME)
            }
        }
    }
}