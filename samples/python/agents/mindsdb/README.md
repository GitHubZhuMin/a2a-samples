## MindsDB企业数据代理

由Gemini 2.5 flash + MindsDB提供支持。此示例使用A2A来跨数百个联合数据源（包括数据库、数据湖和SaaS应用程序）连接、查询和分析数据。

该代理接收用户的自然语言查询，并将其转换为适合MindsDB的SQL查询，处理跨多个数据源的数据联合。它可以：

- 从包括数据库、数据湖和SaaS应用程序在内的各种数据源查询数据
- 跨联合数据源执行分析
- 处理关于您数据的自然语言问题
- 返回来自多个数据源的结构化结果
<img width="597" alt="image" src="https://github.com/user-attachments/assets/3e070239-f2a1-4771-8840-6517bd0c6545" />

## 先决条件

- Python 3.9或更高版本
- MindsDB账户和API凭证（https://mdb.ai）
- 创建一个MindsDB思维（一个可以从数据库查询数据的AI模型），默认情况下我们使用演示模型：`Sales_Data_Expert_Demo_Mind`

## 环境变量

在mdb.ai中，一旦您创建了一个思维（一个可以从数据库查询数据的AI模型），就可以在代理中使用它。

在项目目录中创建一个`.env`文件，并包含以下变量：

```
MINDS_API_KEY=您的MindsDB API密钥
MIND_NAME=您要使用的MindsDB模型名称
```

- `MINDS_API_KEY`：您的MindsDB API密钥（必需）
- `MIND_NAME`：要使用的MindsDB思维的名称（必需）

## 运行示例

1. 导航到示例目录：
    ```bash
    cd samples/python/agents/mindsdb
    ```

2. 运行代理：
    ```bash
    uv run .
    ```

3. 在另一个终端中，运行A2A客户端：
    ```bash
    # 连接到代理（指定带有正确端口的代理URL）
    cd samples/python/hosts/cli
    uv run . --agent http://localhost:10006

    # 如果您在启动代理时更改了端口，请使用该端口
    # uv run . --agent http://localhost:您的端口
    ```
4. 向代理询问关于您数据的问题。

## 示例查询

您可以提出如下问题：

- “潜在客户中高管的比例是多少？”
- “公司规模的分布情况如何？”

该代理将处理跨不同数据源的数据连接和分析的复杂性。