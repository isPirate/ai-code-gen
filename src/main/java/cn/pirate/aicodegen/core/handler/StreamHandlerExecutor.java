package cn.pirate.aicodegen.core.handler;

import cn.pirate.aicodegen.ai.model.message.RenderedStreamItem;
import cn.pirate.aicodegen.model.entity.User;
import cn.pirate.aicodegen.model.enums.CodeGenTypeEnum;
import cn.pirate.aicodegen.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 根据代码生成类型创建合适的流处理器：
 * 1. HTML / MULTI_FILE → SimpleTextStreamHandler（累积代码 + 完成时解析保存）
 * 2. VUE_PROJECT       → JsonMessageStreamHandler（工具调用累积 + 异步 npm build）
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;
    @Resource
    private SimpleTextStreamHandler simpleTextStreamHandler;


    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流（StreamMessage JSON 字符串）
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param codeGenType        代码生成类型
     * @return 处理后的渲染项流
     */
    public Flux<RenderedStreamItem> doExecute(Flux<String> originFlux,
                                              ChatHistoryService chatHistoryService,
                                              long appId, User loginUser, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT ->
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE ->
                    simpleTextStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
