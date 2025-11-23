<template>
  <div class="task-list-view">
    <div class="page-header">
      <h1>任务列表</h1>
      <div class="page-actions">
        <el-input
          v-model="searchTerm"
          placeholder="搜索任务名称或 ID"
          clearable
          class="task-search"
          size="large"
          :prefix-icon="Search"
        />
        <el-button type="primary" class="new-task-btn" @click="$router.push('/app')">
          <el-icon style="margin-right: 6px"><Plus /></el-icon>新建任务
        </el-button>
      </div>
    </div>

    <el-card class="table-card" :body-style="{ padding: '0' }">
      <el-table 
        :data="filteredTasks" 
        v-loading="loading" 
        element-loading-text="加载任务中..."
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: '600' }"
        empty-text="未找到匹配的任务"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <el-table-column label="任务名称" min-width="250" align="center">
          <template #default="scope">
            <div class="task-name-cell">
              <el-icon class="task-icon"><VideoPlay /></el-icon>
              <span class="task-name-text">{{ getTaskDisplayName(scope.row) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="taskStatusText" label="状态" width="140" align="center">
          <template #default="scope">
            <el-tag 
              :type="getStatusTagType(scope.row.taskStatus)" 
              effect="light"
              round
              class="status-tag"
            >
              {{ scope.row.taskStatusText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="durationSeconds" label="时长" width="120" align="center">
          <template #default="scope">
            <span v-if="scope.row.durationSeconds !== null && scope.row.durationSeconds !== undefined">
              {{ formatDuration(scope.row.durationSeconds) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180" class-name="text-muted" align="center" />
        
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button 
              class="view-result-btn"
              size="small"
              :disabled="scope.row.taskStatus !== 6"
              @click="viewResult(scope.row.id)"
            >
              查看结果
            </el-button>
            <el-popconfirm
              title="确认删除该任务吗？相关文件也将被删除！"
              @confirm="handleDelete(scope.row.id)"
              confirm-button-text="确认"
              cancel-button-text="取消"
              width="220"
            >
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getTaskList, deleteTask } from '@/api/task';
import { ElMessage } from 'element-plus';
import { Plus, VideoPlay, Search } from '@element-plus/icons-vue';

const router = useRouter();
const tasks = ref([]);
const loading = ref(true);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const searchTerm = ref('');
let timer = null; // 用于轮询的计时器

const filteredTasks = computed(() => {
  const keyword = searchTerm.value.trim().toLowerCase();
  if (!keyword) return tasks.value;
  return tasks.value.filter(task => {
    const name = getTaskDisplayName(task).toLowerCase();
    const idMatch = String(task.id || '').includes(keyword);
    return name.includes(keyword) || idMatch;
  });
});

onMounted(() => {
  fetchTasks();
  // 开启每 10 秒轮询一次，用于更新处理中的任务状态
  timer = setInterval(fetchTasks, 10000); 
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
  }
});

const fetchTasks = async () => {
  // 只有第一次加载显示 loading，轮询时不显示
  if (tasks.value.length === 0) loading.value = true;
  
  try {
    const response = await getTaskList(currentPage.value - 1, pageSize.value);
    // 后端返回 Page 结构，包含 records, total
    tasks.value = response.records; 
    total.value = response.total;
  } catch (error) {
    console.error('获取任务列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (newPage) => {
  currentPage.value = newPage;
  loading.value = true; // 翻页时显示 loading
  fetchTasks();
};

const getStatusTagType = (status) => {
  switch (status) {
    case 6: // COMPLETED
      return 'success';
    case 7: // FAILED
      return 'danger';
    case 1: // PENDING
    case 2: // DOWNLOADING
      return 'info';
    default: // PROCESSING
      return 'warning'; // 默认处理中为黄色
  }
};

const formatDuration = (seconds) => {
  const totalSeconds = Number(seconds);
  if (Number.isNaN(totalSeconds) || totalSeconds < 0) {
    return '-';
  }
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  if (h > 0) return `${h}h ${m}m ${s}s`;
  return `${m}m ${s}s`;
};

const getTaskDisplayName = (task) => {
  if (!task) {
    return '未命名任务';
  }
  const name = typeof task.taskName === 'string' ? task.taskName.trim() : '';
  return name || '未命名任务';
};

const viewResult = (id) => {
  router.push({ name: 'taskResult', params: { id } });
};

const handleDelete = async (id) => {
  try {
    await deleteTask(id);
    ElMessage.success(`任务 ${id} 删除成功`);
    fetchTasks(); // 刷新列表
  } catch (error) {
    console.error('删除任务失败', error);
  }
};
</script>

<style scoped>
.task-list-view {
  padding-bottom: 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  margin: 0;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-search {
  width: 260px;
}

.table-card {
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.task-name-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.task-icon {
  font-size: 18px;
  color: var(--text-secondary);
}

.task-name-text {
  font-weight: 500;
  color: var(--text-main);
}

.status-tag {
  min-width: 80px;
  text-align: center;
}

.text-muted {
  color: var(--text-secondary);
}

.pagination-wrapper {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-color);
}

.new-task-btn {
  background: var(--el-color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  height: 40px;
  font-weight: 500;
  box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.2);
  transition: all 0.3s ease;
}

.new-task-btn:hover {
  box-shadow: 0 6px 12px -2px rgba(59, 130, 246, 0.3);
  background: var(--el-color-primary-dark-2, #1d4ed8);
}

.view-result-btn {
  background: var(--el-color-primary);
  border: none;
  color: #fff;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  transition: all 0.2s;
}

.view-result-btn:hover:not(:disabled) {
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  background: var(--el-color-primary-dark-2, #1d4ed8);
  color: #fff;
}

.view-result-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}
</style>