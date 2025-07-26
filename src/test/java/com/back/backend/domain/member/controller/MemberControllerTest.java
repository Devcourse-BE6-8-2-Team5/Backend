package com.back.backend.domain.member.controller;

import com.back.domain.member.controller.MemberController;
import com.back.domain.member.entity.Member;
import com.back.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id_for_testing_only",
        "NAVER_CLIENT_SECRET=test_client_secret_for_testing_only"
})
public class MemberControllerTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("회원가입 성공")
    void t1() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "테스트유저",
                                            "password": "12345678910",
                                            "email": "test@example.com"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        Member member = memberService.findByEmail("test@example.com").get();

        resultActions
                .andExpect(handler().handlerType(MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("테스트유저님 환영합니다. 회원 가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(member.getId()))
                .andExpect(jsonPath("$.data.name").value(member.getName()))
                .andExpect(jsonPath("$.data.email").value(member.getEmail()));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식이 유효하지 않음")
    void t2() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "테스트유저",
                                            "password": "12345678910",
                                            "email": "invalidemail"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("유효한 이메일 형식이어야 합니다.")));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void t3() throws Exception {
        // 첫 번째 회원가입
        t1();

        // 두 번째 회원가입 (같은 이메일)
        ResultActions resultActions = mvc
                .perform(
                        post("/api/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "다른유저",
                                            "password": "12345678910",
                                            "email": "test@example.com"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."));
    }

    @Test
    @DisplayName("회원가입 실패 - 이름이 2자 미만")
    void t5() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "길",
                                            "password": "12345678910",
                                            "email": "test@example.com"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이름은 최소 2자 이상이어야 합니다.")));
    }

    @Test
    @DisplayName("회원가입 실패 - 이름 누락")
    void t6() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "",
                                            "password": "12345678910",
                                            "email": "test@example.com"
                                        }
                                        """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void t7() throws Exception {
        // 먼저 회원가입
        mvc.perform(
                post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "테스트유저",
                                "password": "12345678910",
                                "email": "test2@example.com"
                            }
                            """.stripIndent())
        );

        // 로그인 요청
        ResultActions resultActions = mvc
                .perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "test2@example.com",
                                        "password": "12345678910"
                                    }
                                    """.stripIndent())
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("테스트유저님 환영합니다."))
                .andExpect(jsonPath("$.data.accessToken").exists())  // ← accessToken만 확인
                .andExpect(jsonPath("$.data.member").exists());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void t8() throws Exception {
        mvc.perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "email": "notfound@example.com",
                                "password": "12345678910"
                            }
                            """.stripIndent())
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void t9() throws Exception {
        // 회원가입
        mvc.perform(
                post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "테스트유저",
                                "password": "12345678910",
                                "email": "test5@example.com"
                            }
                            """.stripIndent())
        );

        // 로그인(틀린 비밀번호)
        mvc.perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "email": "test5@example.com",
                                "password": "wrongpassword"
                            }
                            """.stripIndent())
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("accessToken으로 마이페이지 접근 성공")
    void t11() throws Exception {
        // 회원가입 및 로그인
        mvc.perform(
                post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "테스트유저",
                                "password": "12345678910",
                                "email": "test3@example.com"
                            }
                            """.stripIndent())
        );

        // 로그인해서 accessToken 획득
        ResultActions loginResult = mvc
                .perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "test3@example.com",
                                        "password": "12345678910"
                                    }
                                    """.stripIndent())
                )
                .andDo(print());

        String accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue();

        // accessToken으로 마이페이지 요청
        ResultActions resultActions = mvc
                .perform(
                        get("/api/members/info")
                                .header("Authorization", "Bearer " + accessToken)  // ← Authorization 헤더 사용
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test3@example.com"));
    }

    @Test
    @DisplayName("accessToken으로 마이페이지 접근 성공 (쿠키 방식)")
    void t11_cookie() throws Exception {
        // 회원가입 및 로그인
        mvc.perform(
                post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "테스트유저",
                                "password": "12345678910",
                                "email": "test3@example.com"
                            }
                            """.stripIndent())
        );

        // 로그인해서 accessToken 획득
        ResultActions loginResult = mvc
                .perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "test3@example.com",
                                        "password": "12345678910"
                                    }
                                    """.stripIndent())
                )
                .andDo(print());

        String accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue();

        // accessToken 쿠키로 마이페이지 요청
        ResultActions resultActions = mvc
                .perform(
                        get("/api/members/info")
                                .cookie(new jakarta.servlet.http.Cookie("accessToken", accessToken))  // ← 쿠키 사용
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test3@example.com"));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void t12() throws Exception {
        // 회원가입 및 로그인
        mvc.perform(
                post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "테스트유저",
                                "password": "12345678910",
                                "email": "test4@example.com"
                            }
                            """.stripIndent())
        );

        ResultActions loginResult = mvc
                .perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "test4@example.com",
                                        "password": "12345678910"
                                    }
                                    """.stripIndent())
                );

        String accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue();

        // 로그아웃 요청
        ResultActions resultActions = mvc
                .perform(
                        delete("/api/members/logout")
                                .cookie(new jakarta.servlet.http.Cookie("accessToken", accessToken))
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));
    }

    @Test
    @DisplayName("로그아웃 후 accessToken으로 마이페이지 접근 시 401/403에러")
    void t10() throws Exception {
        // 회원가입 및 로그인
        mvc.perform(
                post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "테스트유저",
                                "password": "12345678910",
                                "email": "test6@example.com"
                            }
                            """.stripIndent())
        );

        ResultActions loginResult = mvc
                .perform(
                        post("/api/members/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "email": "test6@example.com",
                                        "password": "12345678910"
                                    }
                                    """.stripIndent())
                );

        String accessToken = loginResult.andReturn().getResponse().getCookie("accessToken").getValue();

        // 로그아웃
        mvc.perform(
                delete("/api/members/logout")
                        .cookie(new jakarta.servlet.http.Cookie("accessToken", accessToken))
        );

        // 로그아웃 후 마이페이지 접근
        mvc.perform(
                        get("/api/members/info")
                                .cookie(new jakarta.servlet.http.Cookie("accessToken", accessToken))
                )
                .andDo(print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @DisplayName("인증 없이 마이페이지 접근 시 403에러")
    void t13() throws Exception {
        mvc.perform(
                        get("/api/members/info")
                )
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("만료된 accessToken으로 마이페이지 접근 시 401에러")
    void t14() throws Exception {
        // 만료된 accessToken 사용
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwibmFtZSI6InRlc3QiLCJyb2xlIjoiVVNFUiIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIyfQ.invalid_signature";

        mvc.perform(
                        get("/api/members/info")
                                .header("Authorization", "Bearer " + expiredToken)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}