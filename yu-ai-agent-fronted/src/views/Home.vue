<template>
  <div class="home">
    <div class="home-bg" aria-hidden="true">
      <div class="grid-lines"></div>
      <div class="orb orb-left"></div>
      <div class="orb orb-right"></div>
    </div>

    <section class="hero">
      <h1 class="brand">AI超级智能体</h1>
      <p class="subtitle">/ 探索AI的无限可能 /</p>
    </section>

    <section class="apps">
      <router-link to="/love-app" class="app-card love">
        <div class="icon-wrap">
          <span class="icon-glow"></span>
          <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
            />
          </svg>
        </div>
        <h2>AI恋爱大师</h2>
        <p>智能情感顾问，帮你解答恋爱烦恼</p>
        <span class="cta">立即体验 →</span>
      </router-link>

      <router-link to="/manus-app" class="app-card manus">
        <div class="icon-wrap">
          <span class="icon-glow"></span>
          <svg class="icon robot" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M12 1.5a1 1 0 0 1 1 1V4h4.5A2.5 2.5 0 0 1 20 6.5v9A2.5 2.5 0 0 1 17.5 18H6.5A2.5 2.5 0 0 1 4 15.5v-9A2.5 2.5 0 0 1 6.5 4H11V2.5a1 1 0 0 1 1-1zm-3.25 8a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5zm6.5 0a1.75 1.75 0 1 0 0 3.5 1.75 1.75 0 0 0 0-3.5zM8.5 15.25h7a.75.75 0 0 1 0 1.5h-7a.75.75 0 0 1 0-1.5zM7 19.5h10a1 1 0 0 1 0 2H7a1 1 0 0 1 0-2z"
            />
          </svg>
        </div>
        <h2>AI超级智能体</h2>
        <p>全能型AI助手，解决各类专业问题</p>
        <span class="cta">立即体验 →</span>
      </router-link>
    </section>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  position: relative;
  min-height: 100%;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding:
    calc(48px + var(--safe-top))
    calc(var(--page-pad-x) + var(--safe-right))
    calc(64px + var(--safe-bottom))
    calc(var(--page-pad-x) + var(--safe-left));
  overflow: hidden;
}

.home-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 50% at 50% -20%, rgba(0, 212, 255, 0.12), transparent 55%),
    radial-gradient(ellipse 50% 40% at 80% 80%, rgba(59, 130, 246, 0.08), transparent 50%),
    linear-gradient(180deg, #0b1220 0%, #070b14 100%);
  z-index: 0;
}

.grid-lines {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 212, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, black 20%, transparent 75%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  animation: float 8s ease-in-out infinite;
  pointer-events: none;
}

.orb-left {
  width: clamp(160px, 28vw, 280px);
  height: clamp(160px, 28vw, 280px);
  left: -60px;
  top: 20%;
  background: #ff4d6d;
}

.orb-right {
  width: clamp(180px, 32vw, 320px);
  height: clamp(180px, 32vw, 320px);
  right: -80px;
  bottom: 10%;
  background: #00d4ff;
  animation-delay: -3s;
}

.hero,
.apps {
  position: relative;
  z-index: 1;
  width: 100%;
}

.hero {
  text-align: center;
  margin-bottom: clamp(32px, 5vw, 56px);
  padding: 0 8px;
  animation: heroIn 0.8s ease both;
}

.brand {
  font-family: var(--font-sans);
  font-size: clamp(1.6rem, 5.5vw, 3.4rem);
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--neon-cyan);
  word-break: break-word;
  text-shadow:
    0 0 12px rgba(0, 212, 255, 0.8),
    0 0 40px rgba(0, 212, 255, 0.45),
    0 0 80px rgba(0, 212, 255, 0.2);
  animation: neonPulse 3s ease-in-out infinite;
}

.subtitle {
  margin-top: clamp(12px, 2vw, 18px);
  font-size: clamp(0.8rem, 2vw, 0.95rem);
  letter-spacing: 0.28em;
  color: var(--color-muted);
}

.apps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(16px, 3vw, 28px);
  justify-content: center;
  max-width: 920px;
  margin: 0 auto;
  animation: appsIn 0.85s 0.1s ease both;
}

.app-card {
  width: 100%;
  max-width: 420px;
  justify-self: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: clamp(24px, 3.5vw, 36px) clamp(18px, 3vw, 28px) clamp(22px, 3vw, 32px);
  border-radius: var(--radius);
  border: 1px solid var(--color-border);
  background: linear-gradient(165deg, rgba(21, 28, 44, 0.95), rgba(12, 18, 32, 0.9));
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.03) inset,
    0 16px 48px rgba(0, 0, 0, 0.35),
    var(--shadow-neon);
  transition: transform 0.3s ease, border-color 0.3s, box-shadow 0.3s;
}

@media (hover: hover) and (pointer: fine) {
  .app-card:hover {
    transform: translateY(-6px);
  }

  .app-card.love:hover {
    border-color: rgba(255, 77, 109, 0.45);
    box-shadow:
      0 0 0 1px rgba(255, 77, 109, 0.1) inset,
      0 20px 50px rgba(0, 0, 0, 0.4),
      0 0 32px rgba(255, 77, 109, 0.2);
  }

  .app-card.manus:hover {
    border-color: rgba(59, 130, 246, 0.5);
    box-shadow:
      0 0 0 1px rgba(59, 130, 246, 0.1) inset,
      0 20px 50px rgba(0, 0, 0, 0.4),
      0 0 32px rgba(59, 130, 246, 0.25);
  }

  .app-card:hover .cta {
    transform: scale(1.04);
    box-shadow: 0 0 28px rgba(59, 130, 246, 0.65);
    filter: brightness(1.08);
  }
}

