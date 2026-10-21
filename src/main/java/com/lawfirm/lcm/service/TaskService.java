package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Task;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AuditService auditService;

    private static final java.util.Map<Task.Status, Set<Task.Status>> ALLOWED = java.util.Map.of(
            Task.Status.OPEN, EnumSet.of(Task.Status.IN_PROGRESS, Task.Status.BLOCKED, Task.Status.CANCELLED),
            Task.Status.IN_PROGRESS, EnumSet.of(Task.Status.COMPLETED, Task.Status.BLOCKED, Task.Status.CANCELLED),
            Task.Status.BLOCKED, EnumSet.of(Task.Status.OPEN, Task.Status.IN_PROGRESS, Task.Status.CANCELLED),
            Task.Status.COMPLETED, EnumSet.noneOf(Task.Status.class),
            Task.Status.CANCELLED, EnumSet.noneOf(Task.Status.class)
    );

    public void transition(Task task, Task.Status target, User actor) {
        Set<Task.Status> allowed = ALLOWED.getOrDefault(task.getStatus(), EnumSet.noneOf(Task.Status.class));
        if (!allowed.contains(target)) {
            throw new IllegalStateException("Cannot transition task from " + task.getStatus() + " to " + target);
        }
        String before = task.getStatus().name();
        task.setStatus(target);
        taskRepository.save(task);
        auditService.log(actor, "TASK_STATUS_CHANGE", "Task", task.getId(), before, target.name());
    }
}
