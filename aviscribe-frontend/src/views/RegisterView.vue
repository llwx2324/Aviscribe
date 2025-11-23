<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>创建新账号</h2>
      <p class="subtitle">填写信息以开始使用 Aviscribe</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入 3-32 位用户名" size="large" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" placeholder="可选，便于联系" size="large" />
        </el-form-item>
        <el-form-item label="昵称" prop="displayName">
          <el-input v-model.trim="form.displayName" placeholder="用于展示的名称" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model.trim="form.password" show-password placeholder="至少 6 位字符" size="large" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model.trim="form.confirmPassword" show-password placeholder="请再次输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleSubmit">
          {{ loading ? '注册中...' : '注册并登录' }}
        </el-button>
      </el-form>
      <p class="switch-text">
        已有账号？
        <RouterLink to="/login">立即登录</RouterLink>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  phone: '',
  displayName: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度需在 3-32 个字符之间', trigger: 'blur' }
  ],
  phone: [
    { min: 6, message: '手机号格式不正确', trigger: 'blur' }
  ],
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请重复输入密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!value) {
          callback(new Error('请重复输入密码'))
        } else if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const router = useRouter()
const authStore = useAuthStore()

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = {
        username: form.username,
        phone: form.phone || undefined,
        displayName: form.displayName,
        password: form.password
      }
      const resp = await registerUser(payload)
      authStore.setSession(resp)
      ElMessage.success('注册成功，已为你自动登录')
      router.replace('/app')
    } catch (error) {
      console.error('注册失败', error)
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
  max-width: 480px;
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
