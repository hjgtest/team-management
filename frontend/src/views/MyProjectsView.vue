<template>
  <div class="my-projects-container">
    <!-- 页面标题 -->

    <!-- 活跃项目列表（分层：项目 -> 版本） -->
    <el-card class="card-section main-card" v-loading="loading">
      <div class="card-header">
        <h3 class="title">活跃项目（未开始 & 进行中）</h3>
        <div class="stats">
          <el-tag type="info" effect="light">项目: {{ projects.length }} 个</el-tag>
          <el-tag type="warning" effect="light">版本: {{ totalVersions }} 个</el-tag>
        </div>
      </div>

      <div v-if="projects.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无未开始或进行中的项目" :image-size="120" />
      </div>

      <!-- 分层表格：项目为主表，版本为展开行 -->
      <div v-else>
        <el-table
          :data="projects"
          border
          stripe
          style="width: 100%; margin-top: 12px"
          :header-cell-style="{ background: '#0f172a' }"
          row-key="id"
        >
          <el-table-column type="expand">
            <template #default="props">
              <div class="version-inner-table">
                <h4 class="version-inner-title">{{ props.row.projectName }} - 活跃版本列表</h4>
                <el-table
                  :data="props.row.versions"
                  border
                  size="small"
                  style="width: 100%"
                  :header-cell-style="{ background: '#1e293b' }"
                >
                  <el-table-column prop="versionNumber" label="版本号" min-width="110">
                    <template #default="scope">
                      <div style="font-weight:600">{{ scope.row.versionNumber }}</div>
                      <el-tag v-if="getVersionHealth(scope.row).type === 'danger'" size="small" type="danger" effect="dark" style="margin-top:4px">高风险</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="研发周期" min-width="220">
                    <template #default="scope">
                      <div style="font-size:12px;opacity:.85">
                        {{ formatDate(scope.row.startDate) }} ~ {{ formatDate(scope.row.endDate) }}
                      </div>
                      <el-progress
                        :percentage="getDateProgress(scope.row).percent"
                        :status="getDateProgress(scope.row).status"
                        :stroke-width="6"
                        :show-text="false"
                        style="margin-top:4px"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="提测" min-width="100">
                    <template #default="scope">{{ formatDate(scope.row.testTime) || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="上线" min-width="140">
                    <template #default="scope">
                      <div>{{ formatDate(scope.row.launchTime) || '-' }}</div>
                      <div v-if="scope.row.launchTime" :style="{color: getDaysToLaunchColor(scope.row)}" style="font-size:11px;margin-top:2px">
                        {{ getDaysToLaunchText(scope.row) }}
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="productManager" label="产品" min-width="90">
                    <template #default="scope">{{ scope.row.productManager || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="健康度" min-width="100" align="center">
                    <template #default="scope">
                      <el-tag :type="getVersionHealth(scope.row).type" size="small" effect="dark">
                        {{ getVersionHealth(scope.row).label }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" min-width="90">
                    <template #default="scope">
                      <el-tag :type="getVersionStatusTag(scope.row)">{{ getVersionStatusText(scope.row) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" min-width="200" fixed="right">
                    <template #default="scope">
                      <template v-if="isAdmin()">
                        <el-button size="small" type="primary" @click="editVersion(scope.row, props.row)">编辑</el-button>
                        <el-button size="small" @click="viewAssignments(scope.row, props.row)">分配</el-button>
                      </template>
                      <span v-else style="color:var(--text-muted);font-size:12px">只读</span>
                    </template>
                  </el-table-column>
                </el-table>
                <div v-if="(!props.row.versions || props.row.versions.length === 0)" class="empty-inner">
                  <el-empty description="暂无活跃版本" :image-size="60" />
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="projectName" label="项目名称" min-width="200"></el-table-column>
          <el-table-column prop="description" label="项目描述" min-width="250"></el-table-column>
          <el-table-column prop="status" label="项目状态" min-width="100">
            <template #default="scope">
              <el-tag :type="getProjectStatusTag(scope.row)">{{ getProjectStatusText(scope.row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="活跃版本数" min-width="100" align="center">
            <template #default="scope">
              <el-tag type="warning">{{ (scope.row.versions || []).length }} 个</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="160">
            <template #default="scope">
              <el-button v-if="isAdmin()" size="small" type="primary" @click="viewVersionsFromTable(scope.row)">版本管理</el-button>
              <span v-else style="color:var(--text-muted);font-size:12px">只读</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 版本管理弹窗（复用 ProjectsView 的逻辑） -->
    <el-dialog title="版本管理" v-model="versionDialogVisible" width="1100px" @close="onVersionDialogClose" center :close-on-click-modal="false">
      <div class="version-header">
        <h4 class="title">{{ currentProject.name }} - 版本列表</h4>
        <div class="version-toolbar">
          <el-select v-model="versionSortMode" class="version-sort" @change="versionCurrentPage = 1">
            <el-option label="🔥 风险优先" value="risk" />
            <el-option label="⏰ 上线临近" value="launch" />
            <el-option label="📅 默认" value="default" />
          </el-select>
          <el-select
            v-model="versionStatusFilter"
            placeholder="全部状态"
            clearable
            class="version-status-filter"
            @change="versionCurrentPage = 1"
          >
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
          <el-input
            v-model="versionSearchKeyword"
            placeholder="搜索版本号"
            clearable
            class="version-search"
            @input="handleVersionSearchInput"
            @clear="handleVersionSearchClear"
          >
            <template #prefix>
              <i class="el-icon-search"></i>
            </template>
          </el-input>
          <el-button v-if="isAdmin()" type="primary" @click="showAddVersion" class="add-btn">新增版本</el-button>
        </div>
      </div>

      <div v-if="filteredVersions.length === 0 && !versionLoading" class="empty-state">
        <el-empty :description="versions.length === 0 ? '暂无版本数据' : '当前筛选条件下无数据'" :image-size="100">
          <el-button type="primary" @click="showAddVersion">添加首个版本</el-button>
        </el-empty>
      </div>

      <el-table
        v-loading="versionLoading"
        :data="paginatedVersions"
        border
        stripe
        style="width: 100%; margin-top: 10px"
        :header-cell-style="{ background: '#0f172a' }"
        v-else
      >
        <el-table-column prop="versionNumber" label="版本号" min-width="110">
          <template #default="scope">
            <div style="font-weight:600">{{ scope.row.versionNumber }}</div>
            <el-tag v-if="getVersionHealth(scope.row).type === 'danger'" size="small" type="danger" effect="dark" style="margin-top:4px">高风险</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="研发周期 / 进度" min-width="260">
          <template #default="scope">
            <div style="font-size:12px;opacity:.85">
              {{ formatDate(scope.row.startDate) }} ~ {{ formatDate(scope.row.endDate) }}
            </div>
            <el-progress
              :percentage="getDateProgress(scope.row).percent"
              :status="getDateProgress(scope.row).status"
              :stroke-width="8"
              :show-text="false"
              style="margin-top:4px"
            />
            <div class="progress-text">{{ getDateProgress(scope.row).text }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提测" min-width="110">
          <template #default="scope">{{ formatDate(scope.row.testTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="上线" min-width="160">
          <template #default="scope">
            <div>{{ formatDate(scope.row.launchTime) || '-' }}</div>
            <div v-if="scope.row.launchTime" class="launch-distance" :style="{color: getDaysToLaunchColor(scope.row)}">
              {{ getDaysToLaunchText(scope.row) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="productManager" label="产品" min-width="100">
          <template #default="scope">{{ scope.row.productManager || '-' }}</template>
        </el-table-column>
        <el-table-column label="原型" min-width="80" align="center">
          <template #default="scope">
            <el-link v-if="scope.row.prototypeUrl" :href="scope.row.prototypeUrl" target="_blank" type="primary">查看</el-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="Bug" min-width="80" align="center">
          <template #default="scope">
            <el-tag size="small" :type="(scope.row.bugCount || 0) > 0 ? 'danger' : 'info'">{{ scope.row.bugCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="延期" min-width="170">
          <template #default="scope">
            <el-tag v-if="scope.row.isDelayed" size="small" type="warning">
              延期至 {{ formatDate(scope.row.delayedEndTime) || '-' }}
            </el-tag>
            <span v-else style="opacity:.5">未延期</span>
          </template>
        </el-table-column>
        <el-table-column label="健康度" min-width="110" align="center">
          <template #default="scope">
            <el-tooltip :content="'风险分: ' + getVersionRiskScore(scope.row)" placement="top">
              <el-tag :type="getVersionHealth(scope.row).type" size="small" effect="dark">
                {{ getVersionHealth(scope.row).label }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="scope">
            <el-tag :type="getVersionStatusTag(scope.row)">{{ getVersionStatusText(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="scope">
            <template v-if="isAdmin()">
              <el-button size="small" type="primary" @click="editVersion(scope.row)">编辑</el-button>
              <el-button size="small" @click="copyVersion(scope.row)">复制</el-button>
              <el-button size="small" type="danger" @click="deleteVersion(scope.row.id)">删除</el-button>
              <el-button size="small" type="primary" @click="viewAssignments(scope.row)">分配</el-button>
            </template>
            <span v-else style="color:var(--text-muted);font-size:12px">只读</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredVersions.length > 0" class="pagination-container">
        <el-pagination
          @size-change="handleVersionSizeChange"
          @current-change="handleVersionCurrentChange"
          :current-page="versionCurrentPage"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="versionPageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredVersions.length"
          small
          background
        ></el-pagination>
      </div>
    </el-dialog>

    <!-- 版本新增/编辑弹窗 -->
    <el-dialog :title="versionDialogTitle" v-model="versionEditDialogVisible" width="640px" center :close-on-click-modal="false">
      <el-form ref="versionForm" :model="currentVersion" :rules="versionRules" label-width="100px" label-position="right" class="version-form">
        <el-form-item label="版本号" prop="versionNumber">
          <el-input v-model="currentVersion.versionNumber" placeholder="如 V1.0.0"></el-input>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="研发开始" prop="startDate" class="form-item-half">
            <el-date-picker v-model="currentVersion.startDate" type="date" placeholder="开始日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="研发结束" prop="endDate" class="form-item-half">
            <el-date-picker v-model="currentVersion.endDate" type="date" placeholder="结束日期" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="提测时间" class="form-item-half">
            <el-date-picker v-model="currentVersion.testTime" type="date" placeholder="提测日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="上线时间" class="form-item-half">
            <el-date-picker v-model="currentVersion.launchTime" type="date" placeholder="上线日期" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="产品负责">
          <el-input v-model="currentVersion.productManager" placeholder="产品经理姓名"></el-input>
        </el-form-item>
        <el-form-item label="原型链接">
          <el-input v-model="currentVersion.prototypeUrl" placeholder="https://..."></el-input>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="Bug数" class="form-item-half">
            <el-input-number v-model="currentVersion.bugCount" :min="0" controls-position="right" style="width:100%" />
          </el-form-item>
          <el-form-item label="是否延期" class="form-item-half">
            <el-switch v-model="currentVersion.isDelayed" />
          </el-form-item>
        </div>
        <el-form-item label="延期至" v-if="currentVersion.isDelayed">
          <el-date-picker v-model="currentVersion.delayedEndTime" type="date" placeholder="延期后的结束日期" style="width: 100%" />
        </el-form-item>
        <el-divider content-position="left">🐞 Bug统计详情</el-divider>
        
        <!-- Bug解决 -->
        <el-form-item label="Bug解决">
          <div class="bug-stats-container">
            <div v-for="(item, index) in currentVersion.bugResolvedList" :key="'resolved-'+index" class="bug-stat-row">
              <el-select v-model="item.userId" placeholder="选择人员" style="width: 150px; margin-right: 8px" filterable>
                <el-option v-for="user in users" :key="user.id" :label="user.username" :value="user.id"></el-option>
              </el-select>
              <span style="margin-right: 8px; color: var(--text-secondary)">：</span>
              <div class="level-inputs">
                <div v-for="(level, lIndex) in item.levels" :key="lIndex" class="level-item">
                  <el-input v-model="level.name" placeholder="级别名称" style="width: 80px; margin-right: 4px" />
                  <el-input-number v-model="level.count" :min="0" style="width: 120px; margin-right: 4px" />
                  <span class="level-unit">个</span>
                  <el-button type="danger" icon="Delete" size="mini" @click="removeLevel(item, lIndex)"></el-button>
                </div>
                <el-button type="primary" size="small" icon="Plus" @click="addLevel(item)">添加级别</el-button>
              </div>
              <el-button type="danger" icon="Delete" size="small" @click="removeBugResolvedItem(index)"></el-button>
            </div>
            <div class="bug-actions">
              <el-button type="primary" size="small" icon="Plus" @click="addBugResolvedItem">添加人员</el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="版本描述">
          <el-input v-model="currentVersion.description" type="textarea" rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="versionEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveVersion">确定</el-button>
      </div>
    </el-dialog>


    <!-- 分配管理弹窗 -->
    <el-dialog title="人员分配管理" v-model="assignmentDialogVisible" width="900px" center :close-on-click-modal="false">
      <div class="assignment-header">
        <h4 class="title">{{ currentProject.name }} - {{ currentVersion.versionNumber }}</h4>
        <div>
          <el-button v-if="assignments.length > 0" @click="copyAssignmentNotice" class="add-btn">📋 复制通知文案</el-button>
          <template v-if="isAdmin()">
            <el-button type="success" @click="openBatchDialog" class="add-btn">⚡ 批量智能分配</el-button>
            <el-button type="primary" @click="showAddAssignment" class="add-btn">新增分配</el-button>
          </template>
        </div>
      </div>

      <div v-if="assignments.length === 0 && !assignmentLoading" class="empty-state">
        <el-empty description="暂无分配数据" :image-size="100">
          <el-button type="primary" @click="openBatchDialog">批量智能分配</el-button>
        </el-empty>
      </div>

      <el-table
        v-loading="assignmentLoading"
        :data="assignments"
        border
        stripe
        style="width: 100%; margin-top: 10px"
        :header-cell-style="{ background: '#0f172a' }"
        v-else
      >
        <el-table-column prop="roleName" label="角色" min-width="100"></el-table-column>
        <el-table-column prop="userName" label="人员" min-width="100"></el-table-column>
        <el-table-column prop="feature" label="负责功能" min-width="240"></el-table-column>
        <el-table-column label="操作" min-width="100">
          <template #default="scope">
            <el-button v-if="isAdmin()" size="small" type="danger" @click="deleteAssignment(scope.row.id)">删除</el-button>
            <span v-else style="opacity:.5">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { getActiveProjectsWithVersions } from '@/api/myProjects';
import { getProjectVersions, addProjectVersion, updateProjectVersion, deleteProjectVersion } from '@/api/projectVersions';
import { getProjectAssignments, addProjectAssignment, deleteProjectAssignment, batchAddProjectAssignments } from '@/api/projectAssignments';
import { getRecommend } from '@/api/released';
import { getUsers } from '@/api/users';
import { getRoles } from '@/api/roles';
import { debounce } from 'lodash';
import { isAdmin } from '@/utils/auth';

export default {
  name: 'MyProjectsView',
  data() {
    return {
      loading: false,
      projects: [],

      // 版本相关（复用 ProjectsView 的逻辑）
      versionDialogVisible: false,
      versionLoading: false,
      versions: [],
      versionStatusFilter: '',
      versionSearchKeyword: '',
      versionSortMode: 'risk',
      versionCurrentPage: 1,
      versionPageSize: 5,
      currentVersion: this.emptyVersion(),
      versionEditDialogVisible: false,
      versionDialogTitle: '',
      versionRules: {
        versionNumber: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
        startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
        endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
      },

      // 分配相关
      assignmentDialogVisible: false,
      assignmentLoading: false,
      assignments: [],
      currentAssignment: { id: null, projectVersionId: null, roleId: null, userId: null, feature: '' },
      assignmentEditDialogVisible: false,
      assignmentDialogTitle: '',
      singleRecommendList: [],
      singleRecommendLoading: false,

      // 批量分配
      batchDialogVisible: false,
      batchRows: [],
      batchRecommending: false,
      batchSubmitting: false,

      currentProject: { id: null, name: '', description: '' },

      users: [],
      roles: []
    };
  },
  computed: {
    totalVersions() {
      return this.projects.reduce((sum, p) => sum + (p.versions || []).length, 0);
    },
    filteredVersions() {
      if (this.versionStatusFilter === '' || this.versionStatusFilter === null || this.versionStatusFilter === undefined) {
        return this.versions;
      }
      const status = Number(this.versionStatusFilter);
      return this.versions.filter(v => v.status === status);
    },
    sortedVersions() {
      const arr = [...this.filteredVersions];
      if (this.versionSortMode === 'risk') {
        arr.sort((a, b) => this.getVersionRiskScore(b) - this.getVersionRiskScore(a));
      } else if (this.versionSortMode === 'launch') {
        arr.sort((a, b) => {
          const da = a.launchTime ? new Date(a.launchTime).getTime() : Number.MAX_SAFE_INTEGER;
          const db = b.launchTime ? new Date(b.launchTime).getTime() : Number.MAX_SAFE_INTEGER;
          return da - db;
        });
      }
      return arr;
    },
    paginatedVersions() {
      const start = (this.versionCurrentPage - 1) * this.versionPageSize;
      return this.sortedVersions.slice(start, start + this.versionPageSize);
    },
    totalBatchAssignments() {
      return this.batchRows.reduce((sum, r) => sum + (r.picks ? r.picks.length : 0), 0);
    }
  },
  created() {
    this.fetchActiveProjects();
  },
  methods: {
    isAdmin,
    async fetchActiveProjects() {
      this.loading = true;
      try {
        const res = await getActiveProjectsWithVersions();
        // 数据已经是分层结构：项目 -> 版本列表
        this.projects = (res.data || []).map(p => ({
          ...p,
          name: p.projectName,
          versions: (p.versions || []).map(v => ({
            ...v,
            versionNumber: v.versionName,
            startDate: v.startTime,
            endDate: v.endTime,
            testTime: v.testTime,
            launchTime: v.launchTime,
            productManager: v.productManager,
            prototypeUrl: v.prototypeUrl,
            description: v.description
          }))
        }));
      } catch (e) {
        console.error('获取活跃项目失败:', e);
        this.$message.error('获取项目失败');
      } finally {
        this.loading = false;
      }
    },
    getProjectStatusText(p) { return ({0:'未开始',1:'进行中',2:'已结束'})[p.status] || '未知'; },
    getProjectStatusTag(p) { return ({0:'warning',1:'success',2:'info'})[p.status] || 'info'; },

    // ---------- 表格操作 ----------
    viewVersionsFromTable(project) {
      this.currentProject = { ...project, name: project.projectName };
      this.versionSearchKeyword = '';
      this.versionStatusFilter = '';
      this.versionCurrentPage = 1;
      // 直接使用已加载的版本数据
      this.versions = (project.versions || []).map(v => ({
        ...v,
        versionNumber: v.versionName || v.versionNumber,
        startDate: v.startTime || v.startDate,
        endDate: v.endTime || v.endDate,
        testTime: v.testTime,
        launchTime: v.launchTime,
        productManager: v.productManager,
        prototypeUrl: v.prototypeUrl,
        description: v.description
      }));
      this.versionDialogVisible = true;
    },

    // ---------- 版本相关 ----------
    async viewVersions(project) {
      this.currentProject = { ...project, name: project.projectName };
      this.versionSearchKeyword = '';
      this.versionStatusFilter = '';
      this.versionCurrentPage = 1;
      await this.fetchVersions();
      this.versionDialogVisible = true;
    },
    async fetchVersions() {
      this.versionLoading = true;
      try {
        const res = await getProjectVersions(this.currentProject.id, this.versionSearchKeyword || undefined);
        if (res && res.code === 200 && res.data) {
          const arr = Array.isArray(res.data) ? res.data : [res.data];
          this.versions = arr.map(v => ({
            ...v,
            versionNumber: v.versionName,
            startDate: v.startTime,
            endDate: v.endTime,
            testTime: v.testTime,
            launchTime: v.launchTime,
            productManager: v.productManager,
            prototypeUrl: v.prototypeUrl,
            description: v.description
          }));
        } else {
          this.versions = [];
        }
      } catch (e) {
        console.error('获取版本列表失败:', e);
      } finally {
        this.versionLoading = false;
      }
    },
    emptyVersion() {
      return {
        id: null, projectId: null, versionNumber: '', startDate: '', endDate: '',
        testTime: '', launchTime: '', productManager: '', prototypeUrl: '',
        description: '', bugCount: 0, bugResolvedList: [],
        isDelayed: false, delayedEndTime: ''
      };
    },
    showAddVersion() {
      this.currentVersion = this.emptyVersion();
      this.currentVersion.projectId = this.currentProject.id;
      this.versionDialogTitle = '添加版本';
      if (this.users.length === 0) {
        this.fetchUsersAndRoles();
      }
      this.versionEditDialogVisible = true;
    },
    editVersion(version, project) {
      // 如果传入了项目上下文（从内层表格调用），设置 currentProject
      if (project) {
        this.currentProject = { ...project, name: project.projectName };
      }
      this.currentVersion = {
        id: version.id,
        projectId: version.projectId || this.currentProject.id,
        versionNumber: version.versionNumber || version.versionName || '',
        startDate: version.startDate || version.startTime || '',
        endDate: version.endDate || version.endTime || '',
        testTime: version.testTime || '',
        launchTime: version.launchTime || '',
        productManager: version.productManager || '',
        prototypeUrl: version.prototypeUrl || '',
        description: version.description || '',
        bugCount: version.bugCount || 0,
        bugResolvedList: version.bugResolvedList ? JSON.parse(version.bugResolvedList) : [],
        isDelayed: !!version.isDelayed,
        delayedEndTime: version.delayedEndTime || ''
      };
      this.versionDialogTitle = '编辑版本';
      if (this.users.length === 0) {
        this.fetchUsersAndRoles();
      }
      this.versionEditDialogVisible = true;
    },
    async saveVersion() {
      this.$refs.versionForm.validate(async (valid) => {
        if (!valid) return;
        try {
          const payload = {
            id: this.currentVersion.id,
            projectId: this.currentProject.id,
            versionName: this.currentVersion.versionNumber,
            startTime: this.formatDateForBackend(this.currentVersion.startDate),
            endTime: this.formatDateForBackend(this.currentVersion.endDate),
            testTime: this.formatDateForBackend(this.currentVersion.testTime),
            launchTime: this.formatDateForBackend(this.currentVersion.launchTime),
            productManager: this.currentVersion.productManager,
            prototypeUrl: this.currentVersion.prototypeUrl,
            description: this.currentVersion.description,
            bugCount: this.currentVersion.bugCount || 0,
            bugResolvedList: JSON.stringify(this.currentVersion.bugResolvedList || []),
            isDelayed: !!this.currentVersion.isDelayed,
            delayedEndTime: this.currentVersion.isDelayed ? this.formatDateForBackend(this.currentVersion.delayedEndTime) : null
          };
          if (this.currentVersion.id) {
            await updateProjectVersion(this.currentVersion.id, payload);
            this.$message.success('更新成功');
          } else {
            await addProjectVersion(payload);
            this.$message.success('添加成功');
          }
          this.versionEditDialogVisible = false;
          await this.fetchVersions();
        } catch (e) {
          console.error('保存版本失败:', e);
          this.$message.error('保存失败');
        }
      });
    },
    async deleteVersion(id) {
      try {
        await this.$confirm('确认删除该版本?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' });
        await deleteProjectVersion(id);
        this.$message.success('删除成功');
        await this.fetchVersions();
      } catch (e) {
        if (e !== 'cancel') { console.error(e); this.$message.error('删除失败'); }
      }
    },
    copyVersion(version) {
      this.currentVersion = {
        id: null,
        projectId: this.currentProject.id,
        versionNumber: (version.versionNumber || version.versionName || '') + '_copy',
        startDate: version.startDate || version.startTime || '',
        endDate: version.endDate || version.endTime || '',
        testTime: version.testTime || '',
        launchTime: '',
        productManager: version.productManager || '',
        prototypeUrl: version.prototypeUrl || '',
        description: version.description || '',
        bugCount: 0,
        bugResolvedList: [],
        isDelayed: false,
        delayedEndTime: ''
      };
      this.versionDialogTitle = '复制版本';
      this.versionEditDialogVisible = true;
    },

    // ---------- Bug统计相关方法 ----------
    addBugResolvedItem() {
      this.currentVersion.bugResolvedList.push({ userId: null, levels: [] });
    },
    removeBugResolvedItem(index) {
      this.currentVersion.bugResolvedList.splice(index, 1);
    },
    addLevel(item) {
      this.$prompt('请输入级别名称', '添加级别', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：一级、二级、三级等'
      }).then(({ value }) => {
        if (value && value.trim()) {
          item.levels.push({ name: value.trim(), count: 0 });
          this.$message.success(`已添加「${value.trim()}」`);
        }
      }).catch(() => {});
    },
    removeLevel(item, lIndex) {
      item.levels.splice(lIndex, 1);
    },


    // ---------- 工具方法 ----------
    onVersionDialogClose() {
      this.assignmentDialogVisible = false;
      this.versionEditDialogVisible = false;
      this.assignmentEditDialogVisible = false;
      this.batchDialogVisible = false;
    },
    formatDate(s) {
      if (!s) return '';
      const d = new Date(s);
      if (isNaN(d.getTime())) return '';
      return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
    },
    formatDateForBackend(s) {
      if (!s) return null;
      const d = new Date(s);
      if (isNaN(d.getTime())) return null;
      return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T00:00:00`;
    },
    getDateProgress(v) {
      if (!v.startDate || !v.endDate) return { percent: 0, status: '', text: '未设置日期' };
      const s = new Date(v.startDate).getTime();
      const e = new Date(v.endDate).getTime();
      const now = Date.now();
      if (isNaN(s) || isNaN(e) || e <= s) return { percent: 0, status: '', text: '日期异常' };
      if (now < s) return { percent: 0, status: '', text: '尚未开始' };
      if (now > e) {
        if (v.status === 2) return { percent: 100, status: 'success', text: '已完成' };
        const overDays = Math.round((now - e) / 86400000);
        return { percent: 100, status: 'exception', text: `超期 ${overDays} 天` };
      }
      const pct = Math.round(((now - s) / (e - s)) * 100);
      return { percent: pct, status: pct >= 80 ? 'warning' : 'success', text: `进度 ${pct}%` };
    },
    getDaysToLaunch(v) {
      if (!v.launchTime) return null;
      const lt = new Date(v.launchTime).getTime();
      if (isNaN(lt)) return null;
      const today = new Date(); today.setHours(0,0,0,0);
      return Math.round((lt - today.getTime()) / 86400000);
    },
    getDaysToLaunchText(v) {
      const d = this.getDaysToLaunch(v);
      if (d === null) return '';
      if (d === 0) return '🔥 今日上线';
      if (d > 0) return `还剩 ${d} 天`;
      return `已上线 ${-d} 天`;
    },
    getDaysToLaunchColor(v) {
      const d = this.getDaysToLaunch(v);
      if (d === null) return 'inherit';
      if (d >= 0 && d <= 3) return '#f56c6c';
      if (d >= 0 && d <= 7) return '#e6a23c';
      if (d < 0) return '#909399';
      return '#67c23a';
    },
    getVersionRiskScore(v) {
      let score = 0;
      if (v.isDelayed) score += 100;
      if (v.endDate && Date.now() > new Date(v.endDate).getTime() && v.status !== 2) score += 60;
      const d = this.getDaysToLaunch(v);
      if (d !== null && d >= 0 && d <= 7 && v.status !== 2) score += (8 - d) * 5;
      if ((v.bugCount || 0) > 0) score += Math.min(40, v.bugCount * 5);
      return score;
    },
    getVersionHealth(v) {
      const score = this.getVersionRiskScore(v);
      if (score >= 100) return { label: '🚨 危险', type: 'danger' };
      if (score >= 50) return { label: '⚠ 警示', type: 'warning' };
      if (score > 0) return { label: '• 提醒', type: 'info' };
      return { label: '✓ 良好', type: 'success' };
    },
    getVersionStatusText(v) { return ({0:'未开始',1:'进行中',2:'已结束'})[v.status] || '未知'; },
    getVersionStatusTag(v) { return ({0:'warning',1:'success',2:'info'})[v.status] || 'info'; },

    // ---------- 分配相关 ----------
    async viewAssignments(version, project) {
      // 如果传入了项目上下文，设置 currentProject
      if (project) {
        this.currentProject = { ...project, name: project.projectName };
      }
      this.currentVersion = { ...version };
      await this.fetchUsersAndRoles();
      await this.fetchAssignments();
      this.assignmentDialogVisible = true;
    },
    async fetchAssignments() {
      this.assignmentLoading = true;
      try {
        const res = await getProjectAssignments(this.currentVersion.id);
        this.assignments = (res.data || []).map(a => ({
          ...a,
          roleName: a.roleName || '未知角色',
          userName: this.users.find(u => u.id === a.userId)?.username || '未知用户'
        }));
      } catch (e) {
        console.error('获取分配列表失败:', e);
      } finally {
        this.assignmentLoading = false;
      }
    },
    async fetchUsersAndRoles() {
      try {
        const [u, r] = await Promise.all([getUsers(), getRoles()]);
        this.users = u.data || [];
        this.roles = (r.data || []).map(role => ({ ...role, name: role.roleName || role.name }));
      } catch (e) {
        console.error('获取用户或角色列表失败:', e);
      }
    },
    showAddAssignment() {
      this.currentAssignment = { id: null, projectVersionId: this.currentVersion.id, roleId: null, userId: null, feature: '' };
      this.singleRecommendList = [];
      this.assignmentDialogTitle = '添加分配';
      this.assignmentEditDialogVisible = true;
    },
    async onRoleChangeForSingle(roleId) {
      this.currentAssignment.userId = null;
      if (!roleId) { this.singleRecommendList = []; return; }
      this.singleRecommendLoading = true;
      try {
        const res = await getRecommend({ roleId, projectVersionId: this.currentVersion.id, days: 14 });
        this.singleRecommendList = res.data || [];
      } catch (e) {
        console.error('获取推荐人员失败:', e);
        this.singleRecommendList = [];
      } finally {
        this.singleRecommendLoading = false;
      }
    },
    async saveAssignment() {
      this.$refs.assignmentForm.validate(async (valid) => {
        if (!valid) return;
        try {
          await addProjectAssignment({ ...this.currentAssignment });
          this.$message.success('添加成功');
          this.assignmentEditDialogVisible = false;
          await this.fetchAssignments();
        } catch (e) {
          console.error('保存分配失败:', e);
          this.$message.error('保存失败');
        }
      });
    },
    async deleteAssignment(id) {
      try {
        await this.$confirm('确认删除该分配?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' });
        await deleteProjectAssignment(id);
        this.$message.success('删除成功');
        await this.fetchAssignments();
      } catch (e) {
        if (e !== 'cancel') { console.error(e); this.$message.error('删除失败'); }
      }
    },

    // ---------- 批量智能分配 ----------
    openBatchDialog() {
      this.batchRows = [this.emptyBatchRow()];
      this.batchDialogVisible = true;
    },
    emptyBatchRow() {
      return { feature: '', roleIds: [], picks: [] };
    },
    addBatchRow() { this.batchRows.push(this.emptyBatchRow()); },
    removeBatchRow(idx) { this.batchRows.splice(idx, 1); },
    onBatchRolesChange(row) {
      const oldMap = new Map((row.picks || []).map(p => [p.roleId, p]));
      row.picks = row.roleIds.map(rid => oldMap.get(rid) || { roleId: rid, userId: null, candidates: [], loading: false, reason: '' });
    },
    async smartAssignAll() {
      const valid = this.batchRows.filter(r => r.feature && r.feature.trim() && r.roleIds.length > 0);
      if (valid.length === 0) {
        this.$message.warning('请至少填写一个功能模块并选择角色');
        return;
      }
      this.batchRecommending = true;
      try {
        const tasks = [];
        valid.forEach(row => {
          this.onBatchRolesChange(row);
          row.picks.forEach(pick => {
            pick.loading = true;
            tasks.push((async () => {
              try {
                const res = await getRecommend({ roleId: pick.roleId, projectVersionId: this.currentVersion.id, days: 14 });
                pick.candidates = res.data || [];
                if (pick.candidates.length > 0) {
                  const top = pick.candidates[0];
                  pick.userId = top.userId;
                  pick.reason = this.buildReason(top);
                } else {
                  pick.userId = null;
                  pick.reason = '⚠ 无可用人员';
                }
              } catch (e) {
                pick.candidates = [];
                pick.userId = null;
                pick.reason = '推荐失败';
              } finally {
                pick.loading = false;
              }
            })());
          });
        });
        await Promise.all(tasks);
        this.$message.success(`已为 ${tasks.length} 条任务推荐人员，可微调后提交`);
      } finally {
        this.batchRecommending = false;
      }
    },
    buildReason(u) {
      const parts = [];
      if (u.hasHistory) parts.push('做过本项目');
      if (u.status === 'released') parts.push('当前空闲');
      else parts.push(`${u.daysLeft}天后释放`);
      parts.push(`任务${u.ongoingCount}`);
      return parts.join(' · ');
    },
    async submitBatch() {
      const list = [];
      this.batchRows.forEach(row => {
        if (!row.feature || !row.feature.trim()) return;
        (row.picks || []).forEach(pick => {
          if (pick.userId && pick.roleId) {
            list.push({
              projectVersionId: this.currentVersion.id,
              userId: pick.userId,
              roleId: pick.roleId,
              feature: row.feature.trim()
            });
          }
        });
      });
      if (list.length === 0) {
        this.$message.warning('没有可提交的分配，请先点击「一键智能推荐」或手动选择人员');
        return;
      }
      const existing = new Set(this.assignments.map(a => `${a.userId}|${a.roleId}|${(a.feature || '').trim()}`));
      const dup = list.filter(x => existing.has(`${x.userId}|${x.roleId}|${x.feature}`));
      const fresh = list.filter(x => !existing.has(`${x.userId}|${x.roleId}|${x.feature}`));
      if (dup.length > 0) {
        try {
          await this.$confirm(
            `检测到 ${dup.length} 条重复分配（同人员 / 角色 / 功能已存在）将自动跳过；剩余 ${fresh.length} 条将提交。是否继续？`,
            '重复提醒',
            { confirmButtonText: '继续提交', cancelButtonText: '取消', type: 'warning' }
          );
        } catch (e) { return; }
      }
      if (fresh.length === 0) {
        this.$message.warning('所有分配都已存在，无需重复提交');
        return;
      }
      this.batchSubmitting = true;
      try {
        await batchAddProjectAssignments(fresh);
        this.$message.success(`成功创建 ${fresh.length} 条分配${dup.length > 0 ? `，跳过 ${dup.length} 条重复` : ''}`);
        this.batchDialogVisible = false;
        await this.fetchAssignments();
      } catch (e) {
        console.error('批量分配失败:', e);
        this.$message.error('批量分配失败');
      } finally {
        this.batchSubmitting = false;
      }
    },
    copyAssignmentNotice() {
      if (this.assignments.length === 0) {
        this.$message.warning('暂无分配数据可复制');
        return;
      }
      const lines = [];
      lines.push(`📢 【${this.currentProject.name} - ${this.currentVersion.versionNumber}】任务分配通知`);
      lines.push('─'.repeat(20));
      const grouped = {};
      this.assignments.forEach(a => {
        const role = a.roleName || '未指定';
        if (!grouped[role]) grouped[role] = [];
        grouped[role].push(a);
      });
      Object.keys(grouped).forEach(role => {
        lines.push(`【${role}】`);
        grouped[role].forEach(a => {
          lines.push(`  • @${a.userName} → ${a.feature}`);
        });
      });
      lines.push('─'.repeat(20));
      if (this.currentVersion.startDate || this.currentVersion.endDate) {
        lines.push(`📅 研发周期：${this.formatDate(this.currentVersion.startDate)} ~ ${this.formatDate(this.currentVersion.endDate)}`);
      }
      if (this.currentVersion.testTime) lines.push(`🔬 提测：${this.formatDate(this.currentVersion.testTime)}`);
      if (this.currentVersion.launchTime) lines.push(`🚀 上线：${this.formatDate(this.currentVersion.launchTime)}`);
      const text = lines.join('\n');
      try {
        navigator.clipboard.writeText(text).then(() => this.$message.success('已复制到剪贴板'));
      } catch (e) {
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); this.$message.success('已复制到剪贴板'); }
        catch (err) { this.$message.error('复制失败，请手动复制'); }
        document.body.removeChild(ta);
      }
    },

    // ---------- 分页 ----------
    handleVersionSizeChange(size) { this.versionPageSize = size; this.versionCurrentPage = 1; },
    handleVersionCurrentChange(page) { this.versionCurrentPage = page; },
    handleVersionSearchInput: debounce(function () {
      this.versionCurrentPage = 1;
      this.fetchVersions();
    }, 300),
    handleVersionSearchClear() {
      this.versionCurrentPage = 1;
      this.fetchVersions();
    }
  }
};
</script>

<style scoped>
.my-projects-container {
  max-width: 1400px;
  margin: 0 auto;
  color: var(--text-primary);
}
.user-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px 18px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.25);
  backdrop-filter: blur(12px);
}
.selector-label {
  font-size: 14px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.user-select {
  width: 300px;
}
.card-section { margin-bottom: 18px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title {
  font-size: 16px; font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px; margin: 0;
}
.title::before {
  content: "●"; color: var(--accent-cyan);
  margin-right: 8px; font-size: 12px;
}
.empty-state { padding: 60px 0; text-align: center; }
:deep(.el-empty__description p) { color: var(--text-muted) !important; }

/* 版本相关样式（复用 ProjectsView 的样式） */
.version-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding: 12px 16px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}
.version-header h4 {
  margin: 0; font-size: 14px; font-weight: 600;
  color: var(--text-primary);
  display: flex; align-items: center; gap: 8px;
}
.version-header h4::before {
  content: "◆"; color: var(--accent-cyan); font-size: 10px;
}
.version-toolbar { display: flex; gap: 10px; align-items: center; }
.version-status-filter { width: 140px; }
.version-sort { width: 150px; }
.version-search { width: 200px; }

.progress-text { font-size: 11px; opacity: .7; margin-top: 2px; }
.launch-distance { font-size: 11px; margin-top: 2px; font-weight: 500; }

.main-card {
  background: var(--glass-bg) !important;
  border: 1px solid var(--glass-border) !important;
  border-radius: 14px !important;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
  backdrop-filter: blur(12px);
  overflow: hidden;
}
.pagination-container {
  display: flex; justify-content: flex-end;
  margin-top: 16px; padding: 8px 4px;
}
.form-row { display: flex; gap: 12px; }
.form-item-half { flex: 1; }

.assignment-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding: 12px 16px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}
.batch-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 14px; padding: 10px 14px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}
.batch-tip { margin-left: auto; font-size: 12px; opacity: .75; }
.batch-list { display: flex; flex-direction: column; gap: 12px; max-height: 460px; overflow-y: auto; padding-right: 4px; }
.batch-row {
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}
.batch-row-head { display: flex; align-items: center; gap: 10px; }
.batch-idx {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-blue), var(--accent-cyan));
  color: #fff; font-size: 12px; font-weight: 700; flex-shrink: 0;
}
.batch-feature { flex: 1.2; }
.batch-roles { flex: 1; min-width: 200px; }
.batch-picks {
  margin-top: 10px; padding-top: 10px;
  border-top: 1px dashed var(--glass-border);
  display: flex; flex-direction: column; gap: 8px;
}
.batch-pick { display: flex; align-items: center; gap: 10px; }
.batch-user-select { flex: 1; max-width: 320px; }
.batch-reason { font-size: 12px; opacity: 0.75; color: var(--accent-cyan); }
.dialog-footer {
  display: flex; justify-content: flex-end;
  gap: 12px; margin-top: 16px;
}

/* Bug统计样式 */
.bug-stats-container, .bug-activated-container {
  width: 100%;
}
.bug-stat-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding: 8px;
  background: rgba(30, 41, 59, 0.3);
  border-radius: 6px;
  border: 1px solid var(--glass-border);
}
.bug-stat-row:last-child {
  margin-bottom: 0;
}
.bug-activated-group {
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.4);
  border-radius: 8px;
  border: 1px solid var(--glass-border);
}
.bug-activated-group:last-child {
  margin-bottom: 0;
}
.group-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--glass-border);
}
.times-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-right: 12px;
}
.group-items {
  padding-left: 8px;
}
.bug-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.paste-textarea {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

/* 激活次数输入框样式优化 */
.group-header .el-input-number {
  background: rgba(30, 41, 59, 0.5);
  border-radius: 6px;
}
.group-header .el-input-number__inner {
  color: #fff !important;
  font-weight: 600;
}
@media (max-width: 768px) {
  .user-selector { flex-direction: column; align-items: stretch; }
  .user-select { width: 100%; }
  .version-header, .assignment-header { flex-direction: column; align-items: stretch; gap: 10px; }
}
.version-inner-table {
  padding: 10px 20px 10px 40px;
}
.version-inner-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--accent-cyan);
  margin-bottom: 8px;
}
.empty-inner {
  padding: 10px 20px 10px 40px;
}

/* Bug级别输入框样式 */
.level-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.level-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(30, 41, 59, 0.3);
  padding: 4px 8px;
  border-radius: 6px;
  margin-right: 4px;
}

.level-unit {
  margin-right: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.level-item .el-button--mini {
  padding: 5px 8px;
  height: 24px;
}

.level-item .el-button--mini i {
  font-size: 12px;
}
</style>
