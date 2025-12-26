/**
 * IoT City TypeScript types
 * Matches backend domain models
 */

export interface ClimateConfig {
  meanTemperature: number;
  meanPressure: number;
  meanHumidity: number;
  variancePercentage: number;
}

export interface SensorReading {
  sensorType: string;
  value: number;
  timestamp: number;
}

export interface ClimateReport {
  villeId: string;
  villeName: string;
  aggregatedData: Record<string, number>;
  activeCapteurs: number;
  timestampMillis: number;  // Timestamp in milliseconds from backend
}

export interface VilleInfo {
  villeId: string;
  name: string;
  status: string;
  climateConfig?: ClimateConfig;
  capteursCount?: number;
}

export interface CapteurInfo {
  capteurId: string;
  type: string;
  status: string;
  associatedVilleId?: string;
}
