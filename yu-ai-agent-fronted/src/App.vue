<template>
  <div class="app-shell">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style scoped>
.app-shell {
  width: 100%;
  min-height: 100%;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

.app-shell > :deep(*) {
  flex: 1;
  min-height: 0;
  width: 100%;
}
</style>
