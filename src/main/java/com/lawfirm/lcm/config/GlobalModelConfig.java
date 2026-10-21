package com.lawfirm.lcm.config;

import com.lawfirm.lcm.security.AppUserPrincipal;
import com.lawfirm.lcm.service.AssetVersionService;
import com.lawfirm.lcm.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelConfig {

    private final AssetVersionService assetVersionService;
    private final NotificationService notificationService;

    @ModelAttribute("assetVersion")
    public int assetVersion() {
        return assetVersionService.getVersion();
    }

    @ModelAttribute("csrfToken")
    public CsrfToken csrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute("_csrf");
    }

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return notificationService.unreadCount(principal.getId());
        }
        return 0L;
    }
}
