<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { formatMessage, t } from '../i18n'
import { register } from '../lib/auth'
import { displayError, getRetryAfterSeconds, isRateLimitError } from '../lib/errors'
import { useRetryCooldown } from '../lib/rateLimit'

const username = ref('')
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const { retrySeconds, isRetrying, startRetryCooldown } = useRetryCooldown()
const displayedError = computed(() => (
  isRetrying.value
    ? formatMessage(t.value.register.rateLimited, { seconds: retrySeconds.value })
    : errorMessage.value
))

const router = useRouter()

const handleRegister = async () => {
  if (isSubmitting.value || isRetrying.value) return

  errorMessage.value = ''

  if (password.value !== passwordConfirm.value) {
    errorMessage.value = t.value.register.passwordsMismatch
    return
  }

  isSubmitting.value = true

  try {
    await register(email.value.trim(), password.value)
    router.push('/polls')
  } catch (error) {
    if (isRateLimitError(error)) {
      startRetryCooldown(getRetryAfterSeconds(error))
      return
    }

    errorMessage.value = displayError(error, t.value.register.failed)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>{{ t.register.title }}</h2>
      <p class="auth-sub">{{ t.register.sub }}</p>

      <form @submit.prevent="handleRegister">
        <div class="group">
          <label>{{ t.register.user }}</label>
          <div class="in-wrap">
            <img src="@/assets/icons/login.svg" class="in-icon" alt="U">
            <input v-model="username" type="text" :placeholder="t.register.userPlaceholder" />
          </div>
        </div>

        <div class="group">
          <label>{{ t.register.email }} <span class="required">*</span></label>
          <div class="in-wrap">
            <img src="@/assets/icons/mail.svg" class="in-icon" alt="@">
            <input v-model="email" type="email" :placeholder="t.register.emailPlaceholder" required />
          </div>
        </div>

        <div class="group">
          <label>{{ t.register.pass }} <span class="required">*</span></label>
          <div class="in-wrap">
            <img src="@/assets/icons/lock.svg" class="in-icon" alt="*">
            <input v-model="password" type="password" :placeholder="t.register.passPlaceholder" required />
          </div>
        </div>

        <div class="group">
          <label>{{ t.register.passConfirm }} <span class="required">*</span></label>
          <div class="in-wrap">
            <img src="@/assets/icons/shield-check.svg" class="in-icon" alt="S">
            <input v-model="passwordConfirm" type="password" :placeholder="t.register.passPlaceholder" required />
          </div>
        </div>

        <p class="required-note">{{ t.common.requiredField }}</p>
        <p v-if="displayedError" class="form-error">{{ displayedError }}</p>

        <button type="submit" class="btn-auth" :disabled="isSubmitting || isRetrying">
          {{ isSubmitting ? t.register.loading : t.register.btn }}
        </button>
      </form>

      <div class="auth-footer">
        {{ t.register.footerText }} <router-link to="/login">{{ t.register.footerLink }}</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 60px 24px;
}

.auth-card {
  background: white;
  width: 100%;
  max-width: 460px;
  padding: 40px;
  border-radius: 24px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

h2 {
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -1px;
  margin-bottom: 8px;
  text-align: center;
}

.auth-sub {
  text-align: center;
  color: var(--text-muted);
  margin-bottom: 32px;
  font-size: 0.95rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

label {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--text-main);
  margin-left: 4px;
}

.required {
  color: #dc2626;
  margin-left: 2px;
}

.required-note {
  font-size: 0.8rem;
  color: #64748b;
  margin-top: 4px;
}

.in-wrap {
  position: relative;
}

.in-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  opacity: 0.4;
}

input {
  width: 100%;
  padding: 12px 12px 12px 42px;
  border: 2px solid var(--border);
  border-radius: 12px;
  font-size: 1rem;
  background-color: #fcfcfc;
  transition: all 0.2s ease;
}

input:focus {
  border-color: var(--primary);
  background-color: white;
}

.form-error {
  color: #dc2626;
  font-size: 0.9rem;
}

.btn-auth {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--primary);
  color: white;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  margin-top: 12px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.1s ease;
}

.btn-auth:hover {
  background: var(--primary-dark);
}

.btn-auth:active {
  transform: scale(0.98);
}

.btn-auth:disabled {
  opacity: 0.7;
  cursor: wait;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 0.9rem;
  color: var(--text-muted);
}

.auth-footer a {
  color: var(--primary);
  font-weight: 700;
  text-decoration: none;
}

.auth-footer a:hover {
  text-decoration: underline;
}
</style>
