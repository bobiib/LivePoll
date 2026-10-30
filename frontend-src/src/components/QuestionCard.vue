<script setup>
import { formatMessage, t } from '../i18n';

const props = defineProps({
  question: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['remove']);

const ensureChoiceOptions = (question) => {
  if (!question.options?.length) {
    question.options = ['', ''];
  }
  while (question.options.length < 2) {
    question.options.push('');
  }
};

const setQuestionType = (type) => {
  const q = props.question;
  q.type = type;

  if (type === 'single') {
    ensureChoiceOptions(q);
    q.maxAnswers = 1;
  } else if (type === 'multi') {
    ensureChoiceOptions(q);
    q.maxAnswers = Math.max(2, Number(q.maxAnswers) || 2);
  } else {
    q.maxAnswers = Math.max(1, Number(q.maxAnswers) || 1);
  }
};

const addOption = () => {
  props.question.options.push('');
};

const removeOption = (optIndex) => {
  if (props.question.options.length > 2) {
    props.question.options.splice(optIndex, 1);
  }
};
</script>

<template>
  <div class="q-card">
    <div class="q-card-header">
      <span class="q-number">{{ formatMessage(t.create.questionLabel, { number: index + 1 }) }}</span>
      <button @click="$emit('remove')" class="btn-icon-danger" :title="t.create.delete">
        <img src="@/assets/icons/trash.svg" class="icon-sm" alt="x">
      </button>
    </div>

    <input
        v-model="question.title"
        type="text"
        class="q-title-input"
        :placeholder="t.create.qTitle"
    />

    <div class="type-selector">
      <button :class="{ active: question.type === 'single' }" @click="setQuestionType('single')">
        {{ t.create.typeSingle }}
      </button>
      <button :class="{ active: question.type === 'multi' }" @click="setQuestionType('multi')">
        {{ t.create.typeMulti }}
      </button>
      <button :class="{ active: question.type === 'text' }" @click="setQuestionType('text')">
        {{ t.create.typeText }}
      </button>
    </div>

    <div v-if="question.type === 'single' || question.type === 'multi'" class="options-area">
      <div v-for="(opt, optIndex) in question.options" :key="optIndex" class="option-row">
        <div class="opt-indicator" :class="question.type"></div>
        <input
            v-model="question.options[optIndex]"
            type="text"
            :placeholder="t.create.optionText"
            class="opt-input"
        />
        <button v-if="question.options.length > 2" @click="removeOption(optIndex)" class="btn-icon">
          <img src="@/assets/icons/trash.svg" class="icon-xs" alt="x">
        </button>
      </div>

      <button @click="addOption" class="btn-text">
        <img src="@/assets/icons/plus-circle.svg" class="icon-xs" alt="+">
        {{ t.create.addOption }}
      </button>
    </div>

    <div v-if="question.type === 'text' || question.type === 'multi'" class="text-settings-area">
      <label>{{ t.create.maxAnswers }}</label>
      <input
          v-model.number="question.maxAnswers"
          type="number"
          min="1"
          :max="question.type === 'multi' ? question.options.length : 10"
          class="number-input"
      />
    </div>
  </div>
</template>

<style scoped>
.q-card {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
  transition: all 0.3s ease;
}

.q-card:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.q-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

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

.q-title-input:focus {
  outline: none;
  background: #eef2ff;
}

/* Type Selector (Pills) */
.type-selector {
  display: flex;
  background: #f1f5f9;
  padding: 6px;
  border-radius: 12px;
  margin-bottom: 24px;
  gap: 4px;
}

.type-selector button {
  flex: 1;
  background: transparent;
  border: none;
  padding: 10px;
  border-radius: 8px;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.type-selector button.active {
  background: white;
  color: var(--primary);
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

/* Optionen Area */
.options-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.opt-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border);
  flex-shrink: 0;
}

.opt-indicator.single { border-radius: 50%; }
.opt-indicator.multi { border-radius: 6px; }

.opt-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid var(--border);
  border-radius: 10px;
  font-size: 1rem;
  transition: all 0.2s;
}

.opt-input:focus {
  border-color: var(--primary);
  outline: none;
}

.btn-icon {
  background: none;
  border: none;
  padding: 10px;
  cursor: pointer;
  opacity: 0.4;
}

.btn-icon:hover { opacity: 1; color: #ef4444; }

.btn-text {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: var(--primary);
  font-weight: 700;
  padding: 12px 0;
  cursor: pointer;
  width: max-content;
}

.btn-text:hover { text-decoration: underline; }

/* Text Settings */
.text-settings-area {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
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
</style>
