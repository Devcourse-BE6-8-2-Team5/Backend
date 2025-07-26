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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("회원가입 실패 - 이메일 형식 아님")
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
    @DisplayName("회원가입 실패 - 이메일 중복")
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
}