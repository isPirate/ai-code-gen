package cn.pirate.aicodegen.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 深度思考消息（reasoning_content 片段）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ReasoningMessage extends StreamMessage {

    private String data;

    public ReasoningMessage(String data) {
        super(StreamMessageTypeEnum.REASONING.getValue());
        this.data = data;
    }
}
