/**
 * SAF Platform - IoT City Application
 * 
 * Main entry point that wraps the application with the ClientSessionProvider.
 * The ClientSessionProvider manages the lifecycle of the ClientActor session.
 */

import { IotCityDashboard } from './app/iot-city/IotCityDashboard';
import { ClientSessionProvider } from './app/iot-city/ClientSessionContext';

function App() {
  return (
    <ClientSessionProvider>
      <IotCityDashboard />
    </ClientSessionProvider>
  );
}

export default App;
