import { computed, onUnmounted, ref } from 'vue'

export function useRetryCooldown() {
  const retrySeconds = ref(0)
  const isRetrying = computed(() => retrySeconds.value > 0)
  let retryTimer: ReturnType<typeof window.setInterval> | null = null

  const clearRetryCooldown = () => {
    if (retryTimer !== null) {
      window.clearInterval(retryTimer)
      retryTimer = null
    }

    retrySeconds.value = 0
  }

  const startRetryCooldown = (seconds: number) => {
    clearRetryCooldown()
    retrySeconds.value = Math.max(1, Math.ceil(seconds))

    retryTimer = window.setInterval(() => {
      retrySeconds.value = Math.max(0, retrySeconds.value - 1)

      if (retrySeconds.value === 0 && retryTimer !== null) {
        window.clearInterval(retryTimer)
        retryTimer = null
      }
    }, 1000)
  }

  onUnmounted(clearRetryCooldown)

  return {
    retrySeconds,
    isRetrying,
    startRetryCooldown,
    clearRetryCooldown,
  }
}
