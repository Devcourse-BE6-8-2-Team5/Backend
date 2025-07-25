package com.back.domain.member.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    @Transactional
    public void modify(Member member, String name, String password, String email) {
        member.setName(name);
        member.setPassword(password);
        member.setEmail(email);
        memberRepository.save(member);
    }
}
