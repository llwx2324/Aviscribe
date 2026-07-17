#!/usr/bin/env sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_file=${1:-.env}
case "$env_file" in
  /*) ;;
  *) env_file="$project_root/$env_file" ;;
esac

fail() {
  printf '部署前检查失败：%s\n' "$1" >&2
  exit 1
}

[ -f "$env_file" ] || fail "未找到环境变量文件 $env_file"

get_value() {
  awk -F= -v key="$1" '
    $0 !~ /^[[:space:]]*#/ && $1 == key {
      sub(/^[^=]*=/, "")
      sub(/\r$/, "")
      print
      exit
    }
  ' "$env_file"
}

required='DB_PASSWORD DB_ROOT_PASSWORD AVISCRIBE_JWT_SECRET ALIYUN_NLS_APP_KEY ALIYUN_ACCESS_KEY_ID ALIYUN_ACCESS_KEY_SECRET DS_API_KEY'
for name in $required; do
  value=$(get_value "$name")
  [ -n "$value" ] || fail "$name 未填写"
  case "$value" in
    replace-with*) fail "$name 仍是示例值" ;;
  esac
done

jwt_secret=$(get_value AVISCRIBE_JWT_SECRET)
[ "${#jwt_secret}" -ge 32 ] || fail 'AVISCRIBE_JWT_SECRET 至少需要 32 个字符'

db_password=$(get_value DB_PASSWORD)
db_root_password=$(get_value DB_ROOT_PASSWORD)
[ "$db_password" != "$db_root_password" ] || fail '应用数据库密码和 root 密码不能相同'

command -v docker >/dev/null 2>&1 || fail '未安装 Docker'
docker info --format '{{.ServerVersion}}' >/dev/null 2>&1 || fail 'Docker 引擎不可用'

cd "$project_root"
docker compose --env-file "$env_file" config --quiet || fail 'Docker Compose 配置校验失败'

printf '%s\n' '部署前检查通过：环境变量、Docker 引擎和 Compose 配置均正常。'
