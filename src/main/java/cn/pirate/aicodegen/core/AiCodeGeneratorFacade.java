package cn.pirate.aicodegen.core;

import cn.hutool.json.JSONUtil;
import cn.pirate.aicodegen.ai.AiCodeGeneratorService;
import cn.pirate.aicodegen.ai.AiCodeGeneratorServiceFactory;
import cn.pirate.aicodegen.ai.model.HtmlCodeResult;
import cn.pirate.aicodegen.ai.model.MultiFileCodeResult;
import cn.pirate.aicodegen.ai.model.message.AiResponseMessage;
import cn.pirate.aicodegen.ai.model.message.ReasoningMessage;
import cn.pirate.aicodegen.ai.model.message.ToolExecutedMessage;
import cn.pirate.aicodegen.ai.model.message.ToolRequestMessage;
import cn.pirate.aicodegen.core.parser.CodeParserExecutor;
import cn.pirate.aicodegen.core.saver.CodeFileSaverExecutor;
import cn.pirate.aicodegen.exception.BusinessException;
import cn.pirate.aicodegen.exception.ErrorCode;
import cn.pirate.aicodegen.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;


    /**
     * 统一入口：根据类型生成并保存代码（同步）
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId         应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            // HTML / 多文件：TokenStream → JSON 流，附带"累积代码 + 完成时解析保存"
            case HTML -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateHtmlCodeStream(appId, userMessage);
                yield processCodeTokenStream(tokenStream, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateMultiFileCodeStream(appId, userMessage);
                yield processCodeTokenStream(tokenStream, codeGenTypeEnum, appId);
            }
            // Vue 项目：TokenStream → JSON 流（仅回调转 JSON，工具调用与异步构建由 JsonMessageStreamHandler 负责）
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processVueProjectTokenStream(tokenStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * HTML / MULTI_FILE 的 TokenStream 处理
     * <p>等价于原 processCodeStream：累积 AI 响应文本，流结束后调用解析器与保存器。</p>
     * <p>REASONING 回调产生的消息会进入流但不参与代码累积。</p>
     */
    private Flux<String> processCodeTokenStream(TokenStream tokenStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        codeBuilder.append(partialResponse);
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialThinking(partialThinking -> {
                        ReasoningMessage reasoningMessage = new ReasoningMessage(partialThinking.text());
                        sink.next(JSONUtil.toJsonStr(reasoningMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 流结束后解析并保存代码
                        try {
                            String completeCode = codeBuilder.toString();
                            Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
                        } catch (Exception e) {
                            log.error("保存失败: {}", e.getMessage());
                        }
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * VUE_PROJECT 的 TokenStream 处理
     * <p>只负责把各类回调转成 StreamMessage JSON 字符串推入 Flux；
     * 工具调用累积、对话历史保存、异步 npm 构建等业务逻辑全部下沉到 {@link cn.pirate.aicodegen.core.handler.JsonMessageStreamHandler}。</p>
     */
    private Flux<String> processVueProjectTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialThinking(partialThinking -> {
                        ReasoningMessage reasoningMessage = new ReasoningMessage(partialThinking.text());
                        sink.next(JSONUtil.toJsonStr(reasoningMessage));
                    })
                    .onPartialToolCall(partialToolCall -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(partialToolCall);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

}
