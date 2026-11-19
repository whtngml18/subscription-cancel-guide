package com.whtngml.subscription_cancel_guide.ai;

// AI 변환에 실패했을 때. 메시지는 사용자에게 그대로 보여 줘도 되는 문장으로 쓴다.
public class StepParseException extends RuntimeException {

    public StepParseException(String message) {
        super(message);
    }

    public StepParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
