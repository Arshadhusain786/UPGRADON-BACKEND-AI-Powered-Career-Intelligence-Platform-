package com.nexpath.repository;

import com.nexpath.models.Referral;
import com.nexpath.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    org.springframework.data.domain.Page<Referral> findByReferrerOrderByCreatedAtDesc(User referrer, org.springframework.data.domain.Pageable pageable);
    
    List<Referral> findByReferrerOrderByCreatedAtDesc(User referrer);

    long countByReferrer(User referrer);

    @Query("SELECT SUM(r.rewardCredits) FROM Referral r WHERE r.referrer = :referrer")
    Integer sumRewardCreditsByReferrer(@Param("referrer") User referrer);
}
