package com.back.backend.domain.quiz.detail.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;
import com.back.domain.quiz.detail.service.DetailQuizService;
import com.back.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "NAVER_CLIENT_ID=test_client_id",
        "NAVER_CLIENT_SECRET=test_client_secret",
        "GEMINI_API_KEY=AIzaSyDkp7j5fH_gMC6IRgAVwMFi1BJ_cN4QgQg"
})
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DetailQuizControllerTest {
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

    private Member testMember;


    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Member testUser = memberService.join("testUser", "12341234", "test@test.com");

        // 테스트 Rq에 사용자 지정
        ((TestRq) rq).setActor(testUser);
    }

    @Test
    @DisplayName("GET /api/quiz/detail - 상세 퀴즈 목록 조회")
    void getDetailQuizzes() throws Exception {
        //Given
        int quizCount = (int) detailQuizService.count();

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuizzes"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(quizCount));
    }

    @Test
    @DisplayName("GET /api/quiz/detail/{id} - 상세 퀴즈 단건 조회")
    void getDetailQuiz() throws Exception {
        //Given
        Long quizId = 5L;
        DetailQuiz quiz = detailQuizService.findById(quizId);

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/" + quizId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuiz"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 조회 성공"))
                .andExpect(jsonPath("$.data.question").value(quiz.getQuestion()))
                .andExpect(jsonPath("$.data.option1").value(quiz.getOption1()))
                .andExpect(jsonPath("$.data.option2").value(quiz.getOption2()))
                .andExpect(jsonPath("$.data.option3").value(quiz.getOption3()))
                .andExpect(jsonPath("$.data.correctOption").value(quiz.getCorrectOption().toString()));
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회")
    void getDetailQuizzesByNewsId() throws Exception {
        //Given
        Long newsId = 2L;
        List<DetailQuiz> quizzes = detailQuizService.findByNewsId(newsId);

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/news/" + newsId)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("뉴스 ID로 상세 퀴즈 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].question").value(quizzes.get(0).getQuestion()))
                .andExpect(jsonPath("$.data[1].question").value(quizzes.get(1).getQuestion()))
                .andExpect(jsonPath("$.data[2].question").value(quizzes.get(2).getQuestion()));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/news/{newsId}/regenerate - 뉴스 ID로 상세 퀴즈 생성")
    void generateDetailQuizzes() throws Exception {
        // Given
        Long newsId = 1L;

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/news/{newsId}/regenerate", newsId))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(handler().methodName("generateDetailQuizzes"))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 생성 성공"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].question").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /api/quiz/detail/{id} - 상세 퀴즈 수정")
    void updateDetailQuiz() throws Exception {
        //Given
        Long quizId = 1L;
        DetailQuizDto updatedDto = new DetailQuizDto("수정된 질문", "수정된 옵션1", "수정된 옵션2", "수정된 옵션3", Option.OPTION2);

        //When
        ResultActions resultActions = mvc.perform(put("/api/quiz/detail/{id}", quizId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto))
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("updateDetailQuiz"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 수정 성공"))
                .andExpect(jsonPath("$.data.question").value("수정된 질문"))
                .andExpect(jsonPath("$.data.correctOption").value("OPTION2"));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/submit/{id} - 퀴즈 정답 제출")
    void submitDetailQuizAnswerCorrect() throws Exception {
        // Given
        Long quizId = 1L;
        Option selectedOption = Option.OPTION2;

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/submit/{id}", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectedOption)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("submitDetailQuizAnswer"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
                .andExpect(jsonPath("$.data.quizId").value(quizId))
                .andExpect(jsonPath("$.data.selectedOption").value("OPTION2"))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.gainExp").value(10))
                .andExpect(jsonPath("$.data.quizType").value("DETAIL"));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/submit/{id} - 퀴즈 오답 제출")
    void submitDetailQuizAnswerIncorrect() throws Exception {
        // Given
        Long quizId = 1L;
        Option selectedOption = Option.OPTION1;

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/submit/{id}", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectedOption)))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("submitDetailQuizAnswer"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("퀴즈 정답 제출 성공"))
                .andExpect(jsonPath("$.data.quizId").value(quizId))
                .andExpect(jsonPath("$.data.selectedOption").value("OPTION1"))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.gainExp").value(0))
                .andExpect(jsonPath("$.data.quizType").value("DETAIL"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        Rq testRq() {
            return new TestRq();
        }
    }

    static class TestRq extends Rq {
        private Member actor;

        public TestRq() {
            super(null, null, null); // 실제 req/resp는 필요 없으니 null
        }

        void setActor(Member actor) {
            this.actor = actor;
        }

        @Override
        public Member getActor() {
            return actor;
        }

        @Override
        public String getHeader(String name, String defaultValue) {
            return defaultValue;
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public String getCookieValue(String name, String defaultValue) {
            return defaultValue;
        }

        @Override
        public void setCookie(String name, String value) {
        }

        @Override
        public void deleteCookie(String name) {
        }
    }
}
