import { useEffect, useState } from 'react';
// Note: This file is deprecated. Use IotCityDashboard instead.
// import { agentsApi } from './actorApi';
import type { Agent, AgentStatistics, AgentCreateRequest } from './types';

// Stub to prevent compilation errors (file not currently used)
const agentsApi: any = {};

export function AgentsDashboard() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [stats, setStats] = useState<AgentStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'all' | 'active' | string>('all');
  const [showCreateForm, setShowCreateForm] = useState(false);

  // Chargement des données
  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 5000); // Refresh toutes les 5s
    return () => clearInterval(interval);
  }, [filter]);

  const loadData = async () => {
    try {
      let agentsData: Agent[];
      
      if (filter === 'all') {
        agentsData = await agentsApi.getAllAgents();
      } else if (filter === 'active') {
        agentsData = await agentsApi.getActiveAgents();
      } else {
        agentsData = await agentsApi.getAgentsByType(filter);
      }

      const statsData = await agentsApi.getStatistics();
      
      setAgents(agentsData);
      setStats(statsData);
      setError(null);
    } catch (err) {
      setError('Erreur lors du chargement des agents');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cet agent ?')) return;
    
    try {
      await agentsApi.deleteAgent(id);
      await loadData();
    } catch (err) {
      alert('Erreur lors de la suppression');
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-xl">Chargement...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-xl text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold text-gray-900">
            Annuaire des Agents Météo
          </h1>
          <button
            onClick={() => setShowCreateForm(true)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            + Créer un agent
          </button>
        </div>

        {/* Statistiques */}
        {stats && <StatisticsCards stats={stats} />}

        {/* Filtres */}
        <FilterButtons filter={filter} setFilter={setFilter} stats={stats} />

        {/* Liste des agents */}
        <div className="bg-white rounded-lg shadow">
          <div className="p-4 border-b">
            <h2 className="text-lg font-semibold">
              Agents ({agents.length})
            </h2>
          </div>
          <div className="divide-y">
            {agents.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                Aucun agent trouvé
              </div>
            ) : (
              agents.map((agent) => (
                <AgentRow
                  key={agent.id}
                  agent={agent}
                  onDelete={handleDelete}
                />
              ))
            )}
          </div>
        </div>

        {/* Formulaire de création */}
        {showCreateForm && (
          <CreateAgentModal
            onClose={() => setShowCreateForm(false)}
            onSuccess={() => {
              setShowCreateForm(false);
              loadData();
            }}
          />
        )}
      </div>
    </div>
  );
}

// Composant : Cartes de statistiques
function StatisticsCards({ stats }: { stats: AgentStatistics }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      <StatCard
        title="Total"
        value={stats.totalAgents}
        icon="📊"
        color="gray"
      />
      <StatCard
        title="Actifs"
        value={stats.activeAgents}
        icon="✅"
        color="green"
      />
      <StatCard
        title="Inactifs"
        value={stats.inactiveAgents}
        icon="⚠️"
        color="red"
      />
      <StatCard
        title="Types"
        value={Object.keys(stats.agentsByType).length}
        icon="🏷️"
        color="blue"
      />
    </div>
  );
}

function StatCard({
  title,
  value,
  icon,
  color,
}: {
  title: string;
  value: number;
  icon: string;
  color: string;
}) {
  const colorClasses = {
    gray: 'text-gray-600',
    green: 'text-green-600',
    red: 'text-red-600',
    blue: 'text-blue-600',
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-600">{title}</p>
          <p className={`text-3xl font-bold ${colorClasses[color as keyof typeof colorClasses]}`}>
            {value}
          </p>
        </div>
        <div className="text-4xl">{icon}</div>
      </div>
    </div>
  );
}

