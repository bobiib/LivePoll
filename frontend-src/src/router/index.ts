import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import { isAuthenticated } from '../lib/auth'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView,
        },
        {
            path: '/login',
            name: 'login',
            component: LoginView,
            meta: { guestOnly: true },
        },
        {
            path: '/register',
            name: 'register',
            component: RegisterView,
            meta: { guestOnly: true },
        },
        {
            path: '/room/:id',
            name: 'participant-room',
            component: () => import('../views/ParticipantRoomView.vue'),
        },
        {
            path: '/create',
            name: 'host-create',
            component: () => import('../views/HostCreateView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/polls/:id/edit',
            name: 'host-edit-poll',
            component: () => import('../views/HostCreateView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/polls',
            name: 'host-polls',
            component: () => import('../views/HostPollsView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/polls/:id/history',
            name: 'host-poll-history',
            component: () => import('../views/HostPollHistoryView.vue'),
            meta: { requiresAuth: true },
        },
        // NEU HINZUGEFÜGT: Die Live-Ansicht für den Host
        {
            path: '/live/:id',
            name: 'host-live',
            component: () => import('../views/HostLiveView.vue'),
            meta: { requiresAuth: true },
        },
    ],
})

router.beforeEach((to) => {
    if (to.meta.requiresAuth && !isAuthenticated()) {
        return {
            path: '/login',
            query: { redirect: to.fullPath },
        }
    }

    if (to.meta.guestOnly && isAuthenticated()) {
        return { path: '/polls' }
    }
})

export default router
