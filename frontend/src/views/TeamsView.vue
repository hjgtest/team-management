<template>
  <div class="teams-container">
    <!-- 顶部搜索栏+添加按钮 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索小组名称/描述"
        prefix-icon="el-icon-search"
        clearable
        @input="handleSearch"
        class="search-input"
      ></el-input>
      <el-button type="primary" @click="openAddDialog" class="add-btn">添加小组</el-button>
    </div>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">小组管理</span>
          <div class="stats">
            <el-tag type="info" effect="light">
              总计: {{ teams.length }} 个
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 空状态提示 -->
      <div v-if="filteredTeams.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无小组数据" :image-size="120">
          <el-button type="primary" @click="openAddDialog">添加首个小组</el-button>
        </el-empty>
      </div>

      <el-table
        :data="filteredTeams"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        :header-cell-style="{ background: '#0f172a' }"
      >
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="teamName" label="小组名称" width="150"></el-table-column>
        <el-table-column prop="description" label="描述" width="300"></el-table-column>
        <el-table-column prop="leaderId" label="负责人ID" width="120"></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTeam(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      center
      :close-on-click-modal="false"
    >
      <el-form
        :model="form"
        label-width="100px"
        :rules="formRules"
        ref="formRef"
        label-position="right"
        class="team-form"
      >
        <el-form-item label="小组名称" prop="teamName">
          <el-input v-model="form.teamName" placeholder="请输入小组名称"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入小组描述"></el-input>
        </el-form-item>
        <el-form-item label="负责人ID" prop="leaderId">
          <el-input-number v-model="form.leaderId" :min="1" placeholder="请输入负责人ID"></el-input-number>
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="form.createTime"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 100%"
          >
          </el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// 全局屏蔽ResizeObserver警告
window.ResizeObserver = window.ResizeObserver || function() {
  return {
    observe: () => {},
    unobserve: () => {},
    disconnect: () => {}
  }
}

import { teamApi } from '@/api'

export default {
  name: 'TeamsView',
  data() {
    return {
      loading: false,
      teams: [],
      filteredTeams: [], // 搜索后的小组列表
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        teamName: '',
        description: '',
        leaderId: null,
        createTime: ''
      },
      formRules: {
        teamName: [{ required: true, message: '请输入小组名称', trigger: 'blur' }]
      },
      isEdit: false,
      searchKeyword: '' // 搜索关键词
    }
  },
  mounted() {
    this.loadTeams()
  },
  methods: {
    async loadTeams() {
      this.loading = true
      try {
        const response = await teamApi.getAllTeams()
        this.teams = response.data.data || []
        this.filteredTeams = [...this.teams] // 初始化搜索列表
      } catch (error) {
        console.error('加载小组数据失败:', error)
        this.$message.error('加载小组数据失败')
      } finally {
        this.loading = false
      }
    },

    // 搜索处理
    handleSearch() {
      if (!this.searchKeyword) {
        this.filteredTeams = [...this.teams]
        return
      }
      const keyword = this.searchKeyword.toLowerCase()
      this.filteredTeams = this.teams.filter(
        team =>
          team.teamName.toLowerCase().includes(keyword) ||
          (team.description && team.description.toLowerCase().includes(keyword))
      )
    },

    openAddDialog() {
      this.isEdit = false
      this.dialogTitle = '添加小组'
      this.form = {
        id: null,
        teamName: '',
        description: '',
        leaderId: null,
        createTime: ''
      }
      // 重置表单验证
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑小组'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async submitForm() {
      try {
        // 表单验证
        await this.$refs.formRef.validate()

        if (this.isEdit) {
          await teamApi.updateTeam(this.form.id, this.form)
          this.$message.success('更新成功')
        } else {
          await teamApi.createTeam(this.form)
          this.$message.success('添加成功')
        }

        this.dialogVisible = false
        this.loadTeams() // 重新加载后自动更新搜索列表
      } catch (error) {
        if (error !== false) {
          console.error('提交表单失败:', error)
          this.$message.error('操作失败')
        }
      }
    },
    async deleteTeam(id) {
      try {
        await this.$confirm('确认删除该小组?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await teamApi.deleteTeam(id)
        this.$message.success('删除成功')
        this.loadTeams() // 重新加载后自动更新搜索列表
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除小组失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.teams-container {
  max-width: 1400px;
  margin: 0 auto;
  color: var(--text-primary);
}

/* 顶部工具栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px 18px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  flex-wrap: wrap;
}
.search-input { width: 320px; max-width: 100%; margin-right: auto; }
.add-btn { font-weight: 500; }

/* 主卡片 */
.main-card {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  border-radius: 14px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  overflow: hidden;
}
.main-card :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.55);
  border-bottom: 1px solid var(--glass-border);
  padding: 14px 20px !important;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}
.title::before {
  content: "●";
  color: var(--accent-cyan);
  margin-right: 8px;
  font-size: 12px;
}
.stats { display: flex; gap: 8px; align-items: center; }

.empty-state { padding: 60px 0; text-align: center; }
:deep(.el-empty__description p) { color: var(--text-muted) !important; }

.team-form { padding: 10px 4px 0; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }

@media (max-width: 768px) {
  .search-bar { flex-direction: column; align-items: stretch; }
  .search-input { width: 100%; margin-right: 0; }
}
</style>