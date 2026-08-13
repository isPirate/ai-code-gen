package cn.pirate.aicodegen.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.pirate.aicodegen.ai.model.message.*;
import cn.pirate.aicodegen.model.entity.User;
import cn.pirate.aicodegen.model.enums.ChatHistoryMessageTypeEnum;
import cn.pirate.aicodegen.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML / MULTI_FILE 类型的流式响应：
 * <ul>
 *   <li>累积 AI_RESPONSE 文本到对话历史</li>
 *   <li>REASONING 内容透传给前端（reasoning 类型），不入库</li>
 * </ul>
 *
 * <p>注意：HTML/MULTI_FILE 的代码累积与解析保存由 {@link cn.pirate.aicodegen.core.AiCodeGeneratorFacade} 负责，
 * 本类只关心对话历史持久化与前端渲染分流。</p>
 */
@Slf4j
@Component
public class SimpleTextStreamHandler {

    /**
     * 处理 HTML / MULTI_FILE 流
     *
     * @param originFlux         原始流（StreamMessage JSON 字符串）
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用 ID
     * @param loginUser          登录用户
     */
    public Flux<RenderedStreamItem> handle(Flux<String> originFlux,
                                           ChatHistoryService chatHistoryService,
                                           long appId, User loginUser) {
        StringBuilder chatHistoryBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> handleChunk(chunk, chatHistoryBuilder))
                .filter(item -> StrUtil.isNotEmpty(item.getData()))
                .doOnComplete(() -> {
                    // 思考内容未累积，不会污染对话历史
                    chatHistoryService.addChatMessage(appId, chatHistoryBuilder.toString(),
                            ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage,
                            ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    private RenderedStreamItem handleChunk(String chunk, StringBuilder chatHistoryBuilder) {
        StreamMessage msg = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum type = StreamMessageTypeEnum.getEnumByValue(msg.getType());
        if (type == null) {
            log.error("未知消息类型: {}", msg.getType());
            return RenderedStreamItem.of(StreamMessageTypeEnum.AI_RESPONSE, "");
        }
        switch (type) {
            case AI_RESPONSE -> {
                AiResponseMessage ai = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = ai.getData();
                chatHistoryBuilder.append(data);
                return RenderedStreamItem.of(StreamMessageTypeEnum.AI_RESPONSE, data);
            }
            case REASONING -> {
                ReasoningMessage r = JSONUtil.toBean(chunk, ReasoningMessage.class);
                // 思考内容透传前端，不入库
                return RenderedStreamItem.reasoning(r.getData());
            }
            default -> {
                log.error("SimpleTextStreamHandler 不支持的消息类型: {}", type);
                return RenderedStreamItem.of(StreamMessageTypeEnum.AI_RESPONSE, "");
            }
        }
    }
}
