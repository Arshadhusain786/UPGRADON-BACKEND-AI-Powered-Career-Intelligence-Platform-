package com.nexpath.services;

import com.nexpath.dtos.request.CreatePostRequest;
import com.nexpath.dtos.response.OpportunityPostResponse;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.enums.OpportunityType;
import com.nexpath.enums.PostStatus;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.models.OpportunityPost;
import com.nexpath.models.User;
import com.nexpath.repository.ConnectionRequestRepository;
import com.nexpath.repository.OpportunityPostRepository;
import com.nexpath.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OpportunityPostService {

    private final OpportunityPostRepository postRepo;
    private final ConnectionRequestRepository connRepo;
    private final UserRepository userRepo;
    private final CreditService creditService;

    public OpportunityPostResponse createPost(Long userId, CreatePostRequest req) {
        log.info("Attempting to create opportunity post for user {}: {}", userId, req.getTitle());
        
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        try {
            OpportunityPost post = OpportunityPost.builder()
                    .poster(user)
                    .title(req.getTitle())
                    .company(req.getCompany())
                    .description(req.getDescription())
                    .roleType(req.getRoleType() != null ? req.getRoleType() : OpportunityType.FULL_TIME)
                    .location(req.getLocation())
                    .skillsRequired(req.getSkillsRequired())
                    .experienceRequired(req.getExperienceRequired())
                    .maxConnections(req.getMaxConnections() != null ? req.getMaxConnections() : 50)
                    .expiresAt(req.getExpiresAt() != null ? req.getExpiresAt() : LocalDateTime.now().plusDays(30))
                    .status(PostStatus.ACTIVE)
                    .build();

            OpportunityPost saved = postRepo.save(post);
            log.info("Successfully created opportunity post ID: {}", saved.getId());
            return mapToResponse(saved, userId);
        } catch (Exception e) {
            log.error("Failed to create opportunity post for user {}. Error: {}", userId, e.getMessage(), e);
            throw new BadRequestException("Database save failed: " + e.getMessage());
        }
    }

    public Page<OpportunityPostResponse> getActivePosts(Long currentUserId, String search,
                                                       String roleType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OpportunityPost> posts;

        List<PostStatus> statuses = List.of(PostStatus.ACTIVE, PostStatus.BOOSTED);
        
        if (search != null && !search.trim().isEmpty()) {
            posts = postRepo.searchPosts(search.trim(), pageable);
        } else if (roleType != null && !roleType.trim().isEmpty()) {
            try {
                OpportunityType type = OpportunityType.valueOf(roleType.toUpperCase());
                posts = postRepo.findByStatusInAndRoleTypeOrderByIsBoostedDescCreatedAtDesc(statuses, type, pageable);
            } catch (IllegalArgumentException e) {
                posts = postRepo.findByStatusInOrderByIsBoostedDescCreatedAtDesc(statuses, pageable);
            }
        } else {
            posts = postRepo.findByStatusInOrderByIsBoostedDescCreatedAtDesc(statuses, pageable);
        }

        return posts.map(post -> mapToResponse(post, currentUserId));
    }

    public OpportunityPostResponse getPost(Long postId, Long currentUserId) {
        OpportunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new BadRequestException("Opportunity not found"));

        post.setViewCount(post.getViewCount() + 1);
        postRepo.save(post);

        return mapToResponse(post, currentUserId);
    }

    public Page<OpportunityPostResponse> getMyPosts(Long userId, int page, int size) {
        return postRepo.findByPosterIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(post -> mapToResponse(post, userId));
    }

    public OpportunityPostResponse updatePost(Long userId, Long postId, CreatePostRequest req) {
        OpportunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new BadRequestException("Opportunity not found"));

        if (!post.getPoster().getId().equals(userId)) {
            throw new BadRequestException("You do not own this post");
        }

        post.setTitle(req.getTitle());
        post.setCompany(req.getCompany());
        post.setDescription(req.getDescription());
        if (req.getRoleType() != null) post.setRoleType(req.getRoleType());
        post.setLocation(req.getLocation());
        post.setSkillsRequired(req.getSkillsRequired());
        post.setExperienceRequired(req.getExperienceRequired());
        if (req.getMaxConnections() != null) post.setMaxConnections(req.getMaxConnections());
        if (req.getExpiresAt() != null) post.setExpiresAt(req.getExpiresAt());

        return mapToResponse(postRepo.save(post), userId);
    }

    public void closePost(Long userId, Long postId) {
        OpportunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new BadRequestException("Opportunity not found"));

        if (!post.getPoster().getId().equals(userId)) {
            throw new BadRequestException("You do not own this post");
        }

        post.setStatus(PostStatus.CLOSED);
        postRepo.save(post);
    }

    public void boostPost(Long userId, Long postId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        OpportunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new BadRequestException("Opportunity not found"));

        if (!post.getPoster().getId().equals(userId)) {
            throw new BadRequestException("You do not own this post");
        }

        creditService.deductCredits(user, 3, CreditTransactionType.BOOST_POST, "Boosted opportunity post: " + post.getTitle());
        
        post.setBoosted(true);
        post.setBoostedUntil(LocalDateTime.now().plusDays(7));
        post.setStatus(PostStatus.BOOSTED);
        postRepo.save(post);
    }

    private OpportunityPostResponse mapToResponse(OpportunityPost post, Long currentUserId) {
        boolean hasRequested = connRepo.findBySeekerIdAndPostId(currentUserId, post.getId()).isPresent();
        
        User poster = post.getPoster();
        OpportunityPostResponse.PosterInfo posterInfo = null;
        
        if (poster != null) {
            posterInfo = new OpportunityPostResponse.PosterInfo(
                poster.getId(),
                poster.getName(),
                poster.getProfilePicture(),
                poster.getRole() != null ? poster.getRole().name() : "STUDENT"
            );
        }

        return OpportunityPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .company(post.getCompany())
                .description(post.getDescription())
                .roleType(post.getRoleType() != null ? post.getRoleType().name() : null)
                .location(post.getLocation())
                .skillsRequired(post.getSkillsRequired())
                .experienceRequired(post.getExperienceRequired())
                .status(post.getStatus() != null ? post.getStatus().name() : "ACTIVE")
                .isBoosted(post.isBoosted())
                .viewCount(post.getViewCount())
                .connectionCount(post.getConnectionCount())
                .maxConnections(post.getMaxConnections())
                .expiresAt(post.getExpiresAt())
                .createdAt(post.getCreatedAt())
                .hasRequested(currentUserId != null && hasRequested)
                .poster(posterInfo)
                .build();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void expireOldPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<OpportunityPost> activeExpired = postRepo.findByStatusAndExpiresAtBefore(PostStatus.ACTIVE, now);
        List<OpportunityPost> boostedExpired = postRepo.findByStatusAndExpiresAtBefore(PostStatus.BOOSTED, now);
        
        int count = 0;
        for (OpportunityPost post : activeExpired) {
            post.setStatus(PostStatus.CLOSED);
            postRepo.save(post);
            count++;
        }
        for (OpportunityPost post : boostedExpired) {
            post.setStatus(PostStatus.CLOSED);
            postRepo.save(post);
            count++;
        }

        if (count > 0) {
            log.info("Expired {} posts due to expiration date", count);
        }
    }
}
