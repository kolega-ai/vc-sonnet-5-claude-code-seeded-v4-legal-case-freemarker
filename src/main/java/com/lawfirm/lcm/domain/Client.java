package com.lawfirm.lcm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    public enum ClientType { PERSON, ORGANIZATION }
    public enum Status { ACTIVE, INACTIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType type;

    private String email;
    private String phone;

    @Column(length = 500)
    private String address;

    // Taxpayer / national identification number collected at intake so the conflict
    // check can be run against the court registries.
    @Column(length = 64)
    private String nationalId;

    @Column(length = 1000)
    private String conflictCheckNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @OneToOne
    @JoinColumn(name = "portal_user_id")
    private User portalUser;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
