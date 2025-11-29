<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  redirect: {
    type: String,
    default: '/app'
  }
})

const emit = defineEmits(['success', 'switch-register'])

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
      const redirectTarget = props.redirect || route.query.redirect || '/app'
      emit('success')
      router.replace(redirectTarget)
    } catch (error) {
      console.error('登录失败', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="auth-form">
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
      <el-button
        type="primary"
        round
        size="default"
        class="switch-button"
        @click="$emit('switch-register')"
      >立即注册</el-button>
    </p>
  </div>
</template>

<style scoped>
.auth-form {
  width: 100%;
  max-width: 420px;
}

h2 {
  margin-bottom: 8px;
  font-size: 24px;
  text-align: center;
  color: #0f172a;
}

.subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: var(--text-secondary);
}

:deep(.submit-btn) {
  width: 100%;
  margin-top: 8px;
  background-color: #2563eb;
  border-color: #2563eb;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.2px;
}

:deep(.submit-btn:hover) {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
}

.switch-text {
  text-align: center;
  margin-top: 16px;
  color: var(--text-secondary);
}

.switch-button {
  padding: 10px 28px;
  background-color: #3b82f6;
  border-color: #3b82f6;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 6px 12px rgba(59, 130, 246, 0.2);
}

.switch-button:hover {
  background-color: #2563eb;
  border-color: #2563eb;
  color: #fff;
}
</style>
