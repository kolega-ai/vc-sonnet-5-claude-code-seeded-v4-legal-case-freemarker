package com.lawfirm.lcm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "deadline")
@Getter
@Setter
@NoArgsConstructor
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "matter_id")
    private Matter matter;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "responsible_id")
    private User responsible;

    @Column(nullable = false)
    private boolean completed = false;

    private Instant reminderSentAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
