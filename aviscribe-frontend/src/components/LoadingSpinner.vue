<template>
    <div class="loading-container">
      <el-icon class="is-loading" :size="size">
        <Loading />
      </el-icon>
      <div v-if="text" class="loading-text">{{ text }}</div>
    </div>
  </template>
  
  <script setup>
  import { defineProps } from 'vue';
  import { Loading } from '@element-plus/icons-vue'
  
  defineProps({
    text: {
      type: String,
      default: '处理中，请稍候...'
    },
    size: {
      type: Number,
      default: 30
    }
  });
  </script>
  
  <style scoped>
  .loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
  }
  .loading-text {
    margin-top: 10px;
    color: #606266;
  }
  </style>