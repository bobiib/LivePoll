<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import AnimatedStatusCard from '../components/AnimatedStatusCard.vue';
import { formatMessage, t } from '../i18n';
import { API_BASE_URL } from '../lib/auth';
import { displayError, getRetryAfterSeconds, isRateLimitError } from '../lib/errors';
import { getPollDetails, submitVote } from '../lib/polls';
import { useRetryCooldown } from '../lib/rateLimit';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const route = useRoute();
const roomCode = String(route.params.id).toUpperCase();

const roomTitle = ref('');
const questions = ref([]);
const hasStarted = ref(false);
const isFinished = ref(false);
const isLoading = ref(true);
const errorMessage = ref('');
const currentIndex = ref(0);
const selectedOption = ref(null);
const selectedOptions = ref([]);
const textAnswer = ref('');
const answersGiven = ref(0);
const isSubmitting = ref(false);
const { retrySeconds, isRetrying, startRetryCooldown } = useRetryCooldown();

const currentQuestion = computed(() => questions.value[currentIndex.value] || null);
const maxAnswersForCurrent = computed(() => currentQuestion.value?.maxAnswers || 1);
const isWaitingForNext = computed(() => answersGiven.value >= maxAnswersForCurrent.value);
const displayedError = computed(() => (
  isRetrying.value
    ? formatMessage(t.value.room.voteRateLimited, { seconds: retrySeconds.value })
    : errorMessage.value
));

const canSubmit = computed(() => {
  if (!currentQuestion.value || isSubmitting.value || isRetrying.value || isWaitingForNext.value) return false;
  if (currentQuestion.value.type === 'text') return textAnswer.value.trim().length > 0;
  if (currentQuestion.value.type === 'multi') return selectedOptions.value.length > 0;
  return selectedOption.value !== null;
});

let stompClient = null;

const getParticipantId = () => {
  let pid = localStorage.getItem('participantId');
  if (!pid) {
    pid = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36);
    localStorage.setItem('participantId', pid);
  }
  return pid;
};

const getVotedHistory = () => {
  const data = localStorage.getItem(`voted_${roomCode}`);
  return data ? JSON.parse(data) : [];
};

const addVotedHistory = (index) => {
  const history = getVotedHistory();
  if (!history.includes(index)) {
    history.push(index);
    localStorage.setItem(`voted_${roomCode}`, JSON.stringify(history));
  }
};

const checkIfVoted = () => {
  if (getVotedHistory().includes(currentIndex.value)) {
    answersGiven.value = maxAnswersForCurrent.value;
  }
};

const resetAnswerState = () => {
  selectedOption.value = null;
  selectedOptions.value = [];
  textAnswer.value = '';
  answersGiven.value = 0;
};

const toggleMultiOption = (index) => {
  if (selectedOptions.value.includes(index)) {
    selectedOptions.value = selectedOptions.value.filter((item) => item !== index);
    return;
  }

  if (selectedOptions.value.length < maxAnswersForCurrent.value) {
    selectedOptions.value = [...selectedOptions.value, index];
  }
};

const submitAnswer = async () => {
  if (!canSubmit.value || !currentQuestion.value) return;

  isSubmitting.value = true;
  errorMessage.value = '';

  try {
    const pId = getParticipantId();
    if (currentQuestion.value.type === 'text') {
      await submitVote(roomCode, currentIndex.value, textAnswer.value.trim(), pId);
      answersGiven.value++;
      textAnswer.value = '';
    } else if (currentQuestion.value.type === 'multi') {
      await submitVote(roomCode, currentIndex.value, selectedOptions.value, pId);
      answersGiven.value = maxAnswersForCurrent.value;
      selectedOptions.value = [];
    } else {
      await submitVote(roomCode, currentIndex.value, selectedOption.value, pId);
      answersGiven.value = maxAnswersForCurrent.value;
      selectedOption.value = null;
    }
    addVotedHistory(currentIndex.value);
  } catch (error) {
    if (isRateLimitError(error)) {
      startRetryCooldown(getRetryAfterSeconds(error));
      return;
    }

    if (error.status === 409) {
      addVotedHistory(currentIndex.value);
      answersGiven.value = maxAnswersForCurrent.value;
      return;
    }

    errorMessage.value = displayError(error, t.value.room.submitFailed);
  } finally {
    isSubmitting.value = false;
  }
};

