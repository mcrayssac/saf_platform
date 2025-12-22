import { useEffect, useState } from 'react';
import { AgentsDashboard } from './app/agents/AgentsDashboard';
import actorApi from './app/agents/actorApi';

function App() {
  const [sessionInitialized, setSessionInitialized] = useState(false);
  const [initError, setInitError] = useState<string | null>(null);

  useEffect(() => {
    // Initialize actor session on mount
    const initSession = async () => {
      try {
        console.log('🚀 Initializing actor session...');
        await actorApi.initializeSession();
        setSessionInitialized(true);
        console.log('✅ Actor session ready');
      } catch (error) {
        console.error('❌ Failed to initialize session:', error);
        setInitError(error instanceof Error ? error.message : 'Unknown error');
      }
    };

    initSession();

    // Cleanup session on unmount
    return () => {
      console.log('🧹 Cleaning up actor session...');
      actorApi.cleanupSession().catch(err => {
        console.error('Failed to cleanup session:', err);
      });
    };
  }, []);

  if (initError) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <div className="text-center p-8 max-w-md">
          <h1 className="text-2xl font-bold text-destructive mb-4">
            Session Initialization Failed
          </h1>
          <p className="text-muted-foreground mb-4">{initError}</p>
          <p className="text-sm text-muted-foreground">
            Make sure the backend services are running.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!sessionInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Initializing session...</p>
        </div>
      </div>
    );
  }

  return <AgentsDashboard />;
}

export default App;
