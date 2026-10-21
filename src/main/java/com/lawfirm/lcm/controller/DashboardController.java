package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.repository.*;
import com.lawfirm.lcm.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CurrentUser currentUser;
    private final MatterRepository matterRepository;
    private final TaskRepository taskRepository;
    private final DeadlineRepository deadlineRepository;
    private final DocumentRepository documentRepository;
    private final ClientMessageRepository clientMessageRepository;
    private final ClientRepository clientRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = currentUser.get();
        model.addAttribute("user", user);
        switch (user.getRole()) {
            case FIRM_ADMIN -> {
                model.addAttribute("totalClients", clientRepository.count());
                model.addAttribute("totalMatters", matterRepository.count());
                model.addAttribute("pendingConflictReview", matterRepository.findByStatus(Matter.Status.CONFLICT_REVIEW));
                model.addAttribute("recentAudit", auditLogRepository.findTop200ByOrderByTimestampDesc().stream().limit(15).toList());
                return "dashboard/admin";
            }
            case ATTORNEY -> {
                List<Matter> matters = matterRepository.findByAssignedAttorney_Id(user.getId());
                model.addAttribute("matters", matters);
                model.addAttribute("tasks", taskRepository.findByAssignee_IdOrderByDueDateAsc(user.getId()));
                model.addAttribute("deadlines", deadlineRepository.findByResponsible_IdOrderByDueDateAsc(user.getId()));
                return "dashboard/attorney";
            }
            case PARALEGAL -> {
                model.addAttribute("tasks", taskRepository.findByAssignee_IdOrderByDueDateAsc(user.getId()));
                model.addAttribute("deadlines", deadlineRepository.findByResponsible_IdOrderByDueDateAsc(user.getId()));
                return "dashboard/paralegal";
            }
            case CLIENT -> {
                Client client = clientRepository.findByPortalUser_Id(user.getId()).orElse(null);
                List<Matter> matters = client == null ? List.of() : matterRepository.findByClient_Id(client.getId());
                model.addAttribute("matters", matters);
                return "dashboard/client";
            }
        }
        return "redirect:/login";
    }
}