const handleTextEnter = (event) => {
  if (event.shiftKey) return;

  event.preventDefault();
  submitAnswer();
};

const applyCommand = (data) => {
  if (data.command === 'START_POLL') {
    hasStarted.value = true;
    isFinished.value = false;
    currentIndex.value = data.questionIndex || 0;
    resetAnswerState();
    checkIfVoted();
  } else if (data.command === 'NEXT_QUESTION') {
    currentIndex.value = data.questionIndex || 0;
    resetAnswerState();
    checkIfVoted();
  } else if (data.command === 'END_POLL') {
    isFinished.value = true;
  }
};

const connectSocket = () => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
    onConnect: () => {
      stompClient.subscribe(`/topic/poll/${roomCode}/state`, (message) => {
        applyCommand(JSON.parse(message.body));
      }, { participantId: getParticipantId() });
    },
  });
  stompClient.activate();
};

onMounted(async () => {
  try {
    const poll = await getPollDetails(roomCode);
    roomTitle.value = poll.title;
    questions.value = poll.questions;
    currentIndex.value = poll.activeQuestionIndex || 0;
    hasStarted.value = poll.status === 'LIVE';
    isFinished.value = poll.status === 'FINISHED';
    checkIfVoted();
    connectSocket();
  } catch (error) {
    errorMessage.value = displayError(error, t.value.room.notFound);
  } finally {
    isLoading.value = false;
  }
});

onUnmounted(() => {
  if (stompClient) stompClient.deactivate();
});
</script>

<template>
  <main class="room-page">
    <div class="content-wrapper">
      <div v-if="isLoading" class="status-box card">
        <h1>{{ t.room.loading }}</h1>
      </div>

      <div v-else-if="errorMessage && !currentQuestion" class="status-box card error-card">
        <h1>{{ t.room.unavailable }}</h1>
        <p>{{ errorMessage }}</p>
        <router-link to="/" class="btn-secondary">{{ t.room.back }}</router-link>
      </div>

      <div v-else-if="!hasStarted && !isFinished" class="status-box card kahoot-waiting">
        <div class="pulse-circle">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="check-icon"><polyline points="20 6 9 17 4 12"></polyline></svg>
        </div>
        <h1>{{ t.room.youAreIn }}</h1>
        <p>{{ roomTitle || t.room.lookAtScreen }}</p>
        <div class="room-tag">{{ t.hostLive.pin }}: {{ roomCode }}</div>
      </div>

      <div v-else-if="!isFinished && currentQuestion" class="poll-box">
        <AnimatedStatusCard
            v-if="isWaitingForNext"
            compact
            show-dots
            :title="t.room.answerSent"
            :subtitle="t.room.waitNext"
        />

        <div v-else class="answer-flow">
          <header class="q-header">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length) * 100 + '%' }"></div>
            </div>

            <div class="q-meta">
              <span class="q-count">{{ formatMessage(t.room.progress, { current: currentIndex + 1, total: questions.length }) }}</span>
              <span v-if="maxAnswersForCurrent > 1" class="badge-remaining">
                {{ answersGiven }} / {{ maxAnswersForCurrent }}
              </span>
            </div>

            <h2>{{ currentQuestion.title }}</h2>
            <p v-if="currentQuestion.type === 'multi'" class="info-text text-primary">
              {{ formatMessage(t.room.chooseUpTo, { count: maxAnswersForCurrent }) }}
            </p>
          </header>

          <div v-if="currentQuestion.type === 'single'" class="options-list">
            <button
                v-for="(opt, index) in currentQuestion.options"
                :key="index"
                class="option-card"
                :class="{ active: selectedOption === index }"
                @click="selectedOption = index"
            >
              <div class="check-dot"></div>
              <span>{{ opt }}</span>
            </button>
          </div>

          <div v-else-if="currentQuestion.type === 'multi'" class="options-list">
            <button
                v-for="(opt, index) in currentQuestion.options"
                :key="index"
                class="option-card multi"
                :class="{ active: selectedOptions.includes(index) }"
                @click="toggleMultiOption(index)"
            >
              <div class="check-dot"></div>
              <span>{{ opt }}</span>
            </button>
          </div>

          <div v-else class="text-input-area">
            <textarea
                v-model="textAnswer"
                :placeholder="t.room.textPlaceholder"
                maxlength="50"
                @keydown.enter="handleTextEnter"
            ></textarea>
            <div class="char-counter">{{ textAnswer.length }} / 50</div>
          </div>

          <p v-if="displayedError" class="form-error">{{ displayedError }}</p>

          <div class="submit-area">
            <button
                @click="submitAnswer"
                class="btn-primary"
                :disabled="!canSubmit"
            >
              {{ isSubmitting ? t.room.submitting : t.room.submit }}
              <img src="@/assets/icons/arrow-right.svg" class="icon-white icon-sm" alt=">">
            </button>
          </div>
        </div>
      </div>

      <AnimatedStatusCard
          v-else
          compact
          :title="t.room.success"
          :subtitle="t.room.finalSuccessSub"
      >
        <template #actions>
          <router-link to="/" class="status-action-secondary">{{ t.room.back }}</router-link>
        </template>
      </AnimatedStatusCard>
    </div>
  </main>
