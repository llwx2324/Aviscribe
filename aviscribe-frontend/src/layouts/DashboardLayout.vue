<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import HistoryList from '@/components/HistoryList.vue'

const historyListRef = ref(null)
const route = useRoute()
const showSidebar = computed(() => route.meta.showSidebar !== false)

const refreshHistory = () => {
  historyListRef.value?.fetchTasks()
}
</script>

<template>
  <section class="dashboard-layout" :class="{ 'no-sidebar': !showSidebar }">
    <div v-if="showSidebar" class="sidebar">
      <div class="sidebar-section history-section">
        <HistoryList ref="historyListRef" />
      </div>
    </div>

    <div class="content-area">
      <RouterView v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <div class="content-scroll">
            <component :is="Component" @upload-success="refreshHistory" />
          </div>
        </transition>
      </RouterView>
    </div>
  </section>
</template>

<style scoped>
.dashboard-layout {
  display: flex;
  gap: 20px;
  height: 100%;
  max-width: 1600px;
  margin: 0 auto;
  min-height: 0;
  overflow: hidden;
}

.dashboard-layout.no-sidebar {
  gap: 0;
}

.dashboard-layout.no-sidebar .content-area {
  width: 100%;
}

.sidebar {
  width: 380px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
}

.sidebar-section {
  background: transparent;
}

.history-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.content-area {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

@media (max-width: 1024px) {
  .dashboard-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: column;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
