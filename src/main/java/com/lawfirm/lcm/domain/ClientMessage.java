package com.lawfirm.lcm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "client_message")
@Getter
@Setter
@NoArgsConstructor
public class ClientMessage {

    public enum Status { DRAFT, APPROVED, SENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "matter_id")
    private Matter matter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(length = 500)
    private String attachmentName;

    @Column(length = 500)
    private String attachmentStoredName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