</template>

<style scoped>
.room-page { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 72px); padding: 24px; }
.content-wrapper { width: 100%; max-width: 540px; }
.card { background: white; padding: 40px; border-radius: 24px; box-shadow: var(--shadow); border: 1px solid var(--border); text-align: center; }
.poll-box { width: 100%; }

.kahoot-waiting { background: var(--primary); color: white; border: none; }
.kahoot-waiting h1 { font-size: 2.5rem; font-weight: 900; margin-bottom: 12px; color: white; }
.kahoot-waiting p { font-size: 1.2rem; opacity: 0.9; margin-bottom: 32px; }
.pulse-circle { width: 80px; height: 80px; background: rgba(255, 255, 255, 0.2); border-radius: 50%; display: flex; justify-content: center; align-items: center; margin: 0 auto 24px; animation: pulse 2s infinite; }
.check-icon { width: 40px; height: 40px; color: white; }
.room-tag { display: inline-block; background: rgba(0, 0, 0, 0.2); padding: 8px 16px; border-radius: 12px; font-weight: 800; font-size: 1.2rem; }
.error-card h1 { font-size: 1.8rem; margin-bottom: 12px; }
.error-card p { color: var(--text-muted); }

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.5); }
  70% { transform: scale(1); box-shadow: 0 0 0 20px rgba(255, 255, 255, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(255, 255, 255, 0); }
}

.q-header { text-align: center; margin-bottom: 32px; }
.progress-bar { height: 6px; background: var(--border); border-radius: 10px; margin-bottom: 12px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--primary); transition: width 0.4s ease; }
.q-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; gap: 12px; }
.q-count { color: var(--text-muted); font-size: 0.85rem; font-weight: 700; text-transform: uppercase; }
.badge-remaining { background: #e0e7ff; color: var(--primary); padding: 4px 10px; border-radius: 10px; font-size: 0.8rem; font-weight: 700; }
h2 { font-size: 2rem; font-weight: 800; letter-spacing: 0; line-height: 1.2; margin-bottom: 8px; overflow-wrap: anywhere; }
.info-text { font-size: 0.95rem; font-weight: 600; }
.text-primary { color: var(--primary); }

.options-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 32px; }
.option-card { display: flex; align-items: center; gap: 16px; padding: 20px; background: white; border: 2px solid var(--border); border-radius: 16px; cursor: pointer; transition: all 0.2s ease; font-size: 1.1rem; font-weight: 600; text-align: left; overflow-wrap: anywhere; }
.option-card:hover { border-color: var(--primary); }
.option-card.active { border-color: var(--primary); background: #eef2ff; color: var(--primary); }
.check-dot { width: 22px; height: 22px; border: 2px solid var(--border); border-radius: 50%; position: relative; flex-shrink: 0; }
.option-card.multi .check-dot { border-radius: 6px; }
.option-card.active .check-dot { border-color: var(--primary); }
.option-card.active .check-dot::after { content: ''; position: absolute; width: 10px; height: 10px; background: var(--primary); border-radius: 50%; top: 50%; left: 50%; transform: translate(-50%, -50%); }
.option-card.multi.active .check-dot::after { border-radius: 3px; }

textarea { width: 100%; height: 140px; padding: 16px; border-radius: 16px; border: 2px solid var(--border); font-family: inherit; font-size: 1.1rem; margin-bottom: 32px; resize: none; transition: border 0.2s; }
textarea:focus { border-color: var(--primary); outline: none; box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1); }
.char-counter { text-align: right; font-size: 0.85rem; color: var(--text-muted); font-weight: 600; margin-top: 8px; margin-bottom: 24px; }
.form-error { color: #dc2626; font-size: 0.9rem; margin-bottom: 12px; }

.submit-area { width: 100%; }
.btn-primary { width: 100%; display: flex; justify-content: center; align-items: center; gap: 10px; background: var(--primary); color: white; padding: 18px; border-radius: 16px; font-size: 1.1rem; font-weight: 700; border: none; cursor: pointer; transition: all 0.2s; }
.btn-primary:hover:not(:disabled) { background: var(--primary-dark); transform: translateY(-2px); }
.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }

