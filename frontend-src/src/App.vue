<script setup>
import Navbar from './components/Navbar.vue';
</script>

<template>
  <div class="app-layout">
    <Navbar />
    <router-view />
  </div>
</template>

<style>
:root {
  --primary: #6366f1;
  --primary-dark: #4f46e5;
  --secondary: #ffffff;
  --bg: #f8fafc;
  --text-main: #0f172a;
  --text-muted: #64748b;
  --border: #e2e8f0;
  --radius: 14px;
  --shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background-color: var(--bg);
  color: var(--text-main);
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

/* Hilfsklassen für Icons */
.icon-xs { width: 14px; height: 14px; }
.icon-sm { width: 20px; height: 20px; opacity: 0.7; }
.icon-white { filter: brightness(0) invert(1); opacity: 1; }

button, input {
  font-family: inherit;
  outline: none;
}

a {
  text-decoration: none;
  transition: all 0.2s ease;
}
</style>