import {
  AgentCard,
  AgentCapabilities,
  JSONRPCRequest,
  JSONRPCResult,
  JSONRPCError,
  JSONRPCErrorResponse,
  Message,
  Task,
  TaskStatusUpdateEvent,
  TaskArtifactUpdateEvent,
  MessageSendParams,
  SendMessageResponse,
  SendStreamingMessageResponse,
  SendStreamingMessageSuccessResponse,
  TaskQueryParams,
  GetTaskResponse,
  GetTaskSuccessResponse,
  TaskIdParams,
  CancelTaskResponse,
  CancelTaskSuccessResponse,
  TaskPushNotificationConfig, // Renamed from PushNotificationConfigParams for direct schema alignment
  SetTaskPushNotificationConfigRequest,
  SetTaskPushNotificationConfigResponse,
  SetTaskPushNotificationConfigSuccessResponse,
  GetTaskPushNotificationConfigRequest,
  GetTaskPushNotificationConfigResponse,
  GetTaskPushNotificationConfigSuccessResponse,
  TaskResubscriptionRequest,
  A2AError,
  SendMessageSuccessResponse
} from '../schema.js'; // 假设schema.ts在同一目录或已正确配置路径

// 辅助类型：用于流式方法中yield的数据类型
// 包含消息、任务、任务状态更新、任务产物更新
// type A2AStreamEventData = Message | Task | TaskStatusUpdateEvent | TaskArtifactUpdateEvent;
type A2AStreamEventData = Message | Task | TaskStatusUpdateEvent | TaskArtifactUpdateEvent;

/**
 * A2AClient 是一个用于与A2A协议兼容代理交互的TypeScript HTTP客户端。
 */
export class A2AClient {
  private agentBaseUrl: string; // 代理基础URL
  private agentCardPromise: Promise<AgentCard>; // Agent Card的Promise
  private requestIdCounter: number = 1; // 请求ID计数器
  private serviceEndpointUrl?: string; // 从AgentCard获取的服务端点URL

  /**
   * 构造A2AClient实例。
   * 初始化时会从指定的agentBaseUrl获取Agent Card。
   * Agent Card默认位于 `${agentBaseUrl}/.well-known/agent.json`。
   * Agent Card中的url字段将作为RPC服务端点。
   * @param agentBaseUrl A2A代理的基础URL（如 https://agent.example.com）。
   */
  constructor(agentBaseUrl: string) {
    this.agentBaseUrl = agentBaseUrl.replace(/\/$/, ""); // 移除结尾斜杠
    this.agentCardPromise = this._fetchAndCacheAgentCard();
  }

