<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { formatMessage, t } from '../i18n';
import { displayError } from '../lib/errors';
import { createPoll, getSavedPoll, updateSavedPoll } from '../lib/polls';
import QuestionCard from '../components/QuestionCard.vue';

const route = useRoute();
const router = useRouter();

const pollTitle = ref('');
const errorMessage = ref('');
const isLoading = ref(false);
const initialLoadFailed = ref(false);
const submittingAction = ref('');

const pollId = computed(() => (typeof route.params.id === 'string' ? route.params.id : ''));
const isEditMode = computed(() => Boolean(pollId.value));
const pageTitle = computed(() => isEditMode.value ? t.value.create.editTitle : t.value.create.title);
const pageSub = computed(() => isEditMode.value ? t.value.create.editSub : t.value.create.sub);
const isSubmitting = computed(() => Boolean(submittingAction.value));
const submitText = computed(() => {
  if (submittingAction.value) {
    return isEditMode.value ? t.value.create.savingChanges : t.value.create.creating;
  }

  return isEditMode.value ? t.value.create.saveChanges : t.value.create.startPoll;
});
const saveOnlyText = computed(() => (
    submittingAction.value === 'save' ? t.value.create.savingPoll : t.value.create.savePoll
));

const createQuestion = (question = {}) => ({
  id: `${Date.now()}-${Math.random()}`,
  title: question.title || '',
  type: question.type || 'single',
  options: question.options?.length ? [...question.options] : ['', ''],
  maxAnswers: question.maxAnswers || (question.type === 'multi' ? 2 : 1)
});

const questions = ref([createQuestion()]);



const addQuestion = () => {
  questions.value.push(createQuestion());
};

const removeQuestion = (index) => {
  if (questions.value.length > 1) {
    questions.value.splice(index, 1);
    errorMessage.value = '';
  } else {
    errorMessage.value = t.value.create.minOneQuestion;
  }
};

const loadPollForEdit = async () => {
  if (!isEditMode.value) return;

  isLoading.value = true;
  errorMessage.value = '';
  initialLoadFailed.value = false;

  try {
    const poll = await getSavedPoll(pollId.value);
    pollTitle.value = poll.title;
    questions.value = poll.questions.length ? poll.questions.map(createQuestion) : [createQuestion()];
  } catch (error) {
    initialLoadFailed.value = true;
    errorMessage.value = displayError(error, t.value.polls.loadFailed);
  } finally {
    isLoading.value = false;
  }
};

const submitPoll = async (action = 'start') => {
  errorMessage.value = '';
  const title = pollTitle.value.trim();
  const preparedQuestions = questions.value.map(q => {
    const options = q.type === 'text' ? [] : q.options.map(opt => opt.trim()).filter(Boolean);
    const requestedMaxAnswers = Math.max(1, Number(q.maxAnswers) || 1);

    return {
      title: q.title.trim(),
      type: q.type,
      options,
      maxAnswers: q.type === 'single'
          ? 1
          : q.type === 'multi'
              ? Math.min(options.length, requestedMaxAnswers)
              : requestedMaxAnswers
    };
  });

  if (!title) {
    errorMessage.value = t.value.create.missingPollName;
    return;
  }

  if (preparedQuestions.some(q => !q.title)) {
    errorMessage.value = t.value.create.missingQuestionTitles;
    return;
  }

  if (preparedQuestions.some(q => q.type !== 'text' && q.options.length < 2)) {
    errorMessage.value = t.value.create.missingOptions;
    return;
  }

  submittingAction.value = action;

  try {
    if (isEditMode.value) {
      await updateSavedPoll(pollId.value, { title, questions: preparedQuestions });
      router.push('/polls');
      return;
    }

    const payload = await createPoll({ title, questions: preparedQuestions });

    if (action === 'save') {
      router.push('/polls');
      return;
    }

    if (payload.roomCode) {
      router.push(`/live/${payload.roomCode}`);
    } else {
      errorMessage.value = t.value.create.missingRoomCode;
    }
  } catch (error) {
    const fallback = isEditMode.value ? t.value.create.updateFailed : t.value.create.createFailed;
    console.error(fallback, error);
    errorMessage.value = `${fallback} ${displayError(error, t.value.common.unknownError)}`;
  } finally {
    submittingAction.value = '';
  }
};

onMounted(loadPollForEdit);
</script>

