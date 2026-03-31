package com.nexpath.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_credits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "total_credits", nullable = false)
    private Integer totalCredits = 50;

    @Builder.Default
    @Column(name = "free_today_remaining", nullable = false)
    private Integer freeTodayRemaining = 10;

    @Builder.Default
    @Column(name = "last_refill_date")
    private LocalDate lastRefillDate = LocalDate.now();

    @Builder.Default
    @Column(name = "total_earned", nullable = false)
    private Integer totalEarned = 50;

    @Builder.Default
    @Column(name = "total_spent", nullable = false)
    private Integer totalSpent = 0;

    @Builder.Default
    @Column(name = "locked_credits")
    private Integer lockedCredits = 0;

    @Builder.Default
    @Column(name = "free_connections_this_week")
    private Integer freeConnectionsThisWeek = 3;

    @Builder.Default
    @Column(name = "last_weekly_reset")
    private LocalDate lastWeeklyReset = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
