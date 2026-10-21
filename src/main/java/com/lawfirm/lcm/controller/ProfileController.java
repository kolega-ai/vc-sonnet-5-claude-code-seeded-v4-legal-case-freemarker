package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.PasswordChangeForm;
import com.lawfirm.lcm.dto.ProfileForm;
import com.lawfirm.lcm.repository.UserRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String view(Model model) {
        User user = currentUser.get();
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        model.addAttribute("user", user);
        model.addAttribute("profileForm", form);
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        return "profile/view";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute ProfileForm profileForm, BindingResult bindingResult, Model model) {
        User user = currentUser.get();
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
            return "profile/view";
        }
        user.setFullName(profileForm.getFullName());
        userRepository.save(user);
        return "redirect:/profile?updated";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute PasswordChangeForm passwordChangeForm, BindingResult bindingResult, Model model) {
        User user = currentUser.get();
        if (!bindingResult.hasErrors() && !passwordEncoder.matches(passwordChangeForm.getCurrentPassword(), user.getPasswordHash())) {
            bindingResult.rejectValue("currentPassword", "invalid", "Current password is incorrect");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            model.addAttribute("profileForm", form);
            return "profile/view";
        }
        userService.changePassword(user, passwordChangeForm.getNewPassword());
        return "redirect:/profile?passwordChanged";
    }
}
