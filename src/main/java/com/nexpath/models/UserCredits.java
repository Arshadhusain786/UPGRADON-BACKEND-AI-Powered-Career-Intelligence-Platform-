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

    @Column(name = "total_credits", nullable = false)
    private int totalCredits = 50;

    @Column(name = "free_today_remaining", nullable = false)
    private int freeTodayRemaining = 10;

    @Column(name = "last_refill_date", nullable = false)
    private LocalDate lastRefillDate;

    @Column(name = "total_earned", nullable = false)
    private int totalEarned = 50;

    @Column(name = "total_spent", nullable = false)
    private int totalSpent = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
