<script setup>
import { RouterView, RouterLink, useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAuthenticated = computed(() => authStore.isAuthenticated())
const displayName = computed(() => authStore.state.profile?.displayName || '未命名用户')
const isLanding = computed(() => route.path === '/')

const handleLogout = () => {
  authStore.clearSession()
  router.push('/login')
}

const goLogin = () => router.push({ name: 'login', query: { redirect: route.fullPath } })
const goRegister = () => router.push({ name: 'register' })
</script>

<template>
  <el-container class="common-layout" :class="{ 'landing-layout': isLanding }">
    <el-header class="app-header" :class="{ 'landing-mode': isLanding }">
      <div class="header-content">
        <RouterLink to="/" class="logo-link">
          <svg class="brand-logo" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- Sound waves -->
            <rect x="6" y="14" width="4" height="20" rx="2" fill="#3b82f6"/>
            <rect x="14" y="8" width="4" height="32" rx="2" fill="#3b82f6"/>
            <rect x="22" y="14" width="4" height="20" rx="2" fill="#3b82f6"/>
            <!-- Arrow -->
            <path d="M30 24H36M36 24L33 21M36 24L33 27" stroke="#3b82f6" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <!-- Text lines -->
            <rect x="40" y="16" width="8" height="2.5" rx="1.25" fill="#3b82f6"/>
            <rect x="40" y="22.75" width="8" height="2.5" rx="1.25" fill="#3b82f6"/>
            <rect x="40" y="29.5" width="5" height="2.5" rx="1.25" fill="#3b82f6"/>
          </svg>
          <div class="brand-text-container">
            <div class="brand-main-text">
              <span class="brand-avi">Avi</span><span class="brand-scribe">scribe</span>
            </div>
            <span class="brand-sub-text">音视频转录</span>
          </div>
        </RouterLink>

        <div class="header-actions" v-if="!isLanding && isAuthenticated">
          <el-dropdown trigger="click">
            <span class="user-chip">
              <el-icon class="chip-icon"><Monitor /></el-icon>
              <span class="chip-name">{{ displayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/app')">进入工作台</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/app/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/app/tasks')">任务列表</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="header-actions" v-else-if="!isLanding">
          <el-button text class="ghost-link" @click="goLogin">登录</el-button>
          <el-button type="primary" size="small" round @click="goRegister">
            注册账号
          </el-button>
        </div>
      </div>
    </el-header>

    <el-main :class="['main-container', { 'landing-main': isLanding }]">
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.common-layout {
  min-height: 100vh;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 40%, #f8fafc 100%);
  overflow: hidden;
}

.app-header {
  height: 72px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(15, 23, 42, 0.05);
  display: flex;
  align-items: center;
  padding: 0 32px;
}

.app-header.landing-mode {
  background: transparent;
  border-bottom-color: transparent;
  box-shadow: none;
}

.common-layout.landing-layout {
  background: radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.12), transparent 45%),
              linear-gradient(180deg, #eef2ff 0%, #e0e7ff 50%, #eef2ff 100%);
  min-height: auto;
  height: auto;
  overflow: visible;
}

.header-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.logo-link {
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 42px;
  height: 42px;
}

.brand-text-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1;
}

.brand-main-text {
  font-size: 22px;
  color: #0f172a;
  letter-spacing: -0.5px;
  margin-bottom: 2px;
}

.brand-avi {
  font-weight: 800;
  color: #0f172a;
}

.brand-scribe {
  font-weight: 400;
  color: #475569;
}

.brand-sub-text {
  font-size: 10px;
  color: #94a3b8;
  letter-spacing: 1px;
  transform: scale(0.9);
  transform-origin: left;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  font-weight: 500;
  cursor: pointer;
}

.chip-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.chip-name {
  color: #0f172a;
}

:deep(.ghost-link) {
  color: rgba(15, 23, 42, 0.7) !important;
  font-weight: 500;
  font-size: 15px;
  padding: 6px 14px;
  border-radius: 999px;
  transition: color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

:deep(.ghost-link:hover) {
  color: #0f172a !important;
  background: rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

:deep(.ghost-link:active) {
  transform: translateY(0);
}

.header-actions :deep(.el-button--primary) {
  font-size: 15px;
  padding: 0 22px;
  height: 38px;
  border-radius: 999px;
  font-weight: 600;
  box-shadow: 0 6px 12px rgba(59, 130, 246, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.header-actions :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.35);
}

.header-actions :deep(.el-button--primary:active) {
  transform: translateY(0);
}

.main-container {
  flex: 1;
  padding: 20px 24px 32px;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: calc(100vh - var(--app-header-height, 72px));
  box-sizing: border-box;
}

.main-container::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.15), transparent 45%),
              radial-gradient(circle at 80% 0%, rgba(59, 130, 246, 0.08), transparent 40%);
  pointer-events: none;
  z-index: 0;
}

.main-container.landing-main {
  padding: 0;
  height: auto;
  overflow: visible;
}

.main-container.landing-main::before {
  display: none;
}

.main-container > * {
  position: relative;
  z-index: 1;
}

@media (max-width: 768px) {
  .app-header {
    height: auto;
    padding: 16px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>