<template>
  <div class="trend-container">
    <div class="page-header">
      <h2>📉 趋势报表</h2>
      <div class="header-controls">
        <el-radio-group v-model="months" size="small" @change="loadData">
          <el-radio-button :label="3">近3月</el-radio-button>
          <el-radio-button :label="6">近6月</el-radio-button>
          <el-radio-button :label="12">近1年</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="summary-row">
      <div class="summary-card">
        <div class="summary-val">{{ summary.totalVersions }}</div>
        <div class="summary-label">完成版本数</div>
      </div>
      <div class="summary-card">
        <div class="summary-val summary-danger">{{ summary.delayedVersions }}</div>
        <div class="summary-label">延期版本数</div>
      </div>
      <div class="summary-card">
        <div class="summary-val">{{ summary.avgAssignments }}</div>
        <div class="summary-label">月均任务量</div>
      </div>
      <div class="summary-card">
        <div class="summary-val">{{ summary.avgUsers }}</div>
        <div class="summary-label">月均参与人数</div>
      </div>
    </div>

    <div v-if="loading" class="loading-tip">加载中...</div>
    <template v-else>
      <!-- 图表行 -->
      <div class="chart-row">
        <div class="panel">
          <div class="panel-header"><h3>📊 版本完成 & 延期趋势</h3></div>
          <div class="panel-body">
            <div ref="versionChart" class="chart-box"></div>
          </div>
        </div>
        <div class="panel">
          <div class="panel-header"><h3>📈 任务量 & 参与人数</h3></div>
          <div class="panel-body">
            <div ref="taskChart" class="chart-box"></div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="panel full-width">
          <div class="panel-header"><h3>🏗️ 活跃项目数趋势</h3></div>
          <div class="panel-body">
            <div ref="projectChart" class="chart-box-sm"></div>
          </div>
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="panel">
        <div class="panel-header"><h3>📋 月度明细</h3></div>
        <div class="panel-body">
          <el-table :data="monthlyData" stripe>
            <el-table-column prop="label" label="月份" width="100" />
            <el-table-column prop="finishedVersions" label="完成版本" width="100" />
            <el-table-column prop="delayedVersions" label="延期版本" width="100">
              <template #default="{ row }">
                <span :class="{ 'text-danger': row.delayedVersions > 0 }">{{ row.delayedVersions }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="activeAssignments" label="任务量" width="100" />
            <el-table-column prop="activeProjects" label="活跃项目" width="100" />
            <el-table-column prop="activeUsers" label="参与人数" width="100" />
            <el-table-column label="延期率" width="100">
              <template #default="{ row }">
                <span :class="delayRateClass(row)">
                  {{ row.finishedVersions > 0 ? Math.round(row.delayedVersions / row.finishedVersions * 100) : 0 }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import request from '@/utils/request'
import * as echarts from 'echarts'

export default {
  name: 'TrendReportView',
  data() {
    return {
      months: 6,
      monthlyData: [],
      loading: false,
      versionChart: null,
      taskChart: null,
      projectChart: null
    }
  },
  computed: {
    summary() {
      const d = this.monthlyData
      if (!d.length) return { totalVersions: 0, delayedVersions: 0, avgAssignments: 0, avgUsers: 0 }
      const totalV = d.reduce((s, m) => s + m.finishedVersions, 0)
      const delayedV = d.reduce((s, m) => s + m.delayedVersions, 0)
      const totalA = d.reduce((s, m) => s + m.activeAssignments, 0)
      const totalU = d.reduce((s, m) => s + m.activeUsers, 0)
      return {
        totalVersions: totalV,
        delayedVersions: delayedV,
        avgAssignments: Math.round(totalA / d.length),
        avgUsers: Math.round(totalU / d.length)
      }
    }
  },
  mounted() {
    this.loadData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.disposeCharts()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await request({ url: '/api/analytics/monthly-trend', method: 'get', params: { months: this.months } })
        if (res && res.code === 200) {
          this.monthlyData = res.data || []
          this.$nextTick(() => this.renderCharts())
        }
      } catch (e) {
        console.error('trend error', e)
      } finally {
        this.loading = false
      }
    },
    renderCharts() {
      this.renderVersionChart()
      this.renderTaskChart()
      this.renderProjectChart()
    },
    renderVersionChart() {
      if (!this.$refs.versionChart) return
      if (this.versionChart) this.versionChart.dispose()
      this.versionChart = echarts.init(this.$refs.versionChart)
      const labels = this.monthlyData.map(m => m.label)
      const finished = this.monthlyData.map(m => m.finishedVersions)
      const delayed = this.monthlyData.map(m => m.delayedVersions)

      this.versionChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['完成版本', '延期版本'], textStyle: { color: '#94a3b8' }, bottom: 5 },
        grid: { top: 20, right: 20, bottom: 40, left: 50 },
        xAxis: { type: 'category', data: labels, axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#334155' } } },
        yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.3)' } } },
        series: [
          { name: '完成版本', type: 'bar', data: finished, itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] } },
          { name: '延期版本', type: 'bar', data: delayed, itemStyle: { color: '#ef4444', borderRadius: [4, 4, 0, 0] } }
        ]
      })
    },
    renderTaskChart() {
      if (!this.$refs.taskChart) return
      if (this.taskChart) this.taskChart.dispose()
      this.taskChart = echarts.init(this.$refs.taskChart)
      const labels = this.monthlyData.map(m => m.label)
      const assignments = this.monthlyData.map(m => m.activeAssignments)
      const users = this.monthlyData.map(m => m.activeUsers)

      this.taskChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['任务量', '参与人数'], textStyle: { color: '#94a3b8' }, bottom: 5 },
        grid: { top: 20, right: 50, bottom: 40, left: 50 },
        xAxis: { type: 'category', data: labels, axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#334155' } } },
        yAxis: [
          { type: 'value', name: '任务量', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.3)' } }, nameTextStyle: { color: '#64748b' } },
          { type: 'value', name: '人数', axisLabel: { color: '#94a3b8' }, splitLine: { show: false }, nameTextStyle: { color: '#64748b' } }
        ],
        series: [
          { name: '任务量', type: 'line', data: assignments, smooth: true, lineStyle: { color: '#8b5cf6', width: 3 }, itemStyle: { color: '#8b5cf6' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(139,92,246,0.3)' }, { offset: 1, color: 'rgba(139,92,246,0)' }] } } },
          { name: '参与人数', type: 'line', yAxisIndex: 1, data: users, smooth: true, lineStyle: { color: '#38bdf8', width: 3 }, itemStyle: { color: '#38bdf8' } }
        ]
      })
    },
    renderProjectChart() {
      if (!this.$refs.projectChart) return
      if (this.projectChart) this.projectChart.dispose()
      this.projectChart = echarts.init(this.$refs.projectChart)
      const labels = this.monthlyData.map(m => m.label)
      const projects = this.monthlyData.map(m => m.activeProjects)

      this.projectChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { top: 20, right: 20, bottom: 30, left: 50 },
        xAxis: { type: 'category', data: labels, axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#334155' } } },
        yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(51,65,85,0.3)' } } },
        series: [
          { type: 'line', data: projects, smooth: true, lineStyle: { color: '#10b981', width: 3 }, itemStyle: { color: '#10b981' },
            areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.3)' }, { offset: 1, color: 'rgba(16,185,129,0)' }] } } }
        ]
      })
    },
    handleResize() {
      if (this.versionChart) this.versionChart.resize()
      if (this.taskChart) this.taskChart.resize()
      if (this.projectChart) this.projectChart.resize()
    },
    disposeCharts() {
      if (this.versionChart) { this.versionChart.dispose(); this.versionChart = null }
      if (this.taskChart) { this.taskChart.dispose(); this.taskChart = null }
      if (this.projectChart) { this.projectChart.dispose(); this.projectChart = null }
    },
    delayRateClass(row) {
      if (!row.finishedVersions) return ''
      const rate = row.delayedVersions / row.finishedVersions
      if (rate > 0.3) return 'text-danger'
      if (rate > 0.1) return 'text-warn'
      return ''
    }
  }
}
</script>

<style scoped>
.trend-container { max-width: 1400px; margin: 0 auto; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
}
.page-header h2 { margin: 0; font-size: 20px; color: var(--text-primary); }

.summary-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px;
}
.summary-card {
  background: var(--glass-bg); border: 1px solid var(--glass-border);
  border-radius: 12px; padding: 16px; text-align: center;
}
.summary-val { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.summary-label { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.summary-danger { color: var(--danger) !important; }

.chart-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;
}
.full-width { grid-column: 1 / -1; }
.chart-box { height: 320px; width: 100%; }
.chart-box-sm { height: 260px; width: 100%; }

.text-danger { color: var(--danger); font-weight: 600; }
.text-warn { color: var(--warning); font-weight: 600; }

.loading-tip {
  text-align: center; padding: 60px 0; color: var(--text-secondary); font-size: 14px;
}

@media (max-width: 1024px) {
  .chart-row { grid-template-columns: 1fr; }
  .summary-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
