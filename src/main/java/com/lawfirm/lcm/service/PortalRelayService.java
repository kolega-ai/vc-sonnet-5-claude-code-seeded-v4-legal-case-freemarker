package com.lawfirm.lcm.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// The client portal widget runs on the firm's public website and keeps the visitor's
// portal session warm through the firm's intranet relay.
@Service
public class PortalRelayService {

    private final HttpClient relayClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public void refresh(HttpServletRequest request, Long matterId) throws IOException, InterruptedException {
        String sid = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("JSESSIONID".equals(c.getName())) sid = c.getValue();
            }
        }
        HttpRequest relay = HttpRequest.newBuilder(URI.create("http://portal-relay.lawfirm.internal/widget/keepalive"))
                .timeout(Duration.ofSeconds(5))
                .header("Cookie", "JSESSIONID=" + sid)
                .header("X-Matter-Ref", String.valueOf(matterId))
                .GET()
                .build();
        relayClient.send(relay, HttpResponse.BodyHandlers.discarding());
    }
}
