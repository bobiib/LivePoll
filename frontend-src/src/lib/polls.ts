import { request } from './auth'
import { INTERNAL_ERROR_CODES } from './errors'

export type PollQuestionType = 'single' | 'multi' | 'text'

export type PollQuestion = {
  title: string
  type: PollQuestionType
  options: string[]
  maxAnswers: number
  votes: number[]
  answers: string[]
}

export type PollQuestionTemplate = {
  title: string
  type: PollQuestionType
  options: string[]
  maxAnswers: number
}

export type SavedPoll = {
  id: string
  title: string
  roomCode: string
  status: 'LOBBY' | 'LIVE' | 'FINISHED' | string
  activeQuestionIndex: number
  questionCount: number
  questions: PollQuestionTemplate[]
}

export type PollRequest = {
  title: string
  questions: PollQuestionTemplate[]
}

export type CreatePollResponse = {
  roomCode: string
  pollId: string
}

export type PollDetails = {
  title: string
  status: 'LOBBY' | 'LIVE' | 'FINISHED' | string
  activeQuestionIndex: number
  questions: PollQuestion[]
}


export type PollDetails = {
  title: string
  status: 'LOBBY' | 'LIVE' | 'FINISHED' | string
  activeQuestionIndex: number
  questions: PollQuestion[]
}

export type PollMetadata = {
  title: string
  status: 'LOBBY' | 'LIVE' | 'FINISHED' | string
  activeQuestionIndex: number
}

export type QuestionResults = {
  questionIndex: number
  type: PollQuestionType
  votes: number[]
  answers: string[]
}

export type PollSession = {
  id: string
  roomCode: string
  createdAt: string
}

function normalizeSavedPoll(poll: SavedPoll) {
  return {
    ...poll,
    activeQuestionIndex: poll.activeQuestionIndex || 0,
    questionCount: poll.questionCount || poll.questions?.length || 0,
    questions: (poll.questions || []).map((question) => ({
      ...question,
      options: question.options || [],
      maxAnswers: question.maxAnswers || 1,
    })),
  }
}

export async function createPoll(poll: PollRequest) {
  const payload = await request<CreatePollResponse>('/api/polls', {
    method: 'POST',
    body: JSON.stringify(poll),
  })

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingPollStartData)
  }

  return payload.data
}

export async function getPollDetails(roomCode: string) {
  const payload = await request<PollDetails>(`/api/polls/${roomCode.trim().toUpperCase()}/details`)

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingPollData)
  }

  return {
    ...payload.data,
    activeQuestionIndex: payload.data.activeQuestionIndex || 0,
    questions: payload.data.questions.map((question) => ({
      ...question,
      options: question.options || [],
      maxAnswers: question.maxAnswers || 1,
      votes: question.votes || [],
      answers: question.answers || [],
    })),
  }
}

export async function getSavedPolls() {
  const payload = await request<SavedPoll[]>('/api/polls/get-polls')
  return (payload.data || []).map(normalizeSavedPoll)
}

export async function getPollHistory(pollId: string) {
  const payload = await request<PollSession[]>(`/api/polls/${pollId}/sessions`)
  return payload.data || []
}

export async function getSavedPoll(pollId: string) {
  const payload = await request<SavedPoll>(`/api/polls/saved/${pollId}`)

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingPollData)
  }

  return normalizeSavedPoll(payload.data)
}

export async function startSavedPoll(pollId: string) {
  const payload = await request<CreatePollResponse>(`/api/polls/${pollId}/start`, {
    method: 'POST',
  })

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingPollStartData)
  }

  return payload.data
}

export async function updateSavedPoll(pollId: string, poll: PollRequest) {
  const payload = await request<SavedPoll>(`/api/polls/${pollId}`, {
    method: 'PUT',
    body: JSON.stringify(poll),
  })

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingPollData)
  }

  return normalizeSavedPoll(payload.data)
}

export async function deleteSavedPoll(pollId: string) {
  return request<void>(`/api/polls/${pollId}`, {
    method: 'DELETE',
  })
}

export async function validateRoom(roomCode: string) {
  const payload = await request<PollMetadata>(`/api/polls/${roomCode.trim().toUpperCase()}`)

  if (!payload.data) {
    throw new Error(INTERNAL_ERROR_CODES.missingRoomData)
  }


  return {
    ...payload.data,
    activeQuestionIndex: payload.data.activeQuestionIndex || 0,
  }
}

export async function submitVote(roomCode: string, questionIndex: number, answer: string | number | number[], participantId: string) {
  const body = Array.isArray(answer)
    ? { questionIndex, optionIndexes: answer, participantId }
    : typeof answer === 'number'
      ? { questionIndex, optionIndex: answer, participantId }
      : { questionIndex, answer, participantId }

  return request<QuestionResults>(`/api/polls/${roomCode.trim().toUpperCase()}/vote`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
