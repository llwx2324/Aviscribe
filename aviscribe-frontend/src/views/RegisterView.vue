<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

onMounted(() => {
  const redirect = route.query.redirect || '/app'
  router.replace({ path: '/', query: { auth: 'register', redirect } })
})
</script>

<template>
  <div class="auth-redirect">
    正在打开注册窗口...
  </div>
</template>

<style scoped>
.auth-redirect {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}
</style>
