package com.whtngml.subscription_cancel_guide.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// MockMvc는 오류 화면으로 넘기지 않으므로 실제 서버를 띄워 확인한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorPagesTest {

    @LocalServerPort
    int port;

    @Test
    void nativeOversizedSubmissionShowsUploadMessage() throws Exception {
        URI formUri = URI.create("http://localhost:" + port + "/submit");
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        var form = client.send(HttpRequest.newBuilder(formUri).GET().build(), HttpResponse.BodyHandlers.ofString());
        var token = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(form.body());
        assertTrue(token.find());

        String boundary = "test-screenshot-boundary";
        String prefix = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"_csrf\"\r\n\r\n"
                + token.group(1) + "\r\n--" + boundary
                + "\r\nContent-Disposition: form-data; name=\"screenshots\"; filename=\"large.png\""
                + "\r\nContent-Type: image/png\r\n\r\n";
        var body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(prefix, StandardCharsets.UTF_8),
                HttpRequest.BodyPublishers.ofByteArray(new byte[5 * 1024 * 1024 + 1]),
                HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));
        var response = client.send(HttpRequest.newBuilder(formUri)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Referer", formUri.toString()).POST(body).build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(302, response.statusCode());
        URI redirect = formUri.resolve(response.headers().firstValue("Location").orElseThrow());
        assertEquals(formUri, redirect);
        var result = client.send(HttpRequest.newBuilder(redirect).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(result.body().contains("캡처는 한 장에 5MB, 제보 전체는 25MB까지"));
    }

    @Test
    void unknownGuideShowsFriendly404() throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/guide/no-such-service"))
                        .header("Accept", "text/html").build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("찾는 화면이 없어요"));
        assertTrue(res.body().contains("action=\"/search\""));
        assertFalse(res.body().contains("Whitelabel"));
    }
}
