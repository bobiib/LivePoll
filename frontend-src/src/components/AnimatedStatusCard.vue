<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  subtitle?: string
  showDots?: boolean
  compact?: boolean
}>(), {
  subtitle: '',
  showDots: false,
  compact: false,
})
</script>

<template>
  <section class="status-layout" :class="{ compact }">
    <div class="status-card">
      <div class="success-icon-wrapper">
        <svg class="success-checkmark" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52">
          <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none"/>
          <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
        </svg>
      </div>

      <h1 class="status-title">{{ title }}</h1>
      <p v-if="subtitle" class="status-sub">{{ subtitle }}</p>

      <div v-if="showDots" class="waiting-dots">
        <div class="dot"></div>
        <div class="dot"></div>
        <div class="dot"></div>
      </div>

      <div v-if="$slots.actions" class="status-actions">
        <slot name="actions"></slot>
      </div>
    </div>
  </section>
</template>

<style scoped>
.status-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  align-self: center;
  padding: 40px 20px;
  background: radial-gradient(circle at top, #f8fafc 0%, #ffffff 100%);
  animation: fadeIn 0.6s ease-out;
}

.status-layout.compact {
  min-height: 360px;
  padding: 0;
  background: transparent;
}

.status-card {
  width: 100%;
  max-width: 540px;
  padding: 50px 40px;
  text-align: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  box-shadow: 0 20px 40px -15px rgba(99, 102, 241, 0.12), 0 0 0 1px rgba(99, 102, 241, 0.04);
  animation: scaleUp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.compact .status-card {
  padding: 42px 32px;
}

.success-icon-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.success-checkmark {
  width: 80px;
  height: 80px;
  display: block;
  stroke-width: 3.5;
  stroke: #22c55e;
  stroke-miterlimit: 10;
  box-shadow: inset 0 0 0 #22c55e;
  animation: fillAnimation .4s ease-in-out .4s forwards, scaleAnimation .3s ease-in-out .9s both;
  border-radius: 50px;
}

.checkmark-circle {
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  stroke-width: 3.5;
  stroke-miterlimit: 10;
  stroke: #22c55e;
  fill: none;
  animation: strokeAnimation 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

.checkmark-check {
  transform-origin: 50% 50%;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  stroke: #22c55e;
  animation: strokeAnimation 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.6s forwards;
}

.status-title {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 12px;
  letter-spacing: 0;
}

.status-sub {
  font-size: 1.05rem;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 0;
  padding: 0 10px;
}

.waiting-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 28px;
}

.dot {
  width: 12px;
  height: 12px;
  background: #6366f1;
  border-radius: 50%;
  opacity: 0.4;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

.status-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  margin-top: 36px;
}

:slotted(.status-action-primary),
:slotted(.status-action-secondary) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 16px 28px;
  border-radius: 16px;
  font-weight: 700;
  font-size: 1.05rem;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
}

:slotted(.status-action-primary) {
  background: #6366f1;
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

:slotted(.status-action-primary:hover) {
  background: #4f46e5;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.3);
}

:slotted(.status-action-secondary) {
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

:slotted(.status-action-secondary:hover) {
  background: #f1f5f9;
  color: #0f172a;
  border-color: #cbd5e1;
  transform: translateY(-2px);
}

@keyframes strokeAnimation {
  100% {
    stroke-dashoffset: 0;
  }
}

@keyframes scaleAnimation {
  0%, 100% {
    transform: none;
  }
  50% {
    transform: scale3d(1.1, 1.1, 1);
  }
}

@keyframes fillAnimation {
  100% {
    box-shadow: inset 0 0 0 40px rgba(34, 197, 94, 0.08);
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); opacity: 1; }
}
</style>
