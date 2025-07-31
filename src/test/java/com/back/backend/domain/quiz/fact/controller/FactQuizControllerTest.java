package com.back.backend.domain.quiz.fact.controller;

import com.back.backend.global.config.TestRqConfig;
import com.back.backend.global.rq.TestRq;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.quiz.fact.service.FactQuizService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
})
@Import(TestRqConfig.class)
public class FactQuizControllerTest {
    @Autowired
    private FactQuizService factQuizService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Rq rq;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Member testUser = memberService.join("testUser", "12341234", "test@test.com");

        // 테스트 Rq에 사용자 지정
        ((TestRq) rq).setActor(testUser);
    }

    @Test
    @DisplayName("GET /api/quiz/detail - 상세 퀴즈 목록 조회")
    void t1() throws Exception {
        //Given
        int quizCount = (int) factQuizService.count();

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/fact")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getFactQuizzes"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("팩트 퀴즈 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(quizCount));
    }
}
