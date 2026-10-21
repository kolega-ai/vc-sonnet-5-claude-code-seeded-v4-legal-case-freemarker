package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetentionForm {
    @Min(1)
    private int documentRetentionDays;
    @Min(1)
    private int matterRetentionDays;
    @Min(1)
    private int deadlineReminderDays;
}
