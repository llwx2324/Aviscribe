# Aviscribe

Aviscribe 是一个专注于音/视频转写的全栈应用，包含 Spring Boot + MyBatis-Plus 后端与基于 Vue 3 + Element Plus 的前端。当前版本已接入用户体系，支持邮箱/手机号注册、JWT 登录以及多用户任务隔离。

## 目录结构

```
Aviscribe/
├─ aviscribe-backend/     # Spring Boot 服务
├─ aviscribe-frontend/    # Vite + Vue 前端
├─ init.sql               # 数据库初始化脚本（位于 backend 根目录）
└─ README.md
```

## 后端（aviscribe-backend）

### 关键特性
- Spring Boot 3.1 + Spring Security + JWT
- MyBatis-Plus + MySQL，`t_task` 与 `t_user` 建立外键关联
- OSS/YT-DLP/FFmpeg 等音视频处理依赖

### 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.x（或兼容版本）
- FFmpeg、yt-dlp、阿里云 STT/OSS 相关 CLI/依赖

### 配置步骤
1. **数据库初始化**：执行 `aviscribe-backend/init.sql`，创建 `t_user` 与 `t_task`（含 `user_id` 外键）。已有历史数据时，需手动为任务填充 `user_id` 或暂时允许空值后分配。
2. **修改 `application.yml`**：
   - 更新 `spring.datasource.*` 为实际数据库连接。
   - 配置 `aviscribe.file.upload-path`、`ffmpeg.path`、`downloader.yt-dlp-path`。
   - 设置 JWT 相关配置：
     ```yaml
     aviscribe:
       security:
         jwt:
           secret: "长度>=32的随机字符串"
           access-exp-minutes: 30
           refresh-exp-days: 7
     ```
3. **构建/运行**：
   ```bash
   mvn -f aviscribe-backend/pom.xml clean package
   java -jar aviscribe-backend/target/aviscribe-backend-0.0.1-SNAPSHOT.jar
   ```
   默认监听 `http://localhost:8081/api`。

### 用户与鉴权
- 注册：`POST /api/v1/auth/register`，参数包含 `email`、`displayName`、`password`、可选 `phone`。
- 登录：`POST /api/v1/auth/login`，返回 `accessToken`、`refreshToken`、过期时间及用户信息。
- 刷新：`POST /api/v1/auth/refresh`。
- 当前用户：`GET /api/v1/auth/me`。
- 任务接口（`/v1/upload/**`, `/v1/task/**`）默认要求登录；普通用户仅能访问自己的任务，`ADMIN` 可以访问所有任务。

## 前端（aviscribe-frontend）

### 环境要求
- Node.js 18+
- npm 9+/pnpm/yarn 任一

### 启动与构建
```bash
cd aviscribe-frontend
npm install
npm run dev      # 本地调试，默认 http://localhost:5173
npm run build    # 生产构建，输出 dist/
```
Vite dev server 已通过 `vite.config.js` 将 `/api` 代理到 `http://localhost:8081`。

### 前端鉴权流程
- `src/stores/auth.js` 负责本地存储 JWT 及用户信息。
- `src/api/request.js` 在请求前自动携带 `Authorization: Bearer <token>`，遇到 401 会使用刷新令牌重新获取 Access Token。
- 路由守卫：`/app/**` 需登录；`/login`、`/register` 仅允许未登录用户访问。未登录访问受限页面会重定向至登录页并带上 `redirect` 查询参数。
- 顶部导航会根据登录状态展示“登录/注册”按钮或用户头像菜单，可在菜单中退出登录。

### 常见操作
1. 注册一个新账号（前端 `/register` 或 POST API），成功后自动登录。
2. 登录后访问 `/app` 上传任务或 `/app/tasks` 查看历史记录；所有任务列表、详情、删除等操作会自动过滤为当前用户。
3. Token 过期后前端会自动刷新；若刷新失败，会提示重新登录。

## 测试
- 后端：`mvn -f aviscribe-backend/pom.xml test`
- 前端：`npm run build`（可根据需要补充 E2E/单元测试）

## 部署建议
- 生产环境务必将 `AVISCRIBE_JWT_SECRET` 设置为高熵随机串，并通过环境变量注入。
- 后端建议配合 Nginx/Traefik 等反向代理启用 HTTPS。
- 前端构建产物 `dist/` 可部署到任意静态服务器（Vercel、Netlify、Nginx 等），并确保 `/api` 代理到后端地址。
- 若需管理员账号，可在数据库 `t_user` 中手动将 `role` 更新为 `ADMIN`。
