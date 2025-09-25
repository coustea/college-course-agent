<template>
  <div class="contact-page">
    <div class="header">
      <h1 class="page-title">联系我们</h1>
      <div class="subtitle">有任何问题或建议，欢迎与我们取得联系</div>
    </div>

    <div class="cards">
      <div class="card">
        <div class="icon">
          <i class="fas fa-envelope"></i>
        </div>
        <div class="content">
          <h3>意见反馈</h3>
          <p>欢迎反馈您在使用中的体验与建议</p>
          <a href="12345678@qq.com" class="link">12345678@qq.com</a>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.contact-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px;
}

.header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
}

.subtitle {
  color: #64748b;
  margin-top: 8px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 16px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

.icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0f2fe;
  color: #0284c7;
  flex: none;
}

.content h3 {
  margin: 0 0 6px 0;
  color: #111827;
}

.content p {
  margin: 0 0 8px 0;
  color: #6b7280;
}

.link {
  color: #2563eb;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>


