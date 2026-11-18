package com.whtngml.subscription_cancel_guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionForm {

    @NotBlank(message = "서비스 이름을 입력해 주세요.")
    @Size(max = 50, message = "서비스 이름은 50자까지 쓸 수 있어요.")
    private String serviceName;

    @NotBlank(message = "해지 방법을 적어 주세요.")
    @Size(min = 10, max = 1000, message = "해지 방법은 10자 이상 1000자 이하로 적어 주세요.")
    private String rawText;
}
