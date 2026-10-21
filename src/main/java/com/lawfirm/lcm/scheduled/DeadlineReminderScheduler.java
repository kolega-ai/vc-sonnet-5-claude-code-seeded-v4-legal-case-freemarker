package com.lawfirm.lcm.scheduled;

import com.lawfirm.lcm.service.DeadlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeadlineReminderScheduler {

    private final DeadlineService deadlineService;

    @Scheduled(fixedDelayString = "${app.reminder.fixed-delay-ms:300000}", initialDelay = 15000)
    public void runReminderSweep() {
        int count = deadlineService.runReminderSweep();
        if (count > 0) {
            log.info("Deadline reminder sweep sent {} reminder(s)", count);
        }
    }
}
