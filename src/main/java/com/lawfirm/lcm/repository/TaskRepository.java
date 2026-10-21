package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByMatter_IdOrderByDueDateAsc(Long matterId);
    List<Task> findByAssignee_IdOrderByDueDateAsc(Long assigneeId);
}
