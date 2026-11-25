package com.whtngml.subscription_cancel_guide.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

final class SubmissionMultipartLimitFilter extends OncePerRequestFilter {
    private final MultipartResolver resolver;

    SubmissionMultipartLimitFilter(MultipartResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod())
                || !("/submit".equals(path) || "/submit/preview".equals(path))
                || !resolver.isMultipart(request)
                || WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        MultipartHttpServletRequest multipart;
        try {
            // CSRF reads a form parameter before MVC. Resolve multipart once so size errors get a form response.
            multipart = resolver.resolveMultipart(request);
        } catch (MaxUploadSizeExceededException e) {
            String location = request.getContextPath() + UploadLimitAdvice.backPath(request, false);
            var target = UriComponentsBuilder.fromUriString(location).build();
            FlashMap flash = new FlashMap();
            flash.setTargetRequestPath(target.getPath());
            flash.addTargetRequestParams(target.getQueryParams());
            flash.put("uploadError", UploadLimitAdvice.SUBMISSION_UPLOAD_MESSAGE);
            new SessionFlashMapManager().saveOutputFlashMap(flash, request, response);
            response.sendRedirect(response.encodeRedirectURL(location));
            return;
        } catch (MultipartException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            chain.doFilter(multipart, response);
        } finally {
            resolver.cleanupMultipart(multipart);
        }
    }
}
