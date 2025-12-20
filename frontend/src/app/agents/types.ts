export type AgentType = 'CLIENT' | 'VILLE' | 'CAPTEUR';
export type AgentStatus = 'ACTIVE' | 'INACTIVE' | 'UNKNOWN';

export interface Agent {
  id: string;
  type: AgentType;
  state: string;
  runtimeNode: string;
  host: string;
  port: number;
  status: AgentStatus;
  lastHeartbeat: string;
}

export interface AgentStatistics {
  totalAgents: number;
  activeAgents: number;
  inactiveAgents: number;
  agentsByType: Record<string, number>;
}

export interface AgentCreateRequest {
  type: string;
  host: string;
  port: number;
}