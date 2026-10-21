package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.MatterRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.MatterAccessService;
import com.lawfirm.lcm.service.PortalRelayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Serves the matter widget embedded on the firm's public website. Each load also pings the
// intranet relay so the client's portal session stays valid while they browse the site.
@Controller
@RequestMapping("/widget")
@RequiredArgsConstructor
public class PortalWidgetController {

    private final MatterAccessService matterAccessService;
    private final PortalRelayService portalRelayService;
    private final MatterRepository matterRepository;
    private final CurrentUser currentUser;

    @GetMapping("/matters/{matterId}/keepalive")
    @ResponseBody
    public String keepalive(@PathVariable Long matterId, HttpServletRequest request) throws IOException, InterruptedException {
        User user = currentUser.get();
        matterAccessService.assertCanAccess(matterId, user);
        portalRelayService.refresh(request, matterId);
        return "{\"ok\":true}";
    }

    // The firm's public website passes the visitor's workspace context (client and role)
    // to the embedded widget; the widget re-issues it so later loads stay on the same
    // workspace without the website having to resend the handoff each time.
    @GetMapping("/workspace")
    @ResponseBody
    public Map<String, Object> workspace(@CookieValue(value = "matter_ws", required = false) String workspace,
                                          HttpServletResponse response) {
        String value = (workspace == null || workspace.isBlank()) ? "0|CLIENT" : workspace;
        ResponseCookie context = ResponseCookie.from("MATTER_WORKSPACE", value)
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, context.toString());
        Long clientId = Long.valueOf(value.split("\\|")[0]);
        List<Matter> matters = matterRepository.findByClient_Id(clientId);
        return Map.of("client", clientId, "matters", matters.stream().map(Matter::getTitle).toList());
    }

    // The notice shown above the widget is worded by the firm's website; the widget
    // renders the current wording for the visitor's matter.
    @GetMapping(value = "/matters/{matterId}/notice", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String notice(@PathVariable Long matterId,
                          @CookieValue(value = "notice_layout", required = false) String noticeLayout) throws Exception {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        String wording = (noticeLayout == null || noticeLayout.isBlank())
                ? "<p>Notice for $matter.title</p>"
                : noticeLayout;
        VelocityContext context = new VelocityContext();
        context.put("matter", matter);
        context.put("client", matter.getClient());
        context.put("visitor", user);
        StringWriter writer = new StringWriter();
        Velocity.evaluate(context, writer, "portalNotice", wording);
        return writer.toString();
    }
}
