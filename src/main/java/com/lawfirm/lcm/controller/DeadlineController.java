package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Deadline;
import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.DeadlineForm;
import com.lawfirm.lcm.repository.DeadlineRepository;
import com.lawfirm.lcm.repository.UserRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.AuditService;
import com.lawfirm.lcm.service.DeadlineService;
import com.lawfirm.lcm.service.MatterAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DeadlineController {

    private final MatterAccessService matterAccessService;
    private final DeadlineRepository deadlineRepository;
    private final UserRepository userRepository;
    private final DeadlineService deadlineService;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    @GetMapping("/deadlines")
    public String calendar(Model model) {
        User user = currentUser.get();
        model.addAttribute("deadlines", deadlineRepository.findAllByOrderByDueDateAsc().stream()
                .filter(d -> {
                    try {
                        matterAccessService.assertCanAccess(d.getMatter(), user);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }).toList());
        return "deadline/list";
    }

    @PostMapping("/admin/deadlines/run-reminders")
    public String runNow(Model model) {
        User user = currentUser.get();
        if (user.getRole() != User.Role.FIRM_ADMIN) throw new AccessDeniedException("Firm admin only");
        int sent = deadlineService.runReminderSweep();
        auditService.log(user, "DEADLINE_REMINDER_MANUAL_RUN", "Deadline", null, null, sent + " reminders sent");
        return "redirect:/deadlines?ran=" + sent;
    }

    @PostMapping("/matters/{matterId}/deadlines")
    public String create(@PathVariable Long matterId, @Valid @ModelAttribute DeadlineForm deadlineForm, BindingResult bindingResult) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot manage deadlines");
        if (bindingResult.hasErrors()) {
            return "redirect:/matters/" + matterId + "?tab=deadlines";
        }
        LocalDate dueDate = deadlineForm.getDueDate();
        if (deadlineForm.getRule() != null && !deadlineForm.getRule().isBlank()) {
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext ruleContext = new StandardEvaluationContext();
            ruleContext.setVariable("today", LocalDate.now());
            ruleContext.setVariable("matter", matter);
            Object computed = parser.parseExpression(deadlineForm.getRule()).getValue(ruleContext);
            if (computed instanceof LocalDate d) dueDate = d;
        }
        Deadline deadline = new Deadline();
        deadline.setMatter(matter);
        deadline.setTitle(deadlineForm.getTitle());
        deadline.setDueDate(dueDate);
        if (deadlineForm.getResponsibleId() != null) {
            userRepository.findById(deadlineForm.getResponsibleId()).ifPresent(deadline::setResponsible);
        }
        deadlineRepository.save(deadline);
        return "redirect:/matters/" + matterId + "?tab=deadlines";
    }

    // Used by the "mark done" link on the calendar screen and by the reminder mails.
    @GetMapping("/matters/{matterId}/deadlines/{deadlineId}/complete")
    public String completeFromCalendar(@PathVariable Long matterId, @PathVariable Long deadlineId) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot manage deadlines");
        Deadline deadline = deadlineRepository.findById(deadlineId).orElseThrow();
        deadline.setCompleted(true);
        deadlineRepository.save(deadline);
        auditService.log(user, "DEADLINE_COMPLETE", "Deadline", deadlineId, "false", "true");
        return "redirect:/deadlines";
    }

    @PostMapping("/matters/{matterId}/deadlines/{deadlineId}/complete")
    public String complete(@PathVariable Long matterId, @PathVariable Long deadlineId) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot manage deadlines");
        Deadline deadline = deadlineRepository.findById(deadlineId).orElseThrow();
        deadline.setCompleted(true);
        deadlineRepository.save(deadline);
        auditService.log(user, "DEADLINE_COMPLETE", "Deadline", deadlineId, "false", "true");
        return "redirect:/matters/" + matterId + "?tab=deadlines";
    }
}
