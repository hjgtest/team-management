<template>
  <div class="bug-stats-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span> Bug统计考核报表</span>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="选择月份">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="选择月份"
            format="YYYY年MM月"
            value-format="YYYY-MM"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="generateReport" :loading="loading">生成报表</el-button>
        </el-form-item>
      </el-form>

      <!-- 汇总信息 -->
      <el-alert
        v-if="reportData.summary"
        title="本月概况"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <div class="summary-content">
          <span>总人数：{{ reportData.summary.totalUsers }}人</span>
          <span>平均Bug密度：{{ reportData.summary.avgBugDensity.toFixed(2) }}</span>
          <span v-if="reportData.summary.bestPerformer">最佳表现：{{ reportData.summary.bestPerformer }}</span>
        </div>
      </el-alert>

      <!-- 统计表格 -->
      <el-table
        v-loading="loading"
        :data="reportData.rankings"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#0f172a', color: '#fff' }"
      >
        <el-table-column prop="rank" label="排名" width="60" align="center">
          <template #default="scope">
            <el-tag :type="getRankTagType(scope.row.rank)" size="small" effect="dark">
              {{ scope.row.rank }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="姓名" width="100" align="center"></el-table-column>
        <el-table-column prop="projectCount" label="项目数" width="80" align="center"></el-table-column>
        <el-table-column prop="versionCount" label="版本数" width="80" align="center"></el-table-column>
        <el-table-column prop="workDays" label="工作天" width="80" align="center"></el-table-column>
        <el-table-column prop="featureCount" label="功能数" width="80" align="center"></el-table-column>
        
        <el-table-column label="Bug详情" min-width="300">
          <template #default="scope">
            <div class="bug-levels">
              <el-tag
                v-for="(level, idx) in scope.row.bugLevels"
                :key="idx"
                size="small"
                :type="getBugLevelType(level.levelName)"
                style="margin-right: 5px; margin-bottom: 5px"
              >
                {{ level.levelName }} {{ level.count }}个
              </el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="totalBugs" label="Bug总数" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.totalBugs > 10 ? 'danger' : 'info'" size="small">
              {{ scope.row.totalBugs }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="bugDensity" label="Bug密度" width="100" align="center">
          <template #default="scope">
            <span :class="getDensityClass(scope.row.bugDensity)">
              {{ scope.row.bugDensity.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="qualityScore" label="Bug质量分" width="110" align="center">
          <template #default="scope">
            <el-progress
              :percentage="scope.row.qualityScore"
              :status="getProgressStatus(scope.row.qualityScore)"
              :stroke-width="12"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="blacklistCount" label="黑榜次数" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.blacklistCount > 0" type="warning" size="small">
              {{ scope.row.blacklistCount }}次
            </el-tag>
            <span v-else style="color: var(--text-muted)">0</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="blacklistDensity" label="黑榜密度" width="110" align="center">
          <template #default="scope">
            <span :class="getBlacklistDensityClass(scope.row.blacklistDensity)">
              {{ scope.row.blacklistDensity.toFixed(3) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="blacklistAdjustment" label="黑榜调整" width="110" align="center">
          <template #default="scope">
            <span :style="{ color: scope.row.blacklistAdjustment >= 0 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
              {{ scope.row.blacklistAdjustment >= 0 ? '+' : '' }}{{ scope.row.blacklistAdjustment.toFixed(1) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="finalScore" label="最终得分" width="110" align="center">
          <template #default="scope">
            <el-progress
              :percentage="scope.row.finalScore"
              :status="getProgressStatus(scope.row.finalScore)"
              :stroke-width="14"
              style="font-weight: bold"
            />
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && reportData.rankings && reportData.rankings.length === 0"
        description="暂无数据，请选择月份后生成报表"
        :image-size="120"
      />

      <!-- 说明 -->
      <el-divider></el-divider>
      <el-alert
        title="指标说明"
        type="success"
        :closable="false"
      >
        <ul>
          <li><strong>Bug密度</strong> = Bug总数 / 工作天数（密度越低越好）</li>
          <li><strong>Bug质量分</strong> = 100 - (Bug密度 × 10)，满分100分</li>
          <li><strong>黑榜密度</strong> = 黑榜事件数 / 功能模块数（考虑工作量因素）</li>
          <li><strong>黑榜调整</strong> = (个人黑榜密度 - 团队平均密度) × 20（正数为加分，负数为扣分）</li>
          <li><strong>最终得分</strong> = Bug质量分 + 黑榜调整分</li>
          <li><strong>工作天数</strong> = 参与版本的研发周期天数（去重计算）</li>
          <li>密度越低、质量分越高，表现越好；黑榜调整体现公平性</li>
        </ul>
      </el-alert>
    </el-card>
  </div>
</template>

<script>
import { getMonthlyReport } from '@/api/bugStats'

export default {
  name: 'BugStatsView',
  data() {
    return {
      loading: false,
      selectedMonth: '',
      reportData: {
        summary: null,
        rankings: []
      }
    }
  },
  methods: {
    async generateReport() {
      if (!this.selectedMonth) {
        this.$message.warning('请选择月份')
        return
      }

      const [year, month] = this.selectedMonth.split('-').map(Number)
      
      this.loading = true
      try {
        const res = await getMonthlyReport({ year, month })
        // 后端直接返回Map，不是标准响应格式
        if (res && res.rankings) {
          this.reportData = res
          this.$message.success('报表生成成功')
        } else {
          this.$message.error('获取数据失败')
        }
      } catch (error) {
        console.error('获取Bug统计报表失败:', error)
        this.$message.error('获取数据失败')
      } finally {
        this.loading = false
      }
    },

    getRankTagType(rank) {
      if (rank === 1) return 'success'
      if (rank === 2) return 'warning'
      if (rank === 3) return 'info'
      return ''
    },

    getBugLevelType(levelName) {
      if (levelName.includes('一') || levelName.toLowerCase().includes('p0')) return 'danger'
      if (levelName.includes('二') || levelName.toLowerCase().includes('p1')) return 'warning'
      if (levelName.includes('三') || levelName.toLowerCase().includes('p2')) return 'info'
      return ''
    },

    getDensityClass(density) {
      if (density <= 0.5) return 'density-low'
      if (density <= 1.0) return 'density-medium'
      return 'density-high'
    },

    getBlacklistDensityClass(density) {
      if (density === 0) return 'density-low'
      if (density <= 0.2) return 'density-medium'
      return 'density-high'
    },

    getProgressStatus(score) {
      if (score >= 90) return 'success'
      if (score >= 70) return 'warning'
      return 'exception'
    }
  }
}
</script>

<style scoped>
.bug-stats-container {
  padding: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
}

.filter-form {
  margin-bottom: 20px;
}

.summary-content {
  display: flex;
  gap: 30px;
  font-size: 14px;
}

.summary-content span {
  font-weight: 500;
}

.bug-levels {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.density-low {
  color: #67c23a;
  font-weight: 600;
}

.density-medium {
  color: #e6a23c;
  font-weight: 600;
}

.density-high {
  color: #f56c6c;
  font-weight: 600;
}
</style>
