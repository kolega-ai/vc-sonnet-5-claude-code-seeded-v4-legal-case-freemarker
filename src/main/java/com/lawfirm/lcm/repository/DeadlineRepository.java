package com.lawfirm.lcm.repository;

import com.lawfirm.lcm.domain.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {
    List<Deadline> findByMatter_IdOrderByDueDateAsc(Long matterId);
    List<Deadline> findByResponsible_IdOrderByDueDateAsc(Long responsibleId);
    List<Deadline> findByCompletedFalseAndDueDateLessThanEqualAndReminderSentAtIsNull(LocalDate cutoff);
    List<Deadline> findAllByOrderByDueDateAsc();
}
