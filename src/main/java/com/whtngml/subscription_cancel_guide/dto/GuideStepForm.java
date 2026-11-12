package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.GuideStep;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuideStepForm {

    @NotNull(message = "단계 번호를 입력해 주세요.")
    @Min(value = 1, message = "단계 번호는 1부터 시작해요.")
    @Max(value = 50, message = "단계 번호는 50까지 쓸 수 있어요.")
    private Integer stepOrder;

    @NotBlank(message = "화면 이름을 입력해 주세요.")
    @Size(max = 50, message = "화면 이름은 50자까지 쓸 수 있어요.")
    private String screenName;

    @NotBlank(message = "누를 버튼 이름을 입력해 주세요.")
    @Size(max = 50, message = "버튼 이름은 50자까지 쓸 수 있어요.")
    private String targetLabel;

    @Pattern(regexp = "^(top-left|top-right|middle|bottom)?$", message = "위치를 목록에서 골라 주세요.")
    private String position;

    @Size(max = 500, message = "설명은 500자까지 쓸 수 있어요.")
    private String description;

    public static GuideStepForm from(GuideStep step) {
        GuideStepForm form = new GuideStepForm();
        form.setStepOrder(step.getStepOrder());
        form.setScreenName(step.getScreenName());
        form.setTargetLabel(step.getTargetLabel());
        form.setPosition(step.getPosition());
        form.setDescription(step.getDescription());
        return form;
    }
}
