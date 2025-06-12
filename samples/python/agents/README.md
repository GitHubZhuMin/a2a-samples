## 示例代理

此目录中的所有代理都是基于不同框架构建的示例，展示了不同的功能。每个代理都作为一个独立的 A2A 服务器运行。

每个代理都可以按照其 README 中的说明作为独立的 A2A 服务器运行。默认情况下，每个代理将在本地主机的不同端口上运行（你可以使用命令行参数进行覆盖）。

要与服务器进行交互，请在宿主应用程序（如 CLI）中使用 A2AClient。有关详细信息，请参阅[宿主应用程序](/samples/python/hosts/README.md)。

## 代理目录

* [**Google ADK**](/samples/python/agents/google_adk/README.md)  
用于（模拟）填写费用报告的示例代理。展示了多轮交互以及通过 A2A 返回/回复 Web 表单的功能。

* [**支持 A2A 协议的 AG2 MCP 代理**](/samples/python/agents/ag2/README.md)  
展示了一个使用 [AG2](https://github.com/ag2ai/ag2) 构建的支持 MCP 的代理，该代理通过 A2A 协议公开。

* [**Azure AI Foundry 代理服务**](/samples/python/agents/azureaifoundry_sdk/azurefoundryagent/README.md)  
使用 [Azure AI Foundry 代理服务](https://learn.microsoft.com/en-us/azure/ai-services/agents/overview) 构建的示例代理。

* [**LangGraph**](/samples/python/agents/langgraph/README.md)  
可以使用工具进行货币转换的示例代理。展示了多轮交互、工具使用和流式更新功能。

* [**CrewAI**](/samples/python/agents/crewai/README.md)  
可以生成图像的示例代理。展示了 CrewAI 的使用以及通过 A2A 发送图像的功能。

* [**LlamaIndex**](/samples/python/agents/llama_index_file_chat/README.md)  
可以解析文件并使用解析后的内容作为上下文与用户进行聊天的示例代理。展示了多轮交互、文件上传和解析以及流式更新功能。

* [**Marvin 联系人提取代理**](/samples/python/agents/marvin/README.md)  
展示了一个使用 [Marvin](https://github.com/prefecthq/marvin) 框架从文本中提取结构化联系人信息的代理，并与 Agent2Agent (A2A) 协议集成。

* [**企业数据代理**](/samples/python/agents/mindsdb/README.md)  
可以回答来自任何数据库、数据仓库、应用程序问题的示例代理。由 Gemini 2.5 flash + MindsDB 提供支持。

* [**语义内核代理**](/samples/python/agents/semantickernel/README.md)  
展示了如何实现一个基于 [语义内核](https://github.com/microsoft/semantic-kernel/) 构建并通过 A2A 协议公开的旅行代理。

* [**旅行规划代理**](/samples/python/agents/travel_planner_agent/README.md)  
基于 Google 官方 [a2a-python](https://github.com/google/a2a-python) SDK 实现的旅行助手演示，并通过 A2A 协议实现。