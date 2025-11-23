<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>登录 Aviscribe</h2>
      <p class="subtitle">输入账号信息以继续使用工作台</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model.trim="form.password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleSubmit">
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form>
      <p class="switch-text">
        还没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度需在 3-32 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const resp = await loginUser({ ...form })
      authStore.setSession(resp)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/app'
      router.replace(redirect)
    } catch (error) {
      console.error('登录失败', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 120px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 16px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 32px 28px;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

h2 {
  margin-bottom: 8px;
  font-size: 24px;
  text-align: center;
}

.subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: var(--text-secondary);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.switch-text {
  text-align: center;
  margin-top: 16px;
}

.switch-text a {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