.icon-wrap {
  position: relative;
  width: clamp(72px, 12vw, 88px);
  height: clamp(72px, 12vw, 88px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: clamp(16px, 2.5vw, 22px);
}

.icon-glow {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid currentColor;
  opacity: 0.9;
}

.app-card.love .icon-wrap {
  color: var(--love-accent);
}

.app-card.love .icon-glow {
  box-shadow:
    0 0 20px var(--love-glow),
    0 0 40px rgba(255, 77, 109, 0.25),
    inset 0 0 20px rgba(255, 77, 109, 0.15);
  background: radial-gradient(circle, rgba(255, 77, 109, 0.2), transparent 70%);
}

.app-card.manus .icon-wrap {
  color: var(--neon-blue);
}

.app-card.manus .icon-glow {
  box-shadow:
    0 0 20px var(--manus-glow),
    0 0 40px rgba(59, 130, 246, 0.3),
    inset 0 0 20px rgba(59, 130, 246, 0.15);
  background: radial-gradient(circle, rgba(59, 130, 246, 0.22), transparent 70%);
}

.icon {
  position: relative;
  z-index: 1;
  width: clamp(32px, 5vw, 40px);
  height: clamp(32px, 5vw, 40px);
}

.app-card.love .icon {
  fill: var(--love-accent);
  filter: drop-shadow(0 0 8px var(--love-glow));
}

.app-card.manus .icon {
  color: #93c5fd;
  filter: drop-shadow(0 0 8px var(--manus-glow));
}

.app-card.manus .icon.robot {
  fill: #93c5fd;
}

.app-card h2 {
  font-size: clamp(1.1rem, 2.4vw, 1.35rem);
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.02em;
}

.app-card p {
  margin-top: 12px;
  margin-bottom: clamp(18px, 3vw, 28px);
  font-size: clamp(0.84rem, 1.8vw, 0.9rem);
  color: var(--color-muted);
  line-height: 1.6;
}

.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: min(150px, 100%);
  min-height: 44px;
  padding: 10px 24px;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 0 18px rgba(59, 130, 246, 0.45);
  transition: transform 0.2s, box-shadow 0.2s, filter 0.2s;
}

@keyframes neonPulse {
  0%,
  100% {
    text-shadow:
      0 0 12px rgba(0, 212, 255, 0.8),
      0 0 40px rgba(0, 212, 255, 0.45),
      0 0 80px rgba(0, 212, 255, 0.2);
  }
  50% {
    text-shadow:
      0 0 18px rgba(0, 212, 255, 1),
      0 0 48px rgba(0, 212, 255, 0.6),
      0 0 96px rgba(0, 212, 255, 0.3);
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-24px);
  }
}

@keyframes heroIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes appsIn {
  from {
    opacity: 0;
    transform: translateY(28px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 大屏桌面 */
@media (min-width: 1200px) {
  .apps {
    max-width: 980px;
    gap: 36px;
  }

  .app-card {
    max-width: 460px;
  }
}

/* 平板竖屏 / 小笔记本 */
@media (max-width: 1024px) {
  .home {
    padding-top: calc(40px + var(--safe-top));
  }

  .apps {
    max-width: 720px;
    gap: 20px;
  }
}

/* 平板横屏偏窄 / 大手机横屏：可并排但卡片更紧凑 */
@media (max-width: 900px) and (min-width: 641px) {
  .app-card {
    padding: 28px 20px 24px;
  }

  .subtitle {
    letter-spacing: 0.18em;
  }
}

/* 手机：单列卡片 */
@media (max-width: 640px) {
  .home {
    justify-content: flex-start;
    padding-top: calc(56px + var(--safe-top));
    padding-bottom: calc(40px + var(--safe-bottom));
  }

  .hero {
    margin-bottom: 28px;
  }

  .brand {
    letter-spacing: 0.02em;
    line-height: 1.25;
  }

  .subtitle {
    letter-spacing: 0.1em;
    padding: 0 4px;
  }

  .apps {
    grid-template-columns: 1fr;
    max-width: 400px;
    gap: 16px;
  }

  .app-card {
    max-width: 100%;
  }

  .orb-left,
  .orb-right {
    opacity: 0.22;
  }

  .grid-lines {
    background-size: 36px 36px;
  }
}

/* 超小屏 */
@media (max-width: 380px) {
  .brand {
    font-size: 1.45rem;
  }

  .cta {
    width: 100%;
  }
}

/* 横屏矮屏手机：避免垂直空间浪费过大 */
@media (max-height: 500px) and (orientation: landscape) {
  .home {
    justify-content: flex-start;
    padding-top: calc(24px + var(--safe-top));
    padding-bottom: calc(24px + var(--safe-bottom));
    overflow-y: auto;
  }

  .hero {
    margin-bottom: 20px;
  }

  .apps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-width: 640px;
  }

  .icon-wrap {
    width: 56px;
    height: 56px;
    margin-bottom: 10px;
  }

  .app-card p {
    margin-bottom: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .brand,
  .orb,
  .hero,
  .apps {
    animation: none;
  }
}
</style>
