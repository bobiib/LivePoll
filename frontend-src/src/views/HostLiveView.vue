<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AnimatedStatusCard from '../components/AnimatedStatusCard.vue';
import Navbar from '../components/Navbar.vue';
import { formatMessage, t } from '../i18n';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_BASE_URL, authState } from '../lib/auth';
import { displayError } from '../lib/errors';
import { getPollDetails } from '../lib/polls';

const route = useRoute();
const router = useRouter();
const roomCode = String(route.params.id).toUpperCase();

const isLobby = ref(true);
const isFinished = ref(false);
const isLoading = ref(true);
const errorMessage = ref('');
const pollTitle = ref('');
const currentIndex = ref(0);
const participantsCount = ref(0);
const questions = ref([]);
const showResults = ref(false);

const currentQuestion = computed(() => questions.value[currentIndex.value] || null);
const isLastQuestion = computed(() => currentIndex.value === questions.value.length - 1);
const answersCount = computed(() => currentQuestion.value?.answers?.length || 0);
const groupedTextAnswers = computed(() => {
  if (!currentQuestion.value || currentQuestion.value.type !== 'text') return [];

  const groups = new Map();

  currentQuestion.value.answers.forEach((rawAnswer) => {
    const text = String(rawAnswer || '').trim();
    if (!text) return;

    const key = text.toLocaleLowerCase();
    const group = groups.get(key);

    if (group) {
      group.count += 1;
      return;
    }

    groups.set(key, { text, count: 1 });
  });

  return [...groups.values()].sort((a, b) => b.count - a.count || a.text.localeCompare(b.text));
});

const dynamicScale = computed(() => {
  if (answersCount.value > 20) return 0.75;
  if (answersCount.value > 12) return 0.85;
  return 1;
});

const maxVotes = computed(() => {
  if (!currentQuestion.value || currentQuestion.value.type === 'text') return 0;
  const max = Math.max(...currentQuestion.value.options.map((option) => option.votes), 0);
  return max === 0 ? 1 : max;
});

let stompClient = null;

const publishCommand = (command, questionIndex = currentIndex.value) => {
  if (!stompClient?.connected) return;

  stompClient.publish({
    destination: `/app/poll/${roomCode}/command`,
    body: JSON.stringify({ command, questionIndex }),
  });
};

const applyResults = (results) => {
  const question = questions.value[results.questionIndex];
  if (!question) return;

  if (question.type === 'text') {
    question.answers = results.answers || [];
    return;
  }

  question.options = question.options.map((option, index) => ({
    ...option,
    votes: results.votes?.[index] || 0,
  }));
};

const connectSocket = () => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
    connectHeaders: authState.token ? { Authorization: `Bearer ${authState.token}` } : {},
    onConnect: () => {
      stompClient.subscribe(`/topic/poll/${roomCode}/host`, (message) => {
        participantsCount.value = parseInt(message.body) || 0;
      });

      stompClient.subscribe(`/topic/poll/${roomCode}/results`, (message) => {
        applyResults(JSON.parse(message.body));
      });
    },
  });
  stompClient.activate();
};

onMounted(async () => {
  try {
    const poll = await getPollDetails(roomCode);
    pollTitle.value = poll.title;
    isLobby.value = poll.status === 'LOBBY';
    isFinished.value = poll.status === 'FINISHED';
    currentIndex.value = poll.activeQuestionIndex || 0;
    if (isFinished.value) {
      showResults.value = true;
      currentIndex.value = 0;
    }
    questions.value = poll.questions.map((question, index) => ({
      id: index,
      title: question.title,
      type: question.type,
      options: question.options.map((text, optionIndex) => ({
        text,
        votes: question.votes?.[optionIndex] || 0,
      })),
      answers: question.answers || [],
    }));
    connectSocket();
  } catch (error) {
    errorMessage.value = displayError(error, t.value.hostLive.loadFailed);
  } finally {
    isLoading.value = false;
  }
});

onUnmounted(() => {
  if (stompClient) stompClient.deactivate();
});

const startPoll = () => {
  isLobby.value = false;
  isFinished.value = false;
  currentIndex.value = 0;
  publishCommand('START_POLL', 0);
};

const handlePrev = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
    if (!isFinished.value) {
      publishCommand('NEXT_QUESTION', currentIndex.value);
    }
  }
};

