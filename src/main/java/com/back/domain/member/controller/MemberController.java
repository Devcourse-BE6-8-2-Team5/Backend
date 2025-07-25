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

    // 요청할때 (이름,비밀번호보냄)
    record LoginReqBody(@NotBlank String name, @NotBlank String password) {}
    //응답할때 (MemberDto, apiKey, accessToken)
    record LoginResBody(MemberDto member, String apiKey, String accessToken) {}

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "회원 로그인", description = "로그인 성공 시 APIKey와 AccessToken을 반환합니다. 쿠키로도 반환됩니다.")
    public RsData<LoginResBody> login(@RequestBody @Valid LoginReqBody reqBody) {

        Member member = memberService.findByName(reqBody.name()).orElseThrow(
                () -> new ServiceException(401,"잘못된 아이디입니다.")
        );

        if(!member.getPassword().equals(reqBody.password())) {
            throw new ServiceException(401,"비밀번호가 일치하지 않습니다.");
        }

        //나중에 인증,인가(시큐리티에서)
        //String accessToken = memberService.genAccessToken(member);

        //임시로 accessToken을 생성 , 시큐리티 부분에서는 지우고 진짜 생성하세요
        String accessToken = "";

        //rq.addCookie("accessToken",accessToken);
        //rq.addCookie("apiKey",member.getApiKey());

        return new RsData<> (
                200,
                "%s님 환영합니다.".formatted(member.getName()),
                new LoginResBody(
                        new MemberDto(member),
                        member.getApiKey(),
                        accessToken
                )
        );

    }

}