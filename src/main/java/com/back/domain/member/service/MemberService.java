package com.back.domain.member.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public Member join(String name, String password, String email) {

        Member member = Member.builder()
                .name(name)
                .password(password)
                .email(email)
                .exp(0)
                .level(1)
                .isAdmin(false)
                .apiKey(UUID.randomUUID().toString())
                .build();

        return memberRepository.save(member);

    }

    public Optional<Member> findByName(String name) {
        return memberRepository.findByName(name);
    }
}
