package com.back.global.rq;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Rq {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final MemberService memberService;

    public Member getActor() {
        return Optional.ofNullable(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof SecurityUser)
                .map(principal -> {
                    SecurityUser securityUser = (SecurityUser) principal;
                    // 권한에서 ROLE_ 접두사를 제거하여 ADMIN/USER로 변환
                    String role = securityUser.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .filter(auth -> auth.startsWith("ROLE_"))
                            .map(auth -> auth.substring(5)) // "ROLE_" 제거
                            .findFirst()
                            .orElse("USER");
                    return new Member(securityUser.getId(), securityUser.getEmail(), securityUser.getName(), role);
                })
                .orElse(null);
    }

    public String getHeader(String name, String defaultValue) {
        return Optional
                .ofNullable(req.getHeader(name))
                .filter(headerValue -> !headerValue.isBlank())
                .orElse(defaultValue);
    }

    public void setHeader(String name, String value) {
        if (value == null) value = "";

        if (value.isBlank()) {
            req.removeAttribute(name);
        } else {
            resp.setHeader(name, value);
        }
    }

    public String getCookieValue(String name, String defaultValue) {
        return Optional
                .ofNullable(req.getCookies())
                .flatMap(
                        cookies ->
                                Arrays.stream(cookies)
                                        .filter(cookie -> cookie.getName().equals(name))
                                        .map(Cookie::getValue)
                                        .filter(value -> !value.isBlank())
                                        .findFirst()
                )
                .orElse(defaultValue);
    }

    // 로컬 개발 환경용 쿠키 설정 (Same-Domain)
    public void setCookie(String name, String value) {
        if (value == null) value = "";

        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        // 로컬에서는 HttpOnly만 설정
        cookie.setHttpOnly(true);

        if (value.isBlank()) cookie.setMaxAge(0);
        else cookie.setMaxAge(60 * 60 * 24 * 365);

        resp.addCookie(cookie);
    }

    // 운영 환경용 쿠키 설정 (Cross-Domain)
    public void setCrossDomainCookie(String name, String value, int maxAgeInSeconds) {
        if (value == null) value = "";

        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // JavaScript 접근 방지
        cookie.setSecure(true); // HTTPS 강제
        cookie.setAttribute("SameSite", "None"); // 크로스 도메인 허용

        if (value.isBlank()) cookie.setMaxAge(0);
        else cookie.setMaxAge(maxAgeInSeconds);

        resp.addCookie(cookie);
    }


    public void deleteCookie(String name) {
        setCookie(name, null);
    }

    @SneakyThrows
    public void sendRedirect(String url) {
        resp.sendRedirect(url);
    }

    public Member getActorFromDb() {
        Member actor = getActor();
        if(actor == null) return null;
        return memberService.findById(actor.getId()).orElse(null);
    }
}
