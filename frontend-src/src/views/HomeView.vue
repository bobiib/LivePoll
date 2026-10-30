<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { t } from '../i18n';
import { displayError } from '../lib/errors';
import { validateRoom } from '../lib/polls';

const roomCode = ref('');
const errorMessage = ref('');
const isJoining = ref(false);
const router = useRouter();

const joinRoom = async () => {
  const code = roomCode.value.trim().toUpperCase();
  errorMessage.value = '';

  if (!/^[A-Z0-9]{6}$/.test(code)) {
    errorMessage.value = t.value.home.invalidRoomCode;
    return;
  }

  isJoining.value = true;

  try {
    await validateRoom(code);
    router.push(`/room/${code}`);
  } catch (error) {
    errorMessage.value = displayError(error, t.value.home.roomNotFound);
  } finally {
    isJoining.value = false;
  }

};
</script>

<template>
  <main class="home-page">
    <div class="content-wrapper">

      <!-- Status Badge oben -->
      <div class="status-badge">
        <img src="@/assets/icons/bolt.svg" class="icon-xs" alt="">
        {{ t.home.badge }}
      </div>

      <!-- Hauptüberschriften -->
      <h1>{{ t.home.title }}</h1>
      <p class="subtitle">{{ t.home.sub }}</p>

      <!-- Die Action Card -->
      <div class="card">
        <div class="input-wrapper">
          <img src="@/assets/icons/hash.svg" class="input-icon" alt="#">
          <input
              v-model="roomCode"
              type="text"
              :placeholder="t.home.input"
              maxlength="6"
              autocomplete="off"
              autocapitalize="characters"
              @keyup.enter="joinRoom"
          />
        </div>

        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

        <button @click="joinRoom" class="btn-main" :disabled="isJoining">
          {{ isJoining ? t.home.checking : t.home.btnJoin }}
          <img src="@/assets/icons/arrow-right.svg" class="icon-sm icon-white" alt=">">
        </button>

        <div class="divider">
          <span>{{ t.home.or }}</span>
        </div>

        <router-link to="/create" class="btn-sub">
          <img src="@/assets/icons/plus-circle.svg" class="icon-sm" alt="+">
          {{ t.home.btnHost }}
        </router-link>
      </div>

    </div>
  </main>
</template>

<style scoped>
/* 1. Haupt-Container: Zentriert alles perfekt auf dem Screen */
.home-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 72px); /* Navbar Höhe abziehen */
  width: 100%;
  padding: 40px 24px;
}

/* 2. Innerer Wrapper für Text und Karte */
.content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 640px;
}

/* Badge Style */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #e0e7ff;
  color: var(--primary);
  padding: 6px 14px;
  border-radius: 100px;
  font-size: 0.85rem;
  font-weight: 700;
  margin-bottom: 24px;
}

/* Typografie */
h1 {
  font-size: 3.5rem;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.05em;
  margin-bottom: 20px;
  color: var(--text-main);
}

.subtitle {
  font-size: 1.25rem;
  color: var(--text-muted);
  margin-bottom: 48px;
}

/* Die Karte (Card) */
.card {
  background: var(--secondary);
  padding: 40px;
  border-radius: 24px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 440px;
}

/* Input Feld Design */
.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  opacity: 0.4;
  pointer-events: none;
}

input {
  width: 100%;
  padding: 18px 18px 18px 52px;
  font-size: 1.25rem;
  font-weight: 700;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  background: #f8fafc;
  letter-spacing: 2px;
  transition: all 0.2s ease;
  text-align: left;
}

input:focus {
  border-color: var(--primary);
  background: white;
  outline: none;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.form-error {
  color: #dc2626;
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0;
  text-align: left;
}

/* Primärer Button (Beitreten) */
.btn-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--primary);
  color: white;
  padding: 18px;
  font-size: 1.1rem;
  font-weight: 700;
  border-radius: var(--radius);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-main:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
}

.btn-main:active {
  transform: translateY(0);
}

.btn-main:disabled {
  opacity: 0.6;
  cursor: wait;
  transform: none;
}

/* Trenner (oder) */
.divider {
  margin: 12px 0;
  display: flex;
  align-items: center;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.divider::before, .divider::after {
  content: "";
  flex: 1;
  border-bottom: 1px solid var(--border);
}

.divider span {
  padding: 0 15px;
}

/* Sekundärer Button (Hosten) */
.btn-sub {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  color: var(--text-main);
  font-weight: 700;
  background: white;
  text-decoration: none;
  transition: all 0.2s ease;
}

.btn-sub:hover {
  background: #f8fafc;
  border-color: var(--text-main);
}
</style>
