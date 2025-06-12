# 示例代码

此代码用于随着规范的推进展示 A2A 功能。

示例分为 3 个子目录：

* [**通用**](/samples/python/common)
    * 注意：请勿使用此代码进行进一步开发。请使用此处的 A2A Python SDK：https://github.com/google/a2a-python/

* [**代理**](/samples/python/agents/README.md)  
用多种框架编写的示例代理，可使用工具执行示例任务。这些都使用通用的 A2A 服务器。

* [**主机**](/samples/python/hosts/README.md)  
使用 A2A 客户端的主机应用程序。包括一个 CLI，它展示了单个代理的简单任务完成情况；一个 Mesop 网络应用程序，它可以与多个代理进行交互；以及一个编排代理，它将任务委派给多个远程 A2A 代理之一。

## 前提条件

- Python 3.13 或更高版本
- [UV](https://docs.astral.sh/uv/)

## 运行示例

运行一个（或多个）[代理](/samples/python/agents/README.md) A2A 服务器和一个[主机应用程序](/samples/python/hosts/README.md)。

以下示例将使用 Python CLI 主机运行 LangGraph 代理：

1. 导航到代理目录：
    ```bash
    cd samples/python/agents/langgraph
    ```
2. 运行一个代理：
    ```bash
    uv run .
    ```
3. 在另一个终端中，导航到 CLI 目录：
    ```bash
    cd samples/python/hosts/cli
    ```
4. 运行示例客户端
    ```
    uv run .
    ```
---
**注意：** 
这是示例代码，并非生产级别的库。
---