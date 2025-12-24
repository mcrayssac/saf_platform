/**
 * IoT City API - City-specific operations through saf-control
 * Frontend ONLY communicates with saf-control, never directly with runtimes
 */

import actorApi from '../agents/actorApi';
import type { VilleInfo } from './types';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1';
const API_KEY = import.meta.env.VITE_API_KEY || 'test';

/**
 * Make authenticated request to saf-control
 */
async function authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const headers = {
    'Content-Type': 'application/json',
    'X-API-KEY': API_KEY,
    ...options.headers,
  };

  return fetch(url, {
    ...options,
    headers,
  });
}

export const iotCityApi = {
  /**
   * List all available cities (VilleActors in ville-service)
   */
  listCities: async (): Promise<VilleInfo[]> => {
    // Get all actors from ville-service
    const response = await authenticatedFetch(`${API_BASE}/actors/by-service/ville-service`);
    if (!response.ok) {
      throw new Error(`Failed to list cities: ${response.statusText}`);
    }
    const actors = await response.json();
    
    // Transform actor list to VilleInfo format
    return actors.map((actor: any) => ({
      villeId: actor.actorId,
      name: actor.properties?.name || 'Unknown',
      country: actor.properties?.country || 'Unknown',
      targetTemperature: actor.properties?.targetTemperature || 20.0,
      targetHumidity: actor.properties?.targetHumidity || 50.0,
      ...actor.properties,
    }));
  },

  /**
   * Get city information by ID (through saf-control)
   */
  getCity: async (villeId: string): Promise<VilleInfo> => {
    const response = await authenticatedFetch(`${API_BASE}/actors/${villeId}`);
    if (!response.ok) {
      throw new Error(`Failed to get city: ${response.statusText}`);
    }
    const actor = await response.json();
    
    return {
      villeId: actor.actorId,
      name: actor.properties?.name || 'Unknown',
      country: actor.properties?.country || 'Unknown',
      targetTemperature: actor.properties?.targetTemperature || 20.0,
      targetHumidity: actor.properties?.targetHumidity || 50.0,
      ...actor.properties,
    };
  },

  /**
   * Enter a city (CLIENT actor sends RegisterClient message to VILLE actor)
   * This subscribes the client to receive climate reports via WebSocket
   */
  enterCity: async (villeId: string): Promise<void> => {
    await actorApi.sendMessageToClient({
      type: 'ENTER_VILLE',
      content: { villeId },
    });
  },

  /**
   * Leave current city (CLIENT actor sends UnregisterClient message)
   */
  leaveCity: async (): Promise<void> => {
    await actorApi.sendMessageToClient({
      type: 'LEAVE_VILLE',
      content: {},
    });
  },

  /**
   * Get WebSocket URL for current CLIENT actor
   * WebSocket connection goes through client-service (routed via saf-control)
   */
  getWebSocketUrl: (): string => {
    const actorId = actorApi.getCurrentActorId();
    if (!actorId) {
      throw new Error('No active actor session');
    }
    // WebSocket connection to client-service via saf-control
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsHost = API_BASE.replace('http://', '').replace('https://', '').replace('/api/v1', '');
    return `${wsProtocol}//${wsHost}/ws/actors/${actorId}`;
  },
};

export default iotCityApi;
