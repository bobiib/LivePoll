<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { formatMessage, t } from '../i18n';
import { displayError } from '../lib/errors';
import { deleteSavedPoll, getSavedPolls, startSavedPoll } from '../lib/polls';

const router = useRouter();

const polls = ref([]);
const isLoading = ref(true);
const errorMessage = ref('');
const startingPollId = ref('');
const deletingPollId = ref('');
const pendingDeletePoll = ref(null);

const sortedPolls = computed(() => [...polls.value].sort((a, b) => a.title.localeCompare(b.title)));

const statusText = (status) => {
  if (status === 'LIVE') return t.value.polls.statusLive;
  if (status === 'FINISHED') return t.value.polls.statusFinished;
  return t.value.polls.statusLobby;
};

const statusClass = (status) => String(status || 'LOBBY').toLowerCase();

const questionTypeText = (type) => {
  if (type === 'multi') return t.value.create.typeMulti;
  if (type === 'text') return t.value.create.typeText;
  return t.value.create.typeSingle;
};

const loadPolls = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    polls.value = await getSavedPolls();
  } catch (error) {
    errorMessage.value = displayError(error, t.value.polls.loadFailed);
  } finally {
    isLoading.value = false;
  }
};

const handleStartPoll = async (pollId) => {
  startingPollId.value = pollId;
  errorMessage.value = '';

  try {
    const startedPoll = await startSavedPoll(pollId);
    router.push(`/live/${startedPoll.roomCode}`);
  } catch (error) {
    errorMessage.value = displayError(error, t.value.polls.startFailed);
  } finally {
    startingPollId.value = '';
  }
};

const handleEditPoll = (pollId) => {
  router.push(`/polls/${pollId}/edit`);
};

const handleViewHistory = (pollId) => {
  router.push(`/polls/${pollId}/history`);
};

const handleDeletePoll = (poll) => {
  pendingDeletePoll.value = poll;
};

const cancelDeletePoll = () => {
  if (deletingPollId.value) return;
  pendingDeletePoll.value = null;
};

const confirmDeletePoll = async () => {
  if (!pendingDeletePoll.value) return;

  const poll = pendingDeletePoll.value;
  deletingPollId.value = poll.id;
  errorMessage.value = '';

  try {
    await deleteSavedPoll(poll.id);
    polls.value = polls.value.filter((item) => item.id !== poll.id);
    pendingDeletePoll.value = null;
  } catch (error) {
    errorMessage.value = displayError(error, t.value.polls.deleteFailed);
  } finally {
    deletingPollId.value = '';
  }
};

onMounted(loadPolls);
</script>

