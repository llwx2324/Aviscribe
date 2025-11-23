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
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 0 60px;
}

@media (max-width: 1024px) {
  .upload-simple {
    padding: 16px 16px 40px;
  }
}
</style>