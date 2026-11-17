<script setup>
import NavBar from './components/NavBar.vue'
import AppFooter from './components/AppFooter.vue'
</script>

<template>
  <NavBar v-if="!$route.meta.bare" />
  <RouterView />
  <AppFooter v-if="!$route.meta.bare" />
</template>