package com.whtngml.subscription_cancel_guide.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// MockMvc는 오류 화면으로 넘기지 않으므로 실제 서버를 띄워 확인한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorPagesTest {

    @LocalServerPort
    int port;

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
