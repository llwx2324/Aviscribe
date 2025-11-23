<template>
  <el-card class="upload-card" :body-style="{ padding: '0' }">
    <div class="card-header-wrapper">
      <div class="card-header">
        <div class="header-context">
          <div class="header-title">
            <el-icon class="header-icon"><VideoCamera /></el-icon>
            <div class="title-copy">
              <p class="eyebrow">Workspace</p>
              <span class="title-main">新建转录任务</span>
            </div>
          </div>
          <p class="header-desc">上传文件或粘贴链接即可启动流程，剩下的交给 Aviscribe。</p>
        </div>
        <el-tag type="primary" effect="light" round>主流音/视频格式（MP4 · MKV · MOV · FLAC · AAC 等），≤500MB</el-tag>
      </div>
    </div>

    <div class="card-body">
      <el-tabs v-model="uploadType" class="custom-tabs">
            <el-tab-pane label="本地文件上传" name="local">
              <el-form :model="localForm" label-position="top" class="upload-form">
            <el-form-item label="选择视频文件">
              <el-upload
                class="upload-demo"
                drag
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                :file-list="fileList"
              >
                <div class="upload-illustration" aria-hidden="true">
                  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                      <linearGradient id="waveGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#5da8ff" />
                        <stop offset="100%" stop-color="#7be3ff" />
                      </linearGradient>
                    </defs>
                    <rect x="10" y="30" width="8" height="60" rx="4" fill="url(#waveGradient)" />
                    <rect x="28" y="15" width="8" height="90" rx="4" fill="url(#waveGradient)" />
                    <rect x="46" y="30" width="8" height="60" rx="4" fill="url(#waveGradient)" />
                    <path d="M70 60h18" stroke="#63b3ff" stroke-width="4" stroke-linecap="round" />
                    <path d="M88 60l-6-6" stroke="#63b3ff" stroke-width="4" stroke-linecap="round" />
                    <path d="M88 60l-6 6" stroke="#63b3ff" stroke-width="4" stroke-linecap="round" />
                    <rect x="96" y="38" width="12" height="5" rx="2.5" fill="#7be3ff" />
                    <rect x="96" y="57" width="12" height="5" rx="2.5" fill="#7be3ff" />
                    <rect x="96" y="76" width="8" height="5" rx="2.5" fill="#7be3ff" />
                  </svg>
                </div>
                <div class="upload-text">
                  <p class="upload-title">拖拽文件上传</p>
                  <p class="upload-subtitle">或 <span>点击选择文件</span></p>
                  <p class="upload-footnote">更建议命名任务，便于后续检索</p>
                </div>
              </el-upload>
            </el-form-item>
            
            <el-form-item label="任务名称">
              <el-input 
                v-model="localForm.title" 
                placeholder="给任务起个名字（可选）" 
                size="large"
              >
                <template #prefix>
                  <el-icon><Edit /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item style="margin-top: 30px;">
              <el-button 
                type="primary" 
                size="large"
                class="submit-btn"
                :loading="isLoading" 
                :disabled="!localForm.file" 
                @click="handleLocalSubmit"
              >
                {{ isLoading ? '正在上传...' : '开始转录' }}
              </el-button>
            </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="视频链接提取" name="url">
              <el-form :model="urlForm" label-position="top" class="upload-form">
            <el-form-item label="视频链接 URL">
              <el-input 
                v-model="urlForm.url" 
                placeholder="粘贴视频 URL，例如 Youtube, Bilibili 链接"
                size="large"
              >
                <template #prefix>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="任务名称">
              <el-input 
                v-model="urlForm.title" 
                placeholder="给任务起个名字（可选）"
                size="large"
              >
                <template #prefix>
                  <el-icon><Edit /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item style="margin-top: 30px;">
              <el-button 
                type="primary" 
                size="large"
                class="submit-btn"
                :loading="isLoading" 
                :disabled="!urlForm.url" 
                @click="handleUrlSubmit"
              >
                {{ isLoading ? '正在提交...' : '提交链接' }}
              </el-button>
            </el-form-item>
              </el-form>
            </el-tab-pane>
      </el-tabs>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { VideoCamera, Edit, Link } from '@element-plus/icons-vue'
import { uploadLocalFile, submitVideoUrl } from '@/api/upload';
import { useRouter } from 'vue-router';

const router = useRouter();
const emit = defineEmits(['upload-success']);

const uploadType = ref('local');
const isLoading = ref(false);
const fileList = ref([]);

const localForm = reactive({
  title: '',
  file: null
});

const urlForm = reactive({
  url: '',
  title: ''
});

const handleFileChange = (uploadFile) => {
  localForm.file = uploadFile.raw;
  fileList.value = [uploadFile]; // 只保留当前文件
};

