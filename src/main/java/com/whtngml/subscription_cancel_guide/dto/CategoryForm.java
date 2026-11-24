package com.whtngml.subscription_cancel_guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryForm {

    private static final String SLUG_MESSAGE = "주소는 영어 소문자, 숫자, -로 30자까지 써 주세요.";

    @NotBlank(message = "카테고리 이름을 입력해 주세요.")
    @Size(max = 30, message = "카테고리 이름은 30자까지 쓸 수 있어요.")
    private String name;

    // 주소는 만든 뒤 바꾸지 않는다. 공백·대문자는 바꿔 주지 않고 거절한다.
    @NotBlank(message = "주소를 입력해 주세요.")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = SLUG_MESSAGE)
    @Size(max = 30, message = SLUG_MESSAGE)
    private String slug;
}
