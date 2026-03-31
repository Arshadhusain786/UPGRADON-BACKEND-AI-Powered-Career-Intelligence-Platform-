package com.nexpath.models;

import com.nexpath.enums.OpportunityType;
import com.nexpath.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "opportunity_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private User poster;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 150)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private OpportunityType roleType;

    @Column(length = 150)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String skillsRequired;

    @Column(length = 100)
    private String experienceRequired;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @Builder.Default
    private boolean isBoosted = false;

    private LocalDateTime boostedUntil;

    @Builder.Default
    private Integer viewCount = 0;

    @Builder.Default
    private Integer connectionCount = 0;

    @Builder.Default
    private Integer maxConnections = 50;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
