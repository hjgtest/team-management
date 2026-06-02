# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

开发团队管理系统 — Spring Boot 2.7.0 + Vue 3 前后端分离项目，使用 MyBatis-Plus 操作 MySQL 数据库。

## 常用命令

### 开发环境

```bash
# 一键构建并启动（前后端打包为单体 jar 运行）
start.bat

# 仅后端开发模式
mvn spring-boot:run

# 前端开发服务器（代理 /api 到后端 localhost:8080）
cd frontend && npm run serve

# 前端构建
cd frontend && npm run build

# 前端 lint
cd frontend && npm run lint
```

### 打包部署

```bash
# Maven 打包
mvn clean package -DskipTests

# 运行 jar
java -jar target/team-management-system-0.0.1-SNAPSHOT.jar
```

### 数据库

```bash
# 初始化/修复数据库
mysql -u root -p < test-db-connection.sql
mysql -u root -p < upgrade-v2.sql
```

## 架构概览

### 后端 (src/main/java/com/teamdev/)

```
Spring Boot 2.7.0 (端口 8080)
├── controller/     — REST 接口层，路由以 /api 开头
│   ├── UserController        /api/users
│   ├── RoleController        /api/roles
│   ├── TeamController        /api/teams
│   ├── ProjectController     /api/projects
│   ├── ProjectVersionController  /api/project-versions
│   ├── ProjectAssignmentController /api/project-assignments
│   ├── DashboardController   /api/dashboard
│   ├── ExportController      /api/export (EasyExcel 导出)
│   └── FileParseController   文件解析
├── service/        — 业务逻辑层
├── mapper/         — MyBatis-Plus Mapper 接口
├── entity/         — 实体类 (使用 Lombok)
└── config/
    └── WebMvcConfig.java     Web MVC 配置
```

### 前端 (frontend/src/)

```
Vue 3 + Vue CLI + Element Plus
├── views/          — 页面组件
│   ├── DashboardView.vue   仪表盘
│   ├── UsersView.vue       人员管理（含 Excel 导出）
│   ├── RolesView.vue       角色管理
│   ├── TeamsView.vue       小组管理
│   ├── ProjectsView.vue    项目管理
│   └── FunctionTest.vue    功能测试页 (/test 路由)
├── api/            — API 请求模块（按功能拆分文件）
│   ├── index.js            主入口
│   ├── users.js, roles.js, teams.js, projects.js 等
│   ├── projectVersions.js
│   └── projectAssignments.js
├── components/     — 可复用组件
├── router/         — Vue Router 路由配置
└── utils/
    └── request.js  Axios 封装
```

### 核心数据模型

| 表 | 实体 | 说明 |
|---|---|---|
| users | User | 用户（含逻辑删除 deleted 字段） |
| roles | Role | 角色 |
| teams | Team | 小组/团队 |
| projects | Project | 项目 |
| project_versions | ProjectVersion | 项目版本（含起止时间） |
| project_assignments | ProjectAssignment | 用户-项目版本关联 |

## 技术要点

- **MyBatis-Plus** 已配置逻辑删除（`deleted` 字段），全局启用 `map-underscore-to-camel-case`
- **Mapper XML** 位于 `src/main/resources/mapper/`，当前只有 `ProjectAssignmentMapper.xml`
- **跨域**: 开发模式下前端通过 `vue.config.js` 的 devServer proxy 解决跨域；无单独的 CORS 配置类（已删除）
- **Excel 导出**: 使用 Alibaba EasyExcel 3.1.1
- **图表**: 使用 ECharts 5.3.0（DashboardView 中）
- **数据库**: MySQL `team_management_db`，root/123456（application.yml）
- **Java 版本**: 1.8
- **文件上传**: 最大 500MB（application.yml 配置）
