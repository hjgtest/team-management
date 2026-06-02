# 开发团队管理系统

## 项目简介

开发团队管理系统是一个基于Spring Boot + Vue.js的前后端分离项目，用于管理开发团队的人员、角色、小组和菜单信息。

## 技术栈

- 后端：Spring Boot、MyBatis-Plus、MySQL
- 前端：Vue.js 3、Element Plus、Axios
- 构建工具：Maven、npm

## 功能模块

1. **人员管理** - 管理团队成员信息
2. **角色管理** - 管理用户角色和权限
3. **小组管理** - 管理开发小组信息
4. **菜单管理** - 管理系统菜单

## 快速开始

### 后端启动

1. 确保已安装JDK 8+ 和 Maven
2. 创建MySQL数据库并执行 [database.sql](./database.sql) 文件
3. 修改 `src/main/resources/application.yml` 中的数据库连接信息
4. 在项目根目录执行：
   ```bash
   mvn spring-boot:run
   ```

### 前端启动

1. 确保已安装 Node.js 和 npm
2. 进入 `frontend` 目录
3. 安装依赖：
   ```bash
   npm install
   ```
4. 启动开发服务器：
   ```bash
   npm run serve
   ```

## API 接口

### 用户管理
- GET /api/users - 获取所有用户
- GET /api/users/{id} - 获取指定用户
- POST /api/users - 创建用户
- PUT /api/users/{id} - 更新用户
- DELETE /api/users/{id} - 删除用户

### 角色管理
- GET /api/roles - 获取所有角色
- GET /api/roles/{id} - 获取指定角色
- POST /api/roles - 创建角色
- PUT /api/roles/{id} - 更新角色
- DELETE /api/roles/{id} - 删除角色

### 小组管理
- GET /api/teams - 获取所有小组
- GET /api/teams/{id} - 获取指定小组
- POST /api/teams - 创建小组
- PUT /api/teams/{id} - 更新小组
- DELETE /api/teams/{id} - 删除小组

### 菜单管理
- GET /api/menus - 获取所有菜单
- GET /api/menus/{id} - 获取指定菜单
- POST /api/menus - 创建菜单
- PUT /api/menus/{id} - 更新菜单
- DELETE /api/menus/{id} - 删除菜单

## 项目结构

```
开发组管理系统/
├── src/
│   └── main/
│       ├── java/com/teamdev/
│       │   ├── TeamManagementSystemApplication.java
│       │   ├── controller/
│       │   │   ├── UserController.java
│       │   │   ├── RoleController.java
│       │   │   ├── TeamController.java
│       │   │   └── MenuController.java
│       │   ├── service/
│       │   │   ├── UserService.java
│       │   │   ├── RoleService.java
│       │   │   ├── TeamService.java
│       │   │   └── MenuService.java
│       │   ├── mapper/
│       │   │   ├── UserMapper.java
│       │   │   ├── RoleMapper.java
│       │   │   ├── TeamMapper.java
│       │   │   └── MenuMapper.java
│       │   └── entity/
│       │       ├── User.java
│       │       ├── Role.java
│       │       ├── Team.java
│       │       └── Menu.java
│       └── resources/
│           └── application.yml
├── frontend/
│   ├── src/
│   │   ├── main.js
│   │   ├── App.vue
│   │   ├── api/
│   │   │   └── index.js
│   │   ├── views/
│   │   │   ├── UsersView.vue
│   │   │   ├── RolesView.vue
│   │   │   ├── TeamsView.vue
│   │   │   └── MenusView.vue
│   │   └── router/
│   │       └── index.js
│   ├── package.json
│   └── vue.config.js
├── database.sql
└── README.md
```

## 数据库设计

系统包含4个核心表：
- users - 用户表
- roles - 角色表
- teams - 小组表
- menus - 菜单表

详细建表语句请查看 [database.sql](./database.sql) 文件。

## 部署说明

1. 后端部署：
   - 打包：`mvn package`
   - 运行：`java -jar target/team-management-system-0.0.1-SNAPSHOT.jar`

2. 前端部署：
   - 构建：`npm run build`
   - 将 frontend/dist 下的文件复制到 src/main/resources/static/ 目录即可

## 注意事项

1. 本系统未实现登录认证功能，实际部署时请根据需要添加安全措施
2. 生产环境请修改数据库连接信息和密码
3. 前端通过代理解决跨域问题，部署时需注意API接口地址配置