<template>
  <main class="create-page">
    <div class="content-wrapper">

      <header class="create-header">
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageSub }}</p>

        <input
            v-model="pollTitle"
            type="text"
            class="poll-title-input"
            :placeholder="t.create.pollName"
        />
      </header>

      <div v-if="isLoading" class="state-box">{{ t.create.loadingPoll }}</div>
      <div v-else-if="initialLoadFailed" class="state-box">
        <p class="form-error">{{ errorMessage }}</p>
      </div>

      <template v-else>
      <div class="questions-list">
        <QuestionCard
            v-for="(q, qIndex) in questions"
            :key="q.id"
            :question="q"
            :index="qIndex"
            @remove="removeQuestion(qIndex)"
        />
      </div>

      <button @click="addQuestion" class="btn-add-q">
        <img src="@/assets/icons/plus-circle.svg" class="icon-sm" alt="+">
        {{ t.create.addQuestion }}
      </button>

      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
      </template>

    </div>

    <div v-if="!isLoading && !initialLoadFailed" class="bottom-bar">
      <div class="bottom-content">
        <span class="q-count">{{ formatMessage(t.create.questionCount, { count: questions.length }) }}</span>
        <div class="bottom-actions">
          <button v-if="!isEditMode" @click="submitPoll('save')" class="btn-save" :disabled="isSubmitting">
            <img src="@/assets/icons/save.svg" class="icon-sm" alt="">
            {{ saveOnlyText }}
          </button>
          <button @click="submitPoll('start')" class="btn-start" :disabled="isSubmitting">
            <img v-if="isEditMode" src="@/assets/icons/edit.svg" class="icon-sm icon-white" alt="">
            <img v-else src="@/assets/icons/play.svg" class="icon-sm icon-white" alt=">">
            {{ submitText }}
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.create-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: calc(100vh - 72px);
  padding: 40px 24px 120px 24px; /* Unten viel Platz für die Sticky Bar */
}

.content-wrapper {
  width: 100%;
  max-width: 760px;
}

.create-header {
  text-align: center;
  margin-bottom: 40px;
}

.create-header h1 {
  font-size: 2.5rem;
  font-weight: 800;
  letter-spacing: 0;
}

.create-header p {
  color: var(--text-muted);
  font-size: 1.1rem;
  margin-bottom: 24px;
}

.poll-title-input {
  width: 100%;
  font-size: 1.8rem;
  font-weight: 800;
  text-align: center;
  border: none;
  background: transparent;
  padding: 10px;
  border-bottom: 2px dashed var(--border);
  color: var(--text-main);
}

.poll-title-input:focus {
  outline: none;
  border-bottom-color: var(--primary);
}

.state-box {
  background: white;
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: var(--shadow);
  color: var(--text-muted);
  font-weight: 800;
  padding: 40px;
  text-align: center;
}

/* Karten System */
.questions-list {

.q-number {
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  font-size: 0.85rem;
  letter-spacing: 0.05em;
}

.btn-icon-danger {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  opacity: 0.5;
  transition: all 0.2s;
}

.btn-icon-danger:hover {
  background: #fef2f2;
  opacity: 1;
}

.q-title-input {
  width: 100%;
  font-size: 1.4rem;
  font-weight: 700;
  border: none;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
}
}

.text-settings-area label {
  font-weight: 600;
  color: var(--text-main);
}

.number-input {
  width: 80px;
  padding: 10px;
  border: 2px solid var(--border);
  border-radius: 8px;
  text-align: center;
  font-weight: 700;
  font-size: 1.1rem;
}

.form-error {
  color: #dc2626;
  font-weight: 700;
  margin-top: 18px;
  text-align: center;
}

/* Big Add Button */
.btn-add-q {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 24px;
  margin-top: 24px;
  background: transparent;
  border: 2px dashed var(--border);
  border-radius: 20px;
  color: var(--text-muted);
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-q:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(99, 102, 241, 0.05);
}

/* Sticky Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--border);
  padding: 16px 24px;
  display: flex;
  justify-content: center;
  z-index: 100;
}

.bottom-content {
  width: 100%;
  max-width: 760px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.q-count {
  font-weight: 700;
  color: var(--text-muted);
  white-space: nowrap;
}

.bottom-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-save,
.btn-start {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  border-radius: 12px;
  font-weight: 800;
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-save {
  background: white;
  border: 2px solid var(--border);
  color: var(--text-main);
}

.btn-save:hover {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-2px);
}

.btn-start {
  background: var(--primary);
  border: none;
  color: white;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}

.btn-start:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
}

.btn-save:disabled,
.btn-start:disabled {
  opacity: 0.7;
  cursor: wait;
  transform: none;
}

@media (max-width: 720px) {
  .bottom-content {
    align-items: stretch;
    flex-direction: column;
  }

  .bottom-actions {
    flex-direction: column-reverse;
  }

  .btn-save,
  .btn-start {
    justify-content: center;
    width: 100%;
  }
}
</style>
