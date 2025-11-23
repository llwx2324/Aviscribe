<template>
  <div class="welcome-container">
    <div class="welcome-content">
      <h1 class="welcome-title">Welcome to Aviscribe</h1>
      <p class="welcome-text">
        Upload a video or select a task from your history to see the results here.
      </p>
    </div>
  </div>
</template>

<style scoped>
.welcome-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  border-radius: var(--card-radius);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.welcome-content {
  text-align: center;
  padding: 40px;
}

.welcome-title {
  font-size: 24px;
  color: var(--text-main);
  margin-bottom: 16px;
}

.welcome-text {
  color: var(--text-secondary);
  font-size: 16px;
}
</style>
