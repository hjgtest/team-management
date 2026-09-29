<template>
  <div id="app">
    <!-- 登录页：不显示侧边栏和顶栏 -->
    <router-view v-if="$route.path === '/login'" v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <!-- 其他页面：显示 layout -->
    <el-container v-else class="app-shell">
      <el-aside width="220px" class="app-aside">
        <div class="brand">
          <div class="brand-logo">DM</div>
          <div class="brand-text">
            <div class="brand-title">开发组管理</div>
            <div class="brand-sub">DEV TEAM SYSTEM</div>
          </div>
        </div>
        <el-menu
          :default-active="$route.path"
          class="side-menu"
          @select="handleMenuSelect"
          :unique-opened="true"
          background-color="transparent"
          text-color="#94a3b8"
          active-text-color="#ffffff"
        >
          <el-menu-item
            v-for="menu in menus"
            :key="menu.id"
            :index="menu.url">
            <span class="menu-icon">{{ menu.icon }}</span>
            <span>{{ menu.menuName }}</span>
          </el-menu-item>
        </el-menu>
        <div class="aside-foot">
          <div class="dot dot-online"></div>
          <span>系统运行正常</span>
        </div>
      </el-aside>

      <el-container class="app-body">
        <el-header class="app-header">
          <div class="header-left">
            <h2 class="page-title">{{ currentTitle }}</h2>
            <span class="page-sub">{{ currentSub }}</span>
          </div>
          <div class="header-right">
            <div class="notification-bell" @click="showNotifications = true">
              <span class="bell-icon">🔔</span>
              <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </div>
            <span class="header-user" v-if="currentUser">
              <span class="user-name">{{ currentUser.username }}</span>
              <el-tag :type="userTypeTag" size="small">{{ userTypeText }}</el-tag>
            </span>
            <el-button type="default" size="small" class="logout-btn" @click="handleLogout">
              退出登录
            </el-button>
            <span class="header-time">{{ now }}</span>
          </div>
        </el-header>
        <el-main class="app-main">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>

    <!-- 通知面板抽屉拉 -->
    <el-drawer
      v-model="showNotifications"
      title="站内通知"
      direction="rtl"
      size="420px"
      :before-close="() => showNotifications = false">
      <div class="notif-panel">
        <div class="notif-actions">
          <el-button size="small" type="primary" @click="scanDeadlines">🔄 扫描预警</el-button>
          <el-button size="small" @click="markAllRead" :disabled="unreadCount === 0">✓ 全部已读</el-button>
        </div>
        <div class="notif-list" v-if="notifications.length">
          <div
            v-for="n in notifications"
            :key="n.id"
            class="notif-item"
            :class="[notifUrgencyClass(n.urgency), { 'notif-read': n.isRead }]"
            @click="!n.isRead && markNotifRead(n.id)">
            <div class="notif-title">
              <span class="notif-dot" v-if="!n.isRead"></span>
              {{ n.title }}
            </div>
            <div class="notif-content">{{ n.content }}</div>
            <div class="notif-time">{{ n.createdTime }}</div>
          </div>
        </div>
        <div v-else class="notif-empty">
          <p>暂无通知</p>
          <p class="notif-empty-hint">点击“扫描预警”检查即将到期的任务</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { getUser, removeToken, removeUser, isAdmin } from '@/utils/auth'
