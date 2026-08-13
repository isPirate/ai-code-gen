package cn.pirate.aicodegen.ai;

import cn.pirate.aicodegen.ai.model.HtmlCodeResult;
import cn.pirate.aicodegen.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成 HTML 代码（流式）
     *
     * @param appId        应用 ID（作为对话记忆 key）
     * @param userMessage  用户消息
     * @return TokenStream，可注册 onPartialThinking / onPartialResponse 等回调
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    TokenStream generateHtmlCodeStream(@MemoryId long appId, @UserMessage String userMessage);

    /**
     * 生成多文件代码（流式）
     *
     * @param appId        应用 ID（作为对话记忆 key）
     * @param userMessage  用户消息
     * @return TokenStream，可注册 onPartialThinking / onPartialResponse 等回调
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    TokenStream generateMultiFileCodeStream(@MemoryId long appId, @UserMessage String userMessage);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param appId        应用 ID（作为对话记忆 key）
     * @param userMessage  用户消息
     * @return TokenStream
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);


}
