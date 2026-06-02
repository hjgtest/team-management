<template>
  <div class="users-container">
    <!-- 顶部搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名/电话"
        prefix-icon="el-icon-search"
        clearable
        @input="handleSearch"
        class="search-input"
      ></el-input>
      <el-button type="primary" @click="openAddDialog" class="add-btn">
        <i class="el-icon-plus"></i> 添加人员
      </el-button>
      <el-button type="success" @click="exportTasks" class="export-btn">
        <i class="el-icon-download"></i> 导出任务（按项目）
      </el-button>
      <el-button type="warning" @click="exportTasksByUser" class="export-btn">
        <i class="el-icon-user"></i> 导出任务（按人员）
      </el-button>
    </div>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">人员管理</span>
          <div class="stats">
            <el-tag type="info" effect="light">
              总计: {{ users.length }} 人
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 空状态提示 -->
      <div v-if="users.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无人员数据" :image-size="120">
          <el-button type="primary" @click="openAddDialog">添加首个人员</el-button>
        </el-empty>
      </div>

      <el-table
        :data="filteredUsers"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        :header-cell-style="{ background: '#0f172a', color: '#f0f9ff' }"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120"></el-table-column>
        <el-table-column prop="phone" label="电话" min-width="150"></el-table-column>
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.gender === '男' ? 'info' : 'success'" size="small">
              {{ scope.row.gender }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" min-width="100"></el-table-column>
        <el-table-column prop="teamName" label="小组" min-width="100"></el-table-column>
        <el-table-column prop="ongoingProjectsCount" label="进行中项目数" min-width="120">
          <template #default="scope">
            <el-tag 
              :type="scope.row.ongoingProjectsCount > 0 ? 'success' : 'info'"
              style="cursor: pointer;"
              @click="showOngoingProjects(scope.row)"
            >
              {{ scope.row.ongoingProjectsCount || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openEditDialog(scope.row)" icon="el-icon-edit" class="operation-btn">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteUser(scope.row.id)" icon="el-icon-delete" class="operation-btn">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页控件 -->
      <div v-if="users.length > 0" class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="users.length"
          background
        >
        </el-pagination>
      </div>
    </el-card>

    <!-- 添加/编辑对话框（修复角色ID匹配） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="550px"
      center
      :close-on-click-modal="false"
      :before-close="handleDialogClose"
      class="custom-dialog"
    >
      <!-- 弹窗头部（增加装饰条） -->
      <template #header>
        <div class="dialog-header">
          <h3 class="dialog-title">{{ dialogTitle }}</h3>
          <div class="dialog-header-line"></div>
        </div>
      </template>

      <el-form
        :model="form"
        label-width="90px"
        :rules="formRules"
        ref="formRef"
        label-position="right"
        class="user-form"
      >
        <!-- 第一行：用户名 -->
        <el-form-item label="用户名" prop="username" class="form-item">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            class="form-control input-style"
            @focus="handleInputFocus"
            @blur="handleInputBlur"
          ></el-input>
        </el-form-item>

        <!-- 第二行：电话 + 性别 -->
        <div class="form-row">
          <el-form-item label="电话" prop="phone" class="form-item-half">
            <el-input
              v-model="form.phone"
              placeholder="请输入电话"
              class="form-control input-style"
              @focus="handleInputFocus"
              @blur="handleInputBlur"
            ></el-input>
          </el-form-item>
          <el-form-item label="性别" prop="gender" class="form-item-half">
            <el-select
              v-model="form.gender"
              placeholder="请选择性别"
              class="form-control select-control select-style"
              @focus="handleInputFocus"
              @blur="handleInputBlur"
            >
              <el-option label="男" value="男"></el-option>
              <el-option label="女" value="女"></el-option>
            </el-select>
          </el-form-item>
        </div>

        <!-- 第三行：年龄 + 角色（字段名匹配接口返回的roleName） -->
        <div class="form-row">
          <el-form-item label="年龄" prop="age" class="form-item-half">
            <el-input-number
              v-model="form.age"
              :min="1"
              :max="100"
              placeholder="请输入年龄"
              class="form-control age-input input-style"
              @focus="handleInputFocus"
              @blur="handleInputBlur"
            ></el-input-number>
          </el-form-item>
          <el-form-item label="角色" prop="roleName" class="form-item-half">
            <el-select
              v-model="form.roleName"
              placeholder="请选择角色"
              class="form-control select-control select-style"
              @focus="handleInputFocus"
              @blur="handleInputBlur"
            >
              <!-- 正确遍历接口返回的roleName字段 -->
              <el-option
                v-for="role in roles"
                :key="role.id"
                :label="role.roleName"
                :value="role.roleName"
              ></el-option>
            </el-select>
          </el-form-item>
        </div>

        <!-- 第四行：小组 -->
        <el-form-item label="小组" prop="teamName" class="form-item">
          <el-select
            v-model="form.teamName"
            placeholder="请选择小组"
            class="form-control select-control select-style"
            @focus="handleInputFocus"
            @blur="handleInputBlur"
          >
            <el-option
              v-for="team in teams"
              :key="team.id"
              :label="team.teamName || team.name"
              :value="team.teamName || team.name"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <!-- 取消按钮 -->
          <el-button
            @click="dialogVisible = false"
            class="dialog-btn cancel-btn cancel-btn-style"
            @mouseenter="handleCancelBtnHover"
            @mouseleave="handleCancelBtnLeave"
          >
            取消
          </el-button>
          <!-- 确认按钮 -->
          <el-button
            type="default"
            @click="submitForm"
            :loading="submitLoading"
            class="dialog-btn confirm-btn confirm-btn-style"
            @mouseenter="handleConfirmBtnHover"
            @mouseleave="handleConfirmBtnLeave"
          >
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 进行中项目详情弹窗 -->
    <el-dialog
      v-model="projectsDialogVisible"
      :title="`${currentUserName} 的进行中项目`"
      width="700px"
      center
      :close-on-click-modal="false"
      class="projects-dialog"
    >
      <div v-loading="projectsLoading">
        <!-- 空状态 -->
        <div v-if="ongoingProjects.length === 0 && !projectsLoading" class="empty-projects">
          <el-empty description="暂无进行中的项目" :image-size="100"></el-empty>
        </div>
        
        <!-- 项目列表 -->
        <div v-else class="projects-list">
          <div 
            v-for="project in ongoingProjects" 
            :key="project.id" 
            class="project-item"
          >
            <div class="project-header">
              <h4 class="project-name">{{ project.projectName }}</h4>
              <el-tag 
                type="success" 
                size="small"
                class="status-tag"
              >
                进行中
              </el-tag>
            </div>
            <div class="project-description">
              {{ project.description || '暂无描述' }}
            </div>
            
            <!-- 版本信息 -->
            <div class="versions-section" v-if="project.versions && project.versions.length > 0">
              <div class="section-title">进行中的版本:</div>
              <div 
                v-for="version in project.versions" 
                :key="version.id" 
                class="version-item"
              >
                <div class="version-header">
                  <span class="version-number">版本 {{ version.versionNumber }}</span>
                  <el-tag type="success" size="small">进行中</el-tag>
                </div>
                <div class="version-description">
                  {{ version.description || '暂无描述' }}
                </div>
                <div class="version-meta">
                  <span class="meta-item">
                    <i class="el-icon-date"></i>
                    开始时间: {{ formatDate(version.startTime) }}
                  </span>
                  <span class="meta-item">
                    <i class="el-icon-date"></i>
                    结束时间: {{ formatDate(version.endTime) }}
                  </span>
                </div>
              </div>
            </div>
            
            <div class="project-meta" v-if="!project.versions || project.versions.length === 0">
              <span class="meta-item">
                <i class="el-icon-date"></i>
                开始时间: {{ formatDate(project.startTime) }}
              </span>
              <span class="meta-item">
                <i class="el-icon-date"></i>
                结束时间: {{ formatDate(project.endTime) }}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="projectsDialogVisible = false" class="dialog-btn cancel-btn">
          关闭
        </el-button>
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

import request from '@/utils/request'
import { userApi, roleApi, teamApi } from '@/api'

export default {
  name: 'UsersView',
  data() {
    return {
      loading: false,
      submitLoading: false,
      users: [],
      filteredUsers: [],
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        username: '',
        phone: '',
        gender: '',
        age: null,
        roleId: null,    // 存储角色ID（提交用）
        roleName: '',    // 存储角色名称（显示用）
        teamId: null,    // 存储小组ID（提交用）
        teamName: ''     // 存储小组名称（显示用）
      },
      formRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
      },
      isEdit: false,
      searchKeyword: '',
      roles: [],  // 角色列表（接口返回：id + roleName）
      teams: [],  // 小组列表
      currentPage: 1,
      pageSize: 10,
      // 进行中项目弹窗相关
      projectsDialogVisible: false,
      projectsLoading: false,
      currentUserName: '',
      ongoingProjects: []
    }
  },
  mounted() {
    this.loadUsers()
    this.loadOptionsData()
  },
  methods: {
    // 处理输入框聚焦
    handleInputFocus(e) {
      e.target.style.borderColor = '#38bdf8'
    },
    // 处理输入框失焦
    handleInputBlur(e) {
      e.target.style.borderColor = '#e5e7eb'
    },
    // 取消按钮hover
    handleCancelBtnHover(e) {
      e.target.style.background = '#f9fafb'
      e.target.style.borderColor = '#d1d5db'
    },
    // 取消按钮离开
    handleCancelBtnLeave(e) {
      e.target.style.background = '#ffffff'
      e.target.style.borderColor = '#e5e7eb'
    },
    // 确认按钮hover
    handleConfirmBtnHover(e) {
      e.target.style.background = '#f0f9ff'
      e.target.style.borderColor = '#2563eb'
    },
    // 确认按钮离开
    handleConfirmBtnLeave(e) {
      e.target.style.background = '#ffffff'
      e.target.style.borderColor = '#38bdf8'
    },
    // 加载用户数据
    async loadUsers() {
      this.loading = true
      try {
        const response = await userApi.getAllUsers()
        this.users = response.data.data
        this.filteredUsers = [...this.users]
      } catch (error) {
        console.error('加载用户数据失败:', error)
        this.$message.error('加载用户数据失败')
      } finally {
        this.loading = false
      }
    },
    // 加载角色、小组数据
    async loadOptionsData() {
      this.loading = true
      try {
        const [rolesRes, teamsRes] = await Promise.all([
          roleApi.getAllRoles(),
          teamApi.getAllTeams()
        ])
        // 确保角色数据正确存储（接口返回的是roleName字段）
        this.roles = rolesRes.data.data || []
        this.teams = teamsRes.data.data || []
      } catch (error) {
        console.error('加载角色/小组数据失败:', error)
        this.$message.error('加载下拉选项失败')
      } finally {
        this.loading = false
      }
    },
    // 搜索功能
    handleSearch() {
      if (!this.searchKeyword) {
        this.filteredUsers = [...this.users]
        return
      }
      const keyword = this.searchKeyword.toLowerCase()
      this.filteredUsers = this.users.filter(
        user =>
          user.username.toLowerCase().includes(keyword) ||
          (user.phone && user.phone.includes(keyword))
      )
      this.currentPage = 1
    },
    // 打开添加弹窗
    openAddDialog() {
      this.isEdit = false
      this.dialogTitle = '添加人员'
      this.resetForm()
      this.dialogVisible = true
    },
    // 打开编辑弹窗
    openEditDialog(row) {
      this.isEdit = true
      this.dialogTitle = '编辑人员'
      this.form = {
        id: row.id,
        username: row.username,
        phone: row.phone,
        gender: row.gender,
        age: row.age,
        roleId: row.roleId,
        roleName: row.roleName,  // 直接使用接口返回的roleName
        teamId: row.teamId,
        teamName: row.teamName
      }
      this.dialogVisible = true
    },
    // 重置表单
    resetForm() {
      this.form = {
        id: null,
        username: '',
        phone: '',
        gender: '',
        age: null,
        roleId: null,
        roleName: '',
        teamId: null,
        teamName: ''
      }
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    // 提交表单（核心修复：精准匹配角色ID）
    async submitForm() {
      try {
        await this.$refs.formRef.validate()
        this.submitLoading = true

        // 1. 精准匹配选中角色名称对应的ID（修复核心）
        const selectedRole = this.roles.find(role => role.roleName === this.form.roleName)
        // 2. 精准匹配选中小组名称对应的ID（兼容teamName/name两种字段）
        const selectedTeam = this.teams.find(team =>
          (team.teamName && team.teamName === this.form.teamName) ||
          (team.name && team.name === this.form.teamName)
        )

        // 3. 构造提交数据（确保ID准确）
        const submitForm = {
          ...this.form,
          roleId: selectedRole ? selectedRole.id : this.form.roleId, // 优先用匹配的ID
          teamId: selectedTeam ? selectedTeam.id : this.form.teamId
        }

        // 4. 提交请求
        if (this.isEdit) {
          await userApi.updateUser(submitForm.id, submitForm)
          this.$message.success('更新成功')
        } else {
          await userApi.createUser(submitForm)
          this.$message.success('添加成功')
        }

        this.dialogVisible = false
        this.loadUsers() // 重新加载数据，确保更新后表格显示最新内容
      } catch (error) {
        console.error('提交表单失败:', error)
        this.$message.error('操作失败：' + (error.message || '未知错误'))
      } finally {
        this.submitLoading = false
      }
    },
    // 删除用户
    async deleteUser(id) {
      try {
        await this.$confirm('确认删除该用户?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userApi.deleteUser(id)
        this.$message.success('删除成功')
        this.loadUsers()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除用户失败:', error)
          this.$message.error('删除失败')
        }
      }
    },

    // 导出任务（使用 EasyExcel 后端导出）
    async exportTasks() {
      this.$message.info('正在生成导出文件...');
      
      try {
        // 方案 1: 使用原生 fetch，但确保正确处理 blob
        const response = await fetch('/api/export/tasks', {
          method: 'GET',
          headers: {
            'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          }
        });
        
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        // 检查 Content-Type
        const contentType = response.headers.get('content-type');
        console.log('响应 Content-Type:', contentType);
        
        // 尝试获取 blob
        const blob = await response.blob();
        
        // 验证 blob 大小和类型
        console.log('Blob 大小:', blob.size, '类型:', blob.type);
        
        if (blob.size === 0) {
          throw new Error('下载的文件为空');
        }
        
        // 如果是文本类型，可能是错误信息
        if (contentType && contentType.includes('text') || contentType && contentType.includes('json')) {
          const text = await blob.text();
          console.error('返回的是文本内容:', text);
          throw new Error('服务器返回错误信息：' + text.substring(0, 300));
        }
        
        // 处理 blob 响应
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `开发组任务_${new Date().toISOString().slice(0, 10)}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        
        this.$message.success('导出成功！');
      } catch (error) {
        console.error('导出失败:', error);
        // 显示具体的错误信息
        let errorMessage = error.message || '未知错误';
        
        // 如果是网络错误或状态码问题
        if (error.name === 'TypeError' && error.message.includes('createObjectURL')) {
          errorMessage = '文件下载失败，请检查后端服务是否正常运行';
        }
        
        this.$message.error('导出失败：' + errorMessage);
      }
    },
    
    // 导出任务（按人员统计）
    async exportTasksByUser() {
      this.$message.info('正在生成导出文件...');
      
      try {
        const response = await fetch('/api/export/tasks-by-user', {
          method: 'GET',
          headers: {
            'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
          }
        });
        
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const blob = await response.blob();
        
        if (blob.size === 0) {
          throw new Error('下载的文件为空');
        }
        
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `人员任务统计_${new Date().toISOString().slice(0, 10)}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        
        this.$message.success('导出成功！');
      } catch (error) {
        console.error('导出失败:', error);
        let errorMessage = error.message || '未知错误';
        
        if (error.name === 'TypeError' && error.message.includes('createObjectURL')) {
          errorMessage = '文件下载失败，请检查后端服务是否正常运行';
        }
        
        this.$message.error('导出失败：' + errorMessage);
      }
    },

    // 生成Excel文件（重构为支持层级结构）
    async generateExcel(todayTasks, weekTasks) {
      // 检查xlsx库是否存在，如果不存在则动态导入
      let XLSX;
      try {
        XLSX = require('xlsx');
      } catch (e) {
        // 如果没有xlsx，尝试动态导入
        try {
          const module = await import('xlsx');
          XLSX = module.default || module;
        } catch (importError) {
          console.error('xlsx库未找到，请先安装: npm install xlsx');
          throw new Error('xlsx库未找到，请先安装: npm install xlsx');
        }
      }
      
      // 创建工作簿
      const wb = XLSX.utils.book_new();
      
      // 创建第一个工作表：当天需要结束的任务
      const todaySheetData = [
        ['项目名称', '版本', '负责的用户'],
        ...todayTasks.map(task => [task.projectName, task.versionNumber, task.responsibleUsers])
      ];
      const ws1 = XLSX.utils.aoa_to_sheet(todaySheetData);
      XLSX.utils.book_append_sheet(wb, ws1, '当天需结束任务');
      
      // 创建第二个工作表：本周需要结束的任务
      const weekSheetData = [
        ['项目名称', '版本', '负责的用户'],
        ...weekTasks.map(task => [task.projectName, task.versionNumber, task.responsibleUsers])
      ];
      const ws2 = XLSX.utils.aoa_to_sheet(weekSheetData);
      XLSX.utils.book_append_sheet(wb, ws2, '本周需结束任务');
      
      // 生成Excel文件并下载
      const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
      const blob = new Blob([wbout], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `开发组任务_${new Date().toISOString().slice(0, 10)}.xlsx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    },
    // 表格行样式
    tableRowClassName(rowData) {
      const rowIndex = rowData.rowIndex;
      if (rowIndex % 2 === 1) {
        return 'table-row-even'
      }
      return ''
    },
    // 关闭弹窗
    handleDialogClose(done) {
      this.resetForm()
      done()
    },
    // 分页-每页条数变化
    handleSizeChange(val) {
      this.pageSize = val
    },
    // 分页-当前页变化
    handleCurrentChange(val) {
      this.currentPage = val
    },
    // 显示用户进行中的项目
    async showOngoingProjects(user) {
      console.log('点击了进行中项目数，用户ID:', user.id, '用户名:', user.username)
      this.currentUserName = user.username
      this.projectsDialogVisible = true
      this.projectsLoading = true
      
      try {
        const response = await request.get(`/api/users/${user.id}/ongoing-projects`)
        // 处理新的数据结构：项目列表，每个项目包含versions数组
        this.ongoingProjects = response.data || []
        
        // 调试日志
        console.log('获取到的项目和版本数据:', this.ongoingProjects)
      } catch (error) {
        console.error('获取进行中项目失败:', error)
        this.$message.error('获取项目信息失败: ' + (error.response?.data?.message || error.message || '未知错误'))
        this.ongoingProjects = []
      } finally {
        this.projectsLoading = false
      }
    },
    // 获取项目状态标签类型
    getProjectStatusTag(project) {
      switch (project.status) {
        case 0: return 'info'    // 未开始
        case 1: return 'success' // 进行中
        case 2: return 'warning' // 已结束
        default: return 'info'
      }
    },
    // 获取项目状态文本
    getProjectStatusText(project) {
      switch (project.status) {
        case 0: return '未开始'
        case 1: return '进行中'
        case 2: return '已结束'
        default: return '未知'
      }
    },
    // 格式化日期（增强版，支持LocalDateTime格式）
    formatDate(dateString) {
      if (!dateString) return '未设置';
      
      // 处理后端返回的LocalDateTime格式："2026-02-05T10:00:00"
      try {
        let date;
        if (typeof dateString === 'string') {
          // 如果是ISO格式，直接使用new Date()
          if (dateString.includes('T')) {
            date = new Date(dateString);
          } else {
            // 尝试解析其他格式
            date = new Date(dateString);
          }
        } else {
          date = dateString;
        }
        
        // 验证日期是否有效
        if (isNaN(date.getTime())) {
          return '无效日期';
        }
        
        return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      } catch (e) {
        console.error('日期解析失败:', dateString, e);
        return '日期解析错误';
      }
    },
    
    // 解析后端LocalDateTime格式的日期字符串
    parseLocalDateTime(dateString) {
      if (!dateString) return null;
      // 处理后端返回的LocalDateTime格式："2026-02-05T10:00:00"
      // 转换为JavaScript Date对象
      try {
        // 如果是ISO格式，直接使用new Date()
        if (dateString.includes('T')) {
          return new Date(dateString);
        }
        // 如果是其他格式，尝试解析
        return new Date(dateString);
      } catch (e) {
        console.error('日期解析失败:', dateString, e);
        return null;
      }
    }
  }
}
</script>

<style scoped>
/* ===== 页面容器 ===== */
.users-container {
  max-width: 1400px;
  margin: 0 auto;
  color: var(--text-primary);
}

/* ===== 顶部工具栏 ===== */
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
.search-input {
  width: 320px;
  max-width: 100%;
}
.add-btn, .export-btn {
  font-weight: 500;
}

/* ===== 主卡片 ===== */
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

/* ===== 空状态 ===== */
.empty-state { padding: 60px 0; text-align: center; }
:deep(.el-empty__description p) { color: var(--text-muted) !important; }

/* ===== 表格行颜色 ===== */
:deep(.el-table .table-row-even) { background: rgba(15, 23, 42, 0.35) !important; }
.operation-btn { padding: 6px 12px !important; margin: 0 3px !important; }

/* ===== 分页 ===== */
.pagination-container {
  margin-top: 16px;
  padding: 14px 16px;
  text-align: right;
  background: rgba(15, 23, 42, 0.4);
  border-top: 1px solid var(--glass-border);
}

/* ===== 表单 ===== */
.user-form { padding: 8px 4px 0; }
.form-row { display: flex; gap: 16px; margin-bottom: 4px; }
.form-item { width: 100%; }
.form-item-half { flex: 1; min-width: 0; }
.form-control { width: 100%; }
.input-style, .select-style, .age-input { width: 100%; }

/* ===== 弹窗头部分割条 ===== */
.dialog-header { position: relative; padding-bottom: 12px; }
.dialog-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.dialog-header-line {
  position: absolute;
  bottom: 0; left: 0;
  width: 56px; height: 3px;
  border-radius: 3px;
  background: linear-gradient(135deg, var(--accent-cyan), var(--accent-blue));
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.5);
}

/* ===== 弹窗底部按钮 ===== */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 6px;
}
.dialog-btn {
  font-weight: 500;
  min-width: 92px;
  padding: 8px 22px !important;
}
.cancel-btn-style {
  background: rgba(30, 41, 59, 0.7) !important;
  border: 1px solid var(--glass-border) !important;
  color: var(--text-secondary) !important;
}
.cancel-btn-style:hover {
  background: rgba(51, 65, 85, 0.85) !important;
  color: var(--text-primary) !important;
}
.confirm-btn-style {
  background: linear-gradient(135deg, var(--accent-blue), var(--accent-cyan)) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}
.confirm-btn-style:hover { filter: brightness(1.12); transform: translateY(-1px); }

/* ===== 进行中项目弹窗 ===== */
.empty-projects { padding: 30px 0; text-align: center; }
.projects-list { max-height: 460px; overflow-y: auto; padding-right: 4px; }
.project-item {
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
  transition: all 0.3s;
}
.project-item:hover {
  border-color: rgba(59, 130, 246, 0.6);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
  transform: translateY(-1px);
}
.project-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px;
}
.project-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.status-tag { font-size: 11px; }
.project-description {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 10px;
}
.versions-section {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed var(--glass-border);
}
.section-title {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}
.version-item {
  background: rgba(30, 41, 59, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.version-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 4px;
}
.version-number { font-size: 13px; font-weight: 600; color: var(--accent-cyan); }
.version-description { font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
.version-meta, .project-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}
.meta-item i { color: var(--accent-cyan); }

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .search-input { width: 100%; }
}
@media (max-width: 768px) {
  .search-bar { flex-direction: column; align-items: stretch; }
  .form-row { flex-direction: column; gap: 0; }
  .form-item-half { width: 100%; }
}
</style>