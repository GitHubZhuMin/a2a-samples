import logging

from typing import Any
from uuid import uuid4

import httpx

from a2a.client import A2ACardResolver, A2AClient
from a2a.types import (
    AgentCard,
    MessageSendParams,
    SendMessageRequest,
    SendStreamingMessageRequest,
)


async def main() -> None:
    PUBLIC_AGENT_CARD_PATH = '/.well-known/agent.json'
    EXTENDED_AGENT_CARD_PATH = '/agent/authenticatedExtendedCard'

    # Configure logging to show INFO level messages
    # 配置日志记录，显示INFO级别的信息
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)  # Get a logger instance
    # 获取一个日志记录器实例

    # --8<-- [start:A2ACardResolver]
    # Initialize base URL for the agent service
    # 初始化代理服务的基础URL
    base_url = 'http://localhost:9999'

    async with httpx.AsyncClient() as httpx_client:
        # Initialize A2ACardResolver
        # 初始化A2ACardResolver，用于解析Agent Card
        resolver = A2ACardResolver(
            httpx_client=httpx_client,
            base_url=base_url,
            # agent_card_path uses default, extended_agent_card_path also uses default
            # agent_card_path和extended_agent_card_path都使用默认值
        )
        # --8<-- [end:A2ACardResolver]

        # Fetch Public Agent Card and Initialize Client
        # 获取公共Agent Card并初始化客户端
        final_agent_card_to_use: AgentCard | None = None

        try:
            logger.info(
                f'Attempting to fetch public agent card from: {base_url}{PUBLIC_AGENT_CARD_PATH}'
            )
            # 尝试从指定路径获取公共Agent Card
            _public_card = (
                await resolver.get_agent_card()
            )  # Fetches from default public path
            # 从默认公共路径获取Agent Card
            logger.info('Successfully fetched public agent card:')
            # 成功获取到公共Agent Card
            logger.info(
                _public_card.model_dump_json(indent=2, exclude_none=True)
            )
            final_agent_card_to_use = _public_card
            logger.info(
                '\nUsing PUBLIC agent card for client initialization (default).'
            )
            # 使用公共Agent Card进行客户端初始化（默认）

            if _public_card.supportsAuthenticatedExtendedCard:
                try:
                    logger.info(
                        f'\nPublic card supports authenticated extended card. Attempting to fetch from: {base_url}{EXTENDED_AGENT_CARD_PATH}'
                    )
                    # 公共卡支持认证扩展卡，尝试获取扩展卡
                    auth_headers_dict = {
                        'Authorization': 'Bearer dummy-token-for-extended-card'
                    }
                    _extended_card = await resolver.get_agent_card(
                        relative_card_path=EXTENDED_AGENT_CARD_PATH,
                        http_kwargs={'headers': auth_headers_dict},
                    )
                    logger.info(
                        'Successfully fetched authenticated extended agent card:'
                    )
                    # 成功获取认证扩展Agent Card
                    logger.info(
                        _extended_card.model_dump_json(
                            indent=2, exclude_none=True
                        )
                    )
                    final_agent_card_to_use = (
                        _extended_card  # Update to use the extended card
                        # 更新为使用扩展卡
                    )
                    logger.info(
                        '\nUsing AUTHENTICATED EXTENDED agent card for client initialization.'
                    )
                    # 使用认证扩展Agent Card进行客户端初始化
                except Exception as e_extended:
                    logger.warning(
                        f'Failed to fetch extended agent card: {e_extended}. Will proceed with public card.',
                        exc_info=True,
                    )
                    # 获取扩展Agent Card失败，将继续使用公共卡
            elif (
                _public_card
            ):  # supportsAuthenticatedExtendedCard is False or None
                # supportsAuthenticatedExtendedCard为False或None
                logger.info(
                    '\nPublic card does not indicate support for an extended card. Using public card.'
                )
                # 公共卡不支持扩展卡，使用公共卡

        except Exception as e:
            logger.error(
                f'Critical error fetching public agent card: {e}', exc_info=True
            )
            # 获取公共Agent Card时发生严重错误
            raise RuntimeError(
                'Failed to fetch the public agent card. Cannot continue.'
            ) from e
            # 获取公共Agent Card失败，无法继续

        # --8<-- [start:send_message]
        # Initialize the A2AClient with the selected agent card
        # 使用选定的Agent Card初始化A2AClient
        client = A2AClient(
            httpx_client=httpx_client, agent_card=final_agent_card_to_use
        )
        logger.info('A2AClient initialized.')
        # A2AClient已初始化

        send_message_payload: dict[str, Any] = {
            'message': {
                'role': 'user',
                'parts': [
                    {'kind': 'text', 'text': 'how much is 10 USD in INR?'}
                ],
                'messageId': uuid4().hex,
            },
        }
        # 构造发送消息的payload
        request = SendMessageRequest(
            id=str(uuid4()), params=MessageSendParams(**send_message_payload)
        )
        # 创建SendMessageRequest对象

        response = await client.send_message(request)
        # 发送消息并等待响应
        print(response.model_dump(mode='json', exclude_none=True))
        # 打印响应内容（以JSON格式，不包含None值）
        # --8<-- [end:send_message]

        # --8<-- [start:send_message_streaming]
        # Prepare a streaming message request
        # 准备流式消息请求
        streaming_request = SendStreamingMessageRequest(
            id=str(uuid4()), params=MessageSendParams(**send_message_payload)
        )

        stream_response = client.send_message_streaming(streaming_request)
        # 发送流式消息请求，获取响应流

        async for chunk in stream_response:
            print(chunk.model_dump(mode='json', exclude_none=True))
            # 逐块打印流式响应内容（以JSON格式，不包含None值）
        # --8<-- [end:send_message_streaming]


if __name__ == '__main__':
    import asyncio

    asyncio.run(main())
    # 运行主异步函数
