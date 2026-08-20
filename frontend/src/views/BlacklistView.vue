<template>
  <div class="blacklist-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🚫 黑榜事件管理</span>
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
          <el-button type="primary" @click="loadEvents">查询</el-button>
          <el-button v-if="isAdmin()" type="success" @click="showAddDialog">添加事件</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计信息 -->
      <el-alert
        v-if="stats.totalEvents > 0"
        :title="`本月共 ${stats.totalEvents} 条黑榜记录`"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <!-- 事件列表 -->
      <el-table
        :data="events"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#0f172a' }"
      >
        <el-table-column prop="eventDate" label="发生日期" width="120">
          <template #default="scope">
            {{ formatDate(scope.row.eventDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="人员" width="120">
          <template #default="scope">
            {{ scope.row.username || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="eventType" label="事件类型" width="150">
          <template #default="scope">
            <el-tag :type="getEventTypeTag(scope.row.eventType)">
              {{ getEventTypeText(scope.row.eventType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severityLevel" label="严重程度" width="100">
          <template #default="scope">
            <el-tag :type="getSeverityTag(scope.row.severityLevel)">
              {{ getSeverityText(scope.row.severityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="eventDescription" label="事件描述" min-width="200">
          <template #default="scope">
            {{ scope.row.eventDescription || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="projectName" label="关联项目" width="150">
          <template #default="scope">
            {{ scope.row.projectName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button v-if="isAdmin()" size="small" type="primary" @click="editEvent(scope.row)">编辑</el-button>
            <el-button v-if="isAdmin()" size="small" type="danger" @click="deleteEvent(scope.row.id)">删除</el-button>
            <span v-if="!isAdmin()" style="color: var(--text-muted); font-size: 12px">只读</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="events.length === 0 && !loading" description="暂无黑榜记录" :image-size="100" />
    </el-card>

    <!-- 添加/编辑事件弹窗 -->
    <el-dialog
      :title="dialogMode === 'add' ? '➕ 添加黑榜事件' : '✏️ 编辑黑榜事件'"
      v-model="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="eventForm" :model="currentEvent" :rules="eventRules" label-width="100px">
        <el-form-item label="选择人员" prop="userId">
          <el-select v-model="currentEvent.userId" placeholder="请选择人员" style="width: 100%" filterable>
            <el-option v-for="user in users" :key="user.id" :label="user.username" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="currentEvent.eventType" placeholder="请选择事件类型" style="width: 100%">
            <el-option label="延期交付" value="DELAY_DELIVERY" />
            <el-option label="需求遗漏" value="REQUIREMENT_MISSING" />
            <el-option label="代码规范违规" value="CODE_VIOLATION" />
            <el-option label="测试不通过" value="TEST_FAILED" />
            <el-option label="沟通问题" value="COMMUNICATION_ISSUE" />
            <el-option label="线上事故" value="PRODUCTION_INCIDENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度" prop="severityLevel">
          <el-radio-group v-model="currentEvent.severityLevel">
            <el-radio :label="1">⚪ 轻微</el-radio>
            <el-radio :label="2">⚪ 一般</el-radio>
            <el-radio :label="3">⚪ 严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="事件描述">
          <el-input
            v-model="currentEvent.eventDescription"
            type="textarea"
            :rows="3"
            placeholder="请简要描述事件（可选）"
          />
        </el-form-item>
        <el-form-item label="发生日期" prop="eventDate">
          <el-date-picker
            v-model="currentEvent.eventDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="currentEvent.projectId" placeholder="请选择项目（可选）" style="width: 100%" clearable>
            <el-option v-for="project in projects" :key="project.id" :label="project.projectName" :value="project.id" />
          </el-select>
        </el-form-item>
        <el-alert
          title="💡 提示：扣分将按「黑榜密度」公平计算，考虑工作量因素"
          type="info"
          :closable="false"
          style="margin-top: 10px"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEvent" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getBlacklistEvents, addBlacklistEvent, updateBlacklistEvent, deleteBlacklistEvent } from '@/api/blacklist'
import { getUsers } from '@/api/users'
import { getProjects } from '@/api/projects'
import { isAdmin } from '@/utils/auth'

export default {
  name: 'BlacklistView',
  data() {
    return {
      selectedMonth: this.getCurrentMonth(),
      events: [],
      users: [],
      projects: [],
      loading: false,
      saving: false,
      dialogVisible: false,
      dialogMode: 'add',
      currentEvent: this.emptyEvent(),
      eventRules: {
        userId: [{ required: true, message: '请选择人员', trigger: 'change' }],
        eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
        severityLevel: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
        eventDate: [{ required: true, message: '请选择发生日期', trigger: 'change' }]
      },
      stats: {
        totalEvents: 0
      }
    }
  },
  created() {
    this.loadUsersAndProjects()
    this.loadEvents()
  },
  methods: {
    isAdmin() {
      return isAdmin()
    },
    getCurrentMonth() {
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      return `${year}-${month}`
    },
    emptyEvent() {
      return {
        id: null,
        userId: null,
        eventType: '',
        eventDescription: '',
        severityLevel: 2,
        eventDate: '',
        projectId: null,
        versionId: null
      }
    },
    async loadUsersAndProjects() {
      try {
        const [usersRes, projectsRes] = await Promise.all([
          getUsers(),
          getProjects()
        ])
        this.users = usersRes.data || []
        this.projects = projectsRes.data || []
      } catch (error) {
        console.error('加载用户和项目失败:', error)
      }
    },
    async loadEvents() {
      this.loading = true
      try {
        const [year, month] = this.selectedMonth.split('-').map(Number)
        const res = await getBlacklistEvents({ year, month })
        if (res.code === 200) {
          this.events = res.data || []
          this.stats.totalEvents = this.events.length
          
          // 补充用户名和项目名
          this.enrichEventData()
        }
      } catch (error) {
        console.error('加载黑榜事件失败:', error)
        this.$message.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    enrichEventData() {
      const userMap = {}
      this.users.forEach(u => { userMap[u.id] = u.username })
      
      const projectMap = {}
      this.projects.forEach(p => { projectMap[p.id] = p.projectName })
      
      this.events.forEach(event => {
        event.username = userMap[event.userId] || '未知'
        event.projectName = projectMap[event.projectId] || null
      })
    },
    showAddDialog() {
      this.currentEvent = this.emptyEvent()
      this.currentEvent.eventDate = new Date()
      this.dialogMode = 'add'
      this.dialogVisible = true
    },
    editEvent(event) {
      this.currentEvent = { ...event }
      this.dialogMode = 'edit'
      this.dialogVisible = true
    },
    async saveEvent() {
      this.$refs.eventForm.validate(async (valid) => {
        if (!valid) return
        
        this.saving = true
        try {
          if (this.dialogMode === 'add') {
            await addBlacklistEvent(this.currentEvent)
            this.$message.success('添加成功')
          } else {
            await updateBlacklistEvent(this.currentEvent.id, this.currentEvent)
            this.$message.success('更新成功')
          }
          this.dialogVisible = false
          this.loadEvents()
        } catch (error) {
          console.error('保存失败:', error)
          this.$message.error('保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    async deleteEvent(id) {
      try {
        await this.$confirm('确认删除该黑榜事件？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteBlacklistEvent(id)
        this.$message.success('删除成功')
        this.loadEvents()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      if (isNaN(d.getTime())) return ''
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    getEventTypeText(type) {
      const map = {
        DELAY_DELIVERY: '延期交付',
        REQUIREMENT_MISSING: '需求遗漏',
        CODE_VIOLATION: '代码规范违规',
        TEST_FAILED: '测试不通过',
        COMMUNICATION_ISSUE: '沟通问题',
        PRODUCTION_INCIDENT: '线上事故',
        OTHER: '其他'
      }
      return map[type] || type
    },
    getEventTypeTag(type) {
      const map = {
        DELAY_DELIVERY: 'warning',
        REQUIREMENT_MISSING: 'danger',
        CODE_VIOLATION: 'info',
        TEST_FAILED: 'warning',
        COMMUNICATION_ISSUE: 'info',
        PRODUCTION_INCIDENT: 'danger',
        OTHER: ''
      }
      return map[type] || ''
    },
    getSeverityText(level) {
      const map = { 1: '轻微', 2: '一般', 3: '严重' }
      return map[level] || '未知'
    },
    getSeverityTag(level) {
      const map = { 1: 'info', 2: 'warning', 3: 'danger' }
      return map[level] || 'info'
    }
  }
}
</script>

<style scoped>
.blacklist-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.filter-form {
  margin-bottom: 16px;
}
</style>
