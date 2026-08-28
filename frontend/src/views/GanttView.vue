<template>
  <div class="gantt-container">
    <!-- 页面标题 -->
    <el-card class="header-card">
      <div class="page-header">
        <h2>📊 项目进度甘特图</h2>
        <div class="toolbar">
          <el-select v-model="viewMode" placeholder="视图模式" style="width: 150px; margin-right: 12px" @change="refreshData">
            <el-option label="全部项目" value="all"></el-option>
            <el-option label="未开始" value="0"></el-option>
            <el-option label="进行中" value="1"></el-option>
            <el-option label="已结束" value="2"></el-option>
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px"
            @change="refreshData"
          ></el-date-picker>
          <el-button type="primary" icon="Refresh" @click="refreshData" style="margin-left: 12px">刷新</el-button>
        </div>
      </div>
      <!-- 图例 -->
      <div class="legend-bar">
        <div class="legend-item"><span class="legend-dot dot-not-started"></span>未开始</div>
        <div class="legend-item"><span class="legend-dot dot-in-progress"></span>进行中</div>
        <div class="legend-item"><span class="legend-dot dot-completed"></span>已完成</div>
        <div class="legend-item"><span class="legend-dot dot-danger"></span>高风险</div>
        <div class="legend-item"><span class="legend-dot dot-warning"></span>警示</div>
        <div class="legend-item"><span class="legend-marker marker-test-legend"></span>提测时间</div>
        <div class="legend-item"><span class="legend-marker marker-launch-legend"></span>上线时间</div>
      </div>
    </el-card>

    <!-- 甘特图主体 -->
    <el-card class="gantt-main-card" v-loading="loading">
      <div v-if="projects.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无项目数据" :image-size="120"></el-empty>
      </div>

      <div v-else class="gantt-wrapper">
        <!-- 左侧：任务列表 -->
        <div class="gantt-left-panel">
          <div class="task-list-header">
            <div class="col-project">项目名称</div>
            <div class="col-status">状态</div>
            <div class="col-manager">负责人</div>
          </div>
          <div class="task-list-body">
            <div
              v-for="project in projects"
              :key="'project-' + project.id"
              class="task-group"
            >
              <!-- 项目组头部 -->
              <div class="group-header" @click="toggleGroup(project)">
                <i :class="project.expanded ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" class="expand-icon"></i>
                <span class="group-name">{{ project.projectName }}</span>
                <el-tag size="mini" :type="getProjectStatusTag(project.status)" style="margin-left: 8px">
                  {{ getProjectStatusText(project.status) }}
                </el-tag>
              </div>

              <!-- 版本任务列表 -->
              <div v-show="project.expanded" class="version-list">
                <div
                  v-for="version in (project.versions || [])"
                  :key="'version-' + version.id"
                  class="version-item"
                  :class="{ active: isCurrentTask(version) }"
                >
                  <div class="version-info">
                    <span class="version-number">{{ version.versionNumber }}</span>
                    <el-tag v-if="getVersionRiskLevel(version).type === 'danger'" size="mini" type="danger" effect="dark">高风险</el-tag>
                  </div>
                  <div class="version-status">
                    <el-tag :type="getVersionStatusTag(version.status)" size="mini">
                      {{ getVersionStatusText(version.status) }}
                    </el-tag>
                  </div>
                  <div class="version-manager">{{ version.productManager || '-' }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：时间轴和条形图 -->
        <div class="gantt-right-panel">
          <!-- 时间刻度（同步滚动） -->
          <div class="timeline-header" ref="timelineHeader">
            <div class="timeline-header-inner" :style="{ width: timelineWidth + 'px' }">
              <div
                v-for="month in timelineMonths"
                :key="'month-' + month.label"
                class="month-cell"
                :style="{ width: month.width + 'px' }"
              >
                <div class="month-label">{{ month.label }}</div>
                <div class="days-row">
                  <div
                    v-for="(day, dIndex) in month.days"
                    :key="'day-' + dIndex"
                    class="day-cell"
                    :class="{ weekend: day.isWeekend, today: day.isToday }"
                  >
                    {{ day.dayNum }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 甘特图内容区域 -->
          <div class="timeline-body" ref="timelineBody" @scroll="syncScroll">
            <div class="timeline-body-inner" :style="{ width: timelineWidth + 'px' }">
              <div
                v-for="project in projects"
                :key="'timeline-project-' + project.id"
                class="timeline-group"
              >
                <!-- 项目组占位行 -->
                <div class="group-row"></div>

                <!-- 版本条形图 -->
                <div v-show="project.expanded" class="timeline-version-list">
                  <div
                    v-for="version in (project.versions || [])"
                    :key="'timeline-version-' + version.id"
                    class="timeline-version-item"
                    :class="{ active: isCurrentTask(version) }"
                    @mouseenter="showTooltip(version, project, $event)"
                    @mousemove="updateTooltipPosition($event)"
                    @mouseleave="hideTooltip"
                  >
                    <!-- 提测标记 -->
                    <div
                      v-if="version.testTime && version.startDate && version.endDate"
                      class="gantt-marker marker-test"
                      :style="getMarkerStyle(version, 'testTime')"
                    >
                      <span class="marker-flag">提</span>
                    </div>
                    <!-- 上线标记 -->
                    <div
                      v-if="version.launchTime && version.startDate && version.endDate"
                      class="gantt-marker marker-launch"
                      :style="getMarkerStyle(version, 'launchTime')"
                    >
                      <span class="marker-flag">上</span>
                    </div>
                    <!-- 任务条形图 -->
                    <div
                      v-if="version.startDate && version.endDate"
                      class="gantt-bar"
                      :class="[getBarColorClass(version)]"
                      :style="getBarStyle(version)"
                      @click="viewAssignments(version, project)"
                    >
                      <span class="bar-text">{{ version.versionNumber }}</span>
                      <span v-if="isOverdue(version)" class="overdue-indicator">⚠️</span>
                    </div>
                    <!-- 无日期提示 -->
                    <div v-else class="no-date-hint">未设置日期</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </el-card>

    <!-- 悬浮提示框（放在el-card外部，避免backdrop-filter裁剪） -->
    <div v-if="currentHoverTask && tooltipPosition" class="custom-tooltip" :style="{ left: tooltipPosition.x + 'px', top: tooltipPosition.y + 'px' }">
      <div class="tooltip-content">
        <h4>{{ currentHoverTask.projectName }} - {{ currentHoverTask.versionNumber }}</h4>
        <p><strong>状态：</strong>{{ getVersionStatusText(currentHoverTask.status) }}</p>
        <p><strong>研发周期：</strong>{{ formatDate(currentHoverTask.startDate) }} ~ {{ formatDate(currentHoverTask.endDate) }}</p>
        <p><strong>提测时间：</strong>{{ formatDate(currentHoverTask.testTime) || '未设置' }}</p>
        <p><strong>上线时间：</strong>{{ formatDate(currentHoverTask.launchTime) || '未设置' }}</p>
        <p><strong>产品负责：</strong>{{ currentHoverTask.productManager || '未分配' }}</p>
        <p v-if="currentHoverTask.bugCount"><strong>Bug数：</strong>{{ currentHoverTask.bugCount }}</p>
        <p v-if="currentHoverTask.isDelayed" style="color: #f56c6c"><strong>已延期至：</strong>{{ formatDate(currentHoverTask.delayedEndTime) }}</p>
        <p><strong>健康度：</strong>
          <el-tag :type="getVersionRiskLevel(currentHoverTask).type" size="mini">
            {{ getVersionRiskLevel(currentHoverTask).label }}
          </el-tag>
        </p>
      </div>
    </div>
    <!-- 人员分配弹窗 -->
    <el-dialog title="人员分配管理" v-model="assignmentDialogVisible" width="900px" center :close-on-click-modal="false">
      <div class="assignment-header">
        <h4 class="assignment-title">{{ currentProject.projectName }} - {{ currentVersion.versionNumber }}</h4>
        <div>
          <el-button v-if="assignments.length > 0" @click="copyAssignmentNotice" class="add-btn">📋 复制通知文案</el-button>
          <el-button v-if="isAdmin()" type="primary" size="small" @click="showAddAssignment">新增分配</el-button>
        </div>
      </div>

      <div v-if="assignments.length === 0 && !assignmentLoading" class="empty-state">
        <el-empty description="暂无分配数据" :image-size="80">
          <el-button v-if="isAdmin()" type="primary" @click="showAddAssignment">添加分配</el-button>
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
        <el-table-column label="操作" min-width="80">
          <template #default="scope">
            <el-button v-if="isAdmin()" size="small" type="danger" @click="deleteAssignment(scope.row.id)">删除</el-button>
            <span v-else style="opacity:.5">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新增分配弹窗 -->
    <el-dialog :title="assignmentDialogTitle" v-model="assignmentEditDialogVisible" width="500px" center :close-on-click-modal="false" append-to-body>
      <el-form ref="assignmentForm" :model="currentAssignment" :rules="assignmentRules" label-width="80px">
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="currentAssignment.roleId" placeholder="选择角色" style="width:100%" filterable>
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName || role.name" :value="String(role.id)"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="人员" prop="userId">
          <el-select v-model="currentAssignment.userId" placeholder="选择人员" style="width:100%" filterable>
            <el-option v-for="user in users" :key="user.id" :label="user.username" :value="user.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="负责功能">
          <el-input v-model="currentAssignment.feature" placeholder="如：登录模块开发"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignmentEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssignment">确定</el-button>
      </template>
    </el-dialog>
  </div>
  </template>

<script>
import { getActiveProjectsWithVersions } from '@/api/myProjects';
import { getProjectAssignments, addProjectAssignment, deleteProjectAssignment } from '@/api/projectAssignments';
import { getUsers } from '@/api/users';
import { getRoles } from '@/api/roles';
import { isAdmin } from '@/utils/auth';
import dayjs from 'dayjs';

export default {
  name: 'GanttView',
  data() {
    return {
      loading: false,
      viewMode: 'all',
      dateRange: null,
      projects: [],
      timelineStart: null,
      timelineEnd: null,
      timelineMonths: [],
      dayWidth: 30, // 每天的像素宽度
      currentHoverTask: null,
      tooltipPosition: null,
      // 人员分配相关
      assignmentDialogVisible: false,
      assignmentLoading: false,
      assignments: [],
      currentAssignment: { id: null, projectVersionId: null, roleId: null, userId: null, feature: '' },
      assignmentEditDialogVisible: false,
      assignmentDialogTitle: '',
      assignmentRules: {
        roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
        userId: [{ required: true, message: '请选择人员', trigger: 'change' }]
      },
      currentVersion: {},
      currentProject: {},
      users: [],
      roles: []
    };
  },
  computed: {
    timelineWidth() {
      return this.timelineMonths.reduce((sum, m) => sum + m.width, 0);
    }
  },
  created() {
    this.initDateRange();
    this.refreshData();
  },
  methods: {
    initDateRange() {
      const now = dayjs();
      this.dateRange = [now.subtract(1, 'month').startOf('month'), now.add(3, 'month').endOf('month')];
    },

    async refreshData() {
      this.loading = true;
      try {
        const res = await getActiveProjectsWithVersions();
        let allProjects = res.data || [];

        // 根据视图模式过滤
        if (this.viewMode !== 'all') {
          const status = parseInt(this.viewMode);
          allProjects = allProjects.filter(p => p.status === status);
        }

        // 根据日期范围过滤
        if (this.dateRange && this.dateRange.length === 2) {
          const startDate = dayjs(this.dateRange[0]).startOf('day');
          const endDate = dayjs(this.dateRange[1]).endOf('day');
          
          allProjects = allProjects.map(project => {
            const filteredVersions = (project.versions || []).filter(v => {
              if (!v.startTime || !v.endTime) return false;
              const vStart = dayjs(v.startTime);
              const vEnd = dayjs(v.endTime);
              return vStart.isBefore(endDate) && vEnd.isAfter(startDate);
            });
            return { ...project, versions: filteredVersions };
          }).filter(p => (p.versions || []).length > 0);
        }

        // 处理数据
        this.projects = allProjects.map(p => ({
          ...p,
          expanded: true,
          versions: (p.versions || []).map(v => ({
            ...v,
            versionNumber: v.versionName || v.versionNumber,
            startDate: v.startTime,
            endDate: v.endTime,
            testTime: v.testTime,
            launchTime: v.launchTime,
            productManager: v.productManager,
            prototypeUrl: v.prototypeUrl,
            description: v.description,
            bugCount: v.bugCount || 0,
            isDelayed: !!v.isDelayed,
            delayedEndTime: v.delayedEndTime
          }))
        }));

        // 计算时间轴范围
        this.calculateTimeline();
      } catch (e) {
        console.error('获取项目数据失败:', e);
        this.$message.error('获取项目数据失败');
      } finally {
        this.loading = false;
      }
    },

    calculateTimeline() {
      if (this.projects.length === 0) return;

      let minDate = null;
      let maxDate = null;

      this.projects.forEach(p => {
        (p.versions || []).forEach(v => {
          if (v.startDate) {
            const start = dayjs(v.startDate);
            if (!minDate || start.isBefore(minDate)) minDate = start;
          }
          if (v.endDate) {
            const end = dayjs(v.endDate);
            if (!maxDate || end.isAfter(maxDate)) maxDate = end;
          }
        });
      });

      if (!minDate || !maxDate) return;

      // 扩展前后缓冲期
      this.timelineStart = minDate.subtract(7, 'day').startOf('week');
      this.timelineEnd = maxDate.add(7, 'day').endOf('week');

      // 生成月份刻度
      this.generateTimelineMonths();
    },

    generateTimelineMonths() {
      this.timelineMonths = [];
      let current = this.timelineStart.clone();

      while (current.isBefore(this.timelineEnd)) {
        const monthLabel = current.format('YYYY年MM月');
        const monthEnd = current.endOf('month');
        // 当月显示范围：从current到min(monthEnd, timelineEnd)
        const rangeEnd = monthEnd.isBefore(this.timelineEnd) ? monthEnd : this.timelineEnd;
        const daysToDisplay = rangeEnd.diff(current, 'day') + 1;

        const monthDays = [];
        for (let i = 0; i < daysToDisplay; i++) {
          const dayDate = current.add(i, 'day');
          monthDays.push({
            dayNum: dayDate.date(),
            isWeekend: dayDate.day() === 0 || dayDate.day() === 6,
            isToday: dayDate.isSame(dayjs(), 'day')
          });
        }

        this.timelineMonths.push({
          label: monthLabel,
          width: monthDays.length * this.dayWidth,
          days: monthDays
        });

        // 移到下个月1号
        current = monthEnd.add(1, 'day');
      }
    },

    toggleGroup(project) {
      project.expanded = !project.expanded;
    },

    syncScroll() {
      if (this.$refs.timelineHeader && this.$refs.timelineBody) {
        this.$refs.timelineHeader.scrollLeft = this.$refs.timelineBody.scrollLeft;
      }
    },

    getBarStyle(version) {
      if (!version.startDate || !version.endDate) return {};

      const start = dayjs(version.startDate);
      const end = dayjs(version.endDate);
      
      const offsetDays = start.diff(this.timelineStart, 'day');
      const durationDays = end.diff(start, 'day') + 1;

      const left = offsetDays * this.dayWidth;
      const width = durationDays * this.dayWidth;

      return {
        left: left + 'px',
        width: Math.max(width, 60) + 'px' // 最小宽度60px
      };
    },

    getMarkerStyle(version, dateField) {
      if (!version[dateField] || !this.timelineStart) return {};
      const target = dayjs(version[dateField]);
      const offsetDays = target.diff(this.timelineStart, 'day');
      const left = offsetDays * this.dayWidth + this.dayWidth / 2; // 居中到当天
      return { left: left + 'px' };
    },

    getBarColorClass(version) {
      const risk = this.getVersionRiskLevel(version);
      if (risk.type === 'danger') return 'bar-danger';
      if (risk.type === 'warning') return 'bar-warning';
      if (version.status === 0) return 'bar-not-started';
      if (version.status === 1) return 'bar-in-progress';
      if (version.status === 2) return 'bar-completed';
      return 'bar-default';
    },

    showTooltip(version, project, event) {
      this.currentHoverTask = {
        ...version,
        projectName: project ? project.projectName : ''
      };

      // 设置提示框位置
      if (event && event.clientX !== undefined) {
        this.tooltipPosition = {
          x: event.clientX + 15,
          y: event.clientY + 15
        };
      }
    },

    updateTooltipPosition(event) {
      if (this.currentHoverTask && event && event.clientX !== undefined) {
        // 防止提示框超出右边界
        const tooltipWidth = 320;
        let x = event.clientX + 15;
        if (x + tooltipWidth > window.innerWidth) {
          x = event.clientX - tooltipWidth - 15;
        }
        // 防止提示框超出下边界
        let y = event.clientY + 15;
        if (y + 200 > window.innerHeight) {
          y = event.clientY - 200;
        }
        this.tooltipPosition = { x, y };
      }
    },

    hideTooltip() {
      this.currentHoverTask = null;
      this.tooltipPosition = null;
    },

    isCurrentTask() {
      // 可以根据需要高亮当前选中的任务
      return false;
    },

    isOverdue(version) {
      if (!version.endDate || version.status === 2) return false;
      return dayjs().isAfter(dayjs(version.endDate));
    },

    // 工具方法
    formatDate(dateStr) {
      if (!dateStr) return '';
      return dayjs(dateStr).format('YYYY-MM-DD');
    },

    getProjectStatusText(status) {
      return ({ 0: '未开始', 1: '进行中', 2: '已结束' })[status] || '未知';
    },

    getProjectStatusTag(status) {
      return ({ 0: 'info', 1: 'success', 2: 'info' })[status] || 'info';
    },

    getVersionStatusText(status) {
      return ({ 0: '未开始', 1: '进行中', 2: '已结束' })[status] || '未知';
    },

    getVersionStatusTag(status) {
      return ({ 0: 'warning', 1: 'success', 2: 'info' })[status] || 'info';
    },

    getVersionRiskScore(version) {
      let score = 0;
      if (version.isDelayed) score += 100;
      if (version.endDate && dayjs().isAfter(dayjs(version.endDate)) && version.status !== 2) score += 60;
      
      if (version.launchTime) {
        const daysToLaunch = dayjs(version.launchTime).diff(dayjs(), 'day');
        if (daysToLaunch >= 0 && daysToLaunch <= 7 && version.status !== 2) {
          score += (8 - daysToLaunch) * 5;
        }
      }
      
      if ((version.bugCount || 0) > 0) score += Math.min(40, version.bugCount * 5);
      
      return score;
    },

    getVersionRiskLevel(version) {
      const score = this.getVersionRiskScore(version);
      if (score >= 100) return { label: '🚨 危险', type: 'danger' };
      if (score >= 50) return { label: '⚠ 警示', type: 'warning' };
      if (score > 0) return { label: '• 提醒', type: 'info' };
      return { label: '✓ 良好', type: 'success' };
    },

    // ---------- 人员分配 ----------
    isAdmin,
    async viewAssignments(version, project) {
      this.currentVersion = { ...version };
      this.currentProject = { ...project };
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
        this.roles = r.data || [];
      } catch (e) {
        console.error('获取用户或角色列表失败:', e);
      }
    },
    showAddAssignment() {
      this.currentAssignment = { id: null, projectVersionId: this.currentVersion.id, roleId: null, userId: null, feature: '' };
      this.assignmentDialogTitle = '新增分配';
      this.assignmentEditDialogVisible = true;
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
    copyAssignmentNotice() {
      if (this.assignments.length === 0) {
        this.$message.warning('暂无分配数据可复制');
        return;
      }
      const lines = [];
      lines.push(`📢 【${this.currentProject.projectName} - ${this.currentVersion.versionNumber}】任务分配通知`);
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
    }
  }
};
</script>

<style scoped>
.gantt-container {
  padding: 20px;
  background: var(--bg-secondary);
  min-height: calc(100vh - 60px);
}

.header-card {
  margin-bottom: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: var(--text-primary);
}

.toolbar {
  display: flex;
  align-items: center;
}

.gantt-main-card {
  overflow: hidden;
}

.empty-state {
  padding: 60px 0;
}

/* 甘特图布局 */
.gantt-wrapper {
  display: flex;
  height: calc(100vh - 280px);
  min-height: 500px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  overflow: hidden;
}

/* 左侧面板 */
.gantt-left-panel {
  width: 350px;
  min-width: 350px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.task-list-header {
  display: flex;
  padding: 12px 16px;
  background: var(--bg-dark);
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
}

.col-project {
  flex: 2;
}

.col-status {
  flex: 1;
  text-align: center;
}

.col-manager {
  flex: 1;
  text-align: center;
}

.task-list-body {
  flex: 1;
  overflow-y: auto;
}

.task-group {
  border-bottom: 1px solid var(--border-color);
}

.group-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  background: var(--bg-secondary);
  transition: background 0.2s;
}

.group-header:hover {
  background: var(--bg-hover);
}

.expand-icon {
  margin-right: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.group-name {
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
}

.version-list {
  background: var(--bg-primary);
}

.version-item {
  display: flex;
  align-items: center;
  padding: 8px 16px 8px 36px;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.2s;
}

.version-item:hover {
  background: var(--bg-hover);
}

.version-item.active {
  background: rgba(64, 158, 255, 0.1);
  border-left: 3px solid #409eff;
}

.version-info {
  flex: 2;
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-number {
  font-size: 13px;
  color: var(--text-primary);
}

.version-status {
  flex: 1;
  text-align: center;
}

.version-manager {
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 右侧面板 */
.gantt-right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-primary);
}

.timeline-header {
  background: var(--bg-dark);
  border-bottom: 1px solid var(--border-color);
  overflow: hidden;
  flex-shrink: 0;
}

.timeline-header-inner {
  display: flex;
}

.month-cell {
  border-right: 1px solid var(--border-color);
}

.month-label {
  padding: 8px;
  text-align: center;
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.days-row {
  display: flex;
}

.day-cell {
  width: 30px;
  min-width: 30px;
  padding: 4px 0;
  text-align: center;
  font-size: 11px;
  color: var(--text-secondary);
  border-right: 1px solid var(--border-light);
}

.day-cell.weekend {
  background: rgba(0, 0, 0, 0.05);
}

.day-cell.today {
  background: rgba(64, 158, 255, 0.2);
  font-weight: 600;
  color: #409eff;
}

.timeline-body {
  flex: 1;
  overflow: auto;
  position: relative;
}

.timeline-body-inner {
  position: relative;
}

.timeline-group {
  border-bottom: 1px solid var(--border-color);
}

.group-row {
  height: 40px;
  background: var(--bg-secondary);
}

.timeline-version-list {
  background: var(--bg-primary);
}

.timeline-version-item {
  position: relative;
  height: 40px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  padding: 0;
}

.timeline-version-item.active {
  background: rgba(64, 158, 255, 0.05);
}

/* 甘特图条形 */
.gantt-bar {
  position: absolute;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}

.gantt-bar:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 10;
  cursor: pointer;
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.assignment-title {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
}

.bar-text {
  font-size: 12px;
  color: white;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 8px;
}

.overdue-indicator {
  margin-left: 4px;
  font-size: 14px;
}

/* 条形颜色 */
.bar-not-started {
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
}

.bar-in-progress {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.bar-completed {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.bar-warning {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.bar-danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.bar-default {
  background: linear-gradient(135deg, #dcdfe6 0%, #e4e7ed 100%);
}

/* 时间节点标记 */
.gantt-marker {
  position: absolute;
  top: 2px;
  bottom: 2px;
  width: 3px;
  z-index: 6;
  border-radius: 2px;
  pointer-events: none;
}

.marker-test {
  background: #fbbf24;
  box-shadow: 0 0 6px rgba(251, 191, 36, 0.8);
}

.marker-launch {
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.8);
}

.marker-flag {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  font-weight: 700;
  color: #1e293b;
  width: 16px;
  height: 14px;
  line-height: 14px;
  text-align: center;
  border-radius: 3px;
  white-space: nowrap;
}

.marker-test .marker-flag {
  background: #fbbf24;
}

.marker-launch .marker-flag {
  background: #34d399;
}

/* 图例 */
.legend-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0 0;
  margin-top: 10px;
  border-top: 1px solid var(--glass-border);
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-dot {
  display: inline-block;
  width: 14px;
  height: 10px;
  border-radius: 3px;
}

.dot-not-started { background: linear-gradient(135deg, #909399, #a6a9ad); }
.dot-in-progress { background: linear-gradient(135deg, #409eff, #66b1ff); }
.dot-completed { background: linear-gradient(135deg, #67c23a, #85ce61); }
.dot-danger { background: linear-gradient(135deg, #f56c6c, #f78989); }
.dot-warning { background: linear-gradient(135deg, #e6a23c, #ebb563); }

.legend-marker {
  display: inline-block;
  width: 3px;
  height: 14px;
  border-radius: 2px;
}

.marker-test-legend { background: #fbbf24; box-shadow: 0 0 4px rgba(251, 191, 36, 0.8); }
.marker-launch-legend { background: #34d399; box-shadow: 0 0 4px rgba(52, 211, 153, 0.8); }

.no-date-hint {
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0.6;
}

/* 滚动条样式 */
.task-list-body::-webkit-scrollbar,
.timeline-body::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.task-list-body::-webkit-scrollbar-thumb,
.timeline-body::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 4px;
}

.task-list-body::-webkit-scrollbar-track,
.timeline-body::-webkit-scrollbar-track {
  background: var(--scrollbar-track);
}

/* 分配弹窗样式 */
.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}

.assignment-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
  margin: 0;
}

.assignment-title::before {
  content: "●";
  color: var(--accent-cyan);
  margin-right: 8px;
  font-size: 12px;
}

.add-btn {
  margin-left: 8px;
}
</style>

<style>
/* 自定义提示框 */
.custom-tooltip {
  position: fixed;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.96);
  border: 1px solid rgba(99, 122, 161, 0.22);
  border-radius: 8px;
  padding: 12px;
  min-width: 280px;
  max-width: 400px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(20px);
  pointer-events: none;
}

.tooltip-content h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #e2e8f0;
  border-bottom: 2px solid #3b82f6;
  padding-bottom: 8px;
}

.tooltip-content p {
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.6;
  color: #94a3b8;
}

.tooltip-content p strong {
  color: #e2e8f0;
  margin-right: 8px;
}
</style>
