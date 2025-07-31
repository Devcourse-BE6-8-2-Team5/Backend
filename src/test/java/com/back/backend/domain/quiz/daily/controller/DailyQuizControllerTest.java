package com.back.backend.domain.quiz.daily.controller;

import com.back.backend.global.config.TestRqConfig;
import com.back.backend.global.rq.TestRq;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.quiz.detail.service.DetailQuizService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "GEMINI_API_KEY=api_key"
})
@Import(TestRqConfig.class)
public class DailyQuizControllerTest {
    @Autowired
    private DetailQuizService detailQuizService;
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


}
