package cn.pirate.aicodegen.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流处理器输出的渲染项
 * <ul>
 *   <li>{@link StreamMessageTypeEnum#REASONING} → 前端 SSE event: thinking</li>
 *   <li>其它类型 → 前端 SSE 默认 message 事件</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RenderedStreamItem {

    private StreamMessageTypeEnum type;

    private String data;

    public static RenderedStreamItem of(StreamMessageTypeEnum type, String data) {
        return new RenderedStreamItem(type, data);
    }

    public static RenderedStreamItem reasoning(String data) {
        return new RenderedStreamItem(StreamMessageTypeEnum.REASONING, data);
    }
}