  /**
   * 从代理的well-known URI获取Agent Card，并缓存其服务端点URL。
   * 构造函数会调用此方法。
   * @returns 返回一个Promise，解析为AgentCard。
   */
  private async _fetchAndCacheAgentCard(): Promise<AgentCard> {
    const agentCardUrl = `${this.agentBaseUrl}/.well-known/agent.json`;
    try {
      const response = await fetch(agentCardUrl, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`无法从${agentCardUrl}获取Agent Card: ${response.status} ${response.statusText}`);
      }
      const agentCard: AgentCard = await response.json();
      if (!agentCard.url) {
        throw new Error("获取到的Agent Card不包含有效的服务端点url字段。");
      }
      this.serviceEndpointUrl = agentCard.url; // 缓存服务端点URL
      console.log("ENDOPINT", this.serviceEndpointUrl);
      return agentCard;
    } catch (error) {
      console.error("获取或解析Agent Card时出错:");
      // 允许Promise reject，调用方可处理异常
      throw error;
    }
  }

  /**
   * 获取Agent Card。
   * 如果传入agentBaseUrl，则从该URL获取Agent Card，否则返回构造时缓存的Agent Card。
   * @param agentBaseUrl 可选，指定代理基础URL。
   * @returns Promise，解析为AgentCard。
   */
  public async getAgentCard(agentBaseUrl?: string): Promise<AgentCard> {
    if (agentBaseUrl) {
      const specificAgentBaseUrl = agentBaseUrl.replace(/\/$/, "");
      const agentCardUrl = `${specificAgentBaseUrl}/.well-known/agent.json`;
      const response = await fetch(agentCardUrl, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`无法从${agentCardUrl}获取Agent Card: ${response.status} ${response.statusText}`);
      }
      return await response.json() as AgentCard;
    }
    // 未指定URL时，返回构造时缓存的Agent Card
    return this.agentCardPromise;
  }

  /**
   * 获取RPC服务端点URL，确保已获取Agent Card。
   * @returns Promise，解析为服务端点URL字符串。
   */
  private async _getServiceEndpoint(): Promise<string> {
    if (this.serviceEndpointUrl) {
      return this.serviceEndpointUrl;
    }
    // 如果serviceEndpointUrl未设置，说明Agent Card还未获取或获取失败
    await this.agentCardPromise;
    if (!this.serviceEndpointUrl) {
      throw new Error("Agent Card未能提供RPC端点URL，可能获取失败。");
    }
    return this.serviceEndpointUrl;
  }

  /**
   * 辅助方法：发起通用JSON-RPC POST请求。
   * @param method RPC方法名
   * @param params 方法参数
   * @returns Promise，解析为RPC响应
   */
  private async _postRpcRequest<TParams, TResponse extends (JSONRPCResult<any> | JSONRPCErrorResponse)>(
    method: string,
    params: TParams
  ): Promise<TResponse> {
    const endpoint = await this._getServiceEndpoint();
    const requestId = this.requestIdCounter++;
    const rpcRequest: JSONRPCRequest = {
      jsonrpc: "2.0",
      method,
      params: params as { [key: string]: any; }, // 强制类型转换，因TParams结构随方法不同
      id: requestId,
    };

    const httpResponse = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json", // 非流式请求期望JSON响应
      },
      body: JSON.stringify(rpcRequest),
    });

    if (!httpResponse.ok) {
      let errorBodyText = '(empty or non-JSON response)';
      try {
        errorBodyText = await httpResponse.text();
        const errorJson = JSON.parse(errorBodyText);
        // 如果body是有效的JSON-RPC错误响应，交由下方标准解析处理
        // 否则根据HTTP状态抛出错误
        if (!errorJson.jsonrpc && errorJson.error) {
          throw new Error(`RPC错误: ${errorJson.error.message} (Code: ${errorJson.error.code}, HTTP状态: ${httpResponse.status}) Data: ${JSON.stringify(errorJson.error.data)}`);
        } else if (!errorJson.jsonrpc) {
          throw new Error(`HTTP错误! 状态: ${httpResponse.status} ${httpResponse.statusText}。响应: ${errorBodyText}`);
        }
      } catch (e: any) {
        if (e.message.startsWith('RPC错误') || e.message.startsWith('HTTP错误')) throw e;
        throw new Error(`HTTP错误! 状态: ${httpResponse.status} ${httpResponse.statusText}。响应: ${errorBodyText}`);
      }
    }

    const rpcResponse = await httpResponse.json();

    if (rpcResponse.id !== requestId) {
      // 响应ID与请求ID不符，可能导致响应处理错误
      console.error(`严重: RPC响应ID不匹配。期望${requestId}，实际${rpcResponse.id}。`);
      // 可根据严格程度选择抛出异常
      // throw new Error(...)
    }

    return rpcResponse as TResponse;
  }


  /**
   * 向代理发送消息。
   * 行为（阻塞/非阻塞）和推送通知配置由params.configuration指定。
   * 可选地，params.message.contextId或params.message.taskId可指定上下文。
   * @param params 发送消息的参数，包括消息内容和配置
   * @returns Promise，解析为SendMessageResponse，可能为Message、Task或错误
   */
  public async sendMessage(params: MessageSendParams): Promise<SendMessageResponse> {
    return this._postRpcRequest<MessageSendParams, SendMessageResponse>("message/send", params);
  }

  /**
   * 向代理发送消息，并通过SSE流式返回响应。
   * 推送通知配置可在params.configuration中指定。
   * 需代理支持流式（AgentCard.capabilities.streaming为true）。
   * @param params 发送消息的参数
   * @returns AsyncGenerator，yield消息、任务、任务状态更新或产物更新
   * 发生错误时抛出异常
   */
  public async *sendMessageStream(params: MessageSendParams): AsyncGenerator<A2AStreamEventData, void, undefined> {
    const agentCard = await this.agentCardPromise; // 确保已获取Agent Card
    if (!agentCard.capabilities?.streaming) {
      throw new Error("代理不支持流式（AgentCard.capabilities.streaming不为true）。");
    }

    const endpoint = await this._getServiceEndpoint();
    const clientRequestId = this.requestIdCounter++; // 本次流请求唯一ID
    const rpcRequest: JSONRPCRequest = { // 建立流的初始JSON-RPC请求
      jsonrpc: "2.0",
      method: "message/stream",
      params: params as { [key: string]: any; },
      id: clientRequestId,
    };

    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "text/event-stream", // SSE流式响应
      },
      body: JSON.stringify(rpcRequest),
    });

    if (!response.ok) {
      // 读取错误body以获取更多信息
      let errorBody = "";
      try {
        errorBody = await response.text();
        const errorJson = JSON.parse(errorBody);
        if (errorJson.error) {
          throw new Error(`建立message/stream流时HTTP错误: ${response.status} ${response.statusText}。RPC错误: ${errorJson.error.message} (Code: ${errorJson.error.code})`);
        }
      } catch (e: any) {
        if (e.message.startsWith('建立message/stream流时HTTP错误')) throw e;
        throw new Error(`建立message/stream流时HTTP错误: ${response.status} ${response.statusText}。响应: ${errorBody || '(empty)'} `);
      }
      throw new Error(`建立message/stream流时HTTP错误: ${response.status} ${response.statusText}`);
    }
    if (!response.headers.get("Content-Type")?.startsWith("text/event-stream")) {
      // 服务端必须设置SSE内容类型
      throw new Error("SSE流响应Content-Type无效，期望'text/event-stream'.");
    }

    // 解析SSE流并yield事件
    // 每个事件的'data'字段为JSON-RPC响应
    yield* this._parseA2ASseStream<A2AStreamEventData>(response, clientRequestId);
  }

  /**
   * 设置或更新指定任务的推送通知配置。
   * 需代理支持推送通知（AgentCard.capabilities.pushNotifications为true）。
   * @param params 包含taskId和推送配置的参数
   * @returns Promise，解析为SetTaskPushNotificationConfigResponse
   */
  public async setTaskPushNotificationConfig(params: TaskPushNotificationConfig): Promise<SetTaskPushNotificationConfigResponse> {
    const agentCard = await this.agentCardPromise;
    if (!agentCard.capabilities?.pushNotifications) {
      throw new Error("代理不支持推送通知（AgentCard.capabilities.pushNotifications不为true）。");
    }
    // params结构与RPC方法要求一致
    return this._postRpcRequest<TaskPushNotificationConfig, SetTaskPushNotificationConfigResponse>(
      "tasks/pushNotificationConfig/set",
      params
    );
  }

  /**
   * 获取指定任务的推送通知配置。
   * @param params 包含taskId的参数
   * @returns Promise，解析为GetTaskPushNotificationConfigResponse
   */
  public async getTaskPushNotificationConfig(params: TaskIdParams): Promise<GetTaskPushNotificationConfigResponse> {
    // params结构与RPC方法要求一致
    return this._postRpcRequest<TaskIdParams, GetTaskPushNotificationConfigResponse>(
      "tasks/pushNotificationConfig/get",
      params
    );
  }


  /**
   * 根据ID获取任务。
   * @param params 包含taskId和可选historyLength的参数
   * @returns Promise，解析为GetTaskResponse，包含Task对象或错误
   */
  public async getTask(params: TaskQueryParams): Promise<GetTaskResponse> {
    return this._postRpcRequest<TaskQueryParams, GetTaskResponse>("tasks/get", params);
  }

  /**
   * 根据ID取消任务。
   * @param params 包含taskId的参数
   * @returns Promise，解析为CancelTaskResponse，包含更新后的Task对象或错误
   */
  public async cancelTask(params: TaskIdParams): Promise<CancelTaskResponse> {
    return this._postRpcRequest<TaskIdParams, CancelTaskResponse>("tasks/cancel", params);
  }

  /**
   * 通过SSE重新订阅任务事件流。
   * 用于之前的SSE连接中断后恢复。
   * 需代理支持流式（capabilities.streaming为true）。
   * @param params 包含taskId的参数
   * @returns AsyncGenerator，yield消息、任务、任务状态更新或产物更新
   */
  public async *resubscribeTask(params: TaskIdParams): AsyncGenerator<A2AStreamEventData, void, undefined> {
    const agentCard = await this.agentCardPromise;
    if (!agentCard.capabilities?.streaming) {
      throw new Error("代理不支持流式（tasks/resubscribe需要）。");
    }

    const endpoint = await this._getServiceEndpoint();
    const clientRequestId = this.requestIdCounter++; // 本次resubscribe请求唯一ID
    const rpcRequest: JSONRPCRequest = { // 建立流的初始JSON-RPC请求
      jsonrpc: "2.0",
      method: "tasks/resubscribe",
      params: params as { [key: string]: any; },
      id: clientRequestId,
    };

    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "text/event-stream",
      },
      body: JSON.stringify(rpcRequest),
    });

    if (!response.ok) {
      let errorBody = "";
      try {
        errorBody = await response.text();
        const errorJson = JSON.parse(errorBody);
        if (errorJson.error) {
          throw new Error(`建立tasks/resubscribe流时HTTP错误: ${response.status} ${response.statusText}。RPC错误: ${errorJson.error.message} (Code: ${errorJson.error.code})`);
        }
      } catch (e: any) {
        if (e.message.startsWith('建立tasks/resubscribe流时HTTP错误')) throw e;
        throw new Error(`建立tasks/resubscribe流时HTTP错误: ${response.status} ${response.statusText}。响应: ${errorBody || '(empty)'} `);
      }
      throw new Error(`建立tasks/resubscribe流时HTTP错误: ${response.status} ${response.statusText}`);
    }
    if (!response.headers.get("Content-Type")?.startsWith("text/event-stream")) {
      throw new Error("SSE流响应Content-Type无效，期望'text/event-stream'.");
    }

    // 事件结构与message/stream一致
    // 每个事件的'data'字段为JSON-RPC响应
    yield* this._parseA2ASseStream<A2AStreamEventData>(response, clientRequestId);
  }

  /**
   * 解析HTTP响应体为A2A SSE事件流。
   * 每个SSE事件的'data'字段应为JSON-RPC 2.0响应对象
   * @param response HTTP响应对象，body为SSE流
   * @param originalRequestId 客户端发起流的请求ID，用于校验事件ID
   * @returns AsyncGenerator，yield每个有效JSON-RPC响应的result字段
   */
  private async *_parseA2ASseStream<TStreamItem>(
    response: Response,
    originalRequestId: number | string | null
  ): AsyncGenerator<TStreamItem, void, undefined> {
    if (!response.body) {
      throw new Error("SSE响应体未定义，无法读取流。");
    }
    const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
    let buffer = ""; // 保存流中未处理的行
    let eventDataBuffer = ""; // 累积当前事件的'data:'内容

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          // 流意外结束后处理最后一条事件
          if (eventDataBuffer.trim()) {
            const result = this._processSseEventData<TStreamItem>(eventDataBuffer, originalRequestId);
            yield result;
          }
          break; // 流结束
        }

        buffer += value; // 新数据追加到buffer
        let lineEndIndex;
        // 处理buffer中的所有完整行
        while ((lineEndIndex = buffer.indexOf('\n')) >= 0) {
          const line = buffer.substring(0, lineEndIndex).trim(); // 获取并去除行首尾空白
          buffer = buffer.substring(lineEndIndex + 1); // 移除已处理行

          if (line === "") { // 空行表示事件结束
            if (eventDataBuffer) { // 有事件数据则处理
              const result = this._processSseEventData<TStreamItem>(eventDataBuffer, originalRequestId);
              yield result;
              eventDataBuffer = ""; // 重置buffer
            }
          } else if (line.startsWith("data:")) {
            eventDataBuffer += line.substring(5).trimStart() + "\n"; // 累加data内容
          } else if (line.startsWith(":")) {
            // SSE注释行，忽略
          } else if (line.includes(":")) {
            // 其他SSE字段如event:、id:、retry:，A2A规范主要关注data字段
          }
        }
      }
    } catch (error: any) {
      // 读取或解析流时出错，记录并抛出
      console.error("读取或解析SSE流出错:", error.message);
      throw error;
    } finally {
      reader.releaseLock(); // 释放reader锁
    }
  }

  /**
   * 处理单个SSE事件的数据字符串，期望为JSON-RPC响应。
   * @param jsonData SSE事件'data:'内容
   * @param originalRequestId 客户端发起流的请求ID
   * @returns 解析后的result字段
   * @throws 数据无效、结构不符、为错误响应或ID不匹配时抛出异常
   */
  private _processSseEventData<TStreamItem>(
    jsonData: string,
    originalRequestId: number | string | null
  ): TStreamItem {
    if (!jsonData.trim()) {
      throw new Error("尝试处理空的SSE事件数据。");
    }
    try {
      // SSE数据可能为多行，需整体作为JSON解析
      const sseJsonRpcResponse = JSON.parse(jsonData.replace(/\n$/, ''));

      // 类型断言为SendStreamingMessageResponse
      const a2aStreamResponse = sseJsonRpcResponse as SendStreamingMessageResponse;

      if (a2aStreamResponse.id !== originalRequestId) {
        // 按JSON-RPC规范，通知类事件可无ID，若有应匹配。A2A规范建议流事件与初始请求绑定。
        console.warn(`SSE事件JSON-RPC响应ID不匹配。客户端请求ID: ${originalRequestId}, 事件响应ID: ${a2aStreamResponse.id}.`);
        // 可根据严格程度选择抛出异常，这里仅警告
      }

      if (a2aStreamResponse.error) {
        const err = a2aStreamResponse.error as (JSONRPCError | A2AError);
        throw new Error(`SSE事件包含错误: ${err.message} (Code: ${err.code}) Data: ${JSON.stringify(err.data)}`);
      }

      // 检查result字段是否存在，成功响应必须有result
      if (!('result' in a2aStreamResponse) || typeof (a2aStreamResponse as SendStreamingMessageSuccessResponse).result === 'undefined') {
        throw new Error(`SSE事件JSON-RPC响应缺少result字段。数据: ${jsonData}`);
      }

      const successResponse = a2aStreamResponse as SendStreamingMessageSuccessResponse;
      return successResponse.result as TStreamItem;
    } catch (e: any) {
      // 捕获JSON.parse等错误
      if (e.message.startsWith("SSE事件包含错误") || e.message.startsWith("SSE事件JSON-RPC响应缺少result字段")) {
        throw e;
      }
      // 其他解析错误或结构异常
      console.error("解析SSE事件数据失败或结构异常:", jsonData, e);
      throw new Error(`解析SSE事件数据失败: \"${jsonData.substring(0, 100)}...\"。原始错误: ${e.message}`);
    }
  }
}
