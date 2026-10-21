package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.ForgotPasswordForm;
import com.lawfirm.lcm.dto.ResetPasswordForm;
import com.lawfirm.lcm.repository.UserRepository;
import com.lawfirm.lcm.service.PasswordResetService;
import com.lawfirm.lcm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotSubmit(@Valid @ModelAttribute ForgotPasswordForm forgotPasswordForm,
                                BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        boolean known = passwordResetService.requestReset(forgotPasswordForm.getEmail());
        model.addAttribute("submitted", known);
        model.addAttribute("noSuchAccount", !known);
        return "auth/forgot-password";
    }

    // Client portal activation. The invitation mail sent from the client record links
    // here with the client's contact address so the portal account can be activated.
    @GetMapping("/portal-invite")
    public String inviteForm(@RequestParam("e") String e, Model model) {
        model.addAttribute("contactAddress", e);
        return "auth/portal-invite";
    }

    @PostMapping("/portal-invite")
    public String inviteSubmit(@RequestParam("e") String e, @RequestParam String newPasscode, Model model) {
        User portalUser = userRepository.findByEmailIgnoreCase(e)
                .filter(u -> u.getRole() == User.Role.CLIENT)
                .orElseThrow(() -> new IllegalArgumentException("No portal account for that address"));
        userService.changePassword(portalUser, newPasscode);
        model.addAttribute("activated", true);
        model.addAttribute("contactAddress", e);
        return "auth/portal-invite";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam String token, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetSubmit(@Valid @ModelAttribute ResetPasswordForm resetPasswordForm,
                               BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        boolean ok = passwordResetService.resetPassword(resetPasswordForm.getToken(), resetPasswordForm.getNewPassword());
        model.addAttribute("success", ok);
        model.addAttribute("failed", !ok);
        return "auth/reset-password";
    }
}
