# OpenAI Code Assistant for PyCharm Community

一个面向 PyCharm Community 的 Kotlin 插件，可连接 OpenAI 兼容的 Chat Completions API。

## 功能

- 在编辑器中按 `Ctrl+Alt+Space` 请求光标处的代码续写；结果显示为补全候选，按 `Tab` 接受，按 `Esc` 忽略。
- 补全请求默认会引用当前选区和当前文件全文；可在设置中分别关闭“引用选中文本”和“引用当前文件全文”。
- 在设置中可启用 Ollama 模式（默认关闭），通过本机或自定义 Ollama 服务的 `/api/chat` 接口完成补全、代码解释和聊天，无需 API Key。
- 聊天侧边栏支持新建、删除会话，停止当前 AI 回复，删除单条消息，以及复制完整会话；会话与消息保存在项目 workspace 配置中，IDE 重启后仍会恢复。
- 自动补全等待秒数可配置（1–30 秒，默认 2 秒）；默认要求等待期间光标保持不动，代码变化会重新开始计时。
- 在选中代码后，从编辑器右键菜单选择 **OpenAI Assistant > Explain Selection with AI** 获取解释。
- 在 **Settings > Tools > OpenAI Code Assistant** 配置 OpenAI 兼容 API，或启用 Ollama 并配置 Ollama 地址和模型；请求超时、提示词和上下文设置对两种模式生效。
- API 密钥保存在 IDE PasswordSafe 中，不写入项目配置文件。

补全请求会把光标前后设定范围内的代码发送给配置的 API 服务；默认还会附带当前选区和当前文件全文，这两项可在设置中分别关闭。选区解释会发送完整选区内容。请仅对你允许发送到该服务的代码使用这些功能。

## API 配置

API base URL 填写服务地址，例如 `https://api.openai.com/v1`；插件会在其后请求 `/chat/completions`。填写服务商提供的模型名称和 API key。更换模型服务商时，确认其支持 OpenAI Chat Completions 请求格式。

启用 Ollama 模式后，默认连接 `http://localhost:11434`，并使用设置中的 Ollama 模型名（默认 `qwen2.5-coder:7b`）。请先启动 Ollama 并拉取相应模型；也可以在设置中修改服务地址、模型名和响应超时（默认 300 秒）。Ollama 模式不会发送 OpenAI API Key。

## 构建与运行

需要 JDK 21 和 Gradle 9.0 或更高版本。建议使用 Gradle 9.7.0；Gradle 9.8 高于 Kotlin Gradle 插件 2.4.20 的完整兼容上限。

```powershell
gradle test
gradle runIde
gradle buildPlugin
```

`runIde` 会启动一个用于试运行的 PyCharm 沙箱实例。打包后的插件 ZIP 位于 `build/distributions/`，可在 PyCharm 的 **Settings > Plugins > Install Plugin from Disk...** 中安装。

## 开发

项目使用 IntelliJ Platform Gradle Plugin，构建目标为 PyCharm 2024.3.5 统一发行版（该版本起 Community 与 Professional 使用同一产品发行包）。当前使用的可下载版本为 2024.3.5。插件功能可在 PyCharm 免费 Community 使用。修改后可运行 `gradle test` 执行单元测试，运行 `gradle verifyPlugin` 检查插件兼容性。

## 抄袭倒卖还有一堆乱七八糟的东西

项目没有参考任何其他项目的源代码，且项目完全免费禁止倒卖倒卖必究!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
本项目与AI ASSISTANT没有任何关系代码完全由ai编译和开发(编译是我编的并非ai dog
没事了🤑🤑🤑🤑67676767676767
7891
























