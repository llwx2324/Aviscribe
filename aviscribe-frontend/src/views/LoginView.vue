<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

onMounted(() => {
  const redirect = route.query.redirect || '/app'
  router.replace({ path: '/', query: { auth: 'login', redirect } })
})
</script>

<template>
  <div class="auth-redirect">
    正在打开登录窗口...
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
