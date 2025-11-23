const fs = require('fs');
const path = require('path');

// 项目根目录
const root = path.join(__dirname, 'aviscribe-frontend');

// 文件/目录结构定义
const structure = {
  'node_modules': {},
  'public': {},
  'src': {
    'api': {
      'task.js': '',
      'upload.js': ''
    },
    'assets': {},
    'components': {
      'LoadingSpinner.vue': '',
      'ResultViewer.vue': '',
      'UploadCard.vue': ''
    },
    'router': {
      'index.js': ''
    },
    'views': {
      'ResultView.vue': '',
      'TaskListView.vue': '',
      'UploadView.vue': ''
    },
    'App.vue': '',
    'main.js': ''
  },
  '.gitignore': '',
  'index.html': '',
  'package.json': '',
  'vite.config.js': ''
};

// 递归创建文件夹和文件
function createStructure(basePath, obj) {
  for (const name in obj) {
    const fullPath = path.join(basePath, name);
    if (typeof obj[name] === 'object') {
      if (!fs.existsSync(fullPath)) fs.mkdirSync(fullPath, { recursive: true });
      createStructure(fullPath, obj[name]);
    } else {
      fs.writeFileSync(fullPath, obj[name] || '');
    }
  }
}

// 开始创建
createStructure(root, structure);

console.log('aviscribe-frontend 项目结构已生成！');
