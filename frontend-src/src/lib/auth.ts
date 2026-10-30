import { reactive } from 'vue'
import { ApiError, INTERNAL_ERROR_CODES, parseRetryAfterSeconds } from './errors'

type ApiResponse<T> = {
  successful: boolean
  message: string
  data: (T & { errorCode?: string }) | null
}

type AuthResponse = {
  JWTToken: string
  email: string
}

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const TOKEN_KEY = 'livepoll.jwt'
const EMAIL_KEY = 'livepoll.email'

export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  email: localStorage.getItem(EMAIL_KEY) || '',
  ready: false,
})

function persistAuth(token: string, email: string) {
  authState.token = token
  authState.email = email
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(EMAIL_KEY, email)
}

export function clearAuth() {
  authState.token = ''
  authState.email = ''
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EMAIL_KEY)
}

export function isAuthenticated() {
  return authState.token.length > 0
}

export async function request<T>(path: string, options: RequestInit = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(authState.token ? { Authorization: `Bearer ${authState.token}` } : {}),
      ...(options.headers || {}),
    },
  })

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null

  if (!response.ok || !payload?.successful) {
    throw new ApiError(
      payload?.message || INTERNAL_ERROR_CODES.requestFailed,
      response.status,
      parseRetryAfterSeconds(response.headers),
      payload?.data?.errorCode || '',
    )
  }

  return payload
}

export async function login(email: string, password: string) {
  const payload = await request<AuthResponse>('/api/user/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingAuthData)
  }

  persistAuth(payload.data.JWTToken, payload.data.email)
  return payload
}

export async function register(email: string, password: string) {
  const payload = await request<AuthResponse>('/api/user/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingAuthData)
  }

  persistAuth(payload.data.JWTToken, payload.data.email)
  return payload
}

export async function initializeAuth() {
  if (!authState.token) {
    authState.ready = true
    return
  }

  try {
    await request<void>('/api/user/auth', { method: 'GET' })
  } catch {
    clearAuth()
  } finally {
    authState.ready = true
  }
}