// Composant : Boutons de filtrage
function FilterButtons({
  filter,
  setFilter,
  stats,
}: {
  filter: string;
  setFilter: (filter: string) => void;
  stats: AgentStatistics | null;
}) {
  const filters = [
    { value: 'all', label: 'Tous', count: stats?.totalAgents },
    { value: 'active', label: 'Actifs', count: stats?.activeAgents },
    ...(stats?.agentsByType
      ? Object.entries(stats.agentsByType).map(([type, count]) => ({
          value: type,
          label: type,
          count,
        }))
      : []),
  ];

  return (
    <div className="flex flex-wrap gap-2">
      {filters.map((f) => (
        <button
          key={f.value}
          onClick={() => setFilter(f.value)}
          className={`px-4 py-2 rounded-lg font-medium transition ${
            filter === f.value
              ? 'bg-blue-600 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-100'
          }`}
        >
          {f.label} ({f.count || 0})
        </button>
      ))}
    </div>
  );
}

// Composant : Ligne d'agent
function AgentRow({
  agent,
  onDelete,
}: {
  agent: Agent;
  onDelete: (id: string) => void;
}) {
  const statusConfig = {
    ACTIVE: { color: 'bg-green-500', label: 'Actif' },
    INACTIVE: { color: 'bg-red-500', label: 'Inactif' },
    UNKNOWN: { color: 'bg-gray-500', label: 'Inconnu' },
  };

  const typeIcons: Record<string, string> = {
    CLIENT: '👤',
    VILLE: '🏙️',
    CAPTEUR: '📡',
  };

  const config = statusConfig[agent.status];
  const icon = typeIcons[agent.type] || '❓';

  return (
    <div className="p-4 hover:bg-gray-50 transition">
      <div className="flex items-center justify-between">
        {/* Infos principales */}
        <div className="flex items-center gap-4 flex-1">
          <div className="text-3xl">{icon}</div>
          
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-gray-900">{agent.id}</h3>
              <span className="px-2 py-1 text-xs rounded bg-blue-100 text-blue-800">
                {agent.type}
              </span>
            </div>
            
            <div className="mt-1 text-sm text-gray-600 space-y-1">
              <div className="flex items-center gap-4">
                <span>🌐 {agent.host}:{agent.port}</span>
                <span>🖥️ {agent.runtimeNode}</span>
                <span>📊 {agent.state}</span>
              </div>
              <div>
                ⏰ Dernier heartbeat:{' '}
                {new Date(agent.lastHeartbeat).toLocaleString('fr-FR')}
              </div>
            </div>
          </div>
        </div>

        {/* Statut et actions */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className={`w-3 h-3 rounded-full ${config.color}`} />
            <span className="text-sm font-medium">{config.label}</span>
          </div>
          
          <button
            onClick={() => onDelete(agent.id)}
            className="px-3 py-1 text-sm text-red-600 hover:bg-red-50 rounded transition"
          >
            Supprimer
          </button>
        </div>
      </div>
    </div>
  );
}

// Composant : Modal de création
function CreateAgentModal({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [formData, setFormData] = useState<AgentCreateRequest>({
    type: 'CAPTEUR',
    host: 'localhost',
    port: 8080,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await agentsApi.createAgent(formData);
      onSuccess();
    } catch (err) {
      setError('Erreur lors de la création');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
        <h2 className="text-2xl font-bold mb-4">Créer un nouvel agent</h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Type d'agent
            </label>
            <select
              value={formData.type}
              onChange={(e) =>
                setFormData({ ...formData, type: e.target.value })
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="CLIENT">CLIENT</option>
              <option value="VILLE">VILLE</option>
              <option value="CAPTEUR">CAPTEUR</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Host
            </label>
            <input
              type="text"
              value={formData.host}
              onChange={(e) =>
                setFormData({ ...formData, host: e.target.value })
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              placeholder="localhost"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Port
            </label>
            <input
              type="number"
              value={formData.port}
              onChange={(e) =>
                setFormData({ ...formData, port: parseInt(e.target.value) })
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              placeholder="8080"
              min="1"
              max="65535"
            />
          </div>

          {error && (
            <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div className="flex gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
            >
              {loading ? 'Création...' : 'Créer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
