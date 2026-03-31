package com.nexpath.services;

import com.nexpath.enums.UserRole;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final CreditService creditService;
    private final ReferralService referralService;
    private final HttpServletRequest request;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String oauthId = oAuth2User.getAttribute("sub"); // Google specific
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getOauthId() == null) {
                user.setOauthProvider(registrationId);
                user.setOauthId(oauthId);
                userRepository.save(user);
            }
        } else {
            // 🔎 Check for referral code in Cookie (set by frontend before redirect)
            String referralCode = null;
            if (request.getCookies() != null) {
                referralCode = Arrays.stream(request.getCookies())
                        .filter(c -> "referralCode".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }

            user = User.builder()
                    .email(email)
                    .name(name)
                    .role(UserRole.STUDENT)
                    .active(true)
                    .emailVerified(true)
                    .oauthProvider(registrationId)
                    .oauthId(oauthId)
                    .referredByCode(referralCode) // Store source
                    .build();
            
            user = userRepository.save(user);
            
            // 💎 INIT & REWARD
            creditService.initializeCredits(user);
            
            if (referralCode != null) {
                referralService.processReferral(user, referralCode);
            }
            
            referralService.generateReferralCode(user);
            log.info("Social User Created with Referral: {}", email);
        }

        return oAuth2User;
    }
}