import request from '@/utils/request'
export default {
  name: 'App',
  data() {
    return {
      // 全量菜单（管理员可见全部）
      allMenus: [
        { id: 0, menuName: '仪表盘',     url: '/dashboard', icon: '📊', sub: 'Dashboard · 全局概览' },
        { id: 1, menuName: '用户管理',   url: '/users',     icon: '👤', sub: 'Users · 成员资料与任务', adminOnly: true },
        { id: 2, menuName: '角色管理',   url: '/roles',     icon: '🛡️', sub: 'Roles · 岗位与权限', adminOnly: true },
        { id: 3, menuName: '小组管理',   url: '/teams',     icon: '🏢', sub: 'Teams · 团队组织', adminOnly: true },
        { id: 4, menuName: '项目管理',   url: '/projects',  icon: '📁', sub: 'Projects · 项目/版本/分配', adminOnly: true },
        { id: 5, menuName: '活跃项目',   url: '/my-projects',  icon: '📋', sub: 'Active Projects · 未开始&进行中' },
        { id: 6, menuName: '人员释放',   url: '/released',  icon: '🚀', sub: 'Released · 可用资源与推荐', adminOnly: true },
        { id: 7, menuName: '智能排期',   url: '/smart-schedule',  icon: '', sub: 'Smart Schedule · 自动排期与分配', adminOnly: true },
        { id: 8, menuName: 'Bug统计考核', url: '/bug-stats',  icon: '', sub: 'Bug Stats · 质量评估报表', adminOnly: true },
        { id: 9, menuName: '黑榜管理',   url: '/blacklist',  icon: '', sub: 'Blacklist · 负面事件记录', adminOnly: true },
        { id: 10, menuName: '项目甘特图', url: '/gantt',  icon: '📈', sub: 'Gantt Chart · 可视化进度跟踪' },
        { id: 11, menuName: '负载热力图', url: '/workload-heatmap',  icon: '🔥', sub: 'Workload Heatmap · 团队负载分布', adminOnly: true },
        { id: 12, menuName: '趋势报表',   url: '/trend-report',  icon: '📉', sub: 'Trend Report · 月度季度趋势分析', adminOnly: true }
      ],
      now: '',
      currentUser: null,
      showNotifications: false,
      notifications: [],
      unreadCount: 0,
      notifLoading: false
    }
  },
  computed: {
    menus() {
      if (isAdmin()) {
        return this.allMenus
      }
      // 非管理员只显示仪表盘和活跃项目
      return this.allMenus.filter(m => !m.adminOnly)
    },
    currentMenu() {
      return this.allMenus.find(m => m.url === this.$route.path) || this.allMenus[0]
    },
    currentTitle() {
      return this.currentMenu.menuName
    },
    currentSub() {
      return this.currentMenu.sub
    },
    userTypeTag() {
      if (!this.currentUser) return ''
      const t = this.currentUser.userType
      return ['danger', 'warning', ''][t] || ''
    },
    userTypeText() {
      if (!this.currentUser) return ''
      const t = this.currentUser.userType
      return ['管理员', '外派', '正常研发成员'][t] || '未知'
    }
  },
  mounted() {
    this.updateTime()
    this._timer = setInterval(this.updateTime, 1000)
    this.loadUser()
    this.loadNotifications()
    this._notifTimer = setInterval(this.loadNotifications, 60000)
  },
  beforeUnmount() {
    if (this._timer) clearInterval(this._timer)
    if (this._notifTimer) clearInterval(this._notifTimer)
  },
  methods: {
    loadUser() {
      this.currentUser = getUser()
    },
    handleMenuSelect(key) {
      if (this.$route.path !== key) this.$router.push(key)
    },
    handleLogout() {
      removeToken()
      removeUser()
      this.currentUser = null
      this.$router.push('/login')
    },
    updateTime() {
      const d = new Date()
      const pad = n => String(n).padStart(2, '0')
      this.now = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    async loadNotifications() {
      try {
        const res = await request({ url: '/api/notifications', method: 'get', params: { limit: 20 } })
        if (res && res.code === 200) {
          this.notifications = res.data || []
          this.unreadCount = res.unreadCount || 0
        }
      } catch (e) { /* ignore */ }
    },
    async scanDeadlines() {
      try {
        await request({ url: '/api/notifications/scan-deadlines', method: 'post' })
        this.loadNotifications()
      } catch (e) { /* ignore */ }
    },
    async markNotifRead(id) {
      try {
        await request({ url: '/api/notifications/' + id + '/read', method: 'put' })
        const n = this.notifications.find(x => x.id === id)
        if (n) n.isRead = 1
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      } catch (e) { /* ignore */ }
    },
    async markAllRead() {
      try {
        await request({ url: '/api/notifications/read-all', method: 'put' })
        this.notifications.forEach(n => n.isRead = 1)
        this.unreadCount = 0
      } catch (e) { /* ignore */ }
    },
    notifUrgencyClass(urgency) {
      if (urgency === 2) return 'notif-urgent'
      if (urgency === 1) return 'notif-warn'
      return 'notif-normal'
    }
  }
}
</script>

<style>
/* ===== 全局主题变量（玻璃拟态 / 深色科技蓝） ===== */
:root {
  --bg-base:        #0b1220;
  --bg-base-2:      #0f172a;
  --bg-card-solid:  #1e293b;
  --glass-bg:       rgba(30, 41, 59, 0.65);
  --glass-bg-strong:rgba(30, 41, 59, 0.85);
  --glass-border:   rgba(99, 122, 161, 0.22);
  --border-solid:   #334155;

  --text-primary:   #e2e8f0;
  --text-secondary: #94a3b8;
  --text-muted:     #64748b;

  --accent-blue:    #3b82f6;
  --accent-cyan:    #38bdf8;
  --accent-purple:  #8b5cf6;
  --accent-pink:    #ec4899;

  --success: #10b981;
  --warning: #f59e0b;
  --danger:  #ef4444;

  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.25);
  --shadow-lg: 0 12px 32px rgba(0, 0, 0, 0.45);
}

