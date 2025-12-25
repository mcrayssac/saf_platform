/**
 * React hook for managing WebSocket climate updates
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import type { ClimateReport, VilleInfo } from './types';
import iotCityApi from './iotCityApi';

interface UseClimateUpdatesResult {
  climateReport: ClimateReport | null;
  villeInfoUpdate: VilleInfo | null;
  isConnected: boolean;
  connectionError: string | null;
  reconnect: () => void;
}

export function useClimateUpdates(): UseClimateUpdatesResult {
  const [climateReport, setClimateReport] = useState<ClimateReport | null>(null);
  const [villeInfoUpdate, setVilleInfoUpdate] = useState<VilleInfo | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const maxReconnectAttempts = 5;

  const connect = useCallback(() => {
    try {
      const wsUrl = iotCityApi.getWebSocketUrl();
      console.log('🔌 Connecting to WebSocket:', wsUrl);

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('✅ WebSocket connected');
        setIsConnected(true);
        setConnectionError(null);
        reconnectAttemptsRef.current = 0;
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          
          // Check if it's a ClimateReport (has aggregatedData)
          if (data.villeId && data.aggregatedData) {
            console.log('📊 Climate report received:', data.villeName);
            setClimateReport(data as ClimateReport);
          }
          // Check if it's a VilleInfo (has climateConfig)
          else if (data.villeId && data.climateConfig) {
            console.log('🏙️ Ville info received:', data.name);
            setVilleInfoUpdate(data as VilleInfo);
          }
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      ws.onerror = (error) => {
        console.error('❌ WebSocket error:', error);
        setConnectionError('WebSocket connection error');
      };

      ws.onclose = (event) => {
        console.log('🔌 WebSocket disconnected', event.code, event.reason);
        setIsConnected(false);

        // Attempt reconnection if not manually closed
        if (event.code !== 1000 && reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current++;
          const delay = 2000 * reconnectAttemptsRef.current;
          console.log(`🔄 Reconnecting in ${delay}ms (attempt ${reconnectAttemptsRef.current}/${maxReconnectAttempts})...`);
          
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
  }, []);

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
    connect();
  }, [connect, disconnect]);

  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    climateReport,
    villeInfoUpdate,
    isConnected,
    connectionError,
    reconnect,
  };
}
