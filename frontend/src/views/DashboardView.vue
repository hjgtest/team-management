<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card" v-for="(item, idx) in statList" :key="idx">
        <div class="stat-icon" :style="{ background: item.color }">{{ item.icon }}</div>
        <div class="stat-content">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
        <div class="stat-trend" v-if="item.trend">{{ item.trend }}</div>
      </div>
    </div>

    <!-- 双栏面板 -->
    <div class="dashboard-grid">
      <!-- 临近截止 -->
      <div class="panel">
        <div class="panel-header">
          <h3>📌 临近截止</h3>
          <span class="panel-tip">未来 {{ deadlineDays }} 天 · 共 {{ deadlines.length }} 项</span>
        </div>
        <div class="panel-body">
          <div v-if="loadingDeadlines" class="empty-tip">加载中…</div>
          <div v-else-if="!deadlines.length" class="empty-tip">暂无即将到期版本 🎉</div>
          <div v-else class="deadline-list">
            <div
              v-for="d in deadlines"
              :key="d.versionId"
              class="deadline-item"
              :class="urgencyClass(d.urgency)">
              <div class="deadline-top">
                <div class="deadline-project">
                  {{ d.projectName }}
                  <span class="pm-tag" v-if="d.productManager">/ PM: {{ d.productManager }}</span>
                </div>
                <el-tag size="small" :type="urgencyTagType(d.urgency)">
                  {{ urgencyLabel(d.urgency) }}
                </el-tag>
              </div>
              <div class="deadline-version">🏷️ {{ d.versionName }}</div>
              <div class="deadline-meta">
                <span class="time-badge">研发截止：<em :class="daysClass(d.daysLeft)">{{ formatDays(d.daysLeft) }}</em></span>
                <span class="time-badge" v-if="d.testDaysLeft != null">提测：<em :class="daysClass(d.testDaysLeft)">{{ formatDays(d.testDaysLeft) }}</em></span>
                <span class="time-badge" v-if="d.launchDaysLeft != null">上线：<em :class="daysClass(d.launchDaysLeft)">{{ formatDays(d.launchDaysLeft) }}</em></span>
              </div>
              <div class="deadline-assignees">
                <span class="assignee-label">👥 负责：</span>
                <el-tag
                  v-for="name in d.assignees"
                  :key="name"
                  class="assignee-tag"
                  size="small"
                  type="info">{{ name }}</el-tag>
                <span v-if="!d.assignees || !d.assignees.length" class="no-assignee">未分配</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 人员负载 -->
      <div class="panel">
        <div class="panel-header">
          <h3>👥 人员负载</h3>
          <span class="panel-tip">点击展开任务 · 共 {{ workload.length }} 人</span>
        </div>
        <div class="panel-body">
          <div v-if="loadingWorkload" class="empty-tip">加载中…</div>
          <div v-else-if="!workload.length" class="empty-tip">暂无人员数据</div>
          <div v-else class="workload-list">
            <div
              v-for="u in workload"
              :key="u.userId"
              class="workload-item"
              :class="{ expanded: expandedUsers.includes(u.userId) }"
              @click="toggleUser(u.userId)">
              <div class="workload-top">
                <div class="user-info">
                  <div class="user-avatar">{{ avatarText(u.username) }}</div>
                  <div class="user-detail">
                    <div class="user-name">{{ u.username }}</div>
                  </div>
                </div>
                <span class="workload-badge" :class="loadBadgeClass(u.ongoingCount)">
                  {{ loadLabel(u.ongoingCount) }} · {{ u.ongoingCount }}
                </span>
              </div>
              <div v-if="u.earliestDeadline" class="earliest-deadline">
                ⏰ 最早截止：{{ formatDate(u.earliestDeadline) }}
                <span class="days-hint" :class="daysClass(daysFromNow(u.earliestDeadline))">
                  {{ formatDays(daysFromNow(u.earliestDeadline)) }}
                </span>
              </div>

              <transition name="slide">
                <div v-if="expandedUsers.includes(u.userId) && u.tasks && u.tasks.length" class="task-details">
                  <div v-for="(t, ti) in u.tasks" :key="ti" class="task-item">
                    <div class="task-name">
                      <span class="task-dot" :class="dotClass(t.daysLeft)"></span>
                      {{ t.projectName }} / {{ t.versionName }}
                      <span class="task-pm" v-if="t.productManager">· PM: {{ t.productManager }}</span>
                    </div>
                    <div class="task-meta">
                      <span>{{ t.feature || '—' }}</span>
                      <span class="task-deadline">截止 <em :class="daysClass(t.daysLeft)">{{ formatDays(t.daysLeft) }}</em></span>
                    </div>
                    <div class="task-extra-times" v-if="t.testDaysLeft != null || t.launchDaysLeft != null">
                      <span v-if="t.testDaysLeft != null">提测：<em :class="daysClass(t.testDaysLeft)">{{ formatDays(t.testDaysLeft) }}</em></span>
                      <span v-if="t.launchDaysLeft != null">上线：<em :class="daysClass(t.launchDaysLeft)">{{ formatDays(t.launchDaysLeft) }}</em></span>
                    </div>
                  </div>
                </div>
              </transition>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'DashboardView',
  data() {
    return {
      deadlineDays: 7,
      overview: {
        totalProjects: 0,
        ongoingProjects: 0,
        finishedProjects: 0,
        notStartedProjects: 0,
        totalUsers: 0
      },
      deadlines: [],
      workload: [],
      loadingDeadlines: false,
      loadingWorkload: false,
      expandedUsers: []
    }
  },
  computed: {
    statList() {
      const o = this.overview
      return [
        { label: '项目总数',   value: o.totalProjects,      icon: '📁', color: 'rgba(59,130,246,0.22)' },
        { label: '进行中',     value: o.ongoingProjects,    icon: '🚀', color: 'rgba(56,189,248,0.22)' },
        { label: '已完成',     value: o.finishedProjects,   icon: '✅', color: 'rgba(16,185,129,0.22)' },
        { label: '未开始',     value: o.notStartedProjects, icon: '⏳', color: 'rgba(245,158,11,0.22)' },
        { label: '团队成员',   value: o.totalUsers,         icon: '👥', color: 'rgba(139,92,246,0.22)' }
      ]
    }
  },
  mounted() {
    this.loadOverview()
    this.loadDeadlines()
    this.loadWorkload()
  },
  methods: {
    async loadOverview() {
      try {
        const res = await request({ url: '/api/dashboard/overview', method: 'get' })
        if (res && res.code === 200 && res.data) {
          this.overview = {
            totalProjects:      res.data.totalProjects ?? 0,
            ongoingProjects:    res.data.ongoingProjects ?? 0,
            finishedProjects:   res.data.finishedProjects ?? 0,
            notStartedProjects: res.data.notStartedProjects ?? 0,
            totalUsers:         res.data.totalUsers ?? 0
          }
        }
      } catch (e) { console.error('overview error', e) }
    },
    async loadDeadlines() {
      this.loadingDeadlines = true
      try {
        const res = await request({ url: '/api/dashboard/upcoming-deadlines', method: 'get', params: { days: this.deadlineDays } })
        if (res && res.code === 200) this.deadlines = res.data || []
      } catch (e) { console.error('deadlines error', e) }
      finally { this.loadingDeadlines = false }
    },
    async loadWorkload() {
      this.loadingWorkload = true
      try {
        const res = await request({ url: '/api/dashboard/user-workload', method: 'get' })
        if (res && res.code === 200) this.workload = res.data || []
      } catch (e) { console.error('workload error', e) }
      finally { this.loadingWorkload = false }
    },
    toggleUser(uid) {
      const i = this.expandedUsers.indexOf(uid)
      if (i >= 0) this.expandedUsers.splice(i, 1)
      else this.expandedUsers.push(uid)
    },
    urgencyClass(u) {
      if (u === 0) return 'urgency-today'
      if (u === 1) return 'urgency-tomorrow'
      if (u === 2) return 'urgency-soon'
      return ''
    },
    urgencyLabel(u) {
      return ['今日到期', '明日到期', '3 天内', '一周内'][u] || '一周内'
    },
    urgencyTagType(u) {
      return ['danger', 'warning', 'warning', 'success'][u] || 'info'
    },
    formatDays(n) {
      if (n == null) return '—'
      if (n < 0) return `已过期 ${Math.abs(n)} 天`
      if (n === 0) return '今天'
      return `${n} 天`
    },
    daysClass(n) {
      if (n == null) return ''
      if (n <= 1) return 'time-urgent'
      if (n <= 3) return 'time-warn'
      return ''
    },
    daysFromNow(dt) {
      if (!dt) return null
      const target = new Date(dt)
      const now = new Date()
      const ms = target.setHours(0,0,0,0) - now.setHours(0,0,0,0)
      return Math.round(ms / 86400000)
    },
    formatDate(dt) {
      if (!dt) return '—'
      const d = new Date(dt)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
    },
    avatarText(name) {
      if (!name) return '?'
      const ch = name.trim().charAt(0)
      return ch.toUpperCase()
    },
    loadBadgeClass(n) {
      if (!n) return 'workload-idle'
      if (n <= 2) return 'workload-normal'
      return 'workload-busy'
    },
    loadLabel(n) {
      if (!n) return '空闲'
      if (n <= 2) return '正常'
      return '繁忙'
    },
    dotClass(d) {
      if (d == null) return 'dot-normal'
      if (d <= 1) return 'dot-danger'
      if (d <= 3) return 'dot-warning'
      return 'dot-normal'
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--glass-bg, rgba(30, 41, 59, 0.75));
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.35s;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(12px);
  position: relative;
  overflow: hidden;
}
.stat-card::after {
  content: "";
  position: absolute;
  top: -40%;
  right: -20%;
  width: 160px; height: 160px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.18), transparent 70%);
  pointer-events: none;
}
.stat-card:hover {
  border-color: rgba(59, 130, 246, 0.6);
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}
.stat-icon {
  width: 46px; height: 46px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  position: relative; z-index: 1;
}
.stat-content { position: relative; z-index: 1; }
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary, #e2e8f0);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 12px;
  color: var(--text-muted, #64748b);
  margin-top: 4px;
  letter-spacing: 1px;
}

