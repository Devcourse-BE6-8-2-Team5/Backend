package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    public Member join(String name, String password, String email) {
        String encodedPassword = passwordEncoder.encode(password);
        Member member = Member.builder()
                .name(name)
                .password(encodedPassword)
                .email(email)
                .exp(0)
                .level(1)
                .role("USER") //기본 권한 USER. 관리자면 "ADMIN"으로 설정하시면 됩니다
                .apiKey(UUID.randomUUID().toString())
                .build();

        return memberRepository.save(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    public Map<String, Object> payload(String accessToken) {
        return authTokenService.payload(accessToken);
    }

    public Optional<Member> findByApiKey(String apiKey) {
        return memberRepository.findByApiKey(apiKey);
    }

    public String genAccessToken(Member member) {
        return authTokenService.genAccessToken(member);
    }

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Transactional
    public void modify(Member member, String name,  String password, String email) {
        member.setName(name);
        member.setPassword(passwordEncoder.encode(password));
        member.setEmail(email);
        memberRepository.save(member);
    }

    public void withdraw(Member member) {
        if(member.isAdmin())
            throw new ServiceException(403,"관리자는 탈퇴할 수 없습니다.");

        memberRepository.delete(member);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }
}
