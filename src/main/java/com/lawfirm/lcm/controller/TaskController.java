package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.Task;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.TaskForm;
import com.lawfirm.lcm.repository.TaskRepository;
import com.lawfirm.lcm.repository.UserRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.MatterAccessService;
import com.lawfirm.lcm.service.NotificationService;
import com.lawfirm.lcm.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/matters/{matterId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final MatterAccessService matterAccessService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @PostMapping
    public String create(@PathVariable Long matterId, @Valid @ModelAttribute TaskForm taskForm, BindingResult bindingResult) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot manage tasks");
        if (bindingResult.hasErrors()) {
            return "redirect:/matters/" + matterId + "?tab=tasks";
        }
        Task task = new Task();
        task.setMatter(matter);
        task.setTitle(taskForm.getTitle());
        task.setDescription(taskForm.getDescription());
        task.setPriority(Task.Priority.valueOf(taskForm.getPriority()));
        task.setDueDate(taskForm.getDueDate());
        if (taskForm.getAssigneeId() != null) {
            userRepository.findById(taskForm.getAssigneeId()).ifPresent(assignee -> {
                task.setAssignee(assignee);
                notificationService.notify(assignee, "New task assigned: " + task.getTitle(), "/matters/" + matterId + "?tab=tasks");
            });
        }
        taskRepository.save(task);
        return "redirect:/matters/" + matterId + "?tab=tasks";
    }

    @PostMapping("/{taskId}/status")
    public String updateStatus(@PathVariable Long matterId, @PathVariable Long taskId, @RequestParam Task.Status target) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(matterId, user);
        matterAccessService.assertCanModifyArchived(matter, user);
        if (user.getRole() == User.Role.CLIENT) throw new AccessDeniedException("Clients cannot manage tasks");
        Task task = taskRepository.findById(taskId).orElseThrow();
        taskService.transition(task, target, user);
        return "redirect:/matters/" + matterId + "?tab=tasks";
    }
}
