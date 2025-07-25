package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;
import lombok.Getter;


@Getter
public class MemberWithAuthDto {
    private Long id;
    private String name;
    private String email;
    private boolean isAdmin;

    public MemberWithAuthDto(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.isAdmin = false;
    }

    public MemberWithAuthDto(Member member) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.isAdmin = member.isAdmin();
    }
}


