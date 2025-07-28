package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.global.standard.util.Ut;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final Rq rq;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.debug("Processing request for " + request.getRequestURI());

        try { // 필터 체인에서 다음 필터로 요청을 전달
            work(request, response, filterChain);
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(rsData.code());
            String jsonResponse = Ut.json.toString(rsData);
            if (jsonResponse == null) {
                jsonResponse = "{\"resultCode\":\"" + rsData.code() + "\",\"msg\":\"" + rsData.message() + "\"}";
            }
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            throw e;
        }
    }

    private void work(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 아래 API 요청이 아니라면 인증 패스 (추후 진짜 api로 변경 예정)
        if (
                !uri.startsWith("/상세퀴즈페이지") &&
                        !uri.startsWith("/오늘의퀴즈페이지") &&
                        !uri.startsWith("/ox퀴즈상세페이지") &&
                        !uri.startsWith("/상세퀴즈제출") &&
                        !uri.startsWith("/오늘의퀴즈제출") &&
                        !uri.startsWith("/ox퀴즈제출") &&
                        !uri.startsWith("/api/members/info") &&
                        !uri.startsWith("/마이페이지내정보수정") &&
                        !uri.startsWith("/마이페이지회원탈퇴") &&
                        !uri.startsWith("/관리자페이지") &&
                        !uri.startsWith("/관리자페이지뉴스삭제")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // 아래 API 요청이라면 인증하지 않고 패스
        if (List.of(
                "/api/members/login",
                "/api/members/logout",
                "/api/members/join"
        ).contains(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey;
        String accessToken;

        String headerAuthorization = rq.getHeader("Authorization", "");

        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw new ServiceException(401, "Authorization 헤더가 Bearer 형식이 아닙니다.");

            String[] headerAuthorizationBits = headerAuthorization.split(" ", 3);

            apiKey = headerAuthorizationBits[1];
            accessToken = headerAuthorizationBits.length == 3 ? headerAuthorizationBits[2] : "";
        } else {
            apiKey = rq.getCookieValue("apiKey", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        logger.debug("apiKey : " + apiKey);
        logger.debug("accessToken : " + accessToken);

        boolean isApiKeyExists = !apiKey.isBlank();
        boolean isAccessTokenExists = !accessToken.isBlank();

        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response);
            return;
        }

        Member member = null;
        boolean isAccessTokenValid = false;

        // accessToken이 있으면 우선 검증
        if (isAccessTokenExists) {
            Map<String, Object> payload = memberService.payload(accessToken);

            if (payload != null) {
                long id = (long) payload.get("id");
                String email = (String) payload.get("email");
                String name = (String) payload.get("name");
                String role = (String) payload.get("role");
                member = new Member(id, email, name, role);
                isAccessTokenValid = true;
            }
        }

        // accessToken이 없으면, apiKey로 member 조회
        if (member == null) {
            member = memberService
                    .findByApiKey(apiKey)
                    .orElseThrow(() -> new ServiceException(401, "API 키가 유효하지 않습니다."));
        }

        // accessToken이 만료됐으면 새로 발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            String actorAccessToken = memberService.genAccessToken(member);

            rq.setCookie("accessToken", actorAccessToken);
            rq.setHeader("Authorization", actorAccessToken);
        }

        // SecurityContext에 인증 정보 저장
        UserDetails user = new SecurityUser(
                member.getId(),
                member.getEmail(),
                member.getName(),
                "",
                member.getAuthorities()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities()
        );
        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}