/**
 * IoT City Dashboard - Client-Centric view for monitoring climate data
 * 
 * This dashboard represents the view of a single ClientActor.
 * It creates a ClientActor on mount, connects to WebSocket, and displays
 * the climate data received by that actor.
 * 
 * Maintains history of climate reports per city for the current session.
 */

import { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ThemeToggle } from '@/components/theme-toggle';
import { ClimateChart } from './ClimateChart';
import { useClimateUpdates } from './useClimateUpdates';
import { useClientSession } from './ClientSessionContext';
import iotCityApi from './iotCityApi';
import type { VilleInfo, ClimateReport } from './types';

export function IotCityDashboard() {
  const { session, isInitializing, error: sessionError } = useClientSession();
  const { climateReport, villeInfoUpdate, getHistoryForCity, isConnected, connectionError, reconnect } = useClimateUpdates();
  
  const [cities, setCities] = useState<VilleInfo[]>([]);
  const [selectedCity, setSelectedCity] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Get history for selected city
  const cityHistory = useMemo(() => {
    if (!selectedCity) return [];
    return getHistoryForCity(selectedCity);
  }, [selectedCity, getHistoryForCity, climateReport]); // climateReport dependency to update when new reports arrive

  // Get latest report for selected city from history (or current climateReport if it matches)
  const latestReport = useMemo((): ClimateReport | null => {
    if (!selectedCity) return null;
    // Use history if available (more recent data at index 0)
    if (cityHistory.length > 0) {
      return cityHistory[0];
    }
    // Fallback to current climateReport if it matches
    if (climateReport && climateReport.villeId === selectedCity) {
      return climateReport;
    }
    return null;
  }, [selectedCity, cityHistory, climateReport]);

  // Load cities when WebSocket is connected (only once)
  useEffect(() => {
    if (session?.actorId && isConnected && cities.length === 0) {
      console.log('[Dashboard] WebSocket connected - loading cities...');
      loadCities();
    }
  }, [session?.actorId, isConnected, cities.length]);

  // Handle ville info updates from WebSocket
  useEffect(() => {
    if (villeInfoUpdate) {
      console.log('[Dashboard] VilleInfoUpdate received:', villeInfoUpdate);
      setCities(prevCities => {
        // Check for duplicates by villeId AND by name (for robustness)
        const existingByVilleId = prevCities.findIndex(c => c.villeId === villeInfoUpdate.villeId);
        const existingByName = prevCities.findIndex(c => c.name === villeInfoUpdate.name);
        
        if (existingByVilleId >= 0) {
          // Update existing city by villeId
          const newCities = [...prevCities];
          newCities[existingByVilleId] = villeInfoUpdate;
          console.log('[Dashboard] Updated city by villeId:', villeInfoUpdate.name);
          return newCities;
        } else if (existingByName >= 0) {
          // Update existing city by name (villeId might differ between basic and detailed)
          const newCities = [...prevCities];
          newCities[existingByName] = villeInfoUpdate;
          console.log('[Dashboard] Updated city by name:', villeInfoUpdate.name);
          return newCities;
        } else {
          // Only add if truly new (check name doesn't already exist)
          console.log('[Dashboard] Adding new city:', villeInfoUpdate.name);
          return [...prevCities, villeInfoUpdate];
        }
      });
    }
  }, [villeInfoUpdate]);

  const loadCities = async () => {
    if (!session?.actorId) return;
    
    try {
      setLoading(true);
      setError(null);
      
      // Get list of city IDs from saf-control
      const cityIds = await iotCityApi.listCities();
      console.log('[Dashboard] Found cities:', cityIds);
      
      // Create basic VilleInfo objects for now
      // Full details will come via WebSocket from RequestVilleInfo responses
      const basicCities: VilleInfo[] = cityIds.map(({ villeId }) => ({
        villeId,
        name: `City ${villeId.substring(0, 8)}`,
        status: 'ACTIVE' as const,
        capteurCount: 0,
        registeredClients: 0,
      }));
      
      setCities(basicCities);
      console.log('[Dashboard] Set cities:', basicCities);
      
      // Request detailed info for each city via actor messaging
      console.log('[Dashboard] Requesting VilleInfo for each city...');
      for (const { villeId } of cityIds) {
        try {
          await iotCityApi.requestCityInfo(session.actorId, villeId);
          console.log(`[Dashboard] RequestVilleInfo sent for ${villeId}`);
        } catch (err) {
          console.warn(`Failed to request info for city ${villeId}:`, err);
        }
      }
      
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load cities');
      console.error('Failed to load cities:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEnterCity = async (villeId: string) => {
    if (!session?.actorId) return;
    
    try {
      await iotCityApi.enterCity(session.actorId, villeId);
      setSelectedCity(villeId);
      console.log('[Dashboard] Entered city:', villeId);
    } catch (err) {
      console.error('Failed to enter city:', err);
      setError(err instanceof Error ? err.message : 'Failed to enter city');
    }
  };

  const handleLeaveCity = async () => {
    if (!session?.actorId || !selectedCity) return;
    
    try {
      await iotCityApi.leaveCity(session.actorId);
      setSelectedCity(null);
      console.log('[Dashboard] Left city');
    } catch (err) {
      console.error('Failed to leave city:', err);
    }
  };

  // Loading state while initializing session
  if (isInitializing) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Initializing session...</p>
          <p className="text-sm text-muted-foreground mt-2">Creating your ClientActor...</p>
        </div>
      </div>
    );
  }

  // Error state if session failed
  if (sessionError && !session) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Card className="max-w-md">
          <CardHeader>
            <CardTitle className="text-destructive">Session Error</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground mb-4">{sessionError}</p>
            <Button onClick={reconnect}>Retry Connection</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 max-w-7xl">
      <div className="mb-8 flex justify-between items-start">
        <div>
          <h1 className="text-4xl font-bold mb-2">IoT City Monitor</h1>
          <p className="text-muted-foreground">Real-time climate monitoring across cities</p>
          {session && (
            <p className="text-xs text-muted-foreground mt-1">
              Session: {session.sessionId.substring(0, 8)}... | Actor: {session.actorId.substring(0, 8)}...
            </p>
          )}
        </div>
        <ThemeToggle />
      </div>

      {error && (
        <Card className="mb-6 border-destructive">
          <CardContent className="pt-6">
            <p className="text-destructive">{error}</p>
            <Button onClick={loadCities} className="mt-4" variant="outline">
              Retry
            </Button>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* City Selection Panel */}
        <div className="lg:col-span-1">
          <Card>
            <CardHeader>
              <CardTitle>Available Cities</CardTitle>
              <CardDescription>Select a city to monitor</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex items-center justify-center py-4">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary"></div>
                </div>
              ) : cities.length === 0 ? (
                <div className="text-center py-4">
                  <p className="text-muted-foreground text-sm">No cities found</p>
                  <Button onClick={loadCities} className="mt-2" variant="outline" size="sm">
                    Refresh
                  </Button>
                </div>
              ) : (
                <div className="space-y-2">
                  {cities.map((city) => {
                    const historyCount = getHistoryForCity(city.villeId).length;
                    const isCurrentCity = selectedCity === city.villeId;
                    const isInAnyCity = selectedCity !== null;
                    return (
                      <Button
                        key={city.villeId}
                        variant={isCurrentCity ? 'default' : 'outline'}
                        className="w-full justify-between"
                        onClick={() => handleEnterCity(city.villeId)}
                        disabled={isInAnyCity}
                      >
                        <span>{city.name}</span>
                        <div className="flex items-center gap-2">
                          {historyCount > 0 && (
                            <Badge variant="secondary" className="text-xs">
                              {historyCount} reports
                            </Badge>
                          )}
                          <Badge variant={city.status === 'ACTIVE' ? 'default' : 'secondary'}>
                            {city.status}
                          </Badge>
                        </div>
                      </Button>
                    );
                  })}
                </div>
              )}
              {selectedCity && (
                <Button
                  onClick={handleLeaveCity}
                  variant="ghost"
                  className="w-full mt-4"
                >
                  Leave Current City
                </Button>
              )}
            </CardContent>
          </Card>

          {/* Connection Status */}
          <Card className="mt-4">
            <CardHeader>
              <CardTitle className="text-sm">Connection Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between">
                <Badge variant={isConnected ? 'default' : 'secondary'}>
                  {isConnected ? 'Connected' : 'Disconnected'}
                </Badge>
                {!isConnected && (
                  <Button size="sm" variant="outline" onClick={reconnect}>
                    Reconnect
                  </Button>
                )}
              </div>
              {connectionError && (
                <p className="text-sm text-destructive mt-2">{connectionError}</p>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Climate Dashboard */}
        <div className="lg:col-span-2">
          {!selectedCity ? (
            <Card>
              <CardContent className="pt-6 text-center py-12">
                <p className="text-muted-foreground text-lg">
                  Select a city to view real-time climate data
                </p>
              </CardContent>
            </Card>
          ) : !latestReport ? (
            <Card>
              <CardContent className="pt-6 text-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
                <p className="text-muted-foreground">Waiting for climate data...</p>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-4">
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>{latestReport.villeName}</CardTitle>
                      <CardDescription>
                        Last updated: {new Date(latestReport.timestampMillis).toLocaleTimeString()}
                      </CardDescription>
                    </div>
                    <Badge>{latestReport.activeCapteurs} sensors active</Badge>
                  </div>
                </CardHeader>
              </Card>

              {/* Climate Metrics */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {Object.entries(latestReport.aggregatedData).map(([type, value]) => (
                  <Card key={type}>
                    <CardHeader>
                      <CardTitle className="text-sm font-medium">
                        {type === 'TEMPERATURE' && 'Temperature'}
                        {type === 'PRESSURE' && 'Pressure'}
                        {type === 'HUMIDITY' && 'Humidity'}
                        {!['TEMPERATURE', 'PRESSURE', 'HUMIDITY'].includes(type) && type}
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-3xl font-bold">
                        {value.toFixed(2)}
                        {type === 'TEMPERATURE' && '°C'}
                        {type === 'PRESSURE' && ' hPa'}
                        {type === 'HUMIDITY' && '%'}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>

              {/* Live Indicator */}
              <Card>
                <CardContent className="pt-6">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
                    <span className="text-sm text-muted-foreground">
                      Live updates - Receiving climate reports from {latestReport.villeName}
                    </span>
                  </div>
                </CardContent>
              </Card>

              {/* Time Series Chart */}
              {cityHistory.length > 1 && (
                <Card>
                  <CardHeader>
                    <CardTitle>Climate Time Series</CardTitle>
                    <CardDescription>
                      {cityHistory.length} data points collected during this session
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ClimateChart data={cityHistory} />
                  </CardContent>
                </Card>
              )}

              {/* History Panel */}
              {cityHistory.length > 1 && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-sm">Report History</CardTitle>
                    <CardDescription>
                      Last 20 reports received
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ScrollArea className="h-48">
                      <div className="space-y-2">
                        {cityHistory.slice(0, 20).map((report, index) => (
                          <div
                            key={`${report.villeId}-${report.timestampMillis}`}
                            className={`p-2 rounded text-sm ${index === 0 ? 'bg-primary/10 border border-primary/20' : 'bg-muted'}`}
                          >
                            <div className="flex justify-between items-center mb-1">
                              <span className="font-medium">
                                {new Date(report.timestampMillis).toLocaleTimeString()}
                              </span>
                              {index === 0 && (
                                <Badge variant="outline" className="text-xs">Latest</Badge>
                              )}
                            </div>
                            <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground">
                              {report.aggregatedData.TEMPERATURE !== undefined && (
                                <span>T: {report.aggregatedData.TEMPERATURE.toFixed(1)} C</span>
                              )}
                              {report.aggregatedData.HUMIDITY !== undefined && (
                                <span>H: {report.aggregatedData.HUMIDITY.toFixed(1)}%</span>
                              )}
                              {report.aggregatedData.PRESSURE !== undefined && (
                                <span>P: {report.aggregatedData.PRESSURE.toFixed(0)} hPa</span>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </ScrollArea>
                  </CardContent>
                </Card>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
