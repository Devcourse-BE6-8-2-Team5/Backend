package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberService memberService;

    // OAuth2 로그인 성공 시 자동 호출
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String oauthUserId = oAuth2User.getName(); // 고유 ID
        String providerTypeCode = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        String nickname = "";
        String profileImgUrl = "";
        String email = oauthUserId + "@" + providerTypeCode.toLowerCase() + ".com";

        switch (providerTypeCode) {
            case "KAKAO":
                Map<String, Object> attributes = oAuth2User.getAttributes();
                Map attributesProperties = (Map) attributes.get("properties");

                nickname = (String) attributesProperties.get("nickname");
                profileImgUrl = (String) attributesProperties.get("profile_image");
                break;
            case "GOOGLE":
                nickname = (String) oAuth2User.getAttributes().get("name");
                profileImgUrl = (String) oAuth2User.getAttributes().get("picture");
                break;
        }

        Member member = memberService.modifyOrJoin(oauthUserId, email, nickname, profileImgUrl).data();

        // securityContext
        return new SecurityUser(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPassword(),
                member.getAuthorities()
        );
    }
}
