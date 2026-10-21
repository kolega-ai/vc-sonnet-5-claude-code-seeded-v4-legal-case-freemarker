package com.lawfirm.lcm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    public enum Category { PLEADING, CONTRACT, CORRESPONDENCE, EVIDENCE, FILING, INTERNAL_MEMO, OTHER }
    public enum PrivilegeLevel { NONE, ATTORNEY_CLIENT, WORK_PRODUCT }
    public enum Visibility { INTERNAL_ONLY, PRIVILEGED, CLIENT_SHARED, PUBLIC_FILING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "matter_id")
    private Matter matter;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrivilegeLevel privilegeLevel = PrivilegeLevel.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.INTERNAL_ONLY;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    private List<DocumentVersion> versions = new ArrayList<>();
}
