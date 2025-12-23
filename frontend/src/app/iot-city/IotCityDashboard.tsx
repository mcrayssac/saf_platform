/**
 * IoT City Dashboard - Main UI for monitoring cities and climate data
 */

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useClimateUpdates } from './useClimateUpdates';
import iotCityApi from './iotCityApi';
import type { VilleInfo } from './types';

export function IotCityDashboard() {
  const [cities, setCities] = useState<VilleInfo[]>([]);
  const [selectedCity, setSelectedCity] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const { climateReport, isConnected, connectionError, reconnect } = useClimateUpdates();

  // Load cities on mount
  useEffect(() => {
    loadCities();
  }, []);

  const loadCities = async () => {
    try {
      setLoading(true);
      setError(null);
      const citiesData = await iotCityApi.listCities();
      setCities(citiesData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load cities');
      console.error('Failed to load cities:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEnterCity = async (villeId: string) => {
    try {
      await iotCityApi.enterCity(villeId);
      setSelectedCity(villeId);
      console.log('✅ Entered city:', villeId);
    } catch (err) {
      console.error('Failed to enter city:', err);
      setError(err instanceof Error ? err.message : 'Failed to enter city');
    }
  };

  const handleLeaveCity = async () => {
    try {
      await iotCityApi.leaveCity();
      setSelectedCity(null);
      console.log('✅ Left city');
    } catch (err) {
      console.error('Failed to leave city:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading cities...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6 max-w-7xl">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">IoT City Monitor</h1>
        <p className="text-muted-foreground">Real-time climate monitoring across cities</p>
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
              <div className="space-y-2">
                {cities.map((city) => (
                  <Button
                    key={city.villeId}
                    variant={selectedCity === city.villeId ? 'default' : 'outline'}
                    className="w-full justify-between"
                    onClick={() => handleEnterCity(city.villeId)}
                    disabled={selectedCity === city.villeId}
                  >
                    <span>{city.name}</span>
                    <Badge variant={city.status === 'ACTIVE' ? 'default' : 'secondary'}>
                      {city.status}
                    </Badge>
                  </Button>
                ))}
              </div>
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

          {/* WebSocket Connection Status */}
          <Card className="mt-4">
            <CardHeader>
              <CardTitle className="text-sm">Connection Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between">
                <Badge variant={isConnected ? 'default' : 'secondary'}>
                  {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
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
          ) : !climateReport ? (
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
                      <CardTitle>{climateReport.villeName}</CardTitle>
                      <CardDescription>
                        Last updated: {new Date(climateReport.timestamp).toLocaleTimeString()}
                      </CardDescription>
                    </div>
                    <Badge>{climateReport.activeCapteurs} sensors active</Badge>
                  </div>
                </CardHeader>
              </Card>

              {/* Climate Metrics */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {Object.entries(climateReport.aggregatedData).map(([type, value]) => (
                  <Card key={type}>
                    <CardHeader>
                      <CardTitle className="text-sm font-medium">
                        {type === 'TEMPERATURE' && '🌡️ Temperature'}
                        {type === 'PRESSURE' && '🌀 Pressure'}
                        {type === 'HUMIDITY' && '💧 Humidity'}
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
                      Live updates every 5 seconds
                    </span>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
