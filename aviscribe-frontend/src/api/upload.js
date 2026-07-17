import request from './request'

// 上传本地文件
export function uploadLocalFile(file, taskName) {
  const formData = new FormData()
  formData.append('file', file)
  if (taskName) {
    formData.append('taskName', taskName)
  }
  
  return request({
    url: '/upload/local',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 提交视频 URL
export function submitVideoUrl(url, taskName) {
  return request({
    url: '/upload/url',
    method: 'post',
    data: { url, taskName }
  })
}