const handleNext = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++;
    if (!isFinished.value) {
      publishCommand('NEXT_QUESTION', currentIndex.value);
    }
  } else if (!isFinished.value) {
    isFinished.value = true;
    publishCommand('END_POLL', currentIndex.value);
  }
};
</script>

<template>
  <div class="live-screen">
    <Navbar />

    <main class="main-stage">
      <div v-if="isLoading" class="state-message">{{ t.hostLive.loading }}</div>
      <div v-else-if="errorMessage" class="state-message error">{{ errorMessage }}</div>

      <div v-else-if="isLobby" class="lobby-layout">
        <div class="lobby-content">
          <p v-if="pollTitle" class="poll-title">{{ pollTitle }}</p>
          <span class="pin-label">{{ t.hostLive.pin }}</span>
          <div class="code-box">{{ roomCode }}</div>
          <div class="status-container">
            <p class="waiting-text">{{ t.hostLive.waitingParticipants }}</p>
            <div class="participant-counter">
              <span class="count-pulse">{{ participantsCount }}</span>
              <span class="ready-text">{{ t.hostLive.participantsReady }}</span>
            </div>
          </div>
          <button @click="startPoll" class="btn-start-central" :disabled="questions.length === 0">{{ t.hostLive.start }}</button>
        </div>
      </div>

      <AnimatedStatusCard
          v-else-if="isFinished && !showResults"
          :title="t.finished.title"
          :subtitle="t.finished.sub"
      >
        <template #actions>
          <button @click="router.push('/create')" class="status-action-primary">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            {{ t.finished.btnCreate }}
          </button>

          <button @click="router.push('/polls')" class="status-action-secondary">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="9"></rect>
              <rect x="14" y="3" width="7" height="5"></rect>
              <rect x="14" y="12" width="7" height="9"></rect>
              <rect x="3" y="16" width="7" height="5"></rect>
            </svg>
            {{ t.finished.btnDashboard }}
          </button>

          <button @click="showResults = true; currentIndex = 0" class="status-action-primary">
            {{ t.finished.btnViewResults }}
          </button>
        </template>
      </AnimatedStatusCard>

      <div v-else-if="currentQuestion" class="poll-session">
        <div class="poll-view" :style="{ transform: `scale(${dynamicScale})` }">
          <h1 class="display-question">{{ currentQuestion.title }}</h1>

          <div v-if="currentQuestion.type === 'text'" class="bento-grid">
            <div
                v-for="(answer, index) in groupedTextAnswers"
                :key="answer.text"
                class="answer-cloud-card"
                :class="{
                  'is-popular': answer.count > 1,
                  'is-long': answer.text.length > 18,
                  'is-short': answer.text.length <= 5
                }"
              :style="{
                animationDelay: (index * 0.05) + 's'
              }"
            >
              <span class="answer-text">{{ answer.text }}</span>
              <span v-if="answer.count > 1" class="answer-count">{{ answer.count }}x</span>
            </div>
            <p v-if="groupedTextAnswers.length === 0" class="empty-results">{{ t.hostLive.noAnswers }}</p>
          </div>

          <div v-else class="bars-stack">
            <div v-for="(opt, index) in currentQuestion.options" :key="index" class="bar-item">
              <div class="bar-info">
                <span>{{ opt.text }}</span>
                <span class="vote-num">{{ opt.votes }}</span>
              </div>
              <div class="bar-rail"><div class="bar-fill" :style="{ width: (opt.votes / maxVotes * 100) + '%' }"></div></div>
            </div>
          </div>
        </div>

        <div class="host-control-bar">
          <div class="host-control-inner">
            <button v-if="currentIndex > 0" @click="handlePrev" class="btn-float-action secondary">
              {{ t.hostLive.prevQuestion }}
            </button>
            <div v-else></div>
            <div class="float-progress">{{ formatMessage(t.hostLive.progress, { current: currentIndex + 1, total: questions.length }) }}</div>
            <button v-if="!(isLastQuestion && isFinished)" @click="handleNext" class="btn-float-action" :class="{ 'finish': isLastQuestion && !isFinished }">
              {{ isLastQuestion && !isFinished ? t.hostLive.finish : t.hostLive.nextQuestion }}
            </button>
            <button v-if="isLastQuestion && isFinished" @click="router.push('/polls')" class="btn-float-action finish">
              {{ t.finished.btnDashboard }}
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.live-screen {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: #ffffff; color: #0f172a; font-family: 'Inter', sans-serif;
  display: flex; flex-direction: column; overflow: hidden;
}

