<template>
  <el-card class="history-card" :body-style="{ padding: '0', height: '100%', display: 'flex', flexDirection: 'column' }">
    <template #header>
      <div class="card-header">
        <div class="card-title">
          <p class="eyebrow">历史记录</p>
          <h3>任务时间线</h3>
        </div>
        <div class="header-actions">
          <el-radio-group v-model="filterType" size="small" @change="handleFilterChange">
            <el-radio-button label="ALL">全部</el-radio-button>
            <el-radio-button label="LOCAL">本地</el-radio-button>
            <el-radio-button label="URL">链接</el-radio-button>
          </el-radio-group>
          <el-button class="refresh-btn" size="small" @click="fetchTasks">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </div>
    </template>
    
    <div class="history-list-container" v-loading="loading">
      <div v-if="tasks.length === 0" class="empty-history">
        Your transcribed files will appear here.
      </div>
      <div v-else class="history-list">
        <div 
          v-for="task in tasks" 
          :key="task.id" 
          class="history-item"
          :class="[{ active: currentTaskId == task.id }, `status-${getStatusClass(task.taskStatus)}`]"
          @click="handleTaskClick(task.id)"
        >
          <div class="item-main">
            <div class="title-wrapper">
              <span class="status-dot"></span>
              <div class="title-block">
                <p class="item-title" :title="task.taskName">{{ getTaskDisplayName(task) }}</p>
                <p class="item-meta-text">{{ formatTime(task.createTime) }} · {{ task.sourceType === SOURCE_TYPE.LOCAL ? '本地上传' : '链接任务' }}</p>
              </div>
            </div>
            <el-tag size="small" :type="getStatusTagType(task.taskStatus)" effect="light" class="status-chip">
              {{ task.taskStatusText }}
            </el-tag>
          </div>
          <div class="item-meta">
            <el-button 
              v-if="task.taskStatus === 6"
              link
              size="small"
              class="icon-button edit-btn"
              @click.stop="openEditDialog(task)"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button 
              v-if="task.taskStatus === 6 || task.taskStatus === 7"
              type="danger" 
              link 
              size="small" 
              @click.stop="handleDelete(task.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      
      <div class="pagination-mini" v-if="total > pageSize">
        <el-pagination
          small
          layout="prev, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </el-card>

  <el-dialog
    v-model="editDialogVisible"
    title="修改任务名称"
    width="420px"
    :close-on-click-modal="false"
  >
    <el-form label-width="80px">
      <el-form-item label="任务名称">
        <el-input
          v-model.trim="editForm.taskName"
          placeholder="请输入任务名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="editDialogVisible = false" :disabled="editLoading">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitTaskName">保存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getTaskList, deleteTask, renameTask } from '@/api/task';
import { ElMessage } from 'element-plus';
import { Refresh, Delete, Edit } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const tasks = ref([]);
const loading = ref(false);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const currentTaskId = ref(null);
const filterType = ref('ALL'); // ALL, LOCAL, URL
const SOURCE_TYPE = Object.freeze({ LOCAL: 1, URL: 2 });
let timer = null;
const editDialogVisible = ref(false);
const editForm = reactive({ id: null, taskName: '' });
const editLoading = ref(false);

// 监听路由变化，高亮当前选中的任务
watch(() => route.params.id, (newId) => {
  currentTaskId.value = newId ? parseInt(newId) : null;
}, { immediate: true });

