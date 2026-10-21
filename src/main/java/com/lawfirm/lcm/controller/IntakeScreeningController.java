package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.ImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Conflict screening of a referring office's intake list. Firm admins run it from the
// import/export screen before a new file is opened for the prospective client.
@Controller
@RequestMapping("/intake")
@RequiredArgsConstructor
public class IntakeScreeningController {

    private final ImportExportService importExportService;
    private final CurrentUser currentUser;

    @PostMapping("/conflict-screen")
    public String screen(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes) {
        User user = currentUser.get();
        if (user.getRole() != User.Role.FIRM_ADMIN) {
            throw new AccessDeniedException("Firm admin only");
        }
        int flagged;
        try {
            flagged = importExportService.screenIntakeList(file, user);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("importProblem", e.toString());
            return "redirect:/admin/import-export";
        }
        return "redirect:/admin/import-export?conflictsFlagged=" + flagged;
    }
}
