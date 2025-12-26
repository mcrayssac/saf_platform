/**
 * IoT City API - City-specific operations through saf-control
 * Frontend ONLY communicates with saf-control via nginx proxy
 */

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
 * Helper to send a command to the ClientActor
 * Creates a complete SimpleMessage object that Jackson can deserialize
 * 
 * IMPORTANT: The backend uses @JsonTypeInfo with @class property for polymorphism
 * So we need to send the full class name: com.acme.saf.actor.core.SimpleMessage
 */
async function sendActorCommand(actorId: string, payload: Record<string, unknown>): Promise<void> {
  const response = await authenticatedFetch(`${API_BASE}/actors/${actorId}/tell`, {
    method: 'POST',
    body: JSON.stringify({
      targetActorId: actorId,
      senderActorId: null,
      message: {
        '@class': 'com.acme.saf.actor.core.SimpleMessage',
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

/**
 * IoT City API functions that use the current ClientActor session
 */
export const iotCityApi = {
  /**
   * List all available cities (VilleActors in ville-service)
   * Returns basic list with actor IDs only
   */
  listCities: async (): Promise<{ villeId: string }[]> => {
    const response = await authenticatedFetch(`${API_BASE}/actors/by-service/ville-service`);
    if (!response.ok) {
      throw new Error(`Failed to list cities: ${response.statusText}`);
    }
    const actors = await response.json();
    
    return actors.map((actor: { actorId: string }) => ({
      villeId: actor.actorId,
    }));
  },

  /**
   * Request city information via actor messaging
   * Sends a command to ClientActor which will request info from VilleActor
   * Response will come back via WebSocket
   */
  requestCityInfo: async (actorId: string, villeId: string): Promise<void> => {
    if (!actorId) {
      throw new Error('No active client actor');
    }

    await sendActorCommand(actorId, {
      command: `GET_VILLE_INFO:${villeId}`
    });
  },

  /**
   * Enter a city (CLIENT actor sends RegisterClient message to VILLE actor)
   * This subscribes the client to receive climate reports via WebSocket
   */
  enterCity: async (actorId: string, villeId: string): Promise<void> => {
    if (!actorId) {
      throw new Error('No active client actor');
    }

    await sendActorCommand(actorId, {
      command: `ENTER:${villeId}`
    });
  },

  /**
   * Leave current city (CLIENT actor sends UnregisterClient message)
   */
  leaveCity: async (actorId: string): Promise<void> => {
    if (!actorId) {
      throw new Error('No active client actor');
    }

    await sendActorCommand(actorId, {
      command: 'LEAVE'
    });
  },
};

export default iotCityApi;
