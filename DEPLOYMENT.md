# Aviscribe 单机部署

## 前置条件

- 一台安装了 Docker Engine 与 Docker Compose 的 Linux 服务器
- 指向服务器的域名，以及由云负载均衡、Caddy 或宿主机 Nginx 提供的 HTTPS
- 已开通阿里云 OSS/NLS 和 DeepSeek，并准备最小权限凭据

## 首次启动

```bash
cp .env.example .env
openssl rand -base64 48
# 将生成值以及数据库、阿里云、DeepSeek 配置写入 .env
bash ./scripts/deploy-preflight.sh
docker compose config
docker compose build
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

访问 `http://服务器地址/healthz` 检查前端；后端健康状态位于
`http://服务器地址/api/actuator/health`。

数据库表由 Flyway 在后端启动时自动迁移。不要再在生产环境手工修改表结构；新增变更应添加新的
`V<n>__description.sql`，已经执行过的迁移文件不可修改。

## 更新

```bash
git pull --ff-only
docker compose build
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

后台处理中任务保存在 MySQL。单实例重启后，应用会自动恢复未完成任务，并复用已经生成的视频、音频和转写文本。

## 备份

至少每日备份 MySQL 与媒体卷，并定期验证恢复流程。数据库备份示例：

```bash
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction aviscribe_db' \
  > aviscribe-$(date +%F).sql
```

媒体文件位于 Docker 卷 `media-data`。备份前应确认目标磁盘空间，并由服务器备份工具或云盘快照处理。

## 上线检查

- `.env` 中没有示例值，JWT 密钥为独立随机值。
- `bash ./scripts/deploy-preflight.sh` 检查通过；脚本只检查配置，不会输出密钥。
- 公网只开放 80/443，不暴露 MySQL 和后端 8082 端口。
- HTTPS、证书自动续期、数据库备份和磁盘容量告警已启用。
- `/api/actuator/health` 返回 `UP`；FFmpeg、yt-dlp、数据库和第三方凭据检查正常。
- 已用一段真实音视频完成上传、转写、排版、重启恢复和删除的验收。
