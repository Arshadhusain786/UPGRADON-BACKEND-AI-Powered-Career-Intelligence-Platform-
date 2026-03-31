package com.nexpath.models;

import com.nexpath.enums.ConnectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "connection_requests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"seeker_id", "post_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private User seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private OpportunityPost post;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private boolean creditLocked = false;

    @Builder.Default
    private boolean isFreeRequest = false;

    @Column(length = 500)
    private String seekerNote;

    @Column(length = 500)
    private String posterNote;

    private LocalDateTime hiredAt;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
