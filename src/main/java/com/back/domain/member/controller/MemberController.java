package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDto;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MemberController", description = "회원 관련 컨트롤러 엔드 포인트")
public class MemberController {
    private final MemberService memberService;

    record JoinReqBody (
            @NotBlank
            @Size(min =5, max= 25)
            String name,

            @NotBlank
            @Size(min = 10, max=50)
            String password,

            @NotBlank
            @Email
            String email
    )  {}

    @PostMapping(value = "/join", produces = "application/json;charset=UTF-8")
    @Transactional
    @Operation(summary = "회원 가입")
    public RsData<MemberDto> join(@RequestBody @Valid JoinReqBody reqBody) {

        memberService.findByName(reqBody.name())
                .ifPresent(_ -> {
                    throw new ServiceException(409, "이미 사용중인 아이디입니다.");
                });

        Member member = memberService.join(reqBody.name(),reqBody.password(), reqBody.email());

        return new RsData<> (
                201,
                "%s님 환영합니다. 회원 가입이 완료되었습니다.".formatted(member.getName()),
                new MemberDto(member)
        );

    }

}