.main-stage {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  position: relative;
  overflow-y: auto;
}
.state-message { margin-top: 120px; font-size: 1.2rem; font-weight: 700; color: #64748b; }
.state-message.error { color: #dc2626; }

/* LOBBY CSS */
.lobby-layout { display: flex; flex-direction: column; justify-content: center; align-items: center; width: 100%; height: 100%; }
.lobby-content { text-align: center; display: flex; flex-direction: column; align-items: center; }
.poll-title { font-size: 1.4rem; font-weight: 800; color: #0f172a; margin-bottom: 20px; }
.pin-label { display: block; font-weight: 900; color: #94a3b8; letter-spacing: 4px; margin-bottom: 8px; font-size: 1.5rem; }
.code-box { background: #6366f1; color: white; font-size: 5rem; font-weight: 900; padding: 10px 40px; border-radius: 24px; letter-spacing: 10px; margin-bottom: 40px; }
.status-container { margin-bottom: 50px; }
.waiting-text { font-size: 1.5rem; font-weight: 600; color: #64748b; margin-bottom: 10px; }
.participant-counter { display: flex; align-items: center; gap: 15px; justify-content: center; }
.count-pulse { font-size: 4rem; font-weight: 900; color: #6366f1; animation: breathe 2s infinite ease-in-out; }
@keyframes breathe { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.1); opacity: 0.8; } }
.btn-start-central { background: #000; color: white; border: none; padding: 20px 80px; border-radius: 100px; font-weight: 800; font-size: 1.5rem; cursor: pointer; }
.btn-start-central:disabled { opacity: 0.4; cursor: not-allowed; }

/* POLL VIEW & INTELLIGENT BENTO */
.poll-session {
  width: 100%;
  min-height: 100%;
}

.poll-view {
  width: 100%; max-width: 1300px; padding: 60px 60px 190px 60px;
  margin: 0 auto;
  display: flex; flex-direction: column; align-items: center;
  transform-origin: top center;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.display-question { font-size: 3.5rem; font-weight: 900; text-align: center; margin-bottom: 60px; letter-spacing: 0; }

.bento-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 14px;
  width: 100%;
  max-width: 1100px;
}

.answer-cloud-card {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 14px 24px;
  overflow: visible;
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  box-shadow: 0 8px 16px -10px rgba(15, 23, 42, 0.2);
  animation: slidePop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.answer-cloud-card.is-popular {
  border-color: rgba(99, 102, 241, 0.28);
  box-shadow: 0 12px 20px -10px rgba(99, 102, 241, 0.3), inset 0 0 0 1px rgba(99, 102, 241, 0.08);
  transform: scale(1.02);
}

.answer-cloud-card.is-short {}
.answer-cloud-card.is-long {}

.answer-text {
  color: #4338ca;
  font-size: 1.25rem;
  font-weight: 800;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.answer-count {
  position: absolute;
  top: -10px;
  right: -10px;
  min-width: 28px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #6366f1;
  color: white;
  font-size: 0.8rem;
  font-weight: 900;
  box-shadow: 0 4px 8px -4px rgba(67, 56, 202, 0.9);
}

.empty-results { font-size: 1.1rem; color: #64748b; font-weight: 700; }

@keyframes slidePop { from { opacity: 0; transform: scale(0.9) translateY(30px); } to { opacity: 1; transform: scale(1) translateY(0); } }

/* CHOICE BARS */
.bars-stack { width: 100%; max-width: 800px; }
.bar-item { margin-bottom: 30px; }
.bar-info { display: flex; justify-content: space-between; gap: 20px; font-weight: 800; font-size: 1.3rem; margin-bottom: 10px; }
.bar-info span:first-child { overflow-wrap: anywhere; }
.bar-rail { height: 16px; background: #f1f5f9; border-radius: 100px; overflow: hidden; }
.bar-fill { height: 100%; background: #6366f1; border-radius: 100px; transition: width 1s ease-out; }

/* HOST CONTROLS */
.host-control-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  padding: 16px 32px;
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid #e2e8f0;
  backdrop-filter: blur(12px);
  box-shadow: 0 -18px 50px rgba(15, 23, 42, 0.08);
}

.host-control-inner {
  width: 100%;
  max-width: 1300px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.float-progress {
  background: #f1f5f9; padding: 12px 24px; border-radius: 100px;
  font-weight: 700; color: #94a3b8;
  white-space: nowrap;
}

.btn-float-action {
  background: #22c55e; color: white; padding: 16px 40px;
  border-radius: 20px; font-weight: 900; font-size: 1.2rem;
  border: none; cursor: pointer; box-shadow: 0 10px 20px rgba(0,0,0,0.1);
  white-space: nowrap;
  transition: all 0.2s ease;
}
.btn-float-action.secondary {
  background: #94a3b8;
}
.btn-float-action.secondary:hover { box-shadow: 0 14px 26px rgba(0,0,0,0.14); }
.btn-float-action:hover { transform: translateY(-2px); box-shadow: 0 14px 26px rgba(0,0,0,0.14); }
.btn-float-action.finish { background: #000; }

@media (max-width: 720px) {
  .live-screen {
    position: fixed;
    min-height: 100svh;
  }

  .main-stage {
    background:
      linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  }

  .state-message {
    margin: auto 20px;
    text-align: center;
  }

  .lobby-layout {
    min-height: 100%;
    padding: 22px 18px 34px;
  }

  .lobby-content {
    width: 100%;
    max-width: 420px;
    min-height: calc(100svh - 150px);
    justify-content: center;
    padding: 26px 18px;
  }

  .poll-title {
    font-size: 1.05rem;
    line-height: 1.25;
    margin-bottom: 18px;
    overflow-wrap: anywhere;
  }

  .pin-label {
    font-size: 0.9rem;
    letter-spacing: 3px;
  }

  .code-box {
    width: 100%;
    padding: 12px 16px;
    border-radius: 18px;
    font-size: clamp(2.6rem, 16vw, 4rem);
    letter-spacing: 6px;
    margin-bottom: 28px;
  }

  .status-container {
    margin-bottom: 28px;
  }

  .waiting-text {
    font-size: 1.05rem;
    line-height: 1.3;
  }

  .participant-counter {
    gap: 10px;
  }

  .count-pulse {
    font-size: 3rem;
  }

  .ready-text {
    color: #64748b;
    font-weight: 800;
  }

  .btn-start-central {
    width: 100%;
    padding: 17px 24px;
    border-radius: 18px;
    font-size: 1.15rem;
  }

  .poll-view {
    padding: 28px 16px 178px 16px;
    transform: none !important;
  }

  .display-question {
    width: 100%;
    font-size: clamp(1.65rem, 8vw, 2.35rem);
    line-height: 1.08;
    margin-bottom: 28px;
    text-align: left;
    overflow-wrap: anywhere;
  }

  .bento-grid {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
    max-width: none;
  }

  .answer-cloud-card,
  .answer-cloud-card.is-short,
  .answer-cloud-card.is-long {
    flex: 0 1 auto;
    width: auto;
    max-width: 100%;
    min-width: 0;
    min-height: auto;
    justify-content: center;
    padding: 12px 18px;
    border-radius: 14px;
    text-align: center;
  }

  .answer-text {
    font-size: 1.1rem;
  }

  .answer-count {
    top: -6px;
    right: -6px;
    transform: scale(0.9);
  }

  .bars-stack {
    max-width: none;
  }

  .bar-item {
    margin-bottom: 12px;
    padding: 15px;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    box-shadow: 0 12px 26px -22px rgba(15, 23, 42, 0.35);
  }

  .bar-info {
    align-items: flex-start;
    gap: 12px;
    font-size: 1rem;
    line-height: 1.25;
  }

  .vote-num {
    min-width: 34px;
    padding: 4px 8px;
    border-radius: 999px;
    background: #eef2ff;
    color: #4338ca;
    text-align: center;
    font-size: 0.9rem;
  }

  .bar-rail {
    height: 12px;
  }

  .host-control-bar {
    padding: 12px 14px max(14px, env(safe-area-inset-bottom));
  }

  .host-control-inner {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .float-progress,
  .btn-float-action {
    width: 100%;
    text-align: center;
  }

  .float-progress {
    padding: 10px 14px;
    border-radius: 14px;
    font-size: 0.9rem;
  }

  .btn-float-action {
    min-height: 56px;
    padding: 14px 18px;
    border-radius: 16px;
    font-size: 1.05rem;
  }
}
</style>
