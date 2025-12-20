import type { Agent, AgentStatistics, AgentCreateRequest } from './types';

const API_BASE = 'http://localhost:8080/agents';

export const agentsApi = {
  /**
   * Liste tous les agents avec leur statut de santé
   */
  getAllAgents: async (): Promise<Agent[]> => {
    const response = await fetch(API_BASE);
    if (!response.ok) throw new Error('Failed to fetch agents');
    return response.json();
  },

  /**
   * Liste uniquement les agents actifs
   */
  getActiveAgents: async (): Promise<Agent[]> => {
    const response = await fetch(`${API_BASE}/active`);
    if (!response.ok) throw new Error('Failed to fetch active agents');
    return response.json();
  },

  /**
   * Filtre les agents par type
   */
  getAgentsByType: async (type: string): Promise<Agent[]> => {
    const response = await fetch(`${API_BASE}/type/${type}`);
    if (!response.ok) throw new Error(`Failed to fetch ${type} agents`);
    return response.json();
  },

  /**
   * Récupère un agent spécifique
   */
  getAgent: async (id: string): Promise<Agent> => {
    const response = await fetch(`${API_BASE}/${id}`);
    if (!response.ok) throw new Error('Agent not found');
    return response.json();
  },

  /**
   * Récupère les statistiques globales
   */
  getStatistics: async (): Promise<AgentStatistics> => {
    const response = await fetch(`${API_BASE}/statistics`);
    if (!response.ok) throw new Error('Failed to fetch statistics');
    return response.json();
  },

  /**
   * Crée un nouvel agent
   */
  createAgent: async (request: AgentCreateRequest): Promise<Agent> => {
    const response = await fetch(API_BASE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!response.ok) throw new Error('Failed to create agent');
    return response.json();
  },

  /**
   * Supprime un agent
   */
  deleteAgent: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete agent');
  },

  /**
   * Envoie un heartbeat
   */
  sendHeartbeat: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/${id}/heartbeat`, {
      method: 'POST',
    });
    if (!response.ok) throw new Error('Failed to send heartbeat');
  },
};