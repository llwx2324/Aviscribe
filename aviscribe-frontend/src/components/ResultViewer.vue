<template>
  <div class="result-viewer-container">
    <div class="result-header">
      <div class="header-left">
        <el-button @click="goBack" circle plain>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2 class="result-title">{{ title }}</h2>
      </div>
      <div class="header-actions">
        <el-button 
          type="primary" 
          @click="copyResult" 
          :disabled="!formattedText"
        >
          <el-icon style="margin-right: 6px"><CopyDocument /></el-icon>
          复制内容
        </el-button>
      </div>
    </div>

    <el-alert 
      v-if="error" 
      :title="error" 
      type="error" 
      show-icon 
      style="margin-bottom: 20px;"
    />

    <el-card class="result-card" :body-style="{ padding: '0' }">
      <div class="markdown-wrapper">
        <div class="markdown-body" v-html="formattedHtml"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { defineProps, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowLeft, CopyDocument } from '@element-plus/icons-vue';
import MarkdownIt from 'markdown-it';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
});

const props = defineProps({
  title: { type: String, required: true },
  formattedText: { type: String, default: '' },
  error: { type: String, default: null }
});

const formattedHtml = computed(() => {
  return props.formattedText ? md.render(props.formattedText) : '<div class="empty-state">排版文档生成中或失败。</div>';
});

const copyResult = () => {
  navigator.clipboard.writeText(props.formattedText).then(() => {
    ElMessage.success('排版内容已复制到剪贴板');
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制');
  });
};

const backTarget = computed(() => route.meta.backTo || '/app/tasks');

const goBack = () => {
  router.push(backTarget.value);
};
</script>

<style scoped>
.result-viewer-container {
  max-width: 1000px;
  margin: 0 auto;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.result-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}


.result-card {
  border: none !important;
  box-shadow: var(--shadow-md) !important;
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 0;
}

.result-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0 !important;
  min-height: 0;
}

.markdown-wrapper {
  padding: 40px;
  background-color: #ffffff;
  flex: 1;
  box-sizing: border-box;
  width: 100%;
  overflow-y: auto;
  min-height: 0;
}

.empty-state {
  color: var(--text-secondary);
  text-align: center;
  padding: 40px;
  font-style: italic;
}

@media (max-width: 768px) {
  .markdown-wrapper {
    padding: 24px;
  }
}
</style>