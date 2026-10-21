package com.lawfirm.lcm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// Singleton row (id = 1) holding firm-wide retention policy. Firm-admin only.
@Entity
@Table(name = "retention_setting")
@Getter
@Setter
@NoArgsConstructor
public class RetentionSetting {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private int documentRetentionDays = 2555;

    @Column(nullable = false)
    private int matterRetentionDays = 3650;

    @Column(nullable = false)
    private int deadlineReminderDays = 3;
}
