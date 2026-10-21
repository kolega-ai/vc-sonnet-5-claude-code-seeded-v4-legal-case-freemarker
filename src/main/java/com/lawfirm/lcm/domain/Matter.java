package com.lawfirm.lcm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "matter")
@Getter
@Setter
@NoArgsConstructor
public class Matter {

    public enum Status { INTAKE, CONFLICT_REVIEW, OPEN, ON_HOLD, CLOSED, ARCHIVED }
    public enum Sensitivity { LOW, MEDIUM, HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "practice_area_id")
    private PracticeArea practiceArea;

    @ManyToOne
    @JoinColumn(name = "assigned_attorney_id")
    private User assignedAttorney;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.INTAKE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sensitivity sensitivityLevel = Sensitivity.LOW;

    private LocalDate openedDate;
    private LocalDate closedDate;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