html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', Avenir, Helvetica, Arial, sans-serif;
  background: var(--bg-base);
  color: var(--text-primary);
}

#app {
  height: 100vh;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--text-primary);
  background:
    radial-gradient(1200px 600px at 10% -10%, rgba(56, 189, 248, 0.12), transparent 60%),
    radial-gradient(900px 600px at 110% 10%, rgba(139, 92, 246, 0.12), transparent 60%),
    radial-gradient(900px 600px at 50% 120%, rgba(59, 130, 246, 0.10), transparent 60%),
    var(--bg-base);
}

.app-shell { height: 100vh; }

/* ===== 侧边栏 ===== */
.app-aside {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.92), rgba(11, 18, 32, 0.92));
  border-right: 1px solid var(--glass-border);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px 18px;
  border-bottom: 1px solid var(--glass-border);
}
.brand-logo {
  width: 38px; height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 14px;
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.45);
  letter-spacing: 0.5px;
}
.brand-title { font-size: 15px; font-weight: 700; color: var(--text-primary); letter-spacing: 1px; }
.brand-sub   { font-size: 10px; color: var(--text-muted); letter-spacing: 1.5px; margin-top: 2px; }

.side-menu {
  border-right: none !important;
  background: transparent !important;
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;
}
.side-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 4px 0;
  border-radius: 10px;
  padding-left: 14px !important;
  font-size: 14px;
  transition: all 0.25s ease;
}
.side-menu .el-menu-item:hover {
  background: rgba(59, 130, 246, 0.12) !important;
  color: var(--text-primary) !important;
}
.side-menu .el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.28), rgba(139, 92, 246, 0.18)) !important;
  color: #fff !important;
  box-shadow: inset 3px 0 0 var(--accent-cyan);
}
.menu-icon {
  display: inline-block;
  width: 22px;
  margin-right: 10px;
  text-align: center;
  font-size: 15px;
}

