package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Deadline;
import com.lawfirm.lcm.domain.RetentionSetting;
import com.lawfirm.lcm.repository.DeadlineRepository;
import com.lawfirm.lcm.repository.RetentionSettingRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;
    private final RetentionSettingRepository retentionSettingRepository;
    private final OutboxService outboxService;
    private final NotificationService notificationService;

    @Value("${app.upload-dir}")
    private String filingDir;

    public int runReminderSweep() {
        int reminderDays = retentionSettingRepository.findById(1L)
                .map(RetentionSetting::getDeadlineReminderDays)
                .orElse(3);
        LocalDate cutoff = LocalDate.now().plusDays(reminderDays);
        List<Deadline> due = deadlineRepository.findByCompletedFalseAndDueDateLessThanEqualAndReminderSentAtIsNull(cutoff);
        for (Deadline d : due) {
            if (d.getResponsible() != null) {
                outboxService.send(d.getResponsible().getEmail(),
                        "Deadline reminder: " + d.getTitle(),
                        renderNotice(d),
                        "DEADLINE_REMINDER");
                notificationService.notify(d.getResponsible(),
                        "Deadline approaching: " + d.getTitle() + " (" + d.getDueDate() + ")",
                        "/matters/" + d.getMatter().getId() + "#deadlines");
            }
            d.setReminderSentAt(Instant.now());
            deadlineRepository.save(d);
        }
        return due.size();
    }

    private String renderNotice(Deadline d) {
        Path noticeTemplate = Paths.get(filingDir).resolve("notice-template.ftl");
        if (!Files.exists(noticeTemplate)) {
            return "The deadline '" + d.getTitle() + "' for matter '" + d.getMatter().getTitle() + "' is due on " + d.getDueDate() + ".";
        }
        try {
            String source = Files.readString(noticeTemplate, StandardCharsets.UTF_8);
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
            Template notice = new Template("notice", source, cfg);
            Map<String, Object> data = new HashMap<>();
            data.put("deadline", d);
            data.put("matter", d.getMatter());
            Writer out = new StringWriter();
            notice.process(data, out);
            return out.toString();
        } catch (Exception ex) {
            return "The deadline '" + d.getTitle() + "' is due on " + d.getDueDate() + ".";
        }
    }
}
