# 模型 API 适配

CompatibleApiProvider 实现 OpenAI 兼容的 chat/completions SSE 和 audio/transcriptions。地址默认要求 HTTPS；debug 允许本地 mock 地址。OkHttp 不自动重试，业务层同样不重试。

SSE 以空行分帧，支持多 data 行；情绪后缀可跨多个 chunk。使用有界通道阻塞背压保证内容不被 trySend 丢弃。没有 DONE 或 finish_reason 的断流视为中断。HTTP 错误显示通用描述，不回显提供商原始响应或 Key。

MockWebServer 验证流式分片、背压、401 无重试和截断响应，不消耗真实额度。各提供商的视觉与语音能力不同，应填写其实际文档支持的模型。
