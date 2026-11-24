package com.whtngml.subscription_cancel_guide.controller;

import com.whtngml.subscription_cancel_guide.domain.Category;
import com.whtngml.subscription_cancel_guide.domain.Subscription;
import com.whtngml.subscription_cancel_guide.repository.CategoryRepository;
import com.whtngml.subscription_cancel_guide.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// 검색엔진용 파일: sitemap.xml(사이트 주소 목록), robots.txt(수집 규칙)
@RestController
@RequiredArgsConstructor
public class SeoController {

    private final CategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, "/", null);
        for (Category category : categoryRepository.findAll(Sort.by("id"))) {
            if (subscriptionRepository.countByCategory(category) > 0) {   // 빈 카테고리는 404라 넣지 않는다
                appendUrl(xml, "/category/" + category.getSlug(), null);
            }
        }
        for (Subscription subscription : subscriptionRepository.findAll(Sort.by("name"))) {
            appendUrl(xml, "/guide/" + subscription.getSlug(), subscription.getVerifiedAt());
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /submit/done

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    private void appendUrl(StringBuilder xml, String path, LocalDate lastModified) {
        xml.append("  <url><loc>").append(escapeXml(baseUrl + path)).append("</loc>");
        if (lastModified != null) {
            xml.append("<lastmod>").append(lastModified).append("</lastmod>");
        }
        xml.append("</url>\n");
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
