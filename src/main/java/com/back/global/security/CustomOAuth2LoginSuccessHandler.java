package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final Rq rq;
    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 로그인된 사용자 정보 가져오기
        Member actor = rq.getActorFromDb();

        // Access Token 생성
        String accessToken = memberService.genAccessToken(actor);
        // Refresh Token으로 apiKey를 사용
        String refreshToken = actor.getApiKey();

        // 기본 리다이렉트 URL (프론트엔드 주소)
        String redirectUrl = "https://news-ox.vercel.app/";

        // OAuth2 인증 요청 시 저장했던 state 값을 읽어옴
        String state = request.getParameter("state");

        if (state != null && !state.isBlank()) {
            try {
                // state 값은 프론트엔드 주소가 Base64로 인코딩된 것
                String decodedUrl = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
                redirectUrl = decodedUrl;
            } catch (IllegalArgumentException e) {
                // Base64 디코딩에 실패하면 기본 URL을 사용합니다.
            }
        }

        // 최종 리다이렉트 URL에 토큰들을 쿼리 파라미터로 추가
        String finalUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        // 최종적으로 프론트엔드로 리다이렉트
        response.sendRedirect(finalUrl);
    }
}
