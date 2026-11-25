package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class SubmissionForm {

    @NotBlank(message = "서비스 이름을 입력해 주세요.")
    @Size(max = 50, message = "서비스 이름은 50자까지 쓸 수 있어요.")
    private String serviceName;

    @NotBlank(message = "해지 방법을 적어 주세요.")
    @Size(min = 10, max = 1000, message = "해지 방법은 10자 이상 1000자 이하로 적어 주세요.")
    private String rawText;

    // AI 미리보기 결과. 화면의 숨은 칸에 담아 두었다가 제출할 때 서버에서 다시 검사한다.
    private String aiStepsJson;

    private List<MultipartFile> screenshots = List.of();

    // ---------- 더 알려 주실 수 있나요? (모두 선택) ----------

    private Set<CancelChannel> channels = EnumSet.noneOf(CancelChannel.class);

    @Size(max = 500, message = "주의할 점은 500자까지 쓸 수 있어요.")
    private String caution;

    private MultipartFile iconFile;

    // 제보 화면 "예시 넣어 보기" 틀의 빈칸. 이게 남아 있으면 틀만 보낸 것으로 본다.
    public static final List<String> TEMPLATE_TOKENS = List.of("(어디쯤)", "(버튼 이름)", "(해지 버튼 이름)");

    public boolean hasTemplateLeft() {
        return rawText != null && TEMPLATE_TOKENS.stream().anyMatch(rawText::contains);
    }
}
