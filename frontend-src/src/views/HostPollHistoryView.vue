<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { formatMessage, t } from '../i18n';
import { displayError } from '../lib/errors';
import { getPollHistory, getSavedPoll } from '../lib/polls';

const route = useRoute();
const router = useRouter();

const pollId = route.params.id;
const pollName = ref('');
const sessions = ref([]);
const isLoading = ref(true);
const errorMessage = ref('');

const loadHistory = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const [poll, history] = await Promise.all([
      getSavedPoll(pollId),
      getPollHistory(pollId)
    ]);
    pollName.value = poll.title;
    sessions.value = history;
  } catch (error) {
    errorMessage.value = displayError(error, "Fehler beim Laden des Verlaufs");
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadHistory();
});

const formatDate = (isoString) => {
  const date = new Date(isoString);
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
};

const handleViewSession = (roomCode) => {
  router.push(`/live/${roomCode}`);
};

</script>

<template>
  <main class="history-view">
    <div class="header">
      <div class="header-content">
        <h1>Ergebnisse: {{ pollName || 'Wird geladen...' }}</h1>
        <p class="subtitle">Wähle aus, von welcher Durchführung du die Ergebnisse sehen möchtest.</p>
      </div>

      <button @click="router.push('/polls')" class="btn-secondary">
        Zurück zum Dashboard
      </button>
    </div>

    <div v-if="isLoading" class="state-container">
      <div class="spinner"></div>
      <p>Lade Verlauf...</p>
    </div>

    <div v-else-if="errorMessage" class="state-container error">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <p>{{ errorMessage }}</p>
      <button @click="loadHistory" class="btn-primary">Erneut versuchen</button>
    </div>

    <div v-else-if="sessions.length === 0" class="state-container empty">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <polyline points="12 6 12 12 16 14"></polyline>
      </svg>
      <h2>Noch keine Ergebnisse vorhanden</h2>
      <p>Diese Umfrage wurde noch nicht beendet.</p>
      <button @click="router.push('/polls')" class="btn-primary">Zurück zum Dashboard</button>
    </div>

    <div v-else class="sessions-grid">
      <div v-for="(session, index) in sessions" :key="session.id" class="session-card">
        <div class="session-info">
          <h3>Durchführung #{{ sessions.length - index }} ({{ formatDate(session.createdAt) }})</h3>
          <p class="room-code">Raumcode: <strong>{{ session.roomCode }}</strong></p>
        </div>
        
        <div class="session-actions">
          <button @click="handleViewSession(session.roomCode)" class="btn-primary">
            Ergebnisse ansehen
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.history-view {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 3rem;
  gap: 2rem;
}

.header-content h1 {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0 0 0.5rem 0;
  background: linear-gradient(135deg, var(--text) 0%, var(--primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: var(--text-muted);
  font-size: 1.1rem;
  margin: 0;
}

.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--surface);
  border-radius: 20px;
  border: 1px solid var(--border);
  text-align: center;
  gap: 1.5rem;
}

.state-container svg {
  color: var(--text-muted);
}

.state-container h2 {
  font-size: 1.5rem;
  margin: 0;
}

.state-container p {
  color: var(--text-muted);
  margin: 0;
  max-width: 400px;
}

.error svg {
  color: var(--error);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.sessions-grid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.session-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s ease;
}

.session-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow);
  border-color: var(--primary);
}

.session-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.room-code {
  margin: 0;
  color: var(--text-muted);
  font-family: monospace;
  font-size: 1.1rem;
}

.room-code strong {
  color: var(--text);
  background: var(--background);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  border: 1px solid var(--border);
}

.btn-primary, .btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  font-size: 1rem;
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
}

.btn-secondary {
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--border);
}

.btn-secondary:hover {
  background: var(--background);
  border-color: var(--text-muted);
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .session-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .session-actions {
    width: 100%;
  }
  
  .session-actions button {
    width: 100%;
  }
}
</style>
