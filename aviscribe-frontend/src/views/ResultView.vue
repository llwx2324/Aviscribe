<template>
  <div class="result-view">
    <div v-if="loading" class="loading-wrapper">
      <LoadingSpinner text="正在获取转录结果..." :size="40" />
    </div>
    <div v-else-if="task" class="viewer-wrapper">
      <ResultViewer
        :title="(task.taskName && task.taskName.trim()) || '未命名任务'"
        :formatted-text="task.formattedText"
        :error="task.errorLog"
      />
    </div>
    <div v-else class="error-state">
      <el-empty description="任务结果加载失败或任务不存在">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { getTaskDetail } from '@/api/task';
import ResultViewer from '@/components/ResultViewer.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { ElMessage } from 'element-plus';

const route = useRoute();
const task = ref(null);
const loading = ref(true);

const fetchResult = async (taskId) => {
  if (!taskId) {
    task.value = null;
    loading.value = false;
    return;
  }

  loading.value = true;
  try {
    const result = await getTaskDetail(taskId);
    task.value = result;

    if (result.taskStatus !== 6 && result.taskStatus !== 7) {
      ElMessage.warning('任务尚未完成，请稍后刷新查看最新状态。');
    }
  } catch (error) {
    console.error('获取结果失败', error);
    task.value = null;
  } finally {
    loading.value = false;
  }
};

watch(
  () => [route.params.id, route.query.refresh],
  ([newId]) => {
    fetchResult(newId);
  },
  { immediate: true }
);
</script>

<style scoped>
.result-view {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-bottom: 40px;
  box-sizing: border-box;
}

.viewer-wrapper,
.loading-wrapper,
.error-state {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.viewer-wrapper {
  align-items: stretch;
  justify-content: stretch;
}

.error-state {
  padding: 60px 0;
}

.loading-wrapper {
  min-height: auto;
}
</style>