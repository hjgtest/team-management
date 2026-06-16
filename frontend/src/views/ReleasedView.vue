<template>
  <div class="page-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="stat-tags">
          <span class="stat-tag">
            <span class="dot dot-released"></span>
            已释放：<strong>{{ releasedCount }}</strong>
          </span>
          <span class="stat-tag">
            <span class="dot dot-upcoming"></span>
            即将释放：<strong>{{ upcomingCount }}</strong>
          </span>
        </div>
      </div>
      <div class="toolbar-right">
        <span class="filter-label">未来 N 天即将释放：</span>
        <el-input-number v-model="days" :min="1" :max="60" size="small" style="width: 110px;" @change="loadList" />
        <el-button type="primary" size="small" @click="loadList">刷新</el-button>
      </div>
    </div>

    <!-- 卡片网格 -->
    <div v-if="loading" class="empty-hint">加载中…</div>
    <div v-else-if="!list.length" class="empty-hint">暂无可释放人员</div>
    <div v-else class="cards-grid">
      <div
        v-for="p in list"
        :key="p.userId"
        class="person-card"
        :class="p.status === 'released' ? 'card-released' : 'card-upcoming'">
        <div class="card-header">
          <div class="person-info">
            <div class="person-avatar">{{ avatarText(p.username) }}</div>
            <div>
              <div class="person-name">{{ p.username }}</div>
              <div class="person-meta">{{ p.roleName || '—' }} · {{ p.teamName }}</div>
            </div>
          </div>
          <el-tag size="small" :type="p.status === 'released' ? 'success' : 'warning'">
            {{ p.status === 'released' ? '已释放' : '即将释放' }}
          </el-tag>
        </div>
        <div class="card-body">
          <div v-if="p.status === 'released'" class="released-badge">
            ✅ 当前空闲，可随时分配
          </div>
          <div v-else class="release-countdown">
            <div class="days-number">{{ p.daysLeft }}</div>
            <div class="days-text">天后释放（{{ formatDate(p.releaseDate) }}）</div>
          </div>

          <div v-if="p.tasks && p.tasks.length" class="task-list">
            <div class="task-list-title">📋 进行中任务（{{ p.tasks.length }}）</div>
            <div v-for="(t, ti) in p.tasks" :key="ti" class="task-row">
              <div style="display: flex; align-items: center;">
                <span class="task-project">{{ t.projectName }}</span>
                <span class="task-version">/ {{ t.versionName }}</span>
                <span class="task-days" :class="taskDaysClass(t.daysLeft)">{{ formatDays(t.daysLeft) }}</span>
              </div>
              <div v-if="t.feature" class="task-feature">· {{ t.feature }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { isAdmin } from '@/utils/auth'
import { getReleasedList } from '@/api/released'

export default {
  name: 'ReleasedView',
  data() {
    return {
      days: 7,
      list: [],
      loading: false
    }
  },
  computed: {
    releasedCount() {
      return this.list.filter(p => p.status === 'released').length
    },
    upcomingCount() {
      return this.list.filter(p => p.status === 'upcoming').length
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    isAdmin() { return isAdmin() },
    async loadList() {
      this.loading = true
      try {
        const res = await getReleasedList(this.days)
        if (res && res.code === 200) this.list = res.data || []
      } catch (e) {
        console.error('released list error', e)
        this.$message && this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    avatarText(name) {
      if (!name) return '?'
      return name.trim().charAt(0).toUpperCase()
    },
    formatDate(dt) {
      if (!dt) return '—'
      const d = new Date(dt)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
    },
    formatDays(n) {
      if (n == null) return '—'
      if (n < 0) return `已过期${Math.abs(n)}天`
      if (n === 0) return '今天'
      return `${n} 天`
    },
    taskDaysClass(n) {
      if (n == null) return ''
      if (n <= 1) return 'text-danger'
      if (n <= 3) return 'text-warning'
      return ''
    }
  }
}
</script>

<style scoped>
.page-container { max-width: 1400px; margin: 0 auto; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 14px 20px;
  background: var(--glass-bg, rgba(30, 41, 59, 0.75));
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  flex-wrap: wrap;
  gap: 12px;
  backdrop-filter: blur(12px);
}
.toolbar-left, .toolbar-right {
  display: flex; gap: 12px; align-items: center;
}
.filter-label {
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
}
.stat-tags { display: flex; gap: 16px; }
.stat-tag {
  font-size: 14px;
  color: var(--text-secondary, #94a3b8);
  display: flex; align-items: center; gap: 6px;
}
.stat-tag strong {
  font-size: 18px;
  margin-left: 4px;
  color: var(--text-primary, #e2e8f0);
  font-variant-numeric: tabular-nums;
}
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot-released { background: var(--success, #10b981); box-shadow: 0 0 8px var(--success, #10b981); }
.dot-upcoming { background: var(--warning, #f59e0b); box-shadow: 0 0 8px var(--warning, #f59e0b); }

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.person-card {
  background: var(--glass-bg, rgba(30, 41, 59, 0.75));
  border: 1px solid var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.35s;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(12px);
}
.person-card:hover {
  border-color: rgba(59, 130, 246, 0.6);
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}
.card-released { border-left: 3px solid var(--success, #10b981); }
.card-upcoming { border-left: 3px solid var(--warning, #f59e0b); }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-solid, #334155);
}
.person-info { display: flex; align-items: center; gap: 12px; }
.person-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-blue, #3b82f6), var(--accent-purple, #8b5cf6));
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.person-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary, #e2e8f0);
}
.person-meta {
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
  margin-top: 2px;
}
.card-body { padding: 14px 16px; }

.released-badge {
  color: var(--success, #10b981);
  font-size: 13px;
  padding: 10px 12px;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 8px;
  text-align: center;
  font-weight: 500;
}
.release-countdown {
  text-align: center;
  padding: 8px 0;
}
.days-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--warning, #f59e0b);
  font-variant-numeric: tabular-nums;
  text-shadow: 0 2px 12px rgba(245, 158, 11, 0.4);
}
.days-text {
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
  margin-top: 2px;
}

.task-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-solid, #334155);
}
.task-list-title {
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}
.task-row {
  padding: 6px 0;
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
  border-bottom: 1px solid rgba(99, 122, 161, 0.12);
  transition: all 0.2s;
}
.task-row:hover {
  background: rgba(59, 130, 246, 0.06);
  border-radius: 4px;
}
.task-row:last-child { border-bottom: none; }
.task-project {
  color: var(--text-primary, #e2e8f0);
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-version {
  color: var(--text-muted, #64748b);
  margin: 0 8px;
  flex-shrink: 0;
}
.task-days {
  color: var(--success, #10b981);
  font-weight: 600;
  flex-shrink: 0;
  min-width: 60px;
  text-align: right;
  margin-left: auto;
}
.task-feature {
  font-size: 12px;
  color: var(--text-muted, #64748b);
  margin-top: 2px;
  padding-left: 8px;
}

.text-danger  { color: var(--danger, #ef4444) !important; }
.text-warning { color: var(--warning, #f59e0b) !important; }

.empty-hint {
  text-align: center;
  padding: 60px 0;
  color: var(--text-secondary, #94a3b8);
  font-size: 14px;
  background: var(--glass-bg, rgba(30, 41, 59, 0.5));
  border: 1px dashed var(--glass-border, rgba(51, 65, 85, 0.5));
  border-radius: 12px;
}

@media (max-width: 768px) {
  .cards-grid { grid-template-columns: 1fr; }
  .toolbar { flex-direction: column; align-items: flex-start; }
}
</style>
