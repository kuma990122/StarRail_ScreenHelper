# JVM 测试

通过 tools/android.ps1 -Task Test 运行。CoreLogicTest 验证情绪跨分片解析、SSE、关系冷却与每日上限、有效时长、提醒版本和上下文预算。CompatibleApiProviderTest 使用本机 MockWebServer，验证完整请求和断流，不需要 API Key。
