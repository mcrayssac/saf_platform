/**
 * ClientSessionContext - Manages the lifecycle of the ClientActor session
 * 
 * This context handles:
 * - Creating a ClientActor when the app starts
 * - Connecting to WebSocket for real-time updates
 * - Cleaning up the ClientActor when the app closes
 */

import { createContext, useContext, useEffect, useState, useCallback, useRef, type ReactNode } from 'react';

const API_BASE = '/api/v1';
const API_KEY = import.meta.env.VITE_API_KEY || 'test';

interface ClientSession {
  sessionId: string;
  actorId: string;
  websocketUrl: string;
}

interface ClientSessionContextType {
  session: ClientSession | null;
  isInitializing: boolean;
  isConnected: boolean;
  error: string | null;
  reconnect: () => void;
}

const ClientSessionContext = createContext<ClientSessionContextType | null>(null);

export function useClientSession() {
  const context = useContext(ClientSessionContext);
  if (!context) {
    throw new Error('useClientSession must be used within a ClientSessionProvider');
  }
  return context;
}

interface ClientSessionProviderProps {
  children: ReactNode;
}

export function ClientSessionProvider({ children }: ClientSessionProviderProps) {
  const [session, setSession] = useState<ClientSession | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const initializingRef = useRef(false);

  // Initialize session on mount
  const initializeSession = useCallback(async () => {
    if (initializingRef.current) return;
    initializingRef.current = true;

    try {
      setIsInitializing(true);
      setError(null);

      // Check for existing session in localStorage
      const storedSession = localStorage.getItem('saf_session');
      const storedActorId = localStorage.getItem('saf_actor_id');
      const storedWebsocketUrl = localStorage.getItem('saf_websocket_url');

      if (storedSession && storedActorId && storedWebsocketUrl) {
        // Verify the session is still valid by checking if actor exists
        try {
          const response = await fetch(`${API_BASE}/actors/${storedActorId}`, {
            headers: { 'X-API-KEY': API_KEY }
          });
          
          if (response.ok) {
            console.log('[Session] Restored existing session:', storedActorId);
            setSession({
              sessionId: storedSession,
              actorId: storedActorId,
              websocketUrl: storedWebsocketUrl
            });
            setIsConnected(true);
            setIsInitializing(false);
            initializingRef.current = false;
            return;
          }
        } catch {
          console.log('[Session] Stored session invalid, creating new one');
        }
        
        // Clear invalid session
        localStorage.removeItem('saf_session');
        localStorage.removeItem('saf_actor_id');
        localStorage.removeItem('saf_websocket_url');
      }

      // Create new session
      const sessionId = crypto.randomUUID();
      console.log('[Session] Creating new ClientActor session:', sessionId);

      const response = await fetch(`${API_BASE}/actors`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-KEY': API_KEY
        },
        body: JSON.stringify({
          serviceId: 'client-service',
          actorType: 'ClientActor',
          params: {
            sessionId,
            name: `Browser Session ${sessionId.substring(0, 8)}`
          }
        })
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(`Failed to create ClientActor: ${response.status} ${text}`);
      }

      const data = await response.json();
      console.log('[Session] ClientActor created:', data);

      // Transform websocket URL for browser access
      // Docker ports are mapped 1:1 (internal:external):
      // - client-service: 8084:8084
      // - ville-service: 8085:8085  
      // - capteur-service: 8086:8086
      let websocketUrl = data.websocketUrl || '';
      websocketUrl = websocketUrl
        .replace('ws://client-service:', 'ws://localhost:')
        .replace('ws://ville-service:', 'ws://localhost:')
        .replace('ws://capteur-service:', 'ws://localhost:');

      // Store session
      localStorage.setItem('saf_session', sessionId);
      localStorage.setItem('saf_actor_id', data.actorId);
      localStorage.setItem('saf_websocket_url', websocketUrl);

      setSession({
        sessionId,
        actorId: data.actorId,
        websocketUrl
      });
      setIsConnected(true);

    } catch (err) {
      console.error('[Session] Failed to initialize session:', err);
      setError(err instanceof Error ? err.message : 'Failed to initialize session');
    } finally {
      setIsInitializing(false);
      initializingRef.current = false;
    }
  }, []);

  // Cleanup session on unmount
  const cleanupSession = useCallback(async () => {
    const actorId = localStorage.getItem('saf_actor_id');
    if (!actorId) return;

    try {
      console.log('[Session] Cleaning up ClientActor:', actorId);
      await fetch(`${API_BASE}/actors/${actorId}`, {
        method: 'DELETE',
        headers: { 'X-API-KEY': API_KEY }
      });
    } catch (err) {
      console.error('Failed to cleanup session:', err);
    }

    localStorage.removeItem('saf_session');
    localStorage.removeItem('saf_actor_id');
    localStorage.removeItem('saf_websocket_url');
  }, []);

  // Initialize on mount
  useEffect(() => {
    initializeSession();

    // Cleanup on window close
    const handleBeforeUnload = () => {
      cleanupSession();
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [initializeSession, cleanupSession]);

  const reconnect = useCallback(() => {
    // Clear current session and reinitialize
    localStorage.removeItem('saf_session');
    localStorage.removeItem('saf_actor_id');
    localStorage.removeItem('saf_websocket_url');
    setSession(null);
    setIsConnected(false);
    initializingRef.current = false;
    initializeSession();
  }, [initializeSession]);

  return (
    <ClientSessionContext.Provider value={{ session, isInitializing, isConnected, error, reconnect }}>
      {children}
    </ClientSessionContext.Provider>
  );
}
