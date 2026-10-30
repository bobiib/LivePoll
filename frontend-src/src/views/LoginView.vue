<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatMessage, t } from '../i18n'
import { login } from '../lib/auth'
import { displayError, getRetryAfterSeconds, isRateLimitError } from '../lib/errors'
import { useRetryCooldown } from '../lib/rateLimit'

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const { retrySeconds, isRetrying, startRetryCooldown } = useRetryCooldown()
const displayedError = computed(() => (
  isRetrying.value
    ? formatMessage(t.value.login.rateLimited, { seconds: retrySeconds.value })
    : errorMessage.value
))

const router = useRouter()
const route = useRoute()

const handleLogin = async () => {
  if (isSubmitting.value || isRetrying.value) return

  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await login(email.value.trim(), password.value)
    const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : '/polls'
    router.push(redirectTarget)
  } catch (error) {
    if (isRateLimitError(error)) {
      startRetryCooldown(getRetryAfterSeconds(error))
      return
    }

    errorMessage.value = displayError(error, t.value.login.failed)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>{{ t.login.title }}</h2>
      <p class="auth-sub">{{ t.login.sub }}</p>

      <form @submit.prevent="handleLogin">
        <div class="group">
          <label>{{ t.login.email }} <span class="required">*</span></label>
          <div class="in-wrap">
            <img src="@/assets/icons/mail.svg" class="in-icon" alt="@">
            <input v-model="email" type="email" :placeholder="t.login.emailPlaceholder" required />
          </div>
        </div>

        <div class="group">
          <label>{{ t.login.pass }} <span class="required">*</span></label>
          <div class="in-wrap">
            <img src="@/assets/icons/lock.svg" class="in-icon" alt="*">
            <input v-model="password" type="password" :placeholder="t.login.passPlaceholder" required />
          </div>
        </div>

        <p class="required-note">{{ t.common.requiredField }}</p>
        <p v-if="displayedError" class="form-error">{{ displayedError }}</p>

        <button type="submit" class="btn-auth" :disabled="isSubmitting || isRetrying">
          {{ isSubmitting ? t.login.loading : t.login.btn }}
        </button>
      </form>

      <div class="auth-footer">
        {{ t.login.footerText }} <router-link to="/register">{{ t.login.footerLink }}</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 80px 24px;
}

.auth-card {
  background: white;
  width: 100%;
  max-width: 440px;
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
}

form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

label {
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--text-main);
}

.required {
  color: #dc2626;
  margin-left: 2px;
}

.required-note {
  font-size: 0.8rem;
  color: #64748b;
  margin-top: -4px;
  margin-bottom: 4px;
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
  opacity: 0.4;
}

input {
  width: 100%;
  padding: 12px 12px 12px 42px;
  border: 2px solid var(--border);
  border-radius: 12px;
  font-size: 1rem;
}

input:focus {
  border-color: var(--primary);
}

.form-error {
  color: #dc2626;
  font-size: 0.9rem;
}

.btn-auth {
  background: var(--primary);
  color: white;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  margin-top: 8px;
  cursor: pointer;
}

.btn-auth:disabled {
  opacity: 0.7;
  cursor: wait;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 0.95rem;
  color: var(--text-muted);
}

.auth-footer a {
  color: var(--primary);
  font-weight: 700;
}
</style>
