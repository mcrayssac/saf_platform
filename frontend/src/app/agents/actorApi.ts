/**
 * Actor API - New architecture using saf-control proxy
 * Frontend manages its own session and actor lifecycle
 */

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';
const API_KEY = import.meta.env.VITE_API_KEY || 'mock-';

// Session management in localStorage
const SESSION_KEY = 'saf_session';
const ACTOR_ID_KEY = 'saf_actor_id';

interface ActorCreateRequest {
  type: string;
  params: Record<string, any>;
}

interface ActorResponse {
  actorId: string;
  type: string;
  status: string;
}

interface MessageRequest {
  type: string;
  content: Record<string, any>;
}

/**
 * Generate a unique session ID
 */
function generateSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Get or create session ID from localStorage
 */
function getSessionId(): string {
  let sessionId = localStorage.getItem(SESSION_KEY);
  if (!sessionId) {
    sessionId = generateSessionId();
    localStorage.setItem(SESSION_KEY, sessionId);
  }
  return sessionId;
}

/**
 * Get stored actor ID from localStorage
 */
function getActorId(): string | null {
  return localStorage.getItem(ACTOR_ID_KEY);
}

/**
 * Store actor ID in localStorage
 */
function setActorId(actorId: string): void {
  localStorage.setItem(ACTOR_ID_KEY, actorId);
}

/**
 * Clear session and actor ID
 */
function clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(ACTOR_ID_KEY);
}

/**
 * Make authenticated request to saf-control
 */
async function authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const headers = {
    'Content-Type': 'application/json',
    'X-API-KEY': API_KEY,
    ...options.headers,
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  return response;
}

export const actorApi = {
  /**
   * Initialize: Create CLIENT actor for this session
   * Call this when the app loads
   */
  initializeSession: async (): Promise<ActorResponse> => {
    const sessionId = getSessionId();
    
    const request: ActorCreateRequest = {
      type: 'CLIENT',
      params: {
        sessionId,
      },
    };

    const response = await authenticatedFetch(`${API_BASE}/actors`, {
      method: 'POST',
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to create CLIENT actor: ${response.statusText}`);
    }

    const actorResponse: ActorResponse = await response.json();
    setActorId(actorResponse.actorId);
    
    console.log('✅ Session initialized:', { sessionId, actorId: actorResponse.actorId });
    return actorResponse;
  },

  /**
   * Get current session's actor ID
   */
  getCurrentActorId: (): string | null => {
    return getActorId();
  },

  /**
   * Get current session ID
   */
  getCurrentSessionId: (): string => {
    return getSessionId();
  },

  /**
   * Send a message to an actor
   */
  sendMessage: async (actorId: string, message: MessageRequest): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE}/actors/${actorId}/messages`, {
      method: 'POST',
      body: JSON.stringify(message),
    });

    if (!response.ok) {
      throw new Error(`Failed to send message: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Send message to current session's CLIENT actor
   */
  sendMessageToClient: async (message: MessageRequest): Promise<any> => {
    const actorId = getActorId();
    if (!actorId) {
      throw new Error('No active actor session. Call initializeSession first.');
    }
    return actorApi.sendMessage(actorId, message);
  },

  /**
   * Query cities (via CLIENT actor)
   */
  queryCities: async (): Promise<any> => {
    return actorApi.sendMessageToClient({
      type: 'QUERY_CITIES',
      content: {},
    });
  },

  /**
   * Query sensors for a city (via CLIENT actor)
   */
  querySensors: async (cityId: string): Promise<any> => {
    return actorApi.sendMessageToClient({
      type: 'QUERY_SENSORS',
      content: { cityId },
    });
  },

  /**
   * Get actor information
   */
  getActor: async (actorId: string): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE}/actors/${actorId}`);
    
    if (!response.ok) {
      throw new Error(`Failed to get actor: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Stop/delete an actor
   */
  stopActor: async (actorId: string): Promise<void> => {
    const response = await authenticatedFetch(`${API_BASE}/actors/${actorId}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error(`Failed to stop actor: ${response.statusText}`);
    }
  },

  /**
   * Cleanup: Stop current session's actor
   * Call this on logout or app unmount
   */
  cleanupSession: async (): Promise<void> => {
    const actorId = getActorId();
    if (actorId) {
      try {
        await actorApi.stopActor(actorId);
        console.log('✅ Session cleaned up:', actorId);
      } catch (error) {
        console.error('Failed to cleanup session:', error);
      }
    }
    clearSession();
  },

  /**
   * Health check
   */
  healthCheck: async (): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE}/actors/health`);
    
    if (!response.ok) {
      throw new Error(`Health check failed: ${response.statusText}`);
    }

    return response.json();
  },

  /**
   * Reset session (for development/testing)
   */
  resetSession: (): void => {
    clearSession();
    console.log('🔄 Session reset');
  },
};

export default actorApi;
