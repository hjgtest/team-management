<template>
  <div class="smart-schedule-container">
    <div class="page-header">
      <h2>智能排期</h2>
      <p class="page-desc">选择项目并输入版本信息，系统将自动根据人员和项目安排进行智能排期</p>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：排期表单 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span>排期信息</span>
          </template>

          <el-form
            ref="scheduleFormRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            label-position="right"
          >
            <!-- 项目选择 -->
            <el-form-item label="选择项目" prop="projectId">
              <el-select
                v-model="form.projectId"
                placeholder="请选择项目"
                filterable
                style="width: 100%"
                @change="onProjectChange"
              >
                <el-option
                  v-for="item in projectOptions"
                  :key="item.id"
                  :label="item.projectName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <!-- 版本号 -->
            <el-form-item label="版本号" prop="versionName">
              <el-input
                v-model="form.versionName"
                placeholder="例如：V1.0.0"
                clearable
              />
            </el-form-item>

            <!-- 产品经理 -->
            <el-form-item label="产品经理" prop="productManager">
              <el-input
                v-model="form.productManager"
                placeholder="请输入产品经理姓名"
                clearable
              />
            </el-form-item>

            <!-- 原型地址 -->
            <el-form-item label="原型地址" prop="prototypeUrl">
              <el-input
                v-model="form.prototypeUrl"
                placeholder="请输入原型地址URL"
                clearable
              />
            </el-form-item>

            <!-- 小组选择 -->
            <el-divider content-position="left">可选小组</el-divider>

            <el-form-item label="选择小组" prop="teamIds">
              <el-select
                v-model="form.teamIds"
                placeholder="不选择则搜索所有小组"
                multiple
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="team in teamOptions"
                  :key="team.id"
                  :label="team.teamName"
                  :value="team.id"
                />
              </el-select>
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                留空表示不限制小组，从所有人员中分配
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 分配项设置 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>分配设置</span>
              <el-button v-if="isAdmin()" type="primary" size="small" @click="addAssignment">
                添加分配项
              </el-button>
              <span v-if="!isAdmin()" style="font-size:12px;color:#909399">只读</span>
            </div>
          </template>

          <div v-if="form.assignments.length === 0" class="empty-tip">
            <el-empty description="请添加需要分配的角色和功能" :image-size="80" />
          </div>

          <div
            v-for="(item, index) in form.assignments"
            :key="index"
            class="assignment-item"
          >
            <el-divider v-if="index > 0" />
            <div class="assignment-header">
              <span class="assignment-index">分配项 {{ index + 1 }}</span>
              <el-button
                v-if="isAdmin()"
                type="danger"
                size="small"
                circle
                @click="removeAssignment(index)"
              >✕</el-button>
            </div>

            <el-form label-width="80px" label-position="right">
              <el-form-item label="角色" :prop="'assignments.' + index + '.roleId'" :rules="roleRule">
                <el-select
                  v-model="item.roleId"
                  placeholder="请选择角色"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="role in roleOptions"
                    :key="role.id"
                    :label="role.roleName"
                    :value="String(role.id)"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="功能描述" :prop="'assignments.' + index + '.feature'" :rules="featureRule">
                <el-input
                  v-model="item.feature"
                  type="textarea"
                  :rows="2"
                  placeholder="请输入该角色负责的功能描述"
                />
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button v-if="isAdmin()" type="primary" size="large" :loading="loading" @click="handleSchedule">
            开始智能排期
          </el-button>
          <el-button size="large" @click="resetForm">
            重置
          </el-button>
        </div>
      </el-col>

      <!-- 右侧：排期规则说明 + 项目历史 -->
      <el-col :span="10">
        <!-- 排期规则 -->
        <el-card shadow="hover">
          <template #header>
            <span>排期规则</span>
          </template>
          <div class="rule-list">
            <div class="rule-item rule-1">
              <span style="font-size: 20px;">✅</span>
              <div>
                <strong>第一优先</strong>
                <p>做过该项目 且 手头任务 &lt; 4 件</p>
              </div>
            </div>
            <div class="rule-item rule-2">
              <span style="font-size: 20px;">✅</span>
              <div>
                <strong>第二优先</strong>
                <p>空闲人员（手头任务 = 0 件）</p>
              </div>
            </div>
            <div class="rule-item rule-3">
              <span style="font-size: 20px;">✅</span>
              <div>
                <strong>第三优先</strong>
                <p>手头任务 &lt; 4 件 但未做过该项目</p>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 项目历史版本 -->
        <el-card shadow="hover" style="margin-top: 20px" v-if="form.projectId">
          <template #header>
            <span>项目历史版本</span>
          </template>
          <div v-loading="historyLoading">
            <div v-if="historyVersions.length === 0" class="empty-tip">
              <el-empty description="暂无历史版本" :image-size="60" />
            </div>
            <div v-else>
              <div
                v-for="ver in historyVersions"
                :key="ver.id"
                class="history-item"
              >
                <div class="history-version-name">{{ ver.versionName }}</div>
                <div class="history-version-info">
                  <span v-if="ver.productManager">PM: {{ ver.productManager }}</span>
                  <span v-if="ver.endTime">{{ ver.endTime }}</span>
                </div>
                <div class="history-assignments">
                  <el-tag
                    v-for="a in ver.assignments"
                    :key="a.id"
                    size="small"
                    style="margin-right: 5px; margin-top: 5px"
                  >
                    {{ a.userName }} - {{ a.roleName || '未设置角色' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 人员负载概览 -->
        <el-card shadow="hover" style="margin-top: 20px">
          <template #header>
            <span>人员负载概览</span>
          </template>
          <div v-loading="userLoading">
            <div v-if="userWorkload.length === 0" class="empty-tip">
              <el-empty description="暂无人员数据" :image-size="60" />
            </div>
            <div v-else>
              <div
                v-for="user in userWorkload"
                :key="user.id"
                class="user-workload-item"
              >
                <div class="user-info">
                  <span class="username">{{ user.username }}</span>
                  <el-tag
                    :type="user.ongoingProjectsCount === 0 ? 'success' : user.ongoingProjectsCount < 4 ? 'warning' : 'danger'"
                    size="small"
                  >
                    {{ user.ongoingProjectsCount || 0 }} 件任务
                  </el-tag>
                </div>
                <div class="user-role">{{ user.roleName || '未分配角色' }} | {{ user.teamName || '未分组' }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 排期结果弹窗 -->
    <el-dialog
      v-model="resultVisible"
      title="智能排期结果"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="scheduleResult">
        <el-alert
          :type="scheduleResult.success ? 'success' : 'error'"
          :title="scheduleResult.message"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
        <div v-if="scheduleResult.startTime" style="margin-bottom: 15px;">
          <el-tag type="info" size="large">
            自动排期时间：{{ formatTime(scheduleResult.startTime) }} 至 {{ formatTime(scheduleResult.endTime) }}
          </el-tag>
        </div>

        <div v-if="scheduleResult.details && scheduleResult.details.length > 0">
          <h4>分配详情：</h4>
          <el-table :data="scheduleResult.details" border style="width: 100%; margin-top: 10px">
            <el-table-column prop="roleName" label="角色" width="120" />
            <el-table-column prop="feature" label="负责功能" min-width="150" />
            <el-table-column prop="username" label="分配人员" width="120" />
            <el-table-column label="优先级说明" min-width="200">
              <template #default="{ row }">
                <el-tag
                  :type="getPriorityType(row)"
                  size="small"
                >
                  {{ row.priorityDesc }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ongoingCount" label="当前任务数" width="100" align="center" />
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" @click="resultVisible = false">
          确定
        </el-button>
        <el-button @click="viewProject">
          查看项目
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 使用 emoji 代替图标，避免额外依赖
import { isAdmin } from '@/utils/auth'
import { getProjects } from '@/api/projects'
import { getRoles } from '@/api/roles'
import { getTeams } from '@/api/teams'
import { getProjectVersions } from '@/api/projectVersions'
import { getProjectAssignments } from '@/api/projectAssignments'
import { getUsers } from '@/api/users'
import { smartSchedule } from '@/api/smartSchedule'

const router = useRouter()

// 表单数据
const scheduleFormRef = ref(null)
const loading = ref(false)
const historyLoading = ref(false)
const userLoading = ref(false)
const resultVisible = ref(false)
const scheduleResult = ref(null)

const form = reactive({
  projectId: null,
  versionName: '',
  productManager: '',
  prototypeUrl: '',
  teamIds: [],
  assignments: []
})

// 选项数据
const projectOptions = ref([])
const roleOptions = ref([])
const teamOptions = ref([])
const historyVersions = ref([])
const userWorkload = ref([])

// 表单校验规则
const rules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  versionName: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  productManager: [{ required: true, message: '请输入产品经理', trigger: 'blur' }],
  prototypeUrl: [{ required: true, message: '请输入原型地址', trigger: 'blur' }]
}

const roleRule = [{ required: true, message: '请选择角色', trigger: 'change' }]
const featureRule = [{ required: true, message: '请输入功能描述', trigger: 'blur' }]

// 加载项目列表
const loadProjects = async () => {
  try {
    const res = await getProjects()
    if (res.code === 200) {
      projectOptions.value = res.data || []
    }
  } catch (err) {
    ElMessage.error('加载项目列表失败')
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const res = await getRoles()
    if (res.code === 200) {
      roleOptions.value = res.data || []
    }
  } catch (err) {
    ElMessage.error('加载角色列表失败')
  }
}

// 加载小组列表
const loadTeams = async () => {
  try {
    const res = await getTeams()
    if (res.code === 200) {
      teamOptions.value = res.data || []
    }
  } catch (err) {
    ElMessage.error('加载小组列表失败')
  }
}

// 加载人员负载
const loadUserWorkload = async () => {
  userLoading.value = true
  try {
    const res = await getUsers()
    if (res.code === 200) {
      userWorkload.value = res.data || []
    }
  } catch (err) {
    ElMessage.error('加载人员数据失败')
  } finally {
    userLoading.value = false
  }
}

// 项目变更时加载历史版本
const onProjectChange = async (projectId) => {
  if (!projectId) {
    historyVersions.value = []
    return
  }

  historyLoading.value = true
  try {
    const res = await getProjectVersions(projectId)
    if (res.code === 200) {
      const versions = res.data || []
      // 加载每个版本的分配信息
      const versionsWithAssignments = await Promise.all(
        versions.map(async (ver) => {
          try {
            const assignRes = await getProjectAssignments(ver.id)
            ver.assignments = assignRes.code === 200 ? (assignRes.data || []) : []
          } catch {
            ver.assignments = []
          }
          return ver
        })
      )
      historyVersions.value = versionsWithAssignments
    }
  } catch (err) {
    ElMessage.error('加载历史版本失败')
  } finally {
    historyLoading.value = false
  }
}

// 添加分配项
const addAssignment = () => {
  form.assignments.push({
    roleId: '',
    feature: ''
  })
}

// 删除分配项
const removeAssignment = (index) => {
  form.assignments.splice(index, 1)
}

// 获取优先级标签类型
const getPriorityType = (row) => {
  if (row.priorityDesc && row.priorityDesc.includes('第一优先')) return 'success'
  if (row.priorityDesc && row.priorityDesc.includes('第二优先')) return ''
  if (row.priorityDesc && row.priorityDesc.includes('第三优先')) return 'warning'
  return 'danger'
}

// 执行智能排期
const handleSchedule = async () => {
  if (!scheduleFormRef.value) return

  try {
    await scheduleFormRef.value.validate()
  } catch {
    return
  }

  if (form.assignments.length === 0) {
    ElMessage.warning('请至少添加一个分配项')
    return
  }

  // 校验每个分配项
  for (let i = 0; i < form.assignments.length; i++) {
    const item = form.assignments[i]
    if (!item.roleId || !item.feature) {
      ElMessage.warning(`请完善分配项 ${i + 1} 的信息`)
      return
    }
  }

  loading.value = true
  try {
    const payload = {
      projectId: form.projectId,
      versionName: form.versionName,
      productManager: form.productManager,
      prototypeUrl: form.prototypeUrl,
      teamIds: form.teamIds.length > 0 ? form.teamIds : null,
      assignments: form.assignments.map(item => ({
        roleId: item.roleId,
        feature: item.feature
      }))
    }

    const res = await smartSchedule(payload)
    if (res.code === 200 && res.success !== false) {
      scheduleResult.value = res.data || { success: true, message: res.message, details: [] }
      resultVisible.value = true
    } else {
      ElMessage.error(res.message || '智能排期失败')
    }
  } catch (err) {
    ElMessage.error('智能排期请求失败: ' + (err.message || err))
  } finally {
    loading.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (scheduleFormRef.value) {
    scheduleFormRef.value.resetFields()
  }
  form.assignments = []
  form.teamIds = []
}

// 格式化时间显示
const formatTime = (time) => {
  if (!time) return ''
  // time 可能是字符串（ISO格式）或对象
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 查看项目
const viewProject = () => {
  resultVisible.value = false
  router.push('/projects')
}

// 初始化
onMounted(() => {
  loadProjects()
  loadRoles()
  loadTeams()
  loadUserWorkload()
})
</script>

<style scoped>
.smart-schedule-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.page-desc {
  color: #909399;
  margin: 0;
  font-size: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.assignment-item {
  margin-bottom: 10px;
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.assignment-index {
  font-weight: bold;
  color: #409EFF;
}

.action-buttons {
  margin-top: 20px;
  text-align: center;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
}

.rule-1 {
  background-color: #f0f9eb;
}

.rule-2 {
  background-color: #ecf5ff;
}

.rule-3 {
  background-color: #fdf6ec;
}

.rule-item strong {
  display: block;
  margin-bottom: 4px;
  color: #1a1a1a;
  font-size: 14px;
}

.rule-item p {
  margin: 0;
  color: #333333;
  font-size: 13px;
  font-weight: 500;
}

.history-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.history-item:last-child {
  border-bottom: none;
}

.history-version-name {
  font-weight: bold;
  color: #303133;
}

.history-version-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  display: flex;
  gap: 15px;
}

.history-assignments {
  margin-top: 5px;
}

.user-workload-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.user-workload-item:last-child {
  border-bottom: none;
}

.user-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.username {
  font-weight: bold;
  color: #e2e8f0;
}

.user-role {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.empty-tip {
  text-align: center;
  padding: 20px 0;
}
</style>
