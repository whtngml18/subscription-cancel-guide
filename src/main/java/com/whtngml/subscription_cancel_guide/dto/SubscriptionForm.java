package com.whtngml.subscription_cancel_guide.dto;

import com.whtngml.subscription_cancel_guide.domain.CancelChannel;
import com.whtngml.subscription_cancel_guide.domain.Difficulty;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
public class SubscriptionForm {

    @NotNull(message = "카테고리를 골라 주세요.")
    private Long categoryId;

    @NotBlank(message = "서비스 이름을 입력해 주세요.")
    @Size(max = 50, message = "서비스 이름은 50자까지 쓸 수 있어요.")
    private String name;

    @NotBlank(message = "slug를 입력해 주세요.")
    @Size(max = 50, message = "slug는 50자까지 쓸 수 있어요.")
    @Pattern(regexp = "^[a-z0-9-]*$", message = "slug는 영어 소문자, 숫자, -만 쓸 수 있어요.")
    private String slug;

    @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "브랜드 색은 #E50914처럼 써 주세요.")
    private String brandColor;

    @Size(max = 1000, message = "환불 안내는 1000자까지 쓸 수 있어요.")
    private String refundInfo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate verifiedAt;

    // ---------- 해지 정보 ----------

    private Set<CancelChannel> channels = EnumSet.noneOf(CancelChannel.class);

    @Size(max = 500, message = "주의할 점은 500자까지 쓸 수 있어요.")
    private String caution;

    private Difficulty difficulty;

    @Size(max = 500, message = "공식 해지 링크는 500자까지 쓸 수 있어요.")
    @Pattern(regexp = "^(https?://[!-~]+)?$", message = "공식 해지 링크는 https://로 시작하는 주소로 써 주세요.")
    private String officialCancelUrl;

    @Min(value = 1, message = "예상 시간은 1분에서 120분 사이로 써 주세요.")
    @Max(value = 120, message = "예상 시간은 1분에서 120분 사이로 써 주세요.")
    private Integer estimatedMinutes;

    // 새 아이콘 (고르지 않으면 지금 아이콘을 그대로 둔다)
    private MultipartFile iconFile;

    public static SubscriptionForm from(Subscription subscription) {
        SubscriptionForm form = new SubscriptionForm();
        form.setCategoryId(subscription.getCategory().getId());
        form.setName(subscription.getName());
        form.setSlug(subscription.getSlug());
        form.setBrandColor(subscription.getBrandColor());
        form.setRefundInfo(subscription.getRefundInfo());
        form.setVerifiedAt(subscription.getVerifiedAt());
        form.setChannels(subscription.getChannels().isEmpty()
                ? EnumSet.noneOf(CancelChannel.class) : EnumSet.copyOf(subscription.getChannels()));
        form.setCaution(subscription.getCaution());
        form.setDifficulty(subscription.getDifficulty());
        form.setOfficialCancelUrl(subscription.getOfficialCancelUrl());
        form.setEstimatedMinutes(subscription.getEstimatedMinutes());
        return form;
    }
}
