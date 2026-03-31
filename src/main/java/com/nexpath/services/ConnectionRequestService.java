package com.nexpath.services;

import com.nexpath.dtos.request.RespondConnectionRequest;
import com.nexpath.dtos.request.SendConnectionRequest;
import com.nexpath.dtos.response.ConnectionRequestResponse;
import com.nexpath.dtos.response.JobDashboardResponse;
import com.nexpath.enums.ConnectionStatus;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.enums.PostStatus;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.exceptions.DuplicateRequestException;
import com.nexpath.exceptions.InsufficientCreditsException;
import com.nexpath.exceptions.UnauthorizedException;
import com.nexpath.models.ConnectionRequest;
import com.nexpath.models.OpportunityPost;
import com.nexpath.models.User;
import com.nexpath.models.UserCredits;
import com.nexpath.repository.ConnectionRequestRepository;
import com.nexpath.repository.OpportunityPostRepository;
import com.nexpath.repository.UserCreditsRepository;
import com.nexpath.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConnectionRequestService {

    private final ConnectionRequestRepository connRepo;
    private final OpportunityPostRepository postRepo;
    private final UserRepository userRepo;
    private final UserCreditsRepository userCreditsRepo;
    private final CreditService creditService;

    public ConnectionRequestResponse sendRequest(Long seekerId, SendConnectionRequest req) {
        User seeker = userRepo.findById(seekerId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        OpportunityPost post = postRepo.findById(req.getPostId())
                .orElseThrow(() -> new BadRequestException("Opportunity not found"));

        if (!post.getStatus().equals(PostStatus.ACTIVE) && !post.getStatus().equals(PostStatus.BOOSTED)) {
            throw new BadRequestException("Opportunity is no longer active");
        }

        if (post.getPoster().getId().equals(seekerId)) {
            throw new BadRequestException("You cannot connect to your own post");
        }

        java.util.Optional<ConnectionRequest> existing = connRepo.findBySeekerIdAndPostId(seekerId, post.getId());
        if (existing.isPresent()) {
            log.info("Returning existing connection request for seeker {} and post {}", seekerId, post.getId());
            return mapToResponse(existing.get());
        }

        if (post.getConnectionCount() >= post.getMaxConnections()) {
            throw new BadRequestException("This opportunity has reached its maximum connection limit");
        }

        UserCredits uc = userCreditsRepo.findByUser(seeker)
                .orElseGet(() -> creditService.initializeCredits(seeker));

        int freeConn = (uc.getFreeConnectionsThisWeek() != null) ? uc.getFreeConnectionsThisWeek() : 0;
        boolean isFree = freeConn > 0;
        boolean creditLocked = false;

        if (isFree) {
            uc.setFreeConnectionsThisWeek(freeConn - 1);
            userCreditsRepo.save(uc);
        } else {
            // Strict Wall: No fallback to standard credits
            throw new BadRequestException("LIMIT_EXCEEDED: Weekly free connection limit reached.");
        }

        ConnectionRequest conn = ConnectionRequest.builder()
                .seeker(seeker)
                .post(post)
                .status(ConnectionStatus.PENDING)
                .message(req.getMessage())
                .creditLocked(creditLocked)
                .isFreeRequest(isFree)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        ConnectionRequest saved = connRepo.save(conn);
        
        post.setConnectionCount(post.getConnectionCount() + 1);
        postRepo.save(post);

        return mapToResponse(saved);
    }

    public ConnectionRequestResponse respondToRequest(Long posterId, Long connectionId,
                                                      RespondConnectionRequest req) {
        ConnectionRequest conn = connRepo.findById(connectionId)
                .orElseThrow(() -> new BadRequestException("Connection request not found"));

        if (!conn.getPost().getPoster().getId().equals(posterId)) {
            throw new UnauthorizedException("You do not own the post associated with this request");
        }

        if (conn.getStatus() != ConnectionStatus.PENDING) {
            throw new BadRequestException("Request has already been processed: " + conn.getStatus());
        }

        if (req.getAction() != ConnectionStatus.ACCEPTED && req.getAction() != ConnectionStatus.REJECTED) {
            throw new BadRequestException("Response must be ACCEPTED or REJECTED");
        }

        if (req.getAction() == ConnectionStatus.ACCEPTED) {
            conn.setStatus(ConnectionStatus.ACCEPTED);
            if (conn.isCreditLocked()) {
                UserCredits uc = userCreditsRepo.findByUser(conn.getSeeker()).get();
                int currentLocked = (uc.getLockedCredits() != null) ? uc.getLockedCredits() : 0;
                uc.setLockedCredits(Math.max(0, currentLocked - 1));
                userCreditsRepo.save(uc);
                creditService.logTransaction(conn.getSeeker(), CreditTransactionType.CONNECTION_DEDUCTED, 0,
                        "Credit deducted — connection accepted for: " + conn.getPost().getTitle(), connectionId.toString());
            }
        } else {
            conn.setStatus(ConnectionStatus.REJECTED);
            conn.setPosterNote(req.getPosterNote());
            if (conn.isCreditLocked()) {
                refundCredit(conn.getSeeker(), connectionId, conn.getPost().getTitle(), "declined");
            }
        }

        return mapToResponse(connRepo.save(conn));
    }

    public void cancelRequest(Long seekerId, Long connectionId) {
        ConnectionRequest conn = connRepo.findById(connectionId)
                .orElseThrow(() -> new BadRequestException("Connection request not found"));

        if (!conn.getSeeker().getId().equals(seekerId)) {
            throw new UnauthorizedException("You did not send this request");
        }

        if (conn.getStatus() != ConnectionStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be cancelled");
        }

        conn.setStatus(ConnectionStatus.CANCELLED);
        if (conn.isCreditLocked()) {
            refundCredit(conn.getSeeker(), connectionId, conn.getPost().getTitle(), "cancelled");
        }
        connRepo.save(conn);
    }

    public ConnectionRequestResponse markAsHired(Long posterId, Long connectionId) {
        ConnectionRequest conn = connRepo.findById(connectionId)
                .orElseThrow(() -> new BadRequestException("Connection request not found"));

        if (!conn.getPost().getPoster().getId().equals(posterId)) {
            throw new UnauthorizedException("You do not own the post associated with this request");
        }

        if (conn.getStatus() != ConnectionStatus.ACCEPTED) {
            throw new BadRequestException("Connection must be ACCEPTED before marking as HIRED");
        }

        conn.setStatus(ConnectionStatus.HIRED);
        conn.setHiredAt(LocalDateTime.now());
        
        User poster = conn.getPost().getPoster();
        creditService.addCredits(poster, 5, CreditTransactionType.HIRE_REWARD,
                "Platform reward — top contributor for hiring " + conn.getSeeker().getName(), connectionId.toString());

        return mapToResponse(connRepo.save(conn));
    }

    public Page<ConnectionRequestResponse> getMySentRequests(Long seekerId, int page, int size) {
        return connRepo.findBySeekerIdOrderByCreatedAtDesc(seekerId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    public Page<ConnectionRequestResponse> getRequestsForMyPosts(Long posterId, int page, int size) {
        return connRepo.findByPostPosterIdOrderByCreatedAtDesc(posterId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    public JobDashboardResponse getJobDashboard(Long userId) {
        UserCredits uc = userCreditsRepo.findByUserId(userId).orElse(null);
        return new JobDashboardResponse(
                postRepo.countByPosterId(userId),
                connRepo.countByPostPosterId(userId),
                connRepo.countBySeekerId(userId),
                connRepo.countBySeekerIdAndStatus(userId, ConnectionStatus.PENDING),
                connRepo.countBySeekerIdAndStatus(userId, ConnectionStatus.ACCEPTED),
                (uc != null && uc.getFreeConnectionsThisWeek() != null) ? uc.getFreeConnectionsThisWeek() : 3,
                (uc != null && uc.getLockedCredits() != null) ? uc.getLockedCredits() : 0,
                (uc != null && uc.getTotalCredits() != null) ? uc.getTotalCredits() : 0
        );
    }

    private void refundCredit(User seeker, Long connectionId, String postTitle, String reason) {
        UserCredits uc = userCreditsRepo.findByUser(seeker).get();
        int currentLocked = (uc.getLockedCredits() != null) ? uc.getLockedCredits() : 0;
        int currentTotal = (uc.getTotalCredits() != null) ? uc.getTotalCredits() : 0;
        int currentEarned = (uc.getTotalEarned() != null) ? uc.getTotalEarned() : 0;
        
        uc.setLockedCredits(Math.max(0, currentLocked - 1));
        uc.setTotalCredits(currentTotal + 1);
        uc.setTotalEarned(currentEarned + 1);
        userCreditsRepo.save(uc);
        creditService.logTransaction(seeker, CreditTransactionType.CONNECTION_REFUNDED, 1,
                "Credit refunded — connection " + reason + " for: " + postTitle, connectionId.toString());
    }

    private ConnectionRequestResponse mapToResponse(ConnectionRequest conn) {
        ConnectionRequestResponse.SeekerInfo seekerInfo = new ConnectionRequestResponse.SeekerInfo(
                conn.getSeeker().getId(),
                conn.getSeeker().getName(),
                conn.getSeeker().getEmail(),
                conn.getSeeker().getProfilePicture(),
                conn.getSeeker().getRole().name()
        );

        ConnectionRequestResponse.PostSummary postSummary = new ConnectionRequestResponse.PostSummary(
                conn.getPost().getId(),
                conn.getPost().getTitle(),
                conn.getPost().getCompany()
        );

        return ConnectionRequestResponse.builder()
                .id(conn.getId())
                .status(conn.getStatus().name())
                .message(conn.getMessage())
                .creditLocked(conn.isCreditLocked())
                .isFreeRequest(conn.isFreeRequest())
                .seekerNote(conn.getSeekerNote())
                .posterNote(conn.getPosterNote())
                .expiresAt(conn.getExpiresAt())
                .createdAt(conn.getCreatedAt())
                .hiredAt(conn.getHiredAt())
                .seeker(seekerInfo)
                .post(postSummary)
                .build();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void expireOldRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<ConnectionRequest> expired = connRepo.findByStatusAndExpiresAtBefore(ConnectionStatus.PENDING, now);
        
        for (ConnectionRequest conn : expired) {
            conn.setStatus(ConnectionStatus.EXPIRED);
            if (conn.isCreditLocked()) {
                refundCredit(conn.getSeeker(), conn.getId(), conn.getPost().getTitle(), "expired");
            }
            connRepo.save(conn);
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} connection requests", expired.size());
        }
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void resetWeeklyFreeConnections() {
        LocalDate today = LocalDate.now();
        List<UserCredits> allCredits = userCreditsRepo.findAll();
        int count = 0;
        for (UserCredits uc : allCredits) {
            uc.setFreeConnectionsThisWeek(3);
            uc.setLastWeeklyReset(today);
            userCreditsRepo.save(uc);
            count++;
        }
        log.info("Reset weekly free connections for {} users", count);
    }
}
