package com.whtngml.subscription_cancel_guide.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * 업로드 한도(2MB)를 넘는 파일을 보내면 오류 화면 대신 보낸 화면으로 돌려보내고 안내 문구를 보여 준다.
 * 돌아갈 주소는 Referer에서 경로만 쓴다 (다른 사이트로 보내지 않기 위해).
 */
@ControllerAdvice
public class UploadLimitAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("uploadError", "아이콘은 200KB까지 올릴 수 있어요.");
        return "redirect:" + backPath(request);
    }

    private String backPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                boolean sameHost = uri.getHost() == null || uri.getHost().equalsIgnoreCase(request.getServerName());
                if (sameHost && uri.getRawPath() != null && uri.getRawPath().startsWith("/")) {
                    return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                }
            } catch (IllegalArgumentException ignored) {
                // 이상한 Referer는 무시하고 제보 화면으로
            }
        }
        return "/submit";
    }
}
