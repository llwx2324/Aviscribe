<script setup>
import UploadCard from '@/components/UploadCard.vue';

defineEmits(['upload-success']);
</script>

<template>
  <div class="upload-simple">
    <UploadCard @upload-success="$emit('upload-success')" />
  </div>
</template>

<style scoped>
.upload-simple {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 0 40px;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

@media (max-width: 1024px) {
  .upload-simple {
    padding: 16px 16px 40px;
  }
}
</style>