<template>
  <div class="heatmap-container">
    <div class="page-header">
      <h2>🔥 团队负载热力图</h2>
      <span class="header-hint">按小组展示成员当前任务负载分布</span>
    </div>

    <!-- 总览统计 -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-val">{{ totalMembers }}</div>
        <div class="summary-label">在职成员</div>
      </div>
      <div class="summary-card">
        <div class="summary-val">{{ avgLoad.toFixed(1) }}</div>
        <div class="summary-label">平均负载</div>
      </div>
      <div class="summary-card">
        <div class="summary-val summary-danger">{{ overloadedCount }}</div>
        <div class="summary-label">超负荷(>3)</div>
      </div>
      <div class="summary-card">
        <div class="summary-val summary-success">{{ idleCount }}</div>
        <div class="summary-label">空闲人员</div>
      </div>
    </div>

    <!-- 负载分布图 -->
    <div class="panel chart-panel">
      <div class="panel-header"><h3>📊 负载分布</h3></div>
      <div class="panel-body">
        <div ref="distChart" class="chart-box"></div>
      </div>
    </div>

    <!-- 按小组展示 -->
    <div v-if="loading" class="loading-tip">加载中...</div>
    <div v-else-if="!groups.length" class="empty-tip">暂无数据</div>
    <div v-else class="team-groups">
      <div v-for="group in groups" :key="group.teamId" class="team-group">
        <div class="group-header">
          <h3>{{ group.teamName }}</h3>
          <div class="group-stats">
            <span>成员 {{ group.members.length }} 人</span>
            <span>平均负载 <em :class="loadClass(group.avgLoad)">{{ group.avgLoad.toFixed(1) }}</em></span>
          </div>
        </div>
        <div class="member-grid">
          <div
            v-for="m in group.members"
            :key="m.userId"
            class="member-card"
            :class="cardClass(m.ongoingCount)">
            <div class="member-top">
              <div class="member-avatar">{{ m.username ? m.username.charAt(0) : '?' }}</div>
              <div class="member-info">
                <div class="member-name">{{ m.username }}</div>
                <div class="member-role">{{ m.roleName }}</div>
              </div>
              <div class="load-badge" :class="badgeClass(m.ongoingCount)">
                {{ m.ongoingCount }}
              </div>
            </div>
            <!-- 负载条 -->
            <div class="load-bar">
              <div class="load-fill" :style="{ width: Math.min(m.ongoingCount / m.maxCapacity * 100, 100) + '%' }"
                   :class="barClass(m.ongoingCount)"></div>
            </div>
            <!-- 任务列表 -->
            <div v-if="m.tasks && m.tasks.length" class="member-tasks">
              <div v-for="(t, ti) in m.tasks" :key="ti" class="mini-task">
                <span class="task-dot" :class="dotClass(t.daysLeft)"></span>
                {{ t.versionName }}
                <span class="task-days" :class="daysClass(t.daysLeft)">{{ formatDays(t.daysLeft) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import * as echarts from 'echarts'

export default {
  name: 'WorkloadHeatmapView',
  data() {
    return {
      groups: [],
      loading: false,
      chart: null
    }
  },
  computed: {
    totalMembers() {
      return this.groups.reduce((sum, g) => sum + g.members.length, 0)
    },
    avgLoad() {
      const all = this.groups.flatMap(g => g.members)
      if (!all.length) return 0
      return all.reduce((sum, m) => sum + m.ongoingCount, 0) / all.length
    },
    overloadedCount() {
      return this.groups.flatMap(g => g.members).filter(m => m.ongoingCount > 3).length
    },
    idleCount() {
      return this.groups.flatMap(g => g.members).filter(m => m.ongoingCount === 0).length
    }
  },
  mounted() {
    this.loadData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    if (this.chart) this.chart.dispose()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await request({ url: '/api/analytics/workload-heatmap', method: 'get' })
        if (res && res.code === 200) {
          this.groups = res.data || []
          this.$nextTick(() => this.renderChart())
        }
      } catch (e) {
        console.error('heatmap error', e)
      } finally {
        this.loading = false
      }
    },
    renderChart() {
      if (!this.$refs.distChart) return
      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(this.$refs.distChart)

      const allMembers = this.groups.flatMap(g => g.members)
      const idle = allMembers.filter(m => m.ongoingCount === 0).length
      const light = allMembers.filter(m => m.ongoingCount >= 1 && m.ongoingCount <= 2).length
      const normal = allMembers.filter(m => m.ongoingCount === 3).length
      const busy = allMembers.filter(m => m.ongoingCount > 3).length

      this.chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
        legend: { bottom: 10, textStyle: { color: '#94a3b8' } },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 8, borderColor: '#1e293b', borderWidth: 2 },
          label: { color: '#e2e8f0', formatter: '{b}\n{c}人' },
          data: [
            { value: idle, name: '空闲(0)', itemStyle: { color: '#64748b' } },
            { value: light, name: '轻量(1-2)', itemStyle: { color: '#10b981' } },
            { value: normal, name: '正常(3)', itemStyle: { color: '#3b82f6' } },
            { value: busy, name: '超负荷(>3)', itemStyle: { color: '#ef4444' } }
          ]
        }]
      })
    },
    handleResize() {
      if (this.chart) this.chart.resize()
    },
    loadClass(v) { return v > 3 ? 'text-danger' : v > 1 ? 'text-warn' : '' },
    cardClass(n) {
      if (n === 0) return 'card-idle'
      if (n <= 2) return 'card-normal'
      if (n <= 3) return 'card-busy'
      return 'card-overload'
    },
    badgeClass(n) {
      if (n === 0) return 'badge-idle'
      if (n <= 2) return 'badge-normal'
      if (n <= 3) return 'badge-warn'
      return 'badge-danger'
    },
    barClass(n) {
      if (n <= 2) return 'bar-ok'
      if (n <= 3) return 'bar-warn'
      return 'bar-danger'
    },
    dotClass(d) {
      if (d == null) return ''
      if (d <= 1) return 'dot-danger'
      if (d <= 3) return 'dot-warn'
      return ''
    },
    daysClass(d) {
      if (d == null) return ''
      if (d <= 1) return 'text-danger'
      if (d <= 3) return 'text-warn'
      return ''
    },
    formatDays(n) {
      if (n == null) return '—'
      if (n < 0) return '已过期' + Math.abs(n) + '天'
      if (n === 0) return '今天'
      return n + '天'
    }
  }
}
</script>

