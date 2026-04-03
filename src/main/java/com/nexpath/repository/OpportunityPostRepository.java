package com.nexpath.repository;

import com.nexpath.enums.OpportunityType;
import com.nexpath.enums.PostStatus;
import com.nexpath.models.OpportunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpportunityPostRepository extends JpaRepository<OpportunityPost, Long> {
    
    Page<OpportunityPost> findByStatusInOrderByIsBoostedDescCreatedAtDesc(
            List<PostStatus> statuses, Pageable p);
    
    Page<OpportunityPost> findByStatusInAndRoleTypeOrderByIsBoostedDescCreatedAtDesc(
            List<PostStatus> statuses, OpportunityType roleType, Pageable p);

    Page<OpportunityPost> findByPosterIdOrderByCreatedAtDesc(Long posterId, Pageable p);
    
    @Query("SELECT p FROM OpportunityPost p WHERE (p.status = 'ACTIVE' OR p.status = 'BOOSTED') AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.company) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.skillsRequired) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<OpportunityPost> searchPosts(@Param("q") String query, Pageable p);
    
    List<OpportunityPost> findByStatusAndExpiresAtBefore(PostStatus status, LocalDateTime now);
    
    long countByPosterId(Long posterId);
}