.btn-secondary { display: inline-block; margin-top: 24px; text-decoration: none; color: var(--primary); font-weight: 700; background: #e0e7ff; padding: 14px 28px; border-radius: 12px; transition: all 0.2s; }
.btn-secondary:hover { background: #c7d2fe; }

@media (max-width: 720px) {
  .room-page {
    align-items: stretch;
    min-height: calc(100svh - 64px);
    padding: 12px 14px max(18px, env(safe-area-inset-bottom));
    background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
  }

  .content-wrapper {
    max-width: none;
    display: flex;
    flex-direction: column;
  }

  .card {
    padding: 28px 20px;
    border-radius: 18px;
  }

  .status-box {
    margin: auto 0;
  }

  .kahoot-waiting {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: min(560px, calc(100svh - 110px));
    justify-content: center;
  }

  .kahoot-waiting h1 {
    font-size: 2rem;
    line-height: 1.1;
  }

  .kahoot-waiting p {
    font-size: 1rem;
    margin-bottom: 24px;
  }

  .pulse-circle {
    width: 64px;
    height: 64px;
    margin-bottom: 18px;
  }

  .check-icon {
    width: 32px;
    height: 32px;
  }

  .room-tag {
    font-size: 1rem;
    padding: 7px 12px;
  }

  .poll-box {
    min-height: calc(100svh - 88px);
    display: flex;
    flex-direction: column;
  }

  .answer-flow {
    display: flex;
    flex-direction: column;
    min-height: 100%;
    padding-bottom: max(18px, env(safe-area-inset-bottom));
  }

  .q-header {
    flex: 0 0 auto;
    margin-bottom: 18px;
    text-align: left;
  }

  .progress-bar {
    height: 7px;
    margin-bottom: 14px;
    background: #e2e8f0;
  }

  .q-meta {
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .q-count {
    font-size: 0.78rem;
    line-height: 1.25;
  }

  .badge-remaining {
    flex-shrink: 0;
  }

  h2 {
    font-size: 1.72rem;
    line-height: 1.15;
    margin-top: 2px;
    margin-bottom: 10px;
  }

  .info-text {
    font-size: 0.92rem;
    line-height: 1.35;
  }

  .options-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 0;
  }

  .option-card {
    min-height: 76px;
    padding: 18px 18px;
    background: rgba(255, 255, 255, 0.96);
    border: 1.5px solid #dbe4f0;
    border-radius: 18px;
    font-size: 1.05rem;
    box-shadow: 0 16px 28px -24px rgba(15, 23, 42, 0.38);
  }

  .option-card.active {
    background: #eef2ff;
    border-color: var(--primary);
    box-shadow: 0 18px 32px -24px rgba(79, 70, 229, 0.65);
  }

  .check-dot {
    width: 20px;
    height: 20px;
    background: white;
    border-color: #cbd5e1;
  }

  .text-input-area {
    display: block;
    margin-bottom: 0;
  }

  textarea {
    height: clamp(190px, 34svh, 280px);
    min-height: 190px;
    padding: 20px;
    margin-bottom: 0;
    background: rgba(255, 255, 255, 0.96);
    border: 1.5px solid #dbe4f0;
    border-radius: 20px;
    font-size: 1.05rem;
    line-height: 1.45;
    box-shadow: 0 16px 30px -24px rgba(15, 23, 42, 0.36);
  }

  textarea:focus {
    background: white;
    border-color: var(--primary);
  }

  .submit-area {
    position: static;
    z-index: 20;
    padding: 16px 0 0;
    background: transparent;
  }

  .btn-primary {
    min-height: 58px;
    border-radius: 16px;
    box-shadow: 0 16px 30px -18px rgba(79, 70, 229, 0.9);
  }

  .form-error {
    margin-bottom: 14px;
  }
}
</style>