const handleFileRemove = () => {
  localForm.file = null;
  fileList.value = [];
};

const handleLocalSubmit = async () => {
  isLoading.value = true;
  try {
    const taskName = localForm.title?.trim() || '';
    const result = await uploadLocalFile(localForm.file, taskName);
    ElMessage.success(`任务 [${result.taskName || '新建任务'}] 创建成功，开始处理！`);
    emit('upload-success');
    // 不再跳转页面，而是留在当前页，让 HistoryList 刷新
    // router.push('/tasks'); 
  } catch (error) {
    // 错误信息已在 request.js 中处理
    console.error(error);
  } finally {
    isLoading.value = false;
  }
};

const handleUrlSubmit = async () => {
  const url = urlForm.url.trim();
  if (!url.startsWith('http')) {
    ElMessage.warning('链接格式不正确，必须以 http 或 https 开头');
    return;
  }
  
  isLoading.value = true;
  try {
    const taskName = urlForm.title?.trim() || '';
    const result = await submitVideoUrl(url, taskName);
    ElMessage.success(`任务 [${result.taskName || '新建任务'}] 创建成功，正在下载/处理！`);
    emit('upload-success');
    // router.push('/tasks');
  } catch (error) {
    console.error(error);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.upload-card {
  width: 100%;
  margin: 0;
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
}

.card-header-wrapper {
  padding: 28px 32px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  background: linear-gradient(135deg, #eef4ff 0%, #f9fbff 100%);
  color: #0f172a;
  border-radius: 28px 28px 0 0;
  box-shadow: 0 10px 30px -18px rgba(15, 23, 42, 0.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.header-context {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 640px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #0f1f3d;
}

.title-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.1;
}

.eyebrow {
  margin: 0;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  font-size: 12px;
  color: #2563eb;
  font-weight: 600;
}

.title-main {
  font-size: 22px;
  font-weight: 600;
}

.header-desc {
  margin: 0;
  color: #475569;
  font-size: 14px;
}

.header-icon {
  color: #3b5bdb;
  font-size: 26px;
}

.card-header :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.75);
  border-color: transparent;
  color: #1e3a8a;
  font-size: 13px;
  padding: 6px 12px;
  height: auto;
  backdrop-filter: blur(4px);
}

.card-body {
  padding: 32px 36px 36px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-top: none;
  border-radius: 0 0 32px 32px;
}

.custom-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: rgba(226, 232, 240, 0.6);
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  height: 48px;
  line-height: 48px;
  padding: 0 20px;
  color: var(--text-secondary);
  transition: color 0.3s;
}

.custom-tabs :deep(.el-tabs__item.is-active) {
  color: #2563eb;
  font-weight: 600;
}

.custom-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #63b3ff, #7be3ff);
}

.upload-form {
  margin-top: 20px;
}

.upload-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-secondary);
}

.upload-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 8px;
}

/* 强制让上传区域内容居中 */
.upload-demo :deep(.el-upload) {
  width: 100%;
}

.upload-demo :deep(.el-upload-dragger) {
  width: 100%;
  height: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  box-sizing: border-box;
  border: 1px dashed rgba(99, 102, 241, 0.35);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(244, 247, 255, 0.95), rgba(255, 255, 255, 0.98));
  transition: all 0.3s;
  position: relative;
}

.upload-demo :deep(.el-upload-dragger:hover) {
  border-color: rgba(59, 130, 246, 0.9);
  background-color: rgba(99, 102, 241, 0.06);
}

.upload-illustration {
  width: 110px;
  height: 110px;
  margin-bottom: 12px;
}

.upload-text {
  text-align: center;
}

.upload-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
}

.upload-subtitle {
  margin: 0;
  font-size: 14px;
  color: #4c566a;
}

.upload-subtitle span {
  color: #2563eb;
  font-weight: 600;
}

.upload-footnote {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

/* 让文件列表也居中显示 */
.upload-demo :deep(.el-upload-list) {
  text-align: center;
  margin-top: 10px;
}

.upload-demo :deep(.el-upload-list__item) {
  display: inline-block;
  width: auto;
  margin: 5px auto;
  float: none;
}

.submit-btn {
  width: 100%;
  font-size: 16px;
  height: 48px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #4f7df3 0%, #6ca8ff 100%);
  transition: all 0.3s ease;
  box-shadow: 0 6px 16px rgba(79, 125, 243, 0.25);
  font-weight: 500;
  letter-spacing: 0.5px;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(79, 125, 243, 0.35);
  background: linear-gradient(135deg, #436fe0 0%, #5e9bff 100%);
}

.submit-btn:active {
  transform: translateY(0);
}

@media (max-width: 1024px) {
  .card-body {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>