<style scoped>
.heatmap-container { max-width: 1400px; margin: 0 auto; }
.page-header {
  display: flex; align-items: baseline; gap: 14px; margin-bottom: 20px;
}
.page-header h2 { margin: 0; font-size: 20px; color: var(--text-primary); }
.header-hint { font-size: 13px; color: var(--text-muted); }

.summary-cards {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px;
}
.summary-card {
  background: var(--glass-bg); border: 1px solid var(--glass-border);
  border-radius: 12px; padding: 16px; text-align: center;
}
.summary-val { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.summary-label { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.summary-danger { color: var(--danger) !important; }
.summary-success { color: var(--success) !important; }

.chart-panel { margin-bottom: 20px; }
.chart-box { height: 300px; width: 100%; }

.team-groups { display: flex; flex-direction: column; gap: 20px; }
.team-group {
  background: var(--glass-bg); border: 1px solid var(--glass-border);
  border-radius: 14px; padding: 18px; box-shadow: 0 4px 16px rgba(0,0,0,0.2);
}
.group-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--glass-border);
}
.group-header h3 { margin: 0; font-size: 16px; color: var(--text-primary); }
.group-stats { display: flex; gap: 16px; font-size: 13px; color: var(--text-secondary); }
.group-stats em { font-style: normal; font-weight: 600; }

.member-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px;
}
.member-card {
  background: rgba(15, 23, 42, 0.6); border: 1px solid var(--glass-border);
  border-radius: 10px; padding: 14px; transition: all 0.25s;
}
.member-card:hover { border-color: rgba(59,130,246,0.5); transform: translateY(-2px); }
.card-idle { border-left: 3px solid #64748b; }
.card-normal { border-left: 3px solid #10b981; }
.card-busy { border-left: 3px solid #3b82f6; }
.card-overload { border-left: 3px solid #ef4444; }

.member-top { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.member-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.member-info { flex: 1; }
.member-name { font-weight: 600; font-size: 14px; color: var(--text-primary); }
.member-role { font-size: 11px; color: var(--text-muted); }
.load-badge {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700;
}
.badge-idle { background: rgba(100,116,139,0.2); color: #94a3b8; }
.badge-normal { background: rgba(16,185,129,0.2); color: #4ade80; }
.badge-warn { background: rgba(59,130,246,0.2); color: #60a5fa; }
.badge-danger { background: rgba(239,68,68,0.2); color: #f87171; }

.load-bar {
  height: 6px; background: rgba(51,65,85,0.5); border-radius: 3px; margin-bottom: 10px; overflow: hidden;
}
.load-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
.bar-ok { background: linear-gradient(90deg, #10b981, #34d399); }
.bar-warn { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.bar-danger { background: linear-gradient(90deg, #ef4444, #f87171); }

.member-tasks { display: flex; flex-direction: column; gap: 4px; }
.mini-task {
  font-size: 12px; color: var(--text-secondary);
  display: flex; align-items: center; gap: 6px;
}
.task-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; background: #64748b; }
.dot-danger { background: #ef4444; }
.dot-warn { background: #f59e0b; }
.task-days { margin-left: auto; font-weight: 600; font-size: 11px; }

.text-danger { color: var(--danger); }
.text-warn { color: var(--warning); }

.loading-tip, .empty-tip {
  text-align: center; padding: 60px 0; color: var(--text-secondary); font-size: 14px;
}

@media (max-width: 768px) {
  .summary-cards { grid-template-columns: repeat(2, 1fr); }
  .member-grid { grid-template-columns: 1fr; }
}
</style>
