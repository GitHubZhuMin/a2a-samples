# 你好，世界示例

仅返回消息事件的“你好，世界”示例代理

## 入门指南

1. 启动服务器

   ```bash
   uv run .
   ```

2. 运行测试客户端

   ```bash
   uv run test_client.py
   ```

## 构建容器镜像

也可以使用容器文件来构建代理。

1. 导航到 `samples/python/agents/helloworld` 目录：

  ```bash
  cd samples/python/agents/helloworld
  ```

2. 构建容器文件

    ```bash
    podman build . -t helloworld-a2a-server
    ```

> [!提示]  
> Podman 是 `docker` 的直接替代品，这些命令中也可以使用它。

3. 运行你的容器

    ```bash
    podman run -p 9999:9999 helloworld-a2a-server
    ```

## 验证

要在另一个终端中进行验证，请运行 A2A 客户端：

```bash
cd samples/python/hosts/cli
uv run . --agent http://localhost:9999
```