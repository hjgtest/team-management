import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import UsersView from '../views/UsersView.vue'
import RolesView from '../views/RolesView.vue'
import TeamsView from '../views/TeamsView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import MyProjectsView from '../views/MyProjectsView.vue'
import ReleasedView from '../views/ReleasedView.vue'
import SmartScheduleView from '../views/SmartScheduleView.vue'
import BugStatsView from '../views/BugStatsView.vue'
import BlacklistView from '../views/BlacklistView.vue'
import GanttView from '../views/GanttView.vue'
import WorkloadHeatmapView from '../views/WorkloadHeatmapView.vue'
import TrendReportView from '../views/TrendReportView.vue'
import LoginView from '../views/LoginView.vue'
import { getToken, isAdmin } from '@/utils/auth'

// 管理员专属路由
// 非管理员访问这些路径时自动跳转到仪表盘
const adminOnlyRoutes = ['/users', '/roles', '/teams', '/projects', '/released', '/smart-schedule', '/blacklist', '/workload-heatmap', '/trend-report']

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView
  },
  {
    path: '/users',
    name: 'Users',
    component: UsersView
  },
  {
    path: '/roles',
    name: 'Roles',
    component: RolesView
  },
  {
    path: '/teams',
    name: 'Teams',
    component: TeamsView
  },
  {
    path: '/projects',
    name: 'Projects',
    component: ProjectsView
  },
  {
    path: '/my-projects',
    name: 'MyProjects',
    component: MyProjectsView
  },
  {
    path: '/released',
    name: 'Released',
    component: ReleasedView
  },
  {
    path: '/smart-schedule',
    name: 'SmartSchedule',
    component: SmartScheduleView
  },
  {
    path: '/bug-stats',
    name: 'BugStats',
    component: BugStatsView
  },
  {
    path: '/blacklist',
    name: 'Blacklist',
    component: BlacklistView
  },
  {
    path: '/gantt',
    name: 'Gantt',
    component: GanttView,
    meta: { requiresAuth: false }
  },
  {
    path: '/workload-heatmap',
    name: 'WorkloadHeatmap',
    component: WorkloadHeatmapView
  },
  {
    path: '/trend-report',
    name: 'TrendReport',
    component: TrendReportView
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()

  if (to.meta.requiresAuth !== false && !token) {
    // 需要登录但未登录，跳转到登录页
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    // 已登录但访问登录页，跳转到首页
    next('/dashboard')
  } else if (token && adminOnlyRoutes.includes(to.path) && !isAdmin()) {
    // 非管理员访问管理员专属页面，跳转到仪表盘
    next('/dashboard')
  } else {
    next()
  }
})

export default router