/* 双栏布局 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

/* 面板 */
.panel {
  background: var(--glass-bg, rgba(30, 41, 59, 0.75));
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 260px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
}
.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-solid, #334155);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.panel-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #e2e8f0);
  margin: 0;
}
.panel-tip {
  font-size: 12px;
  color: var(--text-muted, #64748b);
}
.panel-body {
  padding: 16px;
  overflow-y: auto;
  flex: 1;
}
.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: var(--text-secondary, #94a3b8);
  font-size: 14px;
}

/* 截止日期列表 */
.deadline-list { display: flex; flex-direction: column; gap: 12px; }
.deadline-item {
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 10px;
  padding: 14px 16px;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.deadline-item:hover {
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  transform: translateY(-1px);
}
.deadline-item.urgency-today    { border-left: 3px solid var(--danger, #ef4444); }
.deadline-item.urgency-tomorrow { border-left: 3px solid var(--warning, #f59e0b); }
.deadline-item.urgency-soon     { border-left: 3px solid var(--success, #10b981); }

.deadline-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.deadline-project {
  font-weight: 600;
  color: var(--text-primary, #e2e8f0);
  font-size: 14px;
}
.deadline-version {
  color: var(--text-secondary, #94a3b8);
  font-size: 13px;
  margin-bottom: 4px;
}
.deadline-meta {
  color: var(--text-muted, #64748b);
  font-size: 12px;
  margin-bottom: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.deadline-meta em { font-style: normal; font-weight: 600; color: var(--text-secondary); }
.pm-tag {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-muted, #64748b);
  font-weight: 400;
}
.time-badge { font-size: 11px; }
.time-urgent { color: var(--danger, #ef4444) !important; font-weight: 600; }
.time-warn   { color: var(--warning, #f59e0b) !important; font-weight: 600; }
.deadline-assignees {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}
.assignee-label { font-size: 12px; color: var(--text-muted, #64748b); }
.assignee-tag { font-size: 11px !important; }
.no-assignee {
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
  font-style: italic;
}

/* 人员负载 */
.workload-list { display: flex; flex-direction: column; gap: 8px; }
.workload-item {
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.workload-item:hover {
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  transform: translateY(-1px);
}
.workload-item.expanded { border-color: rgba(59, 130, 246, 0.6); }
.workload-top {
  display: flex; justify-content: space-between; align-items: center;
}
.user-info { display: flex; align-items: center; gap: 12px; }
.user-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-blue, #3b82f6), var(--accent-purple, #8b5cf6));
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.user-detail { display: flex; flex-direction: column; }
.user-name { font-weight: 600; color: var(--text-primary, #e2e8f0); font-size: 14px; }
.user-role { font-size: 12px; color: var(--text-muted, #64748b); }
.workload-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
}
.workload-idle   { background: #334155; color: #94a3b8; }
.workload-normal { background: rgba(22, 163, 74, 0.15); color: #4ade80; }
.workload-busy   { background: rgba(220, 38, 38, 0.15); color: #f87171; }
.earliest-deadline {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted, #64748b);
  display: flex;
  align-items: center;
  gap: 8px;
}
.days-hint { font-weight: 600; }
.days-danger  { color: var(--danger, #ef4444); }
.days-warning { color: var(--warning, #f59e0b); }
.days-normal  { color: var(--success, #10b981); }

/* 任务展开 */
.task-details {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--bg-card-solid, #1e293b);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.task-item {
  background: rgba(30, 41, 59, 0.65);
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 8px;
  padding: 10px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.task-name {
  font-size: 13px;
  color: var(--text-primary, #e2e8f0);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.task-dot {
  width: 8px; height: 8px; border-radius: 50%;
  flex-shrink: 0;
}
.dot-danger  { background: var(--danger, #ef4444); }
.dot-warning { background: var(--warning, #f59e0b); }
.dot-normal  { background: var(--success, #10b981); }
.task-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted, #64748b);
  padding-left: 16px;
}
.task-extra-times {
  display: flex; gap: 12px;
  font-size: 11px;
  color: var(--text-muted, #64748b);
  padding-left: 16px;
  margin-top: 4px;
}
.task-pm { color: var(--text-muted, #475569); }
.task-deadline em {
  font-style: normal; margin-left: 6px; font-weight: 600;
}

/* 动画 */
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
  max-height: 600px;
  overflow: hidden;
}
.slide-enter-from, .slide-leave-to {
  max-height: 0;
  opacity: 0;
}

@media (max-width: 1280px) {
  .stat-cards { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1024px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
}
</style>
