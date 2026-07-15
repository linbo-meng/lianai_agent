# 鱼 AI 智能体应用平台（前端）

基于 Vue 3 + Vite + Axios 的 AI 应用前端，对接本地 Spring Boot 后端。

## 功能

- **主页**：切换进入不同 AI 应用
- **AI 恋爱大师**：聊天室界面，进入页面自动生成 `chatId`，通过 SSE 调用 `doChatWithLoveAppSse`
- **AI 超级智能体**：聊天室界面，通过 SSE 调用 `doChatWithManus`

## 技术栈

- Vue 3
- Vue Router 4
- Axios
- Vite

## 快速开始

```bash
npm install
npm run dev
```

默认开发地址：http://localhost:5173

开发环境已配置代理：`/api` → `http://localhost:8123`

请确保后端服务已在 `http://localhost:8123` 运行。

## 接口说明

| 应用 | 方法 | 路径 | 参数 |
|------|------|------|------|
| AI 恋爱大师 | GET (SSE) | `/api/ai/love_app/chat/sse` | `message`, `chatId` |
| AI 超级智能体 | GET (SSE) | `/api/ai/manus/chat` | `message` |

> 说明：SSE 流式响应使用 `fetch` + `ReadableStream` 消费；Axios 用于常规 HTTP 请求封装。