<template>
  <main class="polls-page">
    <div class="content-wrapper">
      <header class="polls-header">
        <div>
          <h1>{{ t.polls.title }}</h1>
          <p>{{ t.polls.sub }}</p>
        </div>

        <router-link to="/create" class="btn-create">
          <img src="@/assets/icons/plus-circle.svg" class="icon-sm icon-white" alt="+">
          {{ t.polls.create }}
        </router-link>
      </header>

      <div v-if="isLoading" class="state-box">{{ t.polls.loading }}</div>

      <template v-else>
      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <section v-if="sortedPolls.length" class="poll-grid">
        <article v-for="poll in sortedPolls" :key="poll.id" class="poll-card">
          <div class="poll-card-top">
            <span class="status-pill" :class="statusClass(poll.status)">{{ statusText(poll.status) }}</span>
            <span class="room-code">{{ t.polls.room }}: {{ poll.roomCode }}</span>
          </div>

          <h2>{{ poll.title }}</h2>

          <div class="poll-meta">
            <span>{{ poll.questionCount }} {{ t.polls.questions }}</span>
          </div>

          <div class="question-preview">
            <div v-for="(question, index) in poll.questions.slice(0, 3)" :key="index" class="question-row">
              <span class="question-number">{{ index + 1 }}</span>
              <div>
                <strong>{{ question.title }}</strong>
                <small>{{ questionTypeText(question.type) }}</small>
              </div>
            </div>
          </div>

          <div class="poll-actions">
            <button
                @click="handleEditPoll(poll.id)"
                class="btn-card-action"
                :disabled="deletingPollId === poll.id"
            >
              <img src="@/assets/icons/edit.svg" class="icon-sm" alt="">
              {{ t.polls.edit }}
            </button>
            <button
                @click="handleDeletePoll(poll)"
                class="btn-card-action danger"
                :disabled="deletingPollId === poll.id"
            >
              <img src="@/assets/icons/trash.svg" class="icon-sm" alt="">
              {{ deletingPollId === poll.id ? t.polls.deleting : t.polls.delete }}
            </button>
          </div>

          <div v-if="poll.status === 'LOBBY'" class="finished-actions">
            <button
                @click="handleStartPoll(poll.id)"
                class="btn-start"
                :disabled="startingPollId === poll.id || deletingPollId === poll.id"
            >
              <img src="@/assets/icons/play.svg" class="icon-sm icon-white" alt=">">
              {{ startingPollId === poll.id ? t.polls.starting : t.polls.start }}
            </button>
            <router-link
                :to="'/polls/' + poll.id + '/history'"
                class="btn-start btn-resume secondary"
            >
              Ergebnisse
            </router-link>
          </div>
          
          <router-link
              v-else-if="poll.status === 'LIVE'"
              :to="'/live/' + poll.roomCode"
              class="btn-start btn-resume"
          >
            <img src="@/assets/icons/play.svg" class="icon-sm icon-white" alt=">">
            {{ t.polls.resume }}
          </router-link>

          <div v-else class="finished-actions">
            <router-link
                :to="'/polls/' + poll.id + '/history'"
                class="btn-start btn-resume secondary"
            >
              {{ t.finished.btnViewResults }}
            </router-link>
            <button
                @click="handleStartPoll(poll.id)"
                class="btn-start"
                :disabled="startingPollId === poll.id || deletingPollId === poll.id"
            >
              <img src="@/assets/icons/play.svg" class="icon-sm icon-white" alt=">">
              {{ startingPollId === poll.id ? t.polls.starting : t.polls.restart }}
            </button>
          </div>
        </article>
      </section>

      <section v-else class="empty-state">
        <div class="empty-icon">
          <img src="@/assets/icons/hash.svg" class="icon-sm" alt="#">
        </div>
        <h2>{{ t.polls.emptyTitle }}</h2>
        <p>{{ t.polls.emptySub }}</p>
        <router-link to="/create" class="btn-create empty-action">
          <img src="@/assets/icons/plus-circle.svg" class="icon-sm icon-white" alt="+">
          {{ t.polls.create }}
        </router-link>
      </section>
      </template>
    </div>

    <div v-if="pendingDeletePoll" class="modal-backdrop" @click.self="cancelDeletePoll">
      <section class="confirm-dialog" role="dialog" aria-modal="true" :aria-labelledby="`delete-poll-title-${pendingDeletePoll.id}`">
        <div class="confirm-icon">
          <img src="@/assets/icons/trash.svg" class="icon-sm" alt="">
        </div>
        <h2 :id="`delete-poll-title-${pendingDeletePoll.id}`">{{ t.polls.deleteConfirmTitle }}</h2>
        <p>{{ formatMessage(t.polls.deleteConfirm, { title: pendingDeletePoll.title }) }}</p>
        <div class="confirm-actions">
          <button class="btn-modal-secondary" @click="cancelDeletePoll" :disabled="deletingPollId === pendingDeletePoll.id">
            {{ t.polls.deleteCancel }}
          </button>
          <button class="btn-modal-danger" @click="confirmDeletePoll" :disabled="deletingPollId === pendingDeletePoll.id">
            {{ deletingPollId === pendingDeletePoll.id ? t.polls.deleting : t.polls.deleteConfirmAction }}
          </button>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.polls-page {
  min-height: calc(100vh - 72px);
  padding: 48px 24px 80px;
}

.content-wrapper {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
}

.polls-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 32px;
}

.polls-header h1 {
  font-size: 2.6rem;
  font-weight: 800;
  letter-spacing: 0;
  margin-bottom: 8px;
}

.polls-header p {
  color: var(--text-muted);
  font-size: 1.05rem;
}

.btn-create,
.btn-start {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-create {
  padding: 14px 20px;
}

.btn-create:hover,
.btn-start:hover,
.btn-resume:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
}

.btn-resume {
  text-decoration: none;
}

