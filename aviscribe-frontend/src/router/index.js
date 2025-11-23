import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '@/views/LandingView.vue'
import DashboardLayout from '@/layouts/DashboardLayout.vue'
import UploadView from '@/views/UploadView.vue'
import TaskListView from '@/views/TaskListView.vue'
import ResultView from '@/views/ResultView.vue'
import ProfileView from '@/views/ProfileView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory('/'),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingView,
      meta: { title: '欢迎' }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录', guestOnly: true }
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { title: '注册', guestOnly: true }
    },
    {
      path: '/app',
      component: DashboardLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'upload',
          component: UploadView,
          meta: { title: '工作台', showSidebar: true, requiresAuth: true }
        },
        {
          path: 'tasks',
          name: 'taskList',
          component: TaskListView,
          meta: { title: '任务列表', showSidebar: false, requiresAuth: true }
        },
        {
          path: 'result/:id',
          name: 'workspaceResult',
          component: ResultView,
          meta: { title: '转录结果', showSidebar: true, backTo: '/app', requiresAuth: true }
        },
        {
          path: 'tasks/result/:id',
          name: 'taskResult',
          component: ResultView,
          meta: { title: '转录结果', showSidebar: false, backTo: '/app/tasks', requiresAuth: true }
        },
        {
          path: 'profile',
          name: 'profile',
          component: ProfileView,
          meta: { title: '个人资料', showSidebar: false, requiresAuth: true }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} | Aviscribe` : 'Aviscribe'
    const authStore = useAuthStore()
    const isAuthed = authStore.isAuthenticated()

    const requiresAuth = to.matched.some(record => record.meta?.requiresAuth)
    const guestOnly = to.matched.some(record => record.meta?.guestOnly)

    if (requiresAuth && !isAuthed) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    if (guestOnly && isAuthed) {
      return next({ path: '/app' })
    }

    next()
})

export default router