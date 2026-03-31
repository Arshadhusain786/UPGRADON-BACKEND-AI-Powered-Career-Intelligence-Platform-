package com.nexpath.repository;

import com.nexpath.enums.ConnectionStatus;
import com.nexpath.models.ConnectionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {
    
    Optional<ConnectionRequest> findBySeekerIdAndPostId(Long seekerId, Long postId);
    
    Page<ConnectionRequest> findBySeekerIdOrderByCreatedAtDesc(Long seekerId, Pageable p);
    
    Page<ConnectionRequest> findByPostPosterIdOrderByCreatedAtDesc(Long posterId, Pageable p);
    
    Page<ConnectionRequest> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable p);
    
    List<ConnectionRequest> findByStatusAndExpiresAtBefore(ConnectionStatus status, LocalDateTime now);
    
    long countBySeekerIdAndStatus(Long seekerId, ConnectionStatus status);
    
    long countByPostPosterId(Long posterId);
    
    long countBySeekerId(Long seekerId);
}
