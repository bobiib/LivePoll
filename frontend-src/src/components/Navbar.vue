<script setup>
import { useRouter } from 'vue-router'
import { i18nState, toggleLang, t } from '../i18n'
import { authState, clearAuth } from '../lib/auth'

const router = useRouter()

const handleLogout = () => {
  clearAuth()
  router.push('/')
}
</script>

<template>
  <nav class="navbar">
    <div class="nav-content">
      <router-link to="/" class="logo">
        <img src="@/assets/icons/logo.svg" class="logo-img" alt="Logo">
        <span>LivePoll</span>
      </router-link>

      <div class="nav-links">
        <button @click="toggleLang" class="lang-switch">
          <img src="@/assets/icons/globe.svg" class="icon-xs" :alt="t.nav.language">
          {{ i18nState.lang.toUpperCase() }}
        </button>

        <template v-if="authState.token">
          <span class="user-pill">{{ authState.email }}</span>

          <router-link to="/polls" class="nav-item">
            <img src="@/assets/icons/hash.svg" class="icon-sm" :alt="t.nav.polls">
            <span>{{ t.nav.polls }}</span>
          </router-link>

          <router-link to="/create" class="nav-item">
            <img src="@/assets/icons/plus-circle.svg" class="icon-sm" :alt="t.nav.create">
            <span>{{ t.nav.create }}</span>
          </router-link>

          <button @click="handleLogout" class="nav-item nav-button">
            <img src="@/assets/icons/login.svg" class="icon-sm" :alt="t.nav.logout">
            <span>{{ t.nav.logout }}</span>
          </button>
        </template>

        <template v-else>
          <router-link to="/login" class="nav-item">
            <img src="@/assets/icons/login.svg" class="icon-sm" :alt="t.nav.login">
            <span>{{ t.nav.login }}</span>
          </router-link>

          <router-link to="/register" class="nav-cta">
            <img src="@/assets/icons/register.svg" class="icon-sm icon-white" :alt="t.nav.register">
            <span>{{ t.nav.register }}</span>
          </router-link>
        </template>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 72px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: center;
}

.nav-content {
  width: 100%;
  max-width: 1200px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--text-main);
  letter-spacing: -1px;
  text-decoration: none;
}

.logo-img {
  width: 32px;
  height: 32px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-pill {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef2ff;
  color: var(--primary-dark);
  font-size: 0.85rem;
  font-weight: 700;
}

.lang-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px solid var(--border);
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.lang-switch:hover {
  background-color: #f1f5f9;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-weight: 600;
  font-size: 0.95rem;
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 8px;
}

.nav-button {
  background: none;
  border: none;
  cursor: pointer;
}

.nav-item:hover {
  color: var(--primary);
  background-color: rgba(99, 102, 241, 0.05);
}

.nav-cta {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--primary);
  color: white;
  padding: 10px 18px;
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.95rem;
  text-decoration: none;
  box-shadow: 0 4px 6px -1px rgba(99, 102, 241, 0.2);
}

.nav-cta:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.3);
}

.nav-cta:active {
  transform: translateY(0);
}

.icon-xs { width: 14px; height: 14px; }
.icon-sm { width: 18px; height: 18px; }
.icon-white { filter: brightness(0) invert(1); }

@media (max-width: 720px) {
  .navbar {
    height: 64px;
  }

  .nav-content {
    gap: 10px;
    padding: 0 12px;
  }

  .logo {
    flex-shrink: 0;
    gap: 8px;
    font-size: 1.1rem;
    letter-spacing: 0;
  }

  .logo span {
    display: none;
  }

  .logo-img {
    width: 30px;
    height: 30px;
  }

  .nav-links {
    flex: 1;
    justify-content: flex-end;
    gap: 6px;
    min-width: 0;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }

  .user-pill {
    display: none;
  }

  .lang-switch,
  .nav-item,
  .nav-cta {
    flex-shrink: 0;
    min-width: 42px;
    min-height: 42px;
    justify-content: center;
    border-radius: 12px;
  }

  .lang-switch {
    padding: 8px 9px;
  }

  .nav-item {
    padding: 10px;
  }

  .nav-item span,
  .nav-cta span {
    display: none;
  }

  .nav-cta {
    padding: 10px;
  }
}
</style>
