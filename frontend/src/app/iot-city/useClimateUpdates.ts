/**
 * React hook for managing WebSocket climate updates
 * Uses the ClientSessionContext for session management
 * Maintains history of climate reports per city
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import type { ClimateReport, VilleInfo } from './types';
import { useClientSession } from './ClientSessionContext';

// Maximum number of reports to keep per city
const MAX_HISTORY_PER_CITY = 50;

// Map of villeId -> ClimateReport[] (history)
export type ClimateHistoryMap = Record<string, ClimateReport[]>;

interface UseClimateUpdatesResult {
  climateReport: ClimateReport | null;
  villeInfoUpdate: VilleInfo | null;
  climateHistory: ClimateHistoryMap;
  getHistoryForCity: (villeId: string) => ClimateReport[];
  isConnected: boolean;
  connectionError: string | null;
  reconnect: () => void;
}

export function useClimateUpdates(): UseClimateUpdatesResult {
  const { session, isInitializing, error: sessionError, reconnect: reconnectSession } = useClientSession();
  
  const [climateReport, setClimateReport] = useState<ClimateReport | null>(null);
  const [villeInfoUpdate, setVilleInfoUpdate] = useState<VilleInfo | null>(null);
  const [climateHistory, setClimateHistory] = useState<ClimateHistoryMap>({});
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const maxReconnectAttempts = 5;

  // Get history for a specific city
  const getHistoryForCity = useCallback((villeId: string): ClimateReport[] => {
    return climateHistory[villeId] || [];
  }, [climateHistory]);

  // Add a report to history
  const addToHistory = useCallback((report: ClimateReport) => {
    setClimateHistory(prev => {
      const villeId = report.villeId;
      const existingHistory = prev[villeId] || [];
      
      // Add new report to the front of the array
      const newHistory = [report, ...existingHistory];
      
      // Trim to max size
      if (newHistory.length > MAX_HISTORY_PER_CITY) {
        newHistory.length = MAX_HISTORY_PER_CITY;
      }
      
      return {
        ...prev,
        [villeId]: newHistory
      };
    });
  }, []);

  const connect = useCallback(() => {
    if (!session?.websocketUrl) {
      console.log('[WebSocket] No WebSocket URL available yet');
      return;
    }

    try {
      const wsUrl = session.websocketUrl;
      console.log('[WebSocket] Connecting to WebSocket:', wsUrl);

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('[WebSocket] Connected');
        setIsConnected(true);
        setConnectionError(null);
        reconnectAttemptsRef.current = 0;
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('[WebSocket] Message received:', data);
          
          // Check if it's a ClimateReport (has aggregatedData)
          if (data.villeId && data.aggregatedData) {
            console.log('[WebSocket] Climate report received:', data.villeName);
            const report = data as ClimateReport;
            setClimateReport(report);
            // Add to history
            addToHistory(report);
          }
          // Check if it's a VilleInfo (has climateConfig)
          else if (data.villeId && data.climateConfig !== undefined) {
            console.log('[WebSocket] Ville info received:', data.name);
            setVilleInfoUpdate(data as VilleInfo);
          }
          // Handle other message types
          else {
            console.log('[WebSocket] Other message received:', data);
          }
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      ws.onerror = (error) => {
        console.error('[WebSocket] Error:', error);
        setConnectionError('WebSocket connection error');
      };

      ws.onclose = (event) => {
        console.log('[WebSocket] Disconnected', event.code, event.reason);
        setIsConnected(false);

        // Attempt reconnection if not manually closed
        if (event.code !== 1000 && reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current++;
          const delay = 2000 * reconnectAttemptsRef.current;
          console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${reconnectAttemptsRef.current}/${maxReconnectAttempts})...`);
          
          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, delay);
        } else if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
          setConnectionError('Max reconnection attempts reached');
        }
      };
    } catch (error) {
      console.error('Failed to create WebSocket:', error);
      setConnectionError(error instanceof Error ? error.message : 'Unknown error');
    }
  }, [session?.websocketUrl, addToHistory]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (wsRef.current) {
      wsRef.current.close(1000, 'Component unmounting');
      wsRef.current = null;
    }

    setIsConnected(false);
  }, []);

  const reconnect = useCallback(() => {
    disconnect();
    reconnectAttemptsRef.current = 0;
    setConnectionError(null);
    
    // If session is available, reconnect WebSocket
    if (session?.websocketUrl) {
      connect();
    } else {
      // Otherwise, reconnect the whole session
      reconnectSession();
    }
  }, [connect, disconnect, session?.websocketUrl, reconnectSession]);

  // Connect when session becomes available
  useEffect(() => {
    if (session?.websocketUrl && !isInitializing) {
      connect();
    }
    return () => {
      disconnect();
    };
  }, [session?.websocketUrl, isInitializing, connect, disconnect]);

  // Forward session error to connection error
  useEffect(() => {
    if (sessionError) {
      setConnectionError(sessionError);
    }
  }, [sessionError]);

  return {
    climateReport,
    villeInfoUpdate,
    climateHistory,
    getHistoryForCity,
    isConnected,
    connectionError,
    reconnect,
  };
}
