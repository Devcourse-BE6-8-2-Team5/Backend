package com.back.domain.user.controller;


import com.back.domain.user.dto.UserDto;
import com.back.domain.user.entity.User;
import com.back.domain.user.service.UserService;
import com.back.global.rsData.RsData;
import com.back.global.springdoc.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "UserController", description = "회원 관련 컨트롤러 엔드 포인트")
public class UserController {
    private final UserService userService;

    record JoinReqBody (
            @NotBlank
            @Size(min =5, max= 25)
            String name,

            @NotBlank
            @Size(min = 10, max=50)
            String password,

            @NotBlank
            String email
    )  {}

    @PostMapping(value = "/join", produces = "application/json;charset=UTF-8")
    @Transactional
    @Operation(summary = "회원 가입")
    public RsData<UserDto> join(@RequestBody @Valid JoinReqBody reqBody) {

        userService.findByName(reqBody.name())
                .ifPresent(_ -> {
                    throw new ServiceException(409, "이미 사용중인 아이디입니다.");
                });

        User user = userService.join(reqBody.name(),reqBody.password(), reqBody.email());

        return new RsData<> (
                201,
                "%s님 환영합니다. 회원 가입이 완료되었습니다.".formatted(user.getName()),
                new UserDto(user)
        );

    }

}
