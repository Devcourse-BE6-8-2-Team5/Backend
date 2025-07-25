package com.back.domain.member.controller;

import com.back.domain.member.dto.MemberDto;
import com.back.domain.member.dto.MemberWithAuthDto;
import com.back.domain.member.dto.MemberWithInfoDto;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MemberController", description = "회원 관련 컨트롤러 엔드 포인트")
public class MemberController {
    private final MemberService memberService;

    record JoinReqBody(
            @NotBlank
            @Size(min = 5, max = 25)
            String name,

            @NotBlank
            @Size(min = 10, max = 50)
            String password,

            @NotBlank
            @Email
            String email
    ) {
    }

    @PostMapping(value = "/join", produces = "application/json;charset=UTF-8")
    @Transactional
    @Operation(summary = "회원 가입")
    public RsData<MemberDto> join(@RequestBody @Valid JoinReqBody reqBody) {

        memberService.findByName(reqBody.name())
                .ifPresent(_ -> {
                    throw new ServiceException(409, "이미 사용중인 아이디입니다.");
                });

        Member member = memberService.join(reqBody.name(), reqBody.password(), reqBody.email());

        return new RsData<>(
                201,
                "%s님 환영합니다. 회원 가입이 완료되었습니다.".formatted(member.getName()),
                new MemberDto(member)
        );

    }

    // 요청할때 (이름,비밀번호보냄)
    record LoginReqBody(@NotBlank String name, @NotBlank String password) {
    }

    //응답할때 (MemberWithAuthDto, apiKey, accessToken)
    record LoginResBody(MemberWithAuthDto member, String apiKey, String accessToken) {
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "회원 로그인", description = "로그인 성공 시 APIKey와 AccessToken을 반환합니다. 쿠키로도 반환됩니다.")
    public RsData<LoginResBody> login(@RequestBody @Valid LoginReqBody reqBody) {

        Member member = memberService.findByName(reqBody.name()).orElseThrow(
                () -> new ServiceException(401, "잘못된 아이디입니다.")
        );

        if (!member.getPassword().equals(reqBody.password())) {
            throw new ServiceException(401, "비밀번호가 일치하지 않습니다.");
        }

        //나중에 인증,인가(시큐리티에서)
        //String accessToken = memberService.genAccessToken(member);

        //임시로 accessToken을 생성 , 시큐리티 부분에서는 지우고 진짜 생성하세요
        String accessToken = "";

        //rq.addCookie("accessToken",accessToken);
        //rq.addCookie("apiKey",member.getApiKey());

        return new RsData<>(
                200,
                "%s님 환영합니다.".formatted(member.getName()),
                new LoginResBody(
                        new MemberWithAuthDto(member),
                        member.getApiKey(),
                        accessToken
                )
        );

    }

    @Operation(summary = "회원 로그아웃", description = "로그아웃 시 쿠키 삭제")
    @DeleteMapping("/logout")
    public RsData<Void> logout() {
        // 쿠키, 세션등 작업은 rq에서 처리합니다
//        rq.removeCookie("accessToken");
//        rq.removeCookie("apiKey");

        return new RsData<>(
                200,
                "로그아웃 됐습니다.",
                null
        );
    }

    @Operation(summary = "회원 정보 조회")
    @GetMapping("/info")
    @Transactional(readOnly = true)
    public RsData<MemberWithInfoDto> myInfo() {

        //인증,인가
        //Member actor = rq.getActor();
//        Member member = memberService.findById(actor.getId())
//                .orElseThrow(() -> new ServiceException(404, "존재하지 않는 회원입니다."));

        //임의로 생성한 Member 객체 (지우고 위에 member 사용하세요)
        Member member = new Member(1L, "임의", "1234", "test@example", 0, 1, "", false);

        return new RsData<>(
                200,
                "내 정보 조회가 완료되었습니다.",
                new MemberWithInfoDto(member)
        );
    }

    record ModifyReqBody(@NotBlank
                         @Size(min = 5, max = 25)
                         String name,
                         @NotBlank
                         @Size(min = 5, max = 25)
                         String password,
                         @NotBlank
                         @Email String email) {
    }

    @Operation(summary = "회원 정보 수정 (이름,비밀번호,메일)")
    @PutMapping("/info")
    @Transactional
    public RsData<MemberWithAuthDto> modifyInfo(@RequestBody @Valid ModifyReqBody reqBody) {

        //인증,인가
        //Member actor = rq.getActor();
        //Member member = memberService.findById(actor.getId())
        //        .orElseThrow(() -> new ServiceException(404, "존재하지 않는 회원입니다."));

        //임의로 생성함
        Member member = new Member(2L, "임의1", "1234", "test@example", 0, 1, "", false);
        memberService.modify(member, reqBody.name(), reqBody.password(), reqBody.email());

        return new RsData<>(
                200,
                "회원 정보 수정이 완료되었습니다.",
                new MemberWithAuthDto(member)
        );


    }
}