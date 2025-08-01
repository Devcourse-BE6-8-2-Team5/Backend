package com.back.domain.member.member.dto;


import lombok.Getter;

@Getter
public class MemberWithRankDto {

    private String name; //이름
    private String email; //이메일
    private int exp; //경험치
    private int level; //레벨


    public MemberWithRankDto(String name, String email, int exp, int level) {
        this.name = name;
        this.email = email;
        this.exp = exp;
        this.level = level;
    }
}