.finished-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}
.finished-actions .btn-start {
  margin-top: 0;
  padding: 12px 10px;
  font-size: 0.9rem;
}
.btn-start.secondary {
  background: #f1f5f9;
  color: #0f172a;
}
.btn-start.secondary:hover {
  background: #e2e8f0;
}

.poll-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.poll-card {
  display: flex;
  flex-direction: column;
  min-height: 360px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: var(--shadow);
  padding: 24px;
}

.poll-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.status-pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: var(--primary-dark);
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}

.status-pill.live {
  background: #dcfce7;
  color: #15803d;
}

.status-pill.finished {
  background: #f1f5f9;
  color: var(--text-muted);
}

.room-code {
  color: var(--text-muted);
  font-size: 0.82rem;
  font-weight: 700;
  text-align: right;
}

.poll-card h2 {
  font-size: 1.35rem;
  font-weight: 800;
  line-height: 1.25;
  overflow-wrap: anywhere;
  margin-bottom: 12px;
}

.poll-meta {
  color: var(--text-muted);
  font-size: 0.95rem;
  font-weight: 700;
  margin-bottom: 20px;
}

.question-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.question-row {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 10px;
  align-items: start;
}

.question-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f1f5f9;
  color: var(--primary);
  font-size: 0.85rem;
  font-weight: 800;
}

.question-row strong {
  display: block;
  color: var(--text-main);
  font-size: 0.95rem;
  line-height: 1.3;
  overflow-wrap: anywhere;
}

.question-row small {
  color: var(--text-muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.poll-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: auto;
}

.btn-card-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--text-main);
  cursor: pointer;
  font-weight: 800;
  transition: all 0.2s ease;
}

.btn-card-action:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-1px);
}

.btn-card-action.danger {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #dc2626;
}

.btn-card-action.danger:hover:not(:disabled) {
  background: #ffe4e6;
  border-color: #fb7185;
  color: #be123c;
}

.btn-card-action:disabled {
  cursor: wait;
  opacity: 0.65;
  transform: none;
}

.btn-start {
  width: 100%;
  padding: 14px 18px;
  margin-top: 12px;
  font-size: 1rem;
}

.btn-start:disabled {
  opacity: 0.7;
  cursor: wait;
  transform: none;
}

.state-box,
.empty-state {
  background: white;
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: var(--shadow);
  padding: 40px;
  text-align: center;
}

.state-box {
  color: var(--text-muted);
  font-weight: 800;
}

.empty-state {
  max-width: 520px;
  margin: 0 auto;
}

.empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #eef2ff;
  border-radius: 16px;
  margin-bottom: 18px;
}

.empty-state h2 {
  font-size: 1.6rem;
  font-weight: 800;
  margin-bottom: 8px;
}

.empty-state p {
  color: var(--text-muted);
  margin-bottom: 24px;
}

.empty-action {
  width: max-content;
  margin: 0 auto;
}

.form-error {
  color: #dc2626;
  font-weight: 700;
  text-align: center;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(6px);
}

.confirm-dialog {
  width: min(100%, 440px);
  background: white;
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
  padding: 28px;
  text-align: center;
}

.confirm-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  background: #fff1f2;
  border-radius: 14px;
}

.confirm-icon img {
  color: #dc2626;
}

.confirm-dialog h2 {
  color: var(--text-main);
  font-size: 1.45rem;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.2;
  margin-bottom: 10px;
}

.confirm-dialog p {
  color: var(--text-muted);
  font-weight: 700;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.confirm-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.btn-modal-secondary,
.btn-modal-danger {
  min-height: 46px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  font-weight: 800;
  transition: all 0.2s ease;
}

.btn-modal-secondary {
  background: #f1f5f9;
  color: var(--text-main);
}

.btn-modal-secondary:hover:not(:disabled) {
  background: #e2e8f0;
}

.btn-modal-danger {
  background: #dc2626;
  color: white;
}

.btn-modal-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-modal-secondary:disabled,
.btn-modal-danger:disabled {
  cursor: wait;
  opacity: 0.7;
}

@media (max-width: 720px) {
  .polls-page {
    padding: 32px 16px 64px;
  }

  .polls-header {
    align-items: stretch;
    flex-direction: column;
  }

  .polls-header h1 {
    font-size: 2.1rem;
  }

  .btn-create {
    width: 100%;
  }

  .confirm-actions {
    grid-template-columns: 1fr;
  }
}
</style>
