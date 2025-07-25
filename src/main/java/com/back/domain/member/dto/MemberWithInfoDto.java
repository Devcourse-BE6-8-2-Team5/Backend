package com.back.domain.member.dto;

import com.back.domain.member.entity.Member;
import lombok.Getter;


//경험치,레벨까지 포함한 DTO
@Getter
public class MemberWithInfoDto {
    private Long id;
    private String name;
    private String email;
    private int exp;
    private int level;
    private boolean isAdmin;


    public MemberWithInfoDto(Member member) {
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.exp = member.getExp();
        this.level = member.getLevel();
        this.isAdmin = member.isAdmin();
    }
}
