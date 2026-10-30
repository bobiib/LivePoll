export const INTERNAL_ERROR_CODES = {
  requestFailed: 'REQUEST_FAILED',
  missingAuthData: 'MISSING_AUTH_DATA',
  missingPollData: 'MISSING_POLL_DATA',
  missingPollStartData: 'MISSING_POLL_START_DATA',
  missingRoomData: 'MISSING_ROOM_DATA',
} as const

const internalErrorCodes = new Set<string>(Object.values(INTERNAL_ERROR_CODES))

export class ApiError extends Error {
  status: number
  retryAfterSeconds: number | null
  errorCode: string

  constructor(message: string, status: number, retryAfterSeconds: number | null, errorCode = '') {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.retryAfterSeconds = retryAfterSeconds
    this.errorCode = errorCode
  }
}

export function parseRetryAfterSeconds(headers: Headers) {
  const retryAfter = headers.get('X-Rate-Limit-Retry-After-Seconds') || headers.get('Retry-After')

  if (!retryAfter) {
    return null
  }

  const seconds = Number(retryAfter)

  if (Number.isFinite(seconds)) {
    return Math.max(0, Math.ceil(seconds))
  }

  const timestamp = Date.parse(retryAfter)

  if (Number.isFinite(timestamp)) {
    return Math.max(0, Math.ceil((timestamp - Date.now()) / 1000))
  }

  return null
}

export function isRateLimitError(error: unknown): error is ApiError {
  return error instanceof ApiError && error.status === 429
}

export function getRetryAfterSeconds(error: unknown, fallback = 60) {
  if (isRateLimitError(error) && error.retryAfterSeconds !== null) {
    return error.retryAfterSeconds
  }

  return fallback
}

export function displayError(error: unknown, fallback: string) {
  if (error instanceof Error && !internalErrorCodes.has(error.message)) {
    return error.message
  }

  return fallback
}
