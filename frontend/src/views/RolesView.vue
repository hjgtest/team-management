<template>
  <div class="roles-container">
    <!-- 顶部搜索栏+添加按钮 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索角色名称/描述"
        prefix-icon="el-icon-search"
        clearable
        @input="handleSearch"
        class="search-input"
      ></el-input>
      <el-button v-if="isAdmin()" type="primary" @click="openAddDialog" class="add-btn">添加角色</el-button>
    </div>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">角色管理</span>
          <div class="stats">
            <el-tag type="info" effect="light">
              总计: {{ roles.length }} 个
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 空状态提示 -->
      <div v-if="filteredRoles.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无角色数据" :image-size="120">
          <el-button v-if="isAdmin()" type="primary" @click="openAddDialog">添加首个角色</el-button>
          <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
        </el-empty>
      </div>

      <el-table
        :data="filteredRoles"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        :header-cell-style="{ background: '#0f172a' }"
      >
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="roleName" label="角色名称" width="150"></el-table-column>
        <el-table-column prop="roleDescription" label="角色描述" width="300"></el-table-column>
        <el-table-column prop="permissions" label="权限" width="200"></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button v-if="isAdmin()" size="small" type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="isAdmin()" size="small" type="danger" @click="deleteRole(scope.row.id)">删除</el-button>
            <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
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
        class="role-form"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称"></el-input>
        </el-form-item>
        <el-form-item label="角色描述" prop="roleDescription">
          <el-input v-model="form.roleDescription" type="textarea" placeholder="请输入角色描述"></el-input>
        </el-form-item>
        <el-form-item label="权限" prop="permissions">
          <el-input v-model="form.permissions" placeholder="请输入权限标识"></el-input>
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

import { isAdmin } from '@/utils/auth'
import { roleApi } from '@/api'

export default {
  name: 'RolesView',
  data() {
    return {
      loading: false,
      roles: [],
      filteredRoles: [], // 搜索后的角色列表
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        roleName: '',
        roleDescription: '',
        permissions: ''
      },
      formRules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
      },
      isEdit: false,
      searchKeyword: '' // 搜索关键词
    }
  },
  mounted() {
    this.loadRoles()
  },
  methods: {
    isAdmin() { return isAdmin() },
    async loadRoles() {
      this.loading = true
      try {
        const response = await roleApi.getAllRoles()
        this.roles = response.data.data || []
        this.filteredRoles = [...this.roles] // 初始化搜索列表
      } catch (error) {
        console.error('加载角色数据失败:', error)
        this.$message.error('加载角色数据失败')
      } finally {
        this.loading = false
      }
    },

    // 搜索处理
    handleSearch() {
      if (!this.searchKeyword) {
        this.filteredRoles = [...this.roles]
        return
      }
      const keyword = this.searchKeyword.toLowerCase()
      this.filteredRoles = this.roles.filter(
        role =>
          role.roleName.toLowerCase().includes(keyword) ||
          (role.roleDescription && role.roleDescription.toLowerCase().includes(keyword))
      )
    },

    openAddDialog() {
      this.isEdit = false
      this.dialogTitle = '添加角色'
      this.form = {
        id: null,
        roleName: '',
        roleDescription: '',
        permissions: ''
      }
      // 重置表单验证
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑角色'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async submitForm() {
      try {
        // 表单验证
        await this.$refs.formRef.validate()

        if (this.isEdit) {
          await roleApi.updateRole(this.form.id, this.form)
          this.$message.success('更新成功')
        } else {
          await roleApi.createRole(this.form)
          this.$message.success('添加成功')
        }

        this.dialogVisible = false
        this.loadRoles() // 重新加载后自动更新搜索列表
      } catch (error) {
        if (error !== false) {
          console.error('提交表单失败:', error)
          this.$message.error('操作失败')
        }
      } finally {
        this.submitLoading = false
      }
    },
    async deleteRole(id) {
      try {
        await this.$confirm('确认删除该角色?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await roleApi.deleteRole(id)
        this.$message.success('删除成功')
        this.loadRoles() // 重新加载后自动更新搜索列表
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除角色失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.roles-container {
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

.role-form { padding: 10px 4px 0; }

.dialog-footer {
  display: flex; justify-content: flex-end; gap: 12px;
}

@media (max-width: 768px) {
  .search-bar { flex-direction: column; align-items: stretch; }
  .search-input { width: 100%; margin-right: 0; }
}
</style>