.aside-foot {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 18px;
  border-top: 1px solid var(--glass-border);
  font-size: 12px;
  color: var(--text-muted);
}
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot-online {
  background: var(--success);
  box-shadow: 0 0 8px var(--success);
  animation: pulse 1.8s infinite;
}
@keyframes pulse {
  0%   { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70%  { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

/* ===== 顶部 ===== */
.app-body { background: transparent; }
.app-header {
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px !important;
  padding: 0 24px !important;
}
.header-left { display: flex; align-items: baseline; gap: 14px; }
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.4px;
}
.page-sub { color: var(--text-muted); font-size: 12px; }
.header-right .header-time {
  font-size: 13px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  padding: 6px 12px;
  border-radius: 8px;
}

/* ===== 主内容 ===== */
.app-main {
  height: calc(100vh - 60px);
  padding: 22px !important;
  overflow-y: auto;
  color: var(--text-primary);
}

/* ===== 页面切换动画 ===== */
.page-enter-active, .page-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-enter-from { opacity: 0; transform: translateY(8px); }
.page-leave-to   { opacity: 0; transform: translateY(-8px); }

/* ===== Element Plus 全局深色覆盖 ===== */
.el-card {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  color: var(--text-primary) !important;
  backdrop-filter: blur(10px);
}
.el-card__header { border-bottom: 1px solid var(--glass-border) !important; }

.el-input__wrapper, .el-select__wrapper, .el-textarea__inner {
  background: rgba(15, 23, 42, 0.6) !important;
  box-shadow: 0 0 0 1px var(--glass-border) inset !important;
  border-radius: 8px !important;
}
.el-input__wrapper.is-focus, .el-select__wrapper.is-focused {
  box-shadow: 0 0 0 1px var(--accent-blue) inset !important;
}
.el-input__inner, .el-textarea__inner { color: var(--text-primary) !important; }
.el-input__inner::placeholder { color: var(--text-muted) !important; }

.el-button {
  border-radius: 8px !important;
  font-weight: 500 !important;
}
.el-button--primary {
  background: linear-gradient(135deg, var(--accent-blue), var(--accent-cyan)) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35) !important;
}
.el-button--primary:hover { filter: brightness(1.1); transform: translateY(-1px); }
.el-button--success {
  background: linear-gradient(135deg, #10b981, #34d399) !important;
  border: none !important;
}
.el-button--warning {
  background: linear-gradient(135deg, #f59e0b, #fbbf24) !important;
  border: none !important;
}
.el-button--danger {
  background: linear-gradient(135deg, #ef4444, #f87171) !important;
  border: none !important;
}
.el-button--default {
  background: rgba(30, 41, 59, 0.7) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--glass-border) !important;
}
.el-button--default:hover {
  background: rgba(51, 65, 85, 0.85) !important;
  border-color: var(--accent-blue) !important;
  color: #fff !important;
}

/* 表格 */
.el-table {
  background: transparent !important;
  color: var(--text-primary) !important;
  --el-table-border-color: var(--glass-border);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(15, 23, 42, 0.7);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.08);
  --el-table-header-text-color: var(--text-primary);
  --el-table-text-color: var(--text-primary);
}
.el-table th.el-table__cell {
  background: rgba(15, 23, 42, 0.85) !important;
  color: var(--text-primary) !important;
  border-bottom: 1px solid var(--glass-border) !important;
  font-weight: 600;
}
.el-table td.el-table__cell {
  border-bottom: 1px solid rgba(99, 122, 161, 0.12) !important;
  background: transparent !important;
}
.el-table tr:hover > td.el-table__cell {
  background: rgba(59, 130, 246, 0.08) !important;
}
.el-table--border, .el-table--border .el-table__cell { border-color: var(--glass-border) !important; }
.el-table__empty-block { background: transparent; color: var(--text-muted); }

/* 分页 */
.el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-color: var(--text-secondary);
  --el-pagination-hover-color: var(--accent-cyan);
  color: var(--text-secondary);
}
.el-pagination button, .el-pagination .el-pager li {
  background: rgba(30, 41, 59, 0.7) !important;
  color: var(--text-secondary) !important;
  border-radius: 6px !important;
  margin: 0 2px;
}
.el-pagination .el-pager li.is-active {
  background: var(--accent-blue) !important;
  color: #fff !important;
}

/* 弹窗 */
.el-dialog {
  background: rgba(15, 23, 42, 0.95) !important;
  border: 1px solid var(--glass-border);
  border-radius: 14px !important;
  backdrop-filter: blur(20px);
  box-shadow: var(--shadow-lg);
}
.el-dialog__title { color: var(--text-primary) !important; font-weight: 700; }
.el-dialog__header {
  border-bottom: 1px solid var(--glass-border);
  margin-right: 0 !important;
  padding-bottom: 14px !important;
}
.el-dialog__body { color: var(--text-primary) !important; }
.el-overlay { background: rgba(0, 0, 0, 0.55) !important; backdrop-filter: blur(2px); }

/* 表单 */
.el-form-item__label { color: var(--text-secondary) !important; }
.el-form-item__error { color: var(--danger) !important; }

/* 下拉 */
.el-select-dropdown, .el-popper {
  background: rgba(15, 23, 42, 0.96) !important;
  border: 1px solid var(--glass-border) !important;
  color: var(--text-primary) !important;
}
.el-select-dropdown__item { color: var(--text-secondary) !important; }
.el-select-dropdown__item.hover, .el-select-dropdown__item:hover {
  background: rgba(59, 130, 246, 0.15) !important;
  color: var(--text-primary) !important;
}
.el-select-dropdown__item.selected { color: var(--accent-cyan) !important; font-weight: 600; }

/* 标签 */
.el-tag {
  background: rgba(59, 130, 246, 0.15) !important;
  border-color: rgba(59, 130, 246, 0.35) !important;
  color: #93c5fd !important;
  border-radius: 6px !important;
}
.el-tag--success { background: rgba(16, 185, 129, 0.15) !important; border-color: rgba(16,185,129,0.35) !important; color: #6ee7b7 !important; }
.el-tag--warning { background: rgba(245, 158, 11, 0.15) !important; border-color: rgba(245,158,11,0.35) !important; color: #fcd34d !important; }
.el-tag--danger  { background: rgba(239, 68, 68, 0.15) !important; border-color: rgba(239,68,68,0.35) !important; color: #fca5a5 !important; }
.el-tag--info    { background: rgba(100, 116, 139, 0.18) !important; border-color: rgba(100,116,139,0.4) !important; color: #cbd5e1 !important; }

/* 提示 / 警告 */
.el-alert { border-radius: 10px !important; }
.el-alert--warning.is-light { background: rgba(245, 158, 11, 0.12) !important; color: #fcd34d !important; }
.el-alert--success.is-light { background: rgba(16, 185, 129, 0.12) !important; color: #6ee7b7 !important; }
.el-alert--info.is-light    { background: rgba(59, 130, 246, 0.12) !important; color: #93c5fd !important; }
.el-alert--error.is-light   { background: rgba(239, 68, 68, 0.12) !important; color: #fca5a5 !important; }

/* 描述列表、空状态 */
.el-empty__description p { color: var(--text-muted) !important; }

/* 滚动条 */
.app-main::-webkit-scrollbar,
.side-menu::-webkit-scrollbar,
.panel-body::-webkit-scrollbar { width: 8px; height: 8px; }
.app-main::-webkit-scrollbar-thumb,
.side-menu::-webkit-scrollbar-thumb,
.panel-body::-webkit-scrollbar-thumb {
  background: rgba(99, 122, 161, 0.35);
  border-radius: 8px;
}
.app-main::-webkit-scrollbar-thumb:hover { background: rgba(99, 122, 161, 0.6); }
.app-main::-webkit-scrollbar-track { background: transparent; }

/* ===== 通知铃铛 ===== */
.notification-bell {
  position: relative;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  margin-right: 12px;
  transition: all 0.25s;
  display: flex;
  align-items: center;
}
.notification-bell:hover {
  border-color: var(--accent-blue);
  background: rgba(59, 130, 246, 0.12);
}
.bell-icon { font-size: 16px; }
.bell-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  border-radius: 9px;
  text-align: center;
  padding: 0 4px;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.5);
  animation: badgePulse 2s infinite;
}
@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* ===== 通知面板 ===== */
.notif-panel { padding: 0 4px; }
.notif-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.notif-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
}
.notif-item {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.25s;
}
.notif-item:hover {
  border-color: rgba(59, 130, 246, 0.5);
  transform: translateY(-1px);
}
.notif-item.notif-read {
  opacity: 0.55;
  cursor: default;
}
.notif-item.notif-urgent { border-left: 3px solid var(--danger); }
.notif-item.notif-warn { border-left: 3px solid var(--warning); }
.notif-item.notif-normal { border-left: 3px solid var(--accent-blue); }
.notif-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-blue);
  flex-shrink: 0;
  box-shadow: 0 0 6px var(--accent-blue);
}
.notif-content {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.notif-time {
  font-size: 11px;
  color: var(--text-muted);
}
.notif-empty {
  text-align: center;
  padding: 60px 0;
  color: var(--text-secondary);
}
.notif-empty-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

/* 抽屉样式适配深色主题 */
.el-drawer {
  background: rgba(15, 23, 42, 0.97) !important;
  border-left: 1px solid var(--glass-border) !important;
}
.el-drawer__title { color: var(--text-primary) !important; }
.el-drawer__header {
  border-bottom: 1px solid var(--glass-border);
  margin-bottom: 0 !important;
  padding-bottom: 14px !important;
}
</style>