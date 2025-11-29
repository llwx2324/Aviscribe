import request from './request'

// 获取任务列表 (带分页)
export function getTaskList(page, size) {
  return request({
    url: '/task/list',
    method: 'get',
    params: { page, size, sort: 'createTime,desc' }
  })
}

// 获取任务详情 (用于轮询状态和查看结果)
export function getTaskDetail(taskId) {
  return request({
    url: `/task/${taskId}`,
    method: 'get'
  })
}

// 删除任务
export function deleteTask(taskId) {
  return request({
    url: `/task/${taskId}`,
    method: 'delete'
  })
}

export function renameTask(taskId, taskName) {
  return request({
    url: `/task/${taskId}/name`,
    method: 'patch',
    data: { taskName }
  })
}