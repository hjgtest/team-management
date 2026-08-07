<template>
  <div class="projects-container">
    <!-- 顶部搜索栏 + 新增按钮 -->
    <div class="search-bar">
      <el-input
        v-model="searchProjectName"
        placeholder="请输入项目名称"
        clearable
        @input="handleSearchInput"
        @clear="resetSearch"
        class="search-input"
      ></el-input>
      <el-select
        v-model="searchProjectStatus"
        placeholder="全部状态"
        clearable
        @change="handleStatusFilter"
        class="search-status"
      >
        <el-option label="未开始" :value="0" />
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
      <el-button v-if="isAdmin()" type="primary" @click="showAddProject" class="add-btn">新增项目</el-button>
      <el-button type="success" :loading="wechatNoticeLoading" @click="generateWechatNotice" class="add-btn">📱 生成微信通知</el-button>
      <el-button type="warning" @click="openWeeklyReport" class="add-btn">💼 领导周报</el-button>
      <el-button type="info" @click="openExportExcelDialog" class="add-btn">📥 导出周报Excel</el-button>
    </div>

    <!-- 智能 KPI 概览（可点击作为状态过滤快捷键） -->
    <div class="kpi-row">
      <div class="kpi-card kpi-total" :class="{active: searchProjectStatus===''}" @click="setStatusFilter('')">
        <div class="kpi-icon">📊</div>
        <div class="kpi-content">
          <div class="kpi-label">项目总数</div>
          <div class="kpi-value">{{ projects.length }}</div>
        </div>
      </div>
      <div class="kpi-card kpi-running" :class="{active: searchProjectStatus===1}" @click="setStatusFilter(1)">
        <div class="kpi-icon">🚀</div>
        <div class="kpi-content">
          <div class="kpi-label">进行中</div>
          <div class="kpi-value">{{ projectStats.running }}</div>
        </div>
      </div>
      <div class="kpi-card kpi-pending" :class="{active: searchProjectStatus===0}" @click="setStatusFilter(0)">
        <div class="kpi-icon">⏳</div>
        <div class="kpi-content">
          <div class="kpi-label">未开始</div>
          <div class="kpi-value">{{ projectStats.pending }}</div>
        </div>
      </div>
      <div class="kpi-card kpi-done" :class="{active: searchProjectStatus===2}" @click="setStatusFilter(2)">
        <div class="kpi-icon">✅</div>
        <div class="kpi-content">
          <div class="kpi-label">已结束</div>
          <div class="kpi-value">{{ projectStats.done }}</div>
        </div>
      </div>
    </div>

    <!-- 项目管理卡片 -->
    <el-card class="card-section main-card">
      <div class="card-header">
        <h3 class="title">项目管理</h3>
        <div class="stats">
          <el-tag v-if="filteredProjects.length !== projects.length" type="warning" effect="light">
            筛选: {{ filteredProjects.length }} / {{ projects.length }}
          </el-tag>
          <el-tag v-else type="info" effect="light">总计: {{ projects.length }} 个</el-tag>
        </div>
      </div>

      <div v-if="filteredProjects.length === 0 && !loading" class="empty-state">
        <el-empty :description="projects.length === 0 ? '暂无项目数据' : '当前筛选条件下无数据'" :image-size="120">
          <el-button v-if="isAdmin()" type="primary" @click="showAddProject">添加首个项目</el-button>
          <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
        </el-empty>
      </div>

      <el-table
        v-loading="loading"
        :data="paginatedProjects"
        border
        stripe
        style="width: 100%; margin-top: 12px"
        :header-cell-style="{ background: '#0f172a' }"
        v-else
      >
        <el-table-column prop="projectName" label="项目名称" min-width="200"></el-table-column>
        <el-table-column prop="description" label="项目描述" min-width="300"></el-table-column>
        <el-table-column prop="status" label="项目状态" min-width="100">
          <template #default="scope">
            <el-tag :type="getProjectStatusTag(scope.row)">{{ getProjectStatusText(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240">
          <template #default="scope">
            <el-button v-if="isAdmin()" size="small" type="primary" @click="editProject(scope.row)">编辑</el-button>
            <el-button v-if="isAdmin()" size="small" type="danger" @click="deleteProject(scope.row.id)">删除</el-button>
            <el-button size="small" type="primary" @click="viewVersions(scope.row)">版本管理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div v-if="filteredProjects.length > 0" class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredProjects.length"
          background
        ></el-pagination>
      </div>
    </el-card>

    <!-- 项目新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" @close="onDialogClose" center :close-on-click-modal="false">
      <el-form ref="projectForm" :model="currentProject" :rules="projectRules" label-width="80px" label-position="right" class="project-form">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="currentProject.name" placeholder="请输入项目名称"></el-input>
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="currentProject.description" type="textarea" placeholder="请输入项目描述" rows="4"></el-input>
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProject">确定</el-button>
      </div>
    </el-dialog>

    <!-- 版本管理弹窗 -->
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
          <el-button v-if="isAdmin()" type="primary" @click="showAddVersion">添加首个版本</el-button>
          <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
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
            <el-button v-if="isAdmin()" size="small" type="primary" @click="editVersion(scope.row)">编辑</el-button>
            <el-button v-if="isAdmin()" size="small" @click="copyVersion(scope.row)">复制</el-button>
            <el-button v-if="isAdmin()" size="small" type="danger" @click="deleteVersion(scope.row.id)">删除</el-button>
            <el-button v-if="isAdmin()" size="small" type="primary" @click="viewAssignments(scope.row)">分配</el-button>
            <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 版本分页 -->
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

    <!-- 版本新增/编辑弹窗（带提测/上线/产品/原型/描述） -->
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
        <el-divider content-position="left"> Bug统计详情</el-divider>
        
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

        <!-- 激活次数统计 -->
        <el-form-item label="激活次数">
          <div class="bug-activated-container">
            <div v-for="(group, gIndex) in currentVersion.bugActivatedGroups" :key="'group-'+gIndex" class="bug-activated-group">
              <div class="group-header">
                <el-input-number v-model="group.times" :min="1" :max="99" placeholder="次数" style="width: 100px; margin-right: 8px" />
                <span class="times-label">次</span>
                <el-button type="danger" icon="Delete" size="small" @click="removeActivatedGroup(gIndex)"></el-button>
              </div>
              <div class="group-items">
                <div v-for="(item, iIndex) in group.items" :key="'item-'+iIndex" class="bug-stat-row">
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
                  <el-button type="danger" icon="Delete" size="small" @click="removeActivatedItem(gIndex, iIndex)"></el-button>
                </div>
                <div class="bug-actions">
                  <el-button type="primary" size="small" icon="Plus" @click="addActivatedItem(gIndex)">添加人员</el-button>
                </div>
              </div>
            </div>
            <el-button type="success" size="small" icon="Plus" @click="addActivatedGroup">添加激活次数</el-button>
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


    <!-- 领导周报（按工时×1000元/人天自动计算） -->
    <el-dialog title="💼 领导周报 - 工时费用统计" v-model="weeklyReportVisible" width="800px" center :close-on-click-modal="false">
      <div class="weekly-toolbar">
        <span class="weekly-label">报表区间：</span>
        <el-date-picker
          v-model="weeklyReportRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />
        <el-input-number v-model="weeklyReportRate" :min="0" :step="100" controls-position="right" style="width:140px" />
        <span class="weekly-label">元/人天</span>
        <el-button type="primary" :loading="weeklyReportLoading" @click="computeWeeklyReport">生成</el-button>
        <el-button v-if="weeklyReportText" type="success" @click="copyWeeklyReport">📋 复制报表</el-button>
      </div>
      <div class="weekly-toolbar">
        <span class="weekly-label">统计维度：</span>
        <el-radio-group v-model="weeklyReportDimension" size="small">
          <el-radio-button label="user">👤 按人</el-radio-button>
          <el-radio-button label="project">📁 按项目</el-radio-button>
        </el-radio-group>
        <span class="weekly-label" style="margin-left:18px">费用计算：</span>
        <el-radio-group v-model="weeklyReportCostMode" size="small">
          <el-radio-button label="range">📅 仅区间内</el-radio-button>
          <el-radio-button label="total">💰 版本完整周期</el-radio-button>
        </el-radio-group>
      </div>
      <el-input
        v-if="weeklyReportText"
        v-model="weeklyReportText"
        type="textarea"
        :rows="18"
        readonly
        class="weekly-text"
      />
      <div v-else class="weekly-empty">
        <el-empty description="选择区间后点「生成」，系统会按「项目版本研发周期×分配人员×1000」自动统计。" :image-size="100" />
      </div>
    </el-dialog>

    <!-- 导出周报 Excel 弹窗 -->
    <el-dialog title="📥 导出周报 Excel" v-model="exportExcelDialogVisible" width="560px" center :close-on-click-modal="false">
      <div class="weekly-toolbar">
        <span class="weekly-label">本周开始日期：</span>
        <el-date-picker
          v-model="exportWeekStart"
          type="date"
          placeholder="选择本周一日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 220px"
        />
      </div>
      <el-alert
        :closable="false"
        type="info"
        title="导出说明"
        description="以所选日期为本周一，自动按 [本周, 本周+6天] 与 [下周一, 下周日] 两段区间筛选每个人的研发任务，生成姓名/项目/开发内容/上线时间/开发进度/卡点（卡点列留空）的 Excel 文件。"
        style="margin: 10px 0"
      />
      <div class="dialog-footer">
        <el-button @click="exportExcelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exportExcelLoading" @click="exportWeeklyExcel">导出</el-button>
      </div>
    </el-dialog>

    <!-- 分配管理弹窗 -->
    <el-dialog title="人员分配管理" v-model="assignmentDialogVisible" width="900px" center :close-on-click-modal="false">
      <div class="assignment-header">
        <h4 class="title">{{ currentProject.name }} - {{ currentVersion.versionNumber }}</h4>
        <div>
          <el-button v-if="assignments.length > 0" @click="copyAssignmentNotice" class="add-btn">📋 复制通知文案</el-button>
          <el-button v-if="isAdmin()" type="success" @click="openBatchDialog" class="add-btn">⚡ 批量智能分配</el-button>
          <el-button v-if="isAdmin()" type="primary" @click="showAddAssignment" class="add-btn">新增分配</el-button>
        </div>
      </div>

      <div v-if="assignments.length === 0 && !assignmentLoading" class="empty-state">
        <el-empty description="暂无分配数据" :image-size="100">
          <el-button v-if="isAdmin()" type="primary" @click="openBatchDialog">批量智能分配</el-button>
          <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
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
        <span v-if="!isAdmin()" style="font-size:12px;color:var(--text-muted)">只读</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 单条分配新增弹窗：角色 → 推荐人员 → 功能 -->
    <el-dialog :title="assignmentDialogTitle" v-model="assignmentEditDialogVisible" width="560px" center :close-on-click-modal="false">
      <el-form ref="assignmentForm" :model="currentAssignment" :rules="assignmentRules" label-width="90px" label-position="right" class="assignment-form">
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="currentAssignment.roleId" placeholder="请选择角色" style="width: 100%" @change="onRoleChangeForSingle">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="人员" prop="userId">
          <el-select
            v-model="currentAssignment.userId"
            placeholder="请选择人员（按推荐度排序）"
            style="width: 100%"
            filterable
            :loading="singleRecommendLoading"
          >
            <el-option
              v-for="u in singleRecommendList"
              :key="u.userId"
              :label="formatUserOption(u)"
              :value="u.userId"
            >
              <span style="float:left">{{ u.username }}</span>
              <span style="float:right; font-size:12px; opacity:.7">
                {{ u.status === 'released' ? '已释放' : (u.daysLeft + '天后释放') }} · 任务 {{ u.ongoingCount }}
                <span v-if="u.hasHistory" style="color: var(--accent-cyan); margin-left:6px">★ 做过本项目</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="负责功能" prop="feature">
          <el-input v-model="currentAssignment.feature" type="textarea" placeholder="请输入负责功能" rows="3"></el-input>
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="assignmentEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssignment">确定</el-button>
      </div>
    </el-dialog>

    <!-- 批量智能分配弹窗 -->
    <el-dialog title="⚡ 批量智能分配" v-model="batchDialogVisible" width="900px" center :close-on-click-modal="false">
      <el-alert
        title="操作说明"
        type="info"
        description="为每个功能模块选择需要的角色（可多选），点击「一键智能推荐」，系统会按「做过本项目 → 已释放 → 任务最少 → 最快释放」的优先级自动选人；不满意可手动微调。"
        :closable="false"
        style="margin-bottom: 14px"
      />

      <div class="batch-toolbar">
        <el-button v-if="isAdmin()" type="primary" @click="addBatchRow">+ 添加功能模块</el-button>
        <el-button v-if="isAdmin()" type="success" :loading="batchRecommending" @click="smartAssignAll">⚡ 一键智能推荐</el-button>
        <el-button v-if="isAdmin()" :disabled="totalBatchAssignments === 0" @click="rebalanceBatch">⚖ 平衡负载</el-button>
        <span class="batch-tip">共 {{ batchRows.length }} 个模块 / {{ totalBatchAssignments }} 条分配</span>
      </div>

      <div class="batch-list">
        <div v-for="(row, idx) in batchRows" :key="idx" class="batch-row">
          <div class="batch-row-head">
            <span class="batch-idx">#{{ idx + 1 }}</span>
            <el-input v-model="row.feature" placeholder="功能模块（如：用户登录、订单结算）" class="batch-feature" />
            <el-select
              v-model="row.roleIds"
              multiple
              collapse-tags
              placeholder="选择角色（可多选）"
              class="batch-roles"
              @change="onBatchRolesChange(row)"
            >
              <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
            </el-select>
            <el-button v-if="isAdmin()" type="danger" link @click="removeBatchRow(idx)">删除</el-button>
          </div>

          <div v-if="row.picks && row.picks.length > 0" class="batch-picks">
            <div v-for="pick in row.picks" :key="pick.roleId" class="batch-pick">
              <el-tag size="small" type="info">{{ getRoleName(pick.roleId) }}</el-tag>
              <el-select
                v-model="pick.userId"
                placeholder="待推荐"
                filterable
                :loading="pick.loading"
                class="batch-user-select"
              >
                <el-option
                  v-for="u in pick.candidates"
                  :key="u.userId"
                  :label="formatUserOption(u)"
                  :value="u.userId"
                >
                  <span style="float:left">{{ u.username }}</span>
                  <span style="float:right; font-size:12px; opacity:.7">
                    {{ getStatusText(u) }} · 任务 {{ u.ongoingCount }}
                    <span v-if="u.hasHistory" style="color: var(--accent-cyan); margin-left:6px">★</span>
                    <span v-if="u.hasConflictInVersionTime" style="color: #F56C6B; margin-left:6px">⚠ 冲突</span>
                  </span>
                </el-option>
              </el-select>
              <span class="batch-reason" v-if="pick.reason">{{ pick.reason }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button v-if="isAdmin()" type="primary" :loading="batchSubmitting" @click="submitBatch">提交分配（{{ totalBatchAssignments }} 条）</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { isAdmin } from '@/utils/auth';
import { getProjects, addProject, updateProject, deleteProject } from '@/api/projects';
import { getProjectVersions, addProjectVersion, updateProjectVersion, deleteProjectVersion } from '@/api/projectVersions';
import { getProjectAssignments, addProjectAssignment, deleteProjectAssignment, batchAddProjectAssignments } from '@/api/projectAssignments';
import { getRecommend } from '@/api/released';
import { getUsers } from '@/api/users';
import { getRoles } from '@/api/roles';
import { debounce } from 'lodash';
import * as XLSX from 'xlsx';

export default {
  name: 'ProjectsView',
  data() {
    return {
      loading: false,
      // 项目分页
      currentPage: 1,
      pageSize: 10,
      // 项目状态筛选
      searchProjectStatus: '',
      // 项目
      dialogVisible: false,
      dialogTitle: '',
      currentProject: { id: null, name: '', description: '' },
      projects: [],
      projectRules: { name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }] },
      searchProjectName: '',

      // 版本
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

      // 分配
      assignmentDialogVisible: false,
      assignmentLoading: false,
      assignments: [],
      currentAssignment: { id: null, projectVersionId: null, roleId: null, userId: null, feature: '' },
      assignmentEditDialogVisible: false,
      assignmentDialogTitle: '',
      assignmentRules: {
        roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
        userId: [{ required: true, message: '请选择人员', trigger: 'change' }],
        feature: [{ required: true, message: '请输入负责功能', trigger: 'blur' }]
      },
      // 单条分配的推荐人员
      singleRecommendList: [],
      singleRecommendLoading: false,

      // 批量智能分配
      batchDialogVisible: false,
      batchRows: [],
      batchRecommending: false,
      batchSubmitting: false,

      // 微信通知生成
      wechatNoticeLoading: false,

      // 领导周报
      weeklyReportVisible: false,
      weeklyReportLoading: false,
      weeklyReportRange: [],
      weeklyReportRate: 1000,
      weeklyReportText: '',
      weeklyReportDimension: 'user',
      weeklyReportCostMode: 'range',

      // 导出周报 Excel
      exportExcelDialogVisible: false,
      exportExcelLoading: false,
      exportWeekStart: '',

      users: [],
      roles: []
    };
  },
  computed: {
    projectStats() {
      const stats = { pending: 0, running: 0, done: 0 };
      this.projects.forEach(p => {
        if (p.status === 0) stats.pending++;
        else if (p.status === 1) stats.running++;
        else if (p.status === 2) stats.done++;
      });
      return stats;
    },
    filteredProjects() {
      if (this.searchProjectStatus === '' || this.searchProjectStatus === null || this.searchProjectStatus === undefined) {
        return this.projects;
      }
      const status = Number(this.searchProjectStatus);
      return this.projects.filter(p => p.status === status);
    },
    paginatedProjects() {
      const start = (this.currentPage - 1) * this.pageSize;
      return this.filteredProjects.slice(start, start + this.pageSize);
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
    this.fetchProjects();
  },
  mounted() {
    window.addEventListener('error', (e) => {
      if (e.message === 'ResizeObserver loop limit exceeded') e.stopImmediatePropagation();
    });
  },
  methods: {
    isAdmin() { return isAdmin() },
    emptyVersion() {
      return {
        id: null,
        projectId: null,
        versionNumber: '',
        startDate: '',
        endDate: '',
        testTime: '',
        launchTime: '',
        productManager: '',
        prototypeUrl: '',
        description: '',
        bugCount: 0,
        bugResolvedList: [],
        bugActivatedGroups: [],
        isDelayed: false,
        delayedEndTime: ''
      };
    },

    // ---------- 项目 ----------
    handleSearchInput: debounce(function () {
      this.currentPage = 1;
      this.fetchProjects();
    }, 300),
    handleStatusFilter() {
      this.currentPage = 1;
    },
    handleVersionSearchInput: debounce(function () {
      this.versionCurrentPage = 1;
      this.fetchVersions();
    }, 300),
    handleVersionSearchClear() {
      this.versionCurrentPage = 1;
      this.fetchVersions();
    },
    setStatusFilter(s) {
      this.searchProjectStatus = s;
      this.currentPage = 1;
    },
    resetSearch() {
      this.searchProjectName = '';
      this.searchProjectStatus = '';
      this.currentPage = 1;
      this.fetchProjects();
    },
    handleSizeChange(size) { this.pageSize = size; this.currentPage = 1; },
    handleCurrentChange(page) { this.currentPage = page; },
    handleVersionSizeChange(size) { this.versionPageSize = size; this.versionCurrentPage = 1; },
    handleVersionCurrentChange(page) { this.versionCurrentPage = page; },

    async fetchProjects() {
      this.loading = true;
      try {
        const res = await getProjects(this.searchProjectName);
        this.projects = res.data || [];
        const maxPage = Math.max(1, Math.ceil(this.projects.length / this.pageSize));
        if (this.currentPage > maxPage) this.currentPage = maxPage;
      } catch (e) {
        console.error('获取项目列表失败:', e);
      } finally {
        this.loading = false;
      }
    },
    showAddProject() {
      this.currentProject = { id: null, name: '', description: '' };
      this.dialogTitle = '添加项目';
      this.dialogVisible = true;
    },
    editProject(project) {
      this.currentProject = { id: project.id, name: project.projectName, description: project.description };
      this.dialogTitle = '编辑项目';
      this.dialogVisible = true;
      this.$nextTick(() => { if (this.$refs.projectForm) this.$refs.projectForm.clearValidate(); });
    },
    async saveProject() {
      this.$refs.projectForm.validate(async (valid) => {
        if (!valid) return;
        try {
          const data = { ...this.currentProject, projectName: this.currentProject.name };
          if (this.currentProject.id) {
            await updateProject(this.currentProject.id, data);
            this.$message.success('更新成功');
          } else {
            await addProject(data);
            this.$message.success('添加成功');
          }
          this.dialogVisible = false;
          this.fetchProjects();
        } catch (e) {
          console.error('保存项目失败:', e);
          this.$message.error('保存失败');
        }
      });
    },
    async deleteProject(id) {
      try {
        await this.$confirm('确认删除该项目?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' });
        await deleteProject(id);
        this.$message.success('删除成功');
        this.fetchProjects();
      } catch (e) {
        if (e !== 'cancel') { console.error(e); this.$message.error('删除失败'); }
      }
    },

    // ---------- 版本 ----------
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
    showAddVersion() {
      this.currentVersion = this.emptyVersion();
      this.currentVersion.projectId = this.currentProject.id;
      this.versionDialogTitle = '添加版本';
      if (this.users.length === 0) {
        this.fetchUsersAndRoles();
      }
      this.versionEditDialogVisible = true;
    },
    editVersion(version) {
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
        bugActivatedGroups: version.bugActivatedGroups ? JSON.parse(version.bugActivatedGroups) : [],
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
            bugActivatedGroups: JSON.stringify(this.currentVersion.bugActivatedGroups || []),
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

    // ---------- 分配 ----------
    async viewAssignments(version) {
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
          roleName: a.roleName || this.getRoleName(a.roleId) || '未知角色',
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
      // 同步 picks：保留已选 roleId，新增的初始化空 pick
      const oldMap = new Map((row.picks || []).map(p => [p.roleId, p]));
      row.picks = row.roleIds.map(rid => oldMap.get(rid) || { roleId: rid, userId: null, candidates: [], loading: false, reason: '' });
    },
    async smartAssignAll() {
      // 校验
      const valid = this.batchRows.filter(r => r.feature && r.feature.trim() && r.roleIds.length > 0);
      if (valid.length === 0) {
        this.$message.warning('请至少填写一个功能模块并选择角色');
        return;
      }
      this.batchRecommending = true;
      try {
        // 收集所有 (row, roleId) 对，并发请求 recommend
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
      if (u.hasConflictInVersionTime) {
        parts.push('⚠️ 版本时间范围内有冲突');
      }
      if (u.status === 'released') parts.push('当前空闲');
      else if (u.status === 'available') parts.push('当前忙但版本时间内可用');
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
      // 重复检测：与当前 versionId 已有分配对比 (userId | roleId | feature)
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

    // ---------- 工具 ----------
    onVersionDialogClose() {
      this.assignmentDialogVisible = false;
      this.versionEditDialogVisible = false;
      this.assignmentEditDialogVisible = false;
      this.batchDialogVisible = false;
    },
    onDialogClose() { if (this.$refs.projectForm) this.$refs.projectForm.clearValidate(); },
    getRoleName(roleId) {
      const r = this.roles.find(x => String(x.id) === String(roleId));
      return r ? r.name : '';
    },
    formatUserOption(u) {
      const statusText = this.getStatusText(u);
      const conflictTag = u.hasConflictInVersionTime ? ' ⚠️冲突' : '';
      return `${u.username} 【${statusText}】${conflictTag}`;
    },
    getStatusText(u) {
      if (u.hasConflictInVersionTime) return '有冲突';
      if (u.status === 'released') return '已释放';
      if (u.status === 'available') return '可用';
      if (u.status === 'upcoming') return `${u.daysLeft}天后释放`;
      if (u.status === 'busy') return '忙碌';
      return '未知';
    },
    getProjectStatusText(p) { return ({0:'未开始',1:'进行中',2:'已结束'})[p.status] || '未知'; },
    getProjectStatusTag(p) { return ({0:'warning',1:'success',2:'info'})[p.status] || 'info'; },
    getVersionStatusText(v) { return ({0:'未开始',1:'进行中',2:'已结束'})[v.status] || '未知'; },
    getVersionStatusTag(v) { return ({0:'warning',1:'success',2:'info'})[v.status] || 'info'; },
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

    // ---------- 智能计算 ----------
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

    // ---------- 复制版本 ----------
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
        bugActivatedGroups: [],
        isDelayed: false,
        delayedEndTime: ''
      };
      this.versionDialogTitle = '复制版本';
      this.versionEditDialogVisible = true;
      this.$nextTick(() => { if (this.$refs.versionForm) this.$refs.versionForm.clearValidate(); });
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

    // ---------- 激活次数相关方法 ----------
    addActivatedGroup() {
      this.currentVersion.bugActivatedGroups.push({ times: 1, items: [] });
    },
    removeActivatedGroup(gIndex) {
      this.currentVersion.bugActivatedGroups.splice(gIndex, 1);
    },
    addActivatedItem(gIndex) {
      const group = this.currentVersion.bugActivatedGroups[gIndex];
      if (!group.items) {
        group.items = [];
      }
      group.items.push({ userId: null, levels: [] });
    },
    removeActivatedItem(gIndex, iIndex) {
      const group = this.currentVersion.bugActivatedGroups[gIndex];
      if (group && group.items) {
        group.items.splice(iIndex, 1);
      }
    },


    // ---------- 复制通知文案 ----------
    async copyAssignmentNotice() {
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
        await navigator.clipboard.writeText(text);
        this.$message.success('已复制到剪贴板，可粘贴到企微/钉钉');
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

    // ---------- 一键平衡负载 ----------
    rebalanceBatch() {
      const used = new Set();
      let swapped = 0;
      this.batchRows.forEach(row => {
        (row.picks || []).forEach(pick => {
          if (!pick.candidates || pick.candidates.length === 0) return;
          const original = pick.candidates[0];
          let chosen = pick.candidates.find(c => !used.has(c.userId));
          if (!chosen) chosen = original;
          if (chosen.userId !== pick.userId) swapped++;
          pick.userId = chosen.userId;
          const baseReason = this.buildReason(chosen);
          pick.reason = chosen.userId !== original.userId ? baseReason + ' · 已避免重复' : baseReason;
          used.add(chosen.userId);
        });
      });
      this.$message.success(swapped > 0 ? `已平衡，调整了 ${swapped} 位人选` : '当前分配已是最优，无需调整');
    },

    // ---------- 金额格式化 ----------
    formatMoney(v) {
      const n = parseFloat(v) || 0;
      return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },

    // ---------- 全局生成微信通知（跨项目） ----------
    async generateWechatNotice() {
      if (this.projects.length === 0) {
        this.$message.warning('暂无项目数据');
        return;
      }
      this.wechatNoticeLoading = true;
      try {
        // 1. 加载用户/角色字典
        const [usersRes, rolesRes] = await Promise.all([getUsers(), getRoles()]);
        const userMap = {};
        (usersRes.data || []).forEach(u => { userMap[u.id] = u.username; });
        const roleMap = {};
        (rolesRes.data || []).forEach(r => { roleMap[r.id] = r.roleName || r.name; });

        // 2. 并发拉取每个项目下的版本
        const projectVersions = await Promise.all(
          this.projects.map(p =>
            getProjectVersions(p.id)
              .then(r => {
                const arr = r && r.data ? (Array.isArray(r.data) ? r.data : [r.data]) : [];
                return { project: p, versions: arr };
              })
              .catch(() => ({ project: p, versions: [] }))
          )
        );

        // 3. 只保留「进行中」状态的版本
        const inProgressList = [];
        projectVersions.forEach(({ project, versions }) => {
          versions.forEach(v => {
            if (v && v.status === 1) inProgressList.push({ project, version: v });
          });
        });
        if (inProgressList.length === 0) {
          this.$message.warning('暂无「进行中」的版本');
          return;
        }

        // 4. 并发拉取每个进行中版本的分配
        const withAssign = await Promise.all(
          inProgressList.map(item =>
            getProjectAssignments(item.version.id)
              .then(r => ({ ...item, assignments: r.data || [] }))
              .catch(() => ({ ...item, assignments: [] }))
          )
        );

        // 5. 拼接文本
        const lines = [];
        const today = this.formatDate(new Date());
        lines.push(`📢 进行中项目任务通知（${today}）`);
        lines.push('═'.repeat(20));

        let validCount = 0;
        withAssign.forEach(({ project, version, assignments }) => {
          if (!assignments || assignments.length === 0) return;
          validCount++;
          lines.push('');
          lines.push(`【${project.projectName} - ${version.versionName}】`);
          if (version.startTime || version.endTime) {
            lines.push(`📅 研发：${this.formatDate(version.startTime)} ~ ${this.formatDate(version.endTime)}`);
          }
          if (version.testTime) lines.push(`🔬 提测：${this.formatDate(version.testTime)}`);
          if (version.launchTime) lines.push(`🚀 上线：${this.formatDate(version.launchTime)}`);
          if (version.productManager) lines.push(`👔 产品：${version.productManager}`);
          if (version.prototypeUrl) lines.push(`🔗 原型：${version.prototypeUrl}`);
          // 按角色分组
          const grouped = {};
          assignments.forEach(a => {
            const role = a.roleName || roleMap[a.roleId] || '未指定';
            if (!grouped[role]) grouped[role] = [];
            grouped[role].push(a);
          });
          Object.keys(grouped).forEach(role => {
            lines.push(`  【${role}】`);
            grouped[role].forEach(a => {
              const uname = a.userName || userMap[a.userId] || '未知人员';
              lines.push(`    • @${uname} → ${a.feature || '-'}`);
            });
          });
        });

        if (validCount === 0) {
          this.$message.warning('暂无「进行中且已分配人员」的版本');
          return;
        }

        lines.push('');
        lines.push('═'.repeat(20));
        lines.push(`📊 共 ${validCount} 个进行中版本`);
        lines.push('请相关同学关注进度。');

        const text = lines.join('\n');
        try {
          await navigator.clipboard.writeText(text);
          this.$message.success(`已生成 ${validCount} 个进行中版本的通知，已复制到剪贴板`);
        } catch (e) {
          const ta = document.createElement('textarea');
          ta.value = text;
          document.body.appendChild(ta);
          ta.select();
          try { document.execCommand('copy'); this.$message.success('已复制到剪贴板'); }
          catch (err) { this.$message.error('复制失败，请手动复制'); }
          document.body.removeChild(ta);
        }
      } catch (e) {
        console.error('生成微信通知失败:', e);
        this.$message.error('生成失败，请重试');
      } finally {
        this.wechatNoticeLoading = false;
      }
    },

    // ---------- 导出周报 Excel（本周/下周分栏） ----------
    openExportExcelDialog() {
      const now = new Date();
      const day = now.getDay() === 0 ? 7 : now.getDay();
      const monday = new Date(now);
      monday.setDate(now.getDate() - day + 1);
      monday.setHours(0, 0, 0, 0);
      const y = monday.getFullYear();
      const m = String(monday.getMonth() + 1).padStart(2, '0');
      const d = String(monday.getDate()).padStart(2, '0');
      this.exportWeekStart = `${y}-${m}-${d}`;
      this.exportExcelDialogVisible = true;
    },

    async exportWeeklyExcel() {
      if (!this.exportWeekStart) {
        this.$message.warning('请选择本周开始日期');
        return;
      }
      if (this.projects.length === 0) {
        this.$message.warning('暂无项目数据');
        return;
      }
      this.exportExcelLoading = true;
      try {
        const weekStart = new Date(this.exportWeekStart + 'T00:00:00');
        const thisWeekStartTs = weekStart.getTime();
        const thisWeekEndTs = thisWeekStartTs + 7 * 86400000 - 1;
        const nextWeekStartTs = thisWeekStartTs + 7 * 86400000;
        const nextWeekEndTs = thisWeekStartTs + 14 * 86400000 - 1;
        const fmtDay = ts => {
          const dt = new Date(ts);
          const yy = dt.getFullYear();
          const mm = String(dt.getMonth() + 1).padStart(2, '0');
          const dd = String(dt.getDate()).padStart(2, '0');
          return `${yy}/${mm}/${dd}`;
        };

        // 加载用户字典
        const usersRes = await getUsers();
        const userMap = {};
        (usersRes.data || []).forEach(u => { userMap[u.id] = u.username; });

        // 拉每个项目的版本
        const projectVersions = await Promise.all(
          this.projects.map(p =>
            getProjectVersions(p.id)
              .then(r => {
                const arr = r && r.data ? (Array.isArray(r.data) ? r.data : [r.data]) : [];
                return { project: p, versions: arr };
              })
              .catch(() => ({ project: p, versions: [] }))
          )
        );

        // 收集与本周/下周有交集的版本（任意一段即可）
        const candidateTasks = [];
        projectVersions.forEach(({ project, versions }) => {
          versions.forEach(v => {
            if (!v.startTime || !v.endTime) return;
            const vS = new Date(v.startTime).getTime();
            const vE = (v.isDelayed && v.delayedEndTime
              ? new Date(v.delayedEndTime).getTime()
              : new Date(v.endTime).getTime());
            const inThisWeek = Math.max(vS, thisWeekStartTs) <= Math.min(vE, thisWeekEndTs);
            const inNextWeek = Math.max(vS, nextWeekStartTs) <= Math.min(vE, nextWeekEndTs);
            if (inThisWeek || inNextWeek) {
              candidateTasks.push({ project, version: v, inThisWeek, inNextWeek });
            }
          });
        });

        if (candidateTasks.length === 0) {
          this.$message.warning('选定区间内没有可统计的版本');
          return;
        }

        // 拉每个候选版本的分配
        const withAssign = await Promise.all(
          candidateTasks.map(t =>
            getProjectAssignments(t.version.id)
              .then(r => ({ ...t, assignments: r.data || [] }))
              .catch(() => ({ ...t, assignments: [] }))
          )
        );

        // 进度文本
        const statusText = v => {
          if (v.status === 2) return '已完成';
          if (v.status === 1) return '开发中';
          if (v.status === 0) return '未开始';
          return '待定';
        };
        // 上线时间显示
        const launchText = v => {
          if (v.launchTime) return fmtDay(new Date(v.launchTime).getTime());
          const now = Date.now();
          if (v.testTime && new Date(v.testTime).getTime() <= now && v.status !== 2) return '上线测试中';
          return '待定';
        };

        // 按用户聚合：thisWeek / nextWeek 两段，同 user+version 合并 features
        const userBucket = {};
        withAssign.forEach(({ project, version, inThisWeek, inNextWeek, assignments }) => {
          (assignments || []).forEach(a => {
            const uid = a.userId;
            if (uid == null) return;
            if (!userBucket[uid]) {
              userBucket[uid] = {
                userName: a.userName || userMap[uid] || '未知',
                thisWeek: new Map(),
                nextWeek: new Map()
              };
            }
            const fillBucket = (bucket) => {
              if (!bucket.has(version.id)) {
                bucket.set(version.id, {
                  projectName: project.projectName,
                  versionName: version.versionName || '',
                  launch: launchText(version),
                  status: statusText(version),
                  features: []
                });
              }
              if (a.feature) bucket.get(version.id).features.push(a.feature);
            };
            if (inThisWeek) fillBucket(userBucket[uid].thisWeek);
            if (inNextWeek) fillBucket(userBucket[uid].nextWeek);
          });
        });

        const userList = Object.values(userBucket).filter(u => u.thisWeek.size > 0 || u.nextWeek.size > 0);
        if (userList.length === 0) {
          this.$message.warning('选定区间内没有任何分配人员');
          return;
        }
        // 按姓名排序
        userList.sort((a, b) => a.userName.localeCompare(b.userName, 'zh'));

        // 构建 aoa：第 1 行【姓名|本周(合并5列)|下周(合并5列)】，第 2 行子表头
        const aoa = [
          ['姓名', '本周', '', '', '', '', '下周', '', '', '', ''],
          ['', '项目名称', '开发内容', '上线时间', '开发进度', '卡点', '项目名称', '开发内容', '上线时间', '开发进度', '卡点']
        ];
        const merges = [
          { s: { r: 0, c: 0 }, e: { r: 1, c: 0 } },
          { s: { r: 0, c: 1 }, e: { r: 0, c: 5 } },
          { s: { r: 0, c: 6 }, e: { r: 0, c: 10 } }
        ];

        let curRow = 2;
        userList.forEach(u => {
          const thisArr = Array.from(u.thisWeek.values());
          const nextArr = Array.from(u.nextWeek.values());
          const rowCount = Math.max(thisArr.length, nextArr.length, 1);
          const startRow = curRow;
          for (let i = 0; i < rowCount; i++) {
            const row = new Array(11).fill('');
            if (i === 0) row[0] = u.userName;
            const t = thisArr[i];
            if (t) {
              row[1] = t.projectName;
              row[2] = t.features.length ? t.features.join('、') : t.versionName;
              row[3] = t.launch;
              row[4] = t.status;
              row[5] = '';
            }
            const n = nextArr[i];
            if (n) {
              row[6] = n.projectName;
              row[7] = n.features.length ? n.features.join('、') : n.versionName;
              row[8] = n.launch;
              row[9] = n.status;
              row[10] = '';
            }
            aoa.push(row);
            curRow++;
          }
          if (rowCount > 1) {
            merges.push({ s: { r: startRow, c: 0 }, e: { r: startRow + rowCount - 1, c: 0 } });
          }
        });

        const ws = XLSX.utils.aoa_to_sheet(aoa);
        ws['!merges'] = merges;
        ws['!cols'] = [
          { wch: 10 }, { wch: 18 }, { wch: 30 }, { wch: 14 }, { wch: 10 }, { wch: 14 },
          { wch: 18 }, { wch: 30 }, { wch: 14 }, { wch: 10 }, { wch: 14 }
        ];
        const wb = XLSX.utils.book_new();
        const sheetName = '周报';
        XLSX.utils.book_append_sheet(wb, ws, sheetName);
        XLSX.writeFile(wb, `开发组周报_${this.exportWeekStart}.xlsx`);

        this.$message.success(`已导出 ${userList.length} 人的周报`);
        this.exportExcelDialogVisible = false;
      } catch (e) {
        console.error('导出周报失败:', e);
        this.$message.error('导出失败：' + (e.message || e));
      } finally {
        this.exportExcelLoading = false;
      }
    },

    // ---------- 领导周报：按「交集天数 × 1000元/人天」自动计算 ----------
    openWeeklyReport() {
      // 默认本周一到本周日
      const now = new Date();
      const day = now.getDay() === 0 ? 7 : now.getDay();
      const monday = new Date(now);
      monday.setDate(now.getDate() - day + 1);
      monday.setHours(0, 0, 0, 0);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);
      const fmt = d => {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const da = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${da}`;
      };
      this.weeklyReportRange = [fmt(monday), fmt(sunday)];
      this.weeklyReportText = '';
      this.weeklyReportVisible = true;
    },

    async computeWeeklyReport() {
      if (!this.weeklyReportRange || this.weeklyReportRange.length !== 2) {
        this.$message.warning('请选择报表区间');
        return;
      }
      const [s, e] = this.weeklyReportRange;
      const rangeStart = new Date(s + 'T00:00:00').getTime();
      const rangeEnd = new Date(e + 'T23:59:59').getTime();
      const rate = parseFloat(this.weeklyReportRate) || 1000;
      const useTotalCycle = this.weeklyReportCostMode === 'total';
      if (this.projects.length === 0) {
        this.$message.warning('暂无项目数据');
        return;
      }

      this.weeklyReportLoading = true;
      try {
        const [usersRes, rolesRes] = await Promise.all([getUsers(), getRoles()]);
        const userMap = {};
        (usersRes.data || []).forEach(u => { userMap[u.id] = u.username; });
        const roleMap = {};
        (rolesRes.data || []).forEach(r => { roleMap[r.id] = r.roleName || r.name; });

        const projectVersions = await Promise.all(
          this.projects.map(p =>
            getProjectVersions(p.id)
              .then(r => {
                const arr = r && r.data ? (Array.isArray(r.data) ? r.data : [r.data]) : [];
                return { project: p, versions: arr };
              })
              .catch(() => ({ project: p, versions: [] }))
          )
        );

        // 仅保留区间内有交集的版本；days 根据费用模式计算
        const tasks = [];
        projectVersions.forEach(({ project, versions }) => {
          versions.forEach(v => {
            if (!v.startTime || !v.endTime) return;
            const vS = new Date(v.startTime).getTime();
            const vE = (v.isDelayed && v.delayedEndTime
              ? new Date(v.delayedEndTime).getTime()
              : new Date(v.endTime).getTime());
            const lo = Math.max(rangeStart, vS);
            const hi = Math.min(rangeEnd, vE);
            if (hi < lo) return;
            const days = useTotalCycle
              ? (Math.floor((vE - vS) / 86400000) + 1)
              : (Math.floor((hi - lo) / 86400000) + 1);
            if (days <= 0) return;
            tasks.push({ project, version: v, days });
          });
        });

        if (tasks.length === 0) {
          this.weeklyReportText = '';
          this.$message.warning('选定区间内没有可统计的版本');
          return;
        }

        const withAssign = await Promise.all(
          tasks.map(t =>
            getProjectAssignments(t.version.id)
              .then(r => ({ ...t, assignments: r.data || [] }))
              .catch(() => ({ ...t, assignments: [] }))
          )
        );

        const lines = [];

        if (this.weeklyReportDimension === 'project') {
          // 按项目维度聚合
          const projectBucket = {};
          withAssign.forEach(({ project, version, days, assignments }) => {
            if (!assignments || assignments.length === 0) return;
            if (!projectBucket[project.id]) {
              projectBucket[project.id] = {
                projectName: project.projectName,
                versions: [],
                memberSet: new Set(),
                totalDays: 0
              };
            }
            // 同一 user 多 feature 视为并行，days 只计一次
            const uvMap = {};
            assignments.forEach(a => {
              if (!uvMap[a.userId]) {
                uvMap[a.userId] = {
                  userId: a.userId,
                  userName: a.userName || userMap[a.userId] || '未知',
                  roles: new Set(),
                  features: [],
                  days
                };
              }
              const r = a.roleName || roleMap[a.roleId] || '';
              if (r) uvMap[a.userId].roles.add(r);
              if (a.feature) uvMap[a.userId].features.push(a.feature);
            });
            const members = Object.values(uvMap);
            members.forEach(m => {
              projectBucket[project.id].memberSet.add(m.userId);
              projectBucket[project.id].totalDays += m.days;
            });
            projectBucket[project.id].versions.push({
              versionName: version.versionName,
              startTime: version.startTime,
              endTime: version.endTime,
              testTime: version.testTime,
              launchTime: version.launchTime,
              productManager: version.productManager,
              members
            });
          });

          if (Object.keys(projectBucket).length === 0) {
            this.weeklyReportText = '';
            this.$message.warning('选定区间内没有人员参与的分配');
            return;
          }

          const sortedProjects = Object.values(projectBucket).sort((a, b) => b.totalDays - a.totalDays);
          let grandDays = 0;
          let grandCost = 0;
          const bodyLines = [];
          sortedProjects.forEach(p => {
            const cost = p.totalDays * rate;
            grandDays += p.totalDays;
            grandCost += cost;
            bodyLines.push('');
            bodyLines.push(`▎ ${p.projectName}   ·   ${p.memberSet.size} 人 / ${p.totalDays} 人天 / ${this.formatMoney(cost)}`);
            p.versions.forEach(v => {
              const period = (v.startTime || v.endTime)
                ? `（${this.formatDate(v.startTime)} ~ ${this.formatDate(v.endTime)}）`
                : '';
              bodyLines.push('');
              bodyLines.push(`  ▸ ${v.versionName}   ${period}`);
              const meta = [];
              if (v.testTime) meta.push(`提测 ${this.formatDate(v.testTime)}`);
              if (v.launchTime) meta.push(`上线 ${this.formatDate(v.launchTime)}`);
              if (v.productManager) meta.push(`产品 ${v.productManager}`);
              if (meta.length) bodyLines.push(`     ${meta.join('  │  ')}`);
              // 按角色分组（前端/后端/测试...）
              const roleGroups = {};
              v.members.forEach(m => {
                const role = m.roles.size ? Array.from(m.roles).join('/') : '未分组';
                if (!roleGroups[role]) roleGroups[role] = [];
                roleGroups[role].push(m);
              });
              Object.keys(roleGroups).forEach(role => {
                bodyLines.push(`     【${role}】`);
                roleGroups[role].forEach(m => {
                  const featureStr = m.features.length ? m.features.join('、') : '（无明细）';
                  bodyLines.push(`       · ${m.userName}  ——  ${featureStr}  ——  ${m.days} 人天`);
                });
              });
            });
          });

          lines.push('═'.repeat(48));
          lines.push(`  📊 项目周报   ${s} ~ ${e}`);
          lines.push('═'.repeat(48));
          lines.push('');
          lines.push('  【本期总览】');
          lines.push(`    项目数：${sortedProjects.length} 个`);
          lines.push(`    总工时：${grandDays} 人天`);
          lines.push(`    总费用：${this.formatMoney(grandCost)}`);
          lines.push('');
          lines.push('─'.repeat(48));
          bodyLines.forEach(l => lines.push(l));
          lines.push('');
          lines.push('═'.repeat(48));
        } else {
          // 按人维度聚合
          const userBucket = {};
          withAssign.forEach(({ project, version, days, assignments }) => {
            if (!assignments || assignments.length === 0) return;
            const uvMap = {};
            assignments.forEach(a => {
              const key = a.userId + '|' + version.id;
              if (!uvMap[key]) {
                uvMap[key] = {
                  project, version, days,
                  userId: a.userId,
                  userName: a.userName || userMap[a.userId] || '未知',
                  roles: new Set(),
                  features: []
                };
              }
              const r = a.roleName || roleMap[a.roleId] || '';
              if (r) uvMap[key].roles.add(r);
              if (a.feature) uvMap[key].features.push(a.feature);
            });
            Object.values(uvMap).forEach(uv => {
              if (!userBucket[uv.userId]) {
                userBucket[uv.userId] = { userName: uv.userName, totalDays: 0, entries: [] };
              }
              userBucket[uv.userId].totalDays += uv.days;
              userBucket[uv.userId].entries.push({
                projectName: uv.project.projectName,
                versionName: uv.version.versionName,
                roleStr: Array.from(uv.roles).join('/'),
                features: uv.features,
                days: uv.days,
                startTime: uv.version.startTime,
                endTime: uv.version.endTime,
                testTime: uv.version.testTime,
                launchTime: uv.version.launchTime,
                productManager: uv.version.productManager
              });
            });
          });

          if (Object.keys(userBucket).length === 0) {
            this.weeklyReportText = '';
            this.$message.warning('选定区间内没有人员参与的分配');
            return;
          }

          const sortedUsers = Object.values(userBucket).sort((a, b) => b.totalDays - a.totalDays);
          let totalDays = 0;
          let totalCost = 0;
          const bodyLines = [];
          sortedUsers.forEach(u => {
            const cost = u.totalDays * rate;
            totalDays += u.totalDays;
            totalCost += cost;
            bodyLines.push('');
            bodyLines.push(`▎ ${u.userName}   ·   ${u.totalDays} 人天 / ${this.formatMoney(cost)}`);
            u.entries.forEach(en => {
              const featureStr = en.features.length ? en.features.join('、') : '（无明细）';
              const roleStr = en.roleStr ? `[${en.roleStr}] ` : '';
              bodyLines.push('');
              bodyLines.push(`  ▸ ${roleStr}${en.projectName} / ${en.versionName}   ——   ${en.days} 人天`);
              const meta = [];
              if (en.startTime || en.endTime) meta.push(`研发 ${this.formatDate(en.startTime)} ~ ${this.formatDate(en.endTime)}`);
              if (en.testTime) meta.push(`提测 ${this.formatDate(en.testTime)}`);
              if (en.launchTime) meta.push(`上线 ${this.formatDate(en.launchTime)}`);
              if (en.productManager) meta.push(`产品 ${en.productManager}`);
              if (meta.length) bodyLines.push(`     ${meta.join('  │  ')}`);
              bodyLines.push(`     功能：${featureStr}`);
            });
          });

          lines.push('═'.repeat(48));
          lines.push(`  📊 团队周报   ${s} ~ ${e}`);
          lines.push('═'.repeat(48));
          lines.push('');
          lines.push('  【本期总览】');
          lines.push(`    参与人数：${sortedUsers.length} 人`);
          lines.push(`    总工时：${totalDays} 人天`);
          lines.push(`    总费用：${this.formatMoney(totalCost)}`);
          lines.push('');
          lines.push('─'.repeat(48));
          bodyLines.forEach(l => lines.push(l));
          lines.push('');
          lines.push('═'.repeat(48));
        }

        this.weeklyReportText = lines.join('\n');
        this.$message.success('周报已生成');
      } catch (err) {
        console.error('生成周报失败:', err);
        this.$message.error('生成失败，请重试');
      } finally {
        this.weeklyReportLoading = false;
      }
    },

    async copyWeeklyReport() {
      if (!this.weeklyReportText) return;
      try {
        await navigator.clipboard.writeText(this.weeklyReportText);
        this.$message.success('已复制到剪贴板');
      } catch (e) {
        const ta = document.createElement('textarea');
        ta.value = this.weeklyReportText;
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); this.$message.success('已复制到剪贴板'); }
        catch (er) { this.$message.error('复制失败，请手动复制'); }
        document.body.removeChild(ta);
      }
    }
  }
};
</script>

<style scoped>
.projects-container {
  max-width: 1400px;
  margin: 0 auto;
  color: var(--text-primary);
}
.card-section { margin-bottom: 18px; }

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
.search-input { width: 320px; max-width: 100%; }
.search-status { width: 160px; margin-right: auto; }
.add-btn { font-weight: 500; }
.version-toolbar { display: flex; gap: 10px; align-items: center; }
.version-status-filter { width: 140px; }
.version-sort { width: 150px; }
.version-search { width: 200px; }

/* 智能 KPI 概览 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}
.kpi-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(12px);
  cursor: pointer;
  transition: all .25s ease;
  user-select: none;
}
.kpi-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent-cyan);
  box-shadow: 0 8px 24px rgba(34, 211, 238, 0.18);
}
.kpi-card.active {
  border-color: var(--accent-cyan);
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.3), 0 4px 16px rgba(0, 0, 0, 0.2);
}
.kpi-icon { font-size: 28px; line-height: 1; }
.kpi-content { flex: 1; }
.kpi-label { font-size: 12px; color: var(--text-muted); letter-spacing: .5px; }
.kpi-value { font-size: 26px; font-weight: 700; color: var(--text-primary); margin-top: 2px; line-height: 1.1; }
.kpi-running .kpi-value { color: #67c23a; }
.kpi-pending .kpi-value { color: #e6a23c; }
.kpi-done .kpi-value { color: #909399; }
@media (max-width: 900px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
}

/* 表格进度文本、上线距离 */
.progress-text { font-size: 11px; opacity: .7; margin-top: 2px; }
.launch-distance { font-size: 11px; margin-top: 2px; font-weight: 500; }

/* 费用相关 */
.cost-cell { font-weight: 600; color: #f0a020; font-family: 'Consolas', monospace; }
.cost-total { font-weight: 600; letter-spacing: .5px; }

/* 领导周报 */
.weekly-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 4px 0 14px;
}
.weekly-label { color: var(--text-secondary); font-size: 13px; }
.weekly-text :deep(.el-textarea__inner) {
  background: rgba(0,0,0,.35);
  color: #d8e4ff;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.7;
  border: 1px solid var(--glass-border);
}
.weekly-empty {
  padding: 30px 0;
  text-align: center;
  color: var(--text-secondary);
}
.form-hint { font-size: 11px; color: var(--text-muted); margin-top: 2px; line-height: 1.2; }

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
.stats { display: flex; gap: 8px; align-items: center; }

.empty-state { padding: 60px 0; text-align: center; }
:deep(.el-empty__description p) { color: var(--text-muted) !important; }

/* 分页 */
.pagination-container {
  display: flex; justify-content: flex-end;
  margin-top: 16px; padding: 8px 4px;
}

/* 表单 */
.project-form, .version-form, .assignment-form { padding: 10px 4px 0; }
.form-row { display: flex; gap: 12px; }
.form-item-half { flex: 1; }

/* 版本 / 分配 头部 */
.version-header, .assignment-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding: 12px 16px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}
.version-header h4, .assignment-header h4 {
  margin: 0; font-size: 14px; font-weight: 600;
  color: var(--text-primary);
  display: flex; align-items: center; gap: 8px;
}
.version-header h4::before, .assignment-header h4::before {
  content: "◆"; color: var(--accent-cyan); font-size: 10px;
}

/* 批量分配 */
.batch-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 14px; padding: 10px 14px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid var(--glass-border);
  border-radius: 10px;
}
.batch-tip { margin-left: auto; font-size: 12px; opacity: 0.75; }
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

/* 弹窗底部 */
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
.bug-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
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

/* 粘贴对话框样式 */
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

@media (max-width: 768px) {
  .search-bar { flex-direction: column; align-items: stretch; }
  .search-input { width: 100%; margin-right: 0; }
  .version-header, .assignment-header { flex-direction: column; align-items: stretch; gap: 10px; }
  .batch-row-head { flex-direction: column; align-items: stretch; }
}
</style>
