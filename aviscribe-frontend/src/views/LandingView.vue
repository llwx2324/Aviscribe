<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<template>
  <div class="landing-centered">
    <div class="hero-content">
      <div class="landing-logo-wrapper">
        <svg class="landing-brand-logo" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="6" y="14" width="4" height="20" rx="2" fill="#3b82f6"/>
          <rect x="14" y="8" width="4" height="32" rx="2" fill="#3b82f6"/>
          <rect x="22" y="14" width="4" height="20" rx="2" fill="#3b82f6"/>
          <path d="M30 24H36M36 24L33 21M36 24L33 27" stroke="#3b82f6" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          <rect x="40" y="16" width="8" height="2.5" rx="1.25" fill="#3b82f6"/>
          <rect x="40" y="22.75" width="8" height="2.5" rx="1.25" fill="#3b82f6"/>
          <rect x="40" y="29.5" width="5" height="2.5" rx="1.25" fill="#3b82f6"/>
        </svg>
        <div class="landing-brand-text">
          <div class="landing-main-text">
            <span class="brand-avi">Avi</span><span class="brand-scribe">scribe</span>
          </div>
          <span class="landing-sub-text">音视频转录</span>
        </div>
      </div>
      <h1>Transcribe Instantly, Anywhere</h1>
      <p class="subtitle">
        上传音视频，马上就能拿到干净的文稿。
      </p>
      <div class="hero-actions single">
        <el-button type="primary" size="large" class="cta-button" @click="$router.push('/app')">进入工作台</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landing-centered {
  width: 100%;
  min-height: calc(100vh - 72px);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 0 16px 48px;
}

.hero-content {
  text-align: center;
  max-width: 900px;
  padding: 0 24px;
  margin-top: -40px;
  animation: fadeIn 0.8s ease-out;
}

.eyebrow {
  letter-spacing: 0.25em;
  text-transform: uppercase;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 24px;
}

h1 {
  font-size: 48px;
  line-height: 1.2;
  margin-bottom: 24px;
  color: #0f172a;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 18px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 48px;
  max-width: 640px;
  margin-left: auto;
  margin-right: auto;
}

.hero-actions {
  display: flex;
  gap: 24px;
  justify-content: center;
  align-items: center;
}

.hero-actions.single {
  flex-direction: column;
}

.cta-button {
  padding: 24px 48px;
  font-size: 16px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.3), 0 2px 4px -1px rgba(59, 130, 246, 0.15);
}

.cta-button:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 10px 25px -5px rgba(59, 130, 246, 0.4), 0 8px 10px -6px rgba(59, 130, 246, 0.2);
}

.cta-button:active {
  transform: translateY(0) scale(0.98);
}

/* Removed secondary button */

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  h1 {
    font-size: 32px;
  }
  
  .subtitle {
    font-size: 16px;
  }
  
  .cta-button {
    width: 100%;
  }
  
  .hero-actions {
    flex-direction: column;
    width: 100%;
    max-width: 300px;
    margin: 0 auto;
  }

  .hero-content {
    margin-top: 0;
  }
}

.landing-logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
}

.landing-brand-logo {
  width: 64px;
  height: 64px;
}

.landing-brand-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1;
}

.landing-main-text {
  font-size: 36px;
  color: #0f172a;
  letter-spacing: -1px;
  margin-bottom: 4px;
}

.brand-avi {
  font-weight: 800;
  color: #0f172a;
}

.brand-scribe {
  font-weight: 400;
  color: #475569;
}

.landing-sub-text {
  font-size: 14px;
  color: #94a3b8;
  letter-spacing: 2px;
  margin-left: 2px;
}
</style>