const fetchTasks = async () => {
  loading.value = true;
  try {
    // 这里假设后端 getTaskList 支持传 sourceType 参数进行筛选，如果不支持，则需要在前端过滤
    // 暂时假设后端不支持，前端过滤（如果数据量大，建议后端支持）
    // 为了演示，这里还是请求所有，然后前端简单过滤（注意：分页会失效，最好后端支持）
    // 如果后端没改接口支持筛选，那只能请求回来后前端 filter，但分页会有问题。
    // 鉴于需求描述，我们先请求回来展示。如果需要严格筛选，请告知后端增加参数。
    
    const response = await getTaskList(currentPage.value - 1, pageSize.value);
    let allTasks = response.records;

    if (filterType.value !== 'ALL') {
      // 注意：这种前端过滤在分页模式下是不准确的，只能过滤当前页的数据
      const targetType = SOURCE_TYPE[filterType.value] ?? null;
      allTasks = allTasks.filter(t => t.sourceType === targetType);
    }

    tasks.value = allTasks;
    total.value = response.total;
  } catch (error) {
    console.error('Failed to fetch tasks:', error);
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  currentPage.value = 1;
  fetchTasks();
};

const handlePageChange = (page) => {
  currentPage.value = page;
  fetchTasks();
};

const handleTaskClick = (id) => {
  if (currentTaskId.value === id) {
    const query = { ...route.query, refresh: Date.now().toString() };
    router.push({ name: 'workspaceResult', params: { id }, query });
    return;
  }
  router.push({ name: 'workspaceResult', params: { id } });
};

const handleDelete = async (id) => {
  try {
    await deleteTask(id);
    ElMessage.success('Deleted');
    fetchTasks();
    if (currentTaskId.value === id) {
      router.push('/app');
    }
  } catch (error) {
    console.error(error);
  }
};

const openEditDialog = (task) => {
  editForm.id = task.id;
  editForm.taskName = getTaskDisplayName(task);
  editDialogVisible.value = true;
};

const submitTaskName = async () => {
  const trimmed = editForm.taskName ? editForm.taskName.trim() : '';
  if (!trimmed) {
    ElMessage.warning('请输入任务名称');
    return;
  }
  editLoading.value = true;
  try {
    await renameTask(editForm.id, trimmed);
    ElMessage.success('任务名称已更新');
    editDialogVisible.value = false;
    fetchTasks();
    if (currentTaskId.value === editForm.id) {
      const query = { ...route.query, refresh: Date.now().toString() };
      router.replace({ name: 'workspaceResult', params: { id: editForm.id }, query });
    }
  } catch (error) {
    console.error(error);
  } finally {
    editLoading.value = false;
  }
};

const STATUS_NAME_MAP = {
  PENDING: 1,
  DOWNLOADING: 2,
  EXTRACTING_AUDIO: 3,
  TRANSCRIBING: 4,
  FORMATTING: 5,
  COMPLETED: 6,
  FAILED: 7
};

const normalizeStatus = (status) => {
  if (typeof status === 'number') {
    return status;
  }
  if (typeof status === 'string') {
    const trimmed = status.trim();
    const numeric = Number(trimmed);
    if (!Number.isNaN(numeric)) {
      return numeric;
    }
    const upper = trimmed.toUpperCase();
    if (STATUS_NAME_MAP[upper]) {
      return STATUS_NAME_MAP[upper];
    }
  }
  return 0;
};

const getStatusTagType = (status) => {
  switch (normalizeStatus(status)) {
    case 6: return 'success';
    case 7: return 'danger';
    case 1: 
    case 2: return 'info';
    default: return 'warning';
  }
};

const getStatusClass = (status) => {
  switch (normalizeStatus(status)) {
    case 6: return 'done';
    case 7: return 'failed';
    case 1:
    case 2: return 'pending';
    default: return 'running';
  }
};

const getTaskDisplayName = (task) => {
  if (!task) {
    return '未命名任务';
  }
  const name = typeof task.taskName === 'string' ? task.taskName.trim() : '';
  return name || '未命名任务';
};

const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  return date.toLocaleDateString();
};

onMounted(() => {
  fetchTasks();
  timer = setInterval(fetchTasks, 15000); // Poll every 15s
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});

// Expose fetchTasks so parent can call it after upload
defineExpose({ fetchTasks });
</script>

<style scoped>
.history-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: none !important;
  box-shadow: var(--shadow-md) !important;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.85), #ffffff);
}

.history-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-main);
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
}

.history-list-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  padding: 0 0 32px 0;
  scroll-padding-bottom: 32px;
  box-sizing: border-box;
}

.empty-history {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}


.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  flex: 1;
}

.history-item {
  padding: 14px 16px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
  background: rgba(255, 255, 255, 0.9);
}

.history-item:hover {
  transform: translateY(-2px);
  border-color: rgba(14, 165, 233, 0.4);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.history-item.active {
  border: 1px solid rgba(59, 130, 246, 0.4);
  box-shadow: 0 8px 22px rgba(37, 99, 235, 0.15);
}

.item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-wrapper {
  display: flex;
  align-items: center;
  overflow: hidden;
  flex: 1;
  margin-right: 8px;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0; /* 允许文本在 Flex 下正确省略而不挤压状态点 */
}

.item-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta-text {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 10px;
  background: #cbd5f5;
  box-shadow: 0 0 6px rgba(59, 130, 246, 0.4);
  flex-shrink: 0; /* 长标题时保持点的宽度 */
}

.status-chip {
  border-radius: 20px;
  border: none;
  font-weight: 600;
}

.icon-button {
  padding: 0 !important;
  min-height: auto;
  color: #475569;
  background: transparent !important;
}

.icon-button :deep(.el-icon) {
  font-size: 16px;
}

.icon-button.edit-btn {
  color: #2563eb;
}

.icon-button.edit-btn:hover {
  color: #1d4ed8;
  background: transparent !important;
}

.history-item.status-done .status-dot {
  background: #22c55e;
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.4);
}

.history-item.status-running .status-dot {
  background: #38bdf8;
  box-shadow: 0 0 10px rgba(56, 189, 248, 0.4);
}

.history-item.status-pending .status-dot {
  background: #fbbf24;
  box-shadow: 0 0 10px rgba(251, 191, 36, 0.4);
}

.history-item.status-failed .status-dot {
  background: #f87171;
  box-shadow: 0 0 10px rgba(248, 113, 113, 0.4);
}

.pagination-mini {
  padding: 4px 8px 0;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--border-color);
  margin-top: auto;
}

.refresh-btn {
  margin-left: 8px;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%);
  color: #fff;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-btn:hover {
  transform: rotate(180deg);
  background: linear-gradient(135deg, #2563eb 0%, #0284c7 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.refresh-btn :deep(.el-icon) {
  font-size: 16px;
}
</style>
