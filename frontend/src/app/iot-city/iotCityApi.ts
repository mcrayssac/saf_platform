/**
 * IoT City API - City-specific operations through saf-control
 * Frontend ONLY communicates with saf-control, never directly with runtimes
 */

import actorApi from '../agents/actorApi';

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

/**
 * Helper to send a TellActorCommand with proper format
 * Creates a complete SimpleMessage object that Jackson can deserialize
 */
async function sendActorCommand(targetActorId: string, payload: any): Promise<void> {
  const response = await authenticatedFetch(`${API_BASE}/actors/${targetActorId}/tell`, {
    method: 'POST',
    body: JSON.stringify({
      targetActorId: targetActorId,
      senderActorId: null,
      message: {
        // SimpleMessage fields with @type for Jackson polymorphism
        '@type': 'SimpleMessage',
        messageId: crypto.randomUUID(),
        timestamp: new Date().toISOString(),
        correlationId: null,
        payload: payload
      }
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to send actor command: ${response.statusText} - ${errorText}`);
  }
}

export const iotCityApi = {
  /**
   * List all available cities (VilleActors in ville-service)
   * Returns basic list with actor IDs only
   */
  listCities: async (): Promise<{ villeId: string }[]> => {
    // Get all actors from ville-service
    const response = await authenticatedFetch(`${API_BASE}/actors/by-service/ville-service`);
    if (!response.ok) {
      throw new Error(`Failed to list cities: ${response.statusText}`);
    }
    const actors = await response.json();
    
    // Return simple list of ville IDs
    return actors.map((actor: any) => ({
      villeId: actor.actorId,
    }));
  },

  /**
   * Request city information via actor messaging
   * Sends a message to ClientActor which will request info from VilleActor
   * Response will come back via WebSocket
   */
  requestCityInfo: async (villeId: string): Promise<void> => {
    const actorId = actorApi.getCurrentActorId();
    if (!actorId) {
      throw new Error('No active client actor');
    }

    // Send as Map payload with "command" key
    await sendActorCommand(actorId, {
      command: `GET_VILLE_INFO:${villeId}`
    });
  },

  /**
   * Enter a city (CLIENT actor sends RegisterClient message to VILLE actor)
   * This subscribes the client to receive climate reports via WebSocket
   */
  enterCity: async (villeId: string): Promise<void> => {
    const actorId = actorApi.getCurrentActorId();
    if (!actorId) {
      throw new Error('No active client actor');
    }

    // Send as Map payload with "command" key
    await sendActorCommand(actorId, {
      command: `ENTER:${villeId}`
    });
  },

  /**
   * Leave current city (CLIENT actor sends UnregisterClient message)
   */
  leaveCity: async (): Promise<void> => {
    const actorId = actorApi.getCurrentActorId();
    if (!actorId) {
      throw new Error('No active client actor');
    }

    // Send as Map payload with "command" key
    await sendActorCommand(actorId, {
      command: 'LEAVE'
    });
  },

  /**
   * Get WebSocket URL for current CLIENT actor
   * Uses the direct WebSocket URL provided by the backend during actor creation
   * Translates Docker internal URLs to localhost URLs when accessed from browser
   */
  getWebSocketUrl: (): string => {
    const websocketUrl = actorApi.getCurrentWebSocketUrl();
    if (!websocketUrl) {
      throw new Error('No WebSocket URL available. Initialize session first.');
    }
    
    // Translate Docker internal URLs to localhost for browser access
    // ws://client-service:8084 -> ws://localhost:8084
    // ws://ville-service:8082 -> ws://localhost:8082
    // ws://capteur-service:8083 -> ws://localhost:8083
    return websocketUrl
      .replace('ws://client-service:', 'ws://localhost:')
      .replace('ws://ville-service:', 'ws://localhost:')
      .replace('ws://capteur-service:', 'ws://localhost:');
  },
};

export default iotCityApi;
