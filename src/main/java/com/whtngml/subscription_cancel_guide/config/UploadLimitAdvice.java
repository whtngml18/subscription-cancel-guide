package com.whtngml.subscription_cancel_guide.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * 멀티파트 한도를 넘기면 보낸 화면으로 돌려보내고 안내 문구를 보여 준다.
 * 돌아갈 주소는 Referer에서 경로만 쓴다 (다른 사이트로 보내지 않기 위해).
 */
@ControllerAdvice
public class UploadLimitAdvice {
    static final String SUBMISSION_UPLOAD_MESSAGE =
            "캡처는 한 장에 5MB, 제보 전체는 25MB까지 보낼 수 있어요. 캡처를 다시 골라 주세요.";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        boolean admin = request.getRequestURI().startsWith(request.getContextPath() + "/admin/");
        String message = admin ? "아이콘은 200KB까지 올릴 수 있어요."
                : SUBMISSION_UPLOAD_MESSAGE;
        redirectAttributes.addFlashAttribute("uploadError", message);
        return "redirect:" + backPath(request, admin);
    }

    static String backPath(HttpServletRequest request, boolean admin) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                boolean sameHost = uri.getHost() == null || uri.getHost().equalsIgnoreCase(request.getServerName());
                String path = uri.getRawPath();
                String contextPath = request.getContextPath();
                boolean formPath = path != null && (admin ? path.startsWith(contextPath + "/admin/")
                        : path.equals(contextPath + "/submit"));
                if (sameHost && formPath) {
                    return path.substring(contextPath.length()) + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                }
            } catch (IllegalArgumentException ignored) {
                // 이상한 Referer는 무시하고 제보 화면으로
            }
        }
        return admin ? "/admin/guides" : "/submit";
    }
}
