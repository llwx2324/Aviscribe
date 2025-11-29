# Aviscribe

Aviscribe 是一款本地部署优先的语音 / 音视频转写与管理工具，支持上传本地音视频文件或粘贴链接，一键创建「转录任务」，并在工作台中查看历史任务与转写结果。

本仓库包含前后端完整代码，可在本地一键启动体验。

---

## 功能概览

- 🎧 支持常见音视频格式（MP4 / MKV / MOV / FLAC / AAC / MP3 等）
- 📂 任务管理：左侧历史任务列表，支持按来源（本地 / 链接）和时间查看
- 📑 转写结果展示：右侧结果区域与历史任务独立滚动，保持布局稳定
- 🧩 前后端分离架构：前端基于 Vite + Vue 3 + Element Plus，后端基于 Java（Maven）
- 🖥 一键启动 / 停止脚本（本地调试用，不随仓库发布）

---

## 目录结构

```text
Aviscribe/
├─ aviscribe-backend/      # 后端服务（Java + Maven）
│  ├─ src/                 # 业务代码
│  └─ pom.xml              # Maven 配置
├─ aviscribe-frontend/     # 前端应用（Vite + Vue 3）
│  ├─ src/
│  │  ├─ layouts/          # 布局组件（含工作台布局）
│  │  ├─ components/       # 复用组件（如历史任务列表等）
│  │  └─ assets/           # 全局样式与静态资源
│  └─ vite.config.ts
├─ .gitignore
└─ README.md
```

---

## 环境要求

- Node.js 18+（推荐 LTS）
- npm 或 pnpm / yarn（仓库默认使用 npm）
- JDK 17+（视后端 `pom.xml` 要求而定）
- Maven 3.8+（命令行为 `mvn`）

---

## 本地开发启动

### 1. 克隆仓库

```bash
git clone https://github.com/<your-username>/Aviscribe.git
cd Aviscribe
```

### 2. 启动后端

```bash
cd aviscribe-backend
mvn clean package
mvn spring-boot:run        # 或查 pom.xml 中的主类 / 插件
```

> 具体端口和配置请参考 `aviscribe-backend/src/main/resources/application*.yml`。

### 3. 启动前端

```bash
cd ../aviscribe-frontend
npm install
npm run dev
```

默认会在 `http://localhost:5173`（或控制台输出端口）启动前端开发服务器。

---

## 构建与生产部署

### 前端构建

```bash
cd aviscribe-frontend
npm run build
# 打包产物默认输出到 aviscribe-frontend/dist
```

### 后端构建

```bash
cd aviscribe-backend
mvn clean package
# 生成的可执行 jar 位于 aviscribe-backend/target
```

前后端可按需部署到同一服务器或分别部署，通过环境变量 / 配置文件指定后端 API 地址。

---

## 项目约定与开发说明

- 前端使用 **Vite + Vue 3 + TypeScript** 和 **Element Plus**：
  - 布局相关组件集中在 `src/layouts/` 下，如工作台布局（历史任务 + 右侧结果区域）。
  - 全局样式和主题在 `src/assets/` 及 `src/assets/main.css` 中维护。
  - 滚动相关布局（左侧历史任务与右侧结果区域独立滚动）也在布局组件中实现，调整滚动行为时优先修改布局组件而非页面组件。
- 后端使用 **Maven** 管理依赖和构建：
  - 业务入口通常位于 `aviscribe-backend/src/main/java/.../Application.java`（具体路径请查看源码）。
  - 与转写相关的核心接口位于 `controller` / `service` 包下，可在此扩展新能力（如新增任务状态、导出功能等）。
- 环境配置：
  - `.env` / `.env.*` 默认被忽略，不会提交到仓库。
  - 如需分享示例配置，请创建 `*.example` 文件并在 README 中说明。

---

## Git 忽略说明

仓库已配置 `.gitignore`，忽略以下内容以避免无关文件进入版本库：

- 构建产物与依赖：
  - `**/target/`、`**/node_modules/`、`dist/` 等
- 环境与缓存：
  - `.env`、`.env.*`、`.venv/`、`.cache/` 等
- IDE 配置：
  - `.idea/`、`.vscode/`、`*.iml` 等
- 本地辅助脚本和结构说明：
  - `gen_backend.js`、`gen_frontend.js`
  - `start-all.bat`、`stop-all.bat`、`structure.txt`

---

## 许可证

根据实际情况填写，例如：

```text
MIT License
Copyright (c) 2025 <Your Name>
```

---

## 贡献

欢迎提交 Issue 和 Pull Request 来改进 Aviscribe：

1. Fork 本仓库
2. 建立特性分支：`git checkout -b feature/xxx`
3. 提交改动并推送：`git push origin feature/xxx`
4. 在 GitHub 上创建 Pull Request