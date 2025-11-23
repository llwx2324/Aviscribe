<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { fetchProfile, updateProfile, changePassword } from '@/api/auth'

const authStore = useAuthStore()

const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  displayName: '',
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const applyProfile = (profile) => {
  profileForm.displayName = profile?.displayName || ''
  profileForm.phone = profile?.phone || ''
}

watch(
  () => authStore.state.profile,
  (profile) => applyProfile(profile),
  { immediate: true }
)

const profileRules = {
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 40, message: '昵称需在2-40个字符之间', trigger: 'blur' }
  ],
  phone: [
    {
      validator: (_, value, callback) => {
        if (!value) {
          callback()
          return
        }
        const mobilePattern = /^1\d{10}$/
        if (!mobilePattern.test(String(value))) {
          callback(new Error('请输入有效的手机号'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码需在6-64位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const syncProfile = async () => {
  profileLoading.value = true
  try {
    const latest = await fetchProfile()
    authStore.updateProfile(latest)
    ElMessage.success('已同步最新资料')
  } finally {
    profileLoading.value = false
  }
}

const handleProfileSubmit = () => {
  profileFormRef.value?.validate(async (valid) => {
    if (!valid) {
      return
    }
    profileLoading.value = true
    try {
      await updateProfile({ ...profileForm })
      const latest = await fetchProfile()
      authStore.updateProfile(latest)
      ElMessage.success('资料更新成功')
    } finally {
      profileLoading.value = false
    }
  })
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const handlePasswordSubmit = () => {
  passwordFormRef.value?.validate(async (valid) => {
    if (!valid) {
      return
    }
    passwordLoading.value = true
    try {
      await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功')
      resetPasswordForm()
    } finally {
      passwordLoading.value = false
    }
  })
}
</script>

<template>
  <div class="profile-view">
    <div class="page-header">
      <div>
        <p class="eyebrow">Account</p>
        <h1>个人资料中心</h1>
        <p class="subtitle">更新昵称、联系方式，以及修改登录密码</p>
      </div>
      <el-button text @click="syncProfile" :loading="profileLoading">同步最新资料</el-button>
    </div>

    <div class="card-grid">
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <div>
              <h3>基本信息</h3>
              <p>这些信息会显示在控制台并用于通知</p>
            </div>
          </div>
        </template>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="90px"
          status-icon
        >
          <el-form-item label="昵称" prop="displayName">
            <el-input v-model="profileForm.displayName" placeholder="输入昵称" />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" placeholder="可选，方便联系" clearable />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="profileLoading" @click="handleProfileSubmit">
              保存修改
            </el-button>
            <el-button text @click="applyProfile(authStore.state.profile)">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <div>
              <h3>修改密码</h3>
              <p>保持密码安全，建议定期更新</p>
            </div>
          </div>
        </template>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="110px"
          status-icon
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="passwordLoading" @click="handlePasswordSubmit">
              更新密码
            </el-button>
            <el-button text @click="resetPasswordForm">清空</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.profile-view {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 24px;
  background: white;
  border-radius: 24px;
  box-shadow: 0 10px 35px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 13px;
  color: #6366f1;
  margin: 0 0 8px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.subtitle {
  margin: 6px 0 0;
  color: #64748b;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.profile-card {
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.card-header p {
  margin: 6px 0 0;
  color: #94a3b8;
}

:deep(.el-form-item__label) {
  color: #475569;
  font-weight: 500;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
