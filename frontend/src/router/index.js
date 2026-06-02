import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import UsersView from '../views/UsersView.vue'
import RolesView from '../views/RolesView.vue'
import TeamsView from '../views/TeamsView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import ReleasedView from '../views/ReleasedView.vue'

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
    path: '/released',
    name: 'Released',
    component: ReleasedView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
