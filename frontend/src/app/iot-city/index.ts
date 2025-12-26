/**
 * IoT City module exports
 * 
 * This module provides the client-centric dashboard for monitoring
 * climate data across smart cities.
 */

export { IotCityDashboard } from './IotCityDashboard';
export { ClientSessionProvider, useClientSession } from './ClientSessionContext';
export { useClimateUpdates } from './useClimateUpdates';
export { iotCityApi } from './iotCityApi';
export type * from './types';
