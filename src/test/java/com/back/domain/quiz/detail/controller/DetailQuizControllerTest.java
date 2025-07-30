package com.back.domain.quiz.detail.controller;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.AdminNewsService;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.Option;
import com.back.domain.quiz.detail.service.DetailQuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        "GEMINI_API_KEY=AIzaSyDkp7j5fH_gMC6IRgAVwMFi1BJ_cN4QgQg",
        "JWT_SECRET_KEY=jwt_secret_key",
        "KAKAO_OAUTH2_CLIENT_ID=kakao_oauth2_client_id",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DetailQuizControllerTest {
    @Autowired
    private AdminNewsService adminNewsService;

    @Autowired
    private DetailQuizService detailQuizService;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // 테스트에 필요한 퀴즈 데이터를 미리 생성
        // 뉴스 1: 경제 카테고리
        RealNewsDto news1 = RealNewsDto.of(
                1L,
                "소비쿠폰 지급+여름 휴가철 시작…내수경기 반등할까",
                "7월 첫째 주 신용카드 이용금액 작년 대비 12.6% 증가 교육 서비스 이용 9.9% 증가 등 교육·보건이 증가 견인 28일 서울의 한 이마트에 민생회복 소비쿠폰 관련 안내문이 내걸려 있다. 이마트에 따르면 전국 156개 이마트·트레이더스 점포에 입점한 2천600여개 임대매장 중에서 민생회복 소비쿠폰 사용이 가능한 매장은 960여개로 전체의 37% 수준이다. 연합뉴스 정부가 '민생회복 소비쿠폰'을 지급 중인 가운데 본격적인 여름 휴가철에 접어들면서 내수경기 반등에 대한 기대감이 번지고 있다. 29일 통계청 속보성 지표 '나우캐스트'에 따르면 7월 첫째 주(6월 28일∼7월 4일) 신용카드 이용금액은 작년 동기보다 12.6% 증가했다. 7월 둘째 주(7월 5∼11일)도 3.7% 증가하며 작년 대비 상승 흐름을 이어갔다. 업종별로 보면 내수와 밀접한 업종보다는 교육, 보건 등이 증가세를 이끌었다. 7월 둘째 주 교육 서비스 이용금액이 작년 동기보다 9.9% 크게 늘었고 보건 부문도 4.9% 증가했다. 반면 숙박서비스는 2.4% 감소했고, 음식·음료 서비스도 4.2% 줄었다. 식료품과 음료 결제액도 2.0% 감소했다. 정부는 지난 21일 지급을 시작한 소비쿠폰이 '내수 마중물' 역할을 할 것으로 기대하고 있다. '7말 8초'(7월 말부터 8월 초) 여름 휴가철과 맞물려 소비 진작 효과가 커질 가능성도 있다. 정부 관계자는 \"신용카드 주간 결제액은 변동성이 크다\"며 \"최근 일부 지표에서 소비 개선 흐름은 있다\"고 했다. 기획재정부 경제동향을 보면 지난달 카드 국내 승인액은 작년 동월보다 4.5% 증가했고, 한국을 찾은 중국인 관광객 수는 28.8% 늘었다. 소비심리도 개선 흐름을 보이는 상황이다. 이번 달 소비자심리지수(CCSI)는 110.8로, 지난달보다 2.1포인트(p) 올라 2021년 6월(111.1) 이후 4년여 만에 최고치를 경신했다. 지난 3월 93.4, 4월 93.8, 5월 101.8, 6월 108.7에 이어 7월까지 넉 달 연속 상승세다. 다만 휴가철 해외여행 수요가 증가하면서 국내 소비가 해외로 분산될 가능성도 있다. 소비쿠폰도 사용처를 일부 제한하기는 했지만 사교육비와 담배 '사재기' 등에 쓰이고 있다는 지적도 나오고 있어, 실질적인 내수 진작 효과는 향후 지표를 통해 판단될 전망이다.",
                "정부가 '민생회복 소비쿠폰'을 지급 중인 가운데 본격적인 여름 휴가철에 접어들면서 내수경기 반등에... '7말 8초'(7월 말부터 8월 초) 여름 휴가철과 맞물려 소비 진작 효과가 커질 가능성도 있다. 정부 관계자는...",
                "https://n.news.naver.com/mnews/article/088/0000961622?sid=101",
                "https://imgnews.pstatic.net/image/088/2025/07/29/0000961622_001_20250729133508809.jpg?type=w860",
                LocalDateTime.now().minusHours(1),
                "매일신문",
                "정은빈 기자",
                "https://www.imaeil.com/page/view/2025072913253167202",
                NewsCategory.ECONOMY
        );

        // 뉴스 2: 생활 카테고리
        RealNewsDto news2 = RealNewsDto.of(
                2L,
                "여름 제철 간식 감자·찰옥수수 드세요",
                "[서울=뉴시스] 29일 서울 서초구 농협유통 하나로마트 양재점에서 모델들이 여름 제철 간식인 감자와 찰옥수수를 소개하고 있다. (사진=농협유통 제공) 2025.07.29. photo@newsis.com *재판매 및 DB 금지",
                "29일 서울 서초구 농협유통 하나로마트 양재점에서 모델들이 여름 제철 간식인 감자와 찰옥수수를 소개하고 있다. (사진=농협유통 제공) 2025.07.29. photo@newsis.com *재판매 및 DB 금지",
                "https://n.news.naver.com/mnews/article/003/0013391031?sid=101",
                "https://imgnews.pstatic.net/image/003/2025/07/29/NISI20250729_0020908209_web_20250729134833_20250729134922533.jpg?type=w860",
                LocalDateTime.now().minusMinutes(30),
                "뉴시스",
                "류현주 기자",
                "https://www.newsis.com/view/NISI20250729_0020908209",
                NewsCategory.CULTURE
        );

        List<RealNewsDto> savedNewsList = adminNewsService.saveRealNews(List.of(news1, news2));
    }

    @Test
    @DisplayName("GET /api/quiz/detail - 상세 퀴즈 목록 조회")
    void getDetailQuizzes() throws Exception {
        //Given
        DetailQuizDto quiz1 = new DetailQuizDto("question1", "option1", "option2", "option3", Option.OPTION1);
        DetailQuizDto quiz2 = new DetailQuizDto("question2", "option1", "option2", "option3", Option.OPTION3);
        DetailQuizDto quiz3 = new DetailQuizDto("question3", "option1", "option2", "option3", Option.OPTION2);

        detailQuizService.saveQuizzes(1L, List.of(quiz1, quiz2, quiz3));
        detailQuizService.saveQuizzes(2L, List.of(quiz1, quiz2, quiz3));

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail")
                .contentType("application/json")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuizzes"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    @DisplayName("GET /api/quiz/detail/{id} - 상세 퀴즈 단건 조회")
    void getDetailQuiz() throws Exception {
        //Given
        DetailQuizDto quiz1 = new DetailQuizDto("question1", "option1", "option2", "option3", Option.OPTION1);
        DetailQuizDto quiz2 = new DetailQuizDto("question2", "option1", "option2", "option3", Option.OPTION3);
        DetailQuizDto quiz3 = new DetailQuizDto("question3", "option1", "option2", "option3", Option.OPTION2);

        detailQuizService.saveQuizzes(1L, List.of(quiz1, quiz2, quiz3));
        detailQuizService.saveQuizzes(2L, List.of(quiz1, quiz2, quiz3));

        Long quizId = 5L; // 테스트용 퀴즈 ID

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/" + quizId)
                .contentType("application/json")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuiz"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 조회 성공"))
                .andExpect(jsonPath("$.data.question").value("question2"))
                .andExpect(jsonPath("$.data.option1").value("option1"))
                .andExpect(jsonPath("$.data.option2").value("option2"))
                .andExpect(jsonPath("$.data.option3").value("option3"))
                .andExpect(jsonPath("$.data.correctOption").value("OPTION3"));
    }

    @Test
    @DisplayName("GET /api/quiz/detail/news/{newsId} - 뉴스 ID로 상세 퀴즈 목록 조회")
    void getDetailQuizzesByNewsId() throws Exception {
        //Given
        DetailQuizDto quiz1 = new DetailQuizDto("question1-3", "option1", "option2", "option3", Option.OPTION1);
        DetailQuizDto quiz2 = new DetailQuizDto("question2-1", "option1", "option2", "option3", Option.OPTION3);
        DetailQuizDto quiz3 = new DetailQuizDto("question3-2", "option1", "option2", "option3", Option.OPTION2);

        detailQuizService.saveQuizzes(1L, List.of(quiz1, quiz2, quiz3));
        detailQuizService.saveQuizzes(2L, List.of(quiz2, quiz3, quiz1));

        Long newsId = 2L;

        //When
        ResultActions resultActions = mvc.perform(get("/api/quiz/detail/news/" + newsId)
                .contentType("application/json")
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("getDetailQuizzesByNewsId"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("뉴스 ID로 상세 퀴즈 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].question").value("question2-1"))
                .andExpect(jsonPath("$.data[1].question").value("question3-2"))
                .andExpect(jsonPath("$.data[2].question").value("question1-3"));
    }

    @Test
    @DisplayName("POST /api/quiz/detail/news/{newsId}/regenerate - 뉴스 ID로 상세 퀴즈 생성")
    void generateDetailQuizzes() throws Exception {
        // Given
        Long newsId = 1L; // 테스트용 뉴스 ID

        // When
        ResultActions resultActions = mvc.perform(post("/api/quiz/detail/news/{newsId}/regenerate", newsId))
                .andDo(print());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(handler().methodName("generateDetailQuizzes"))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 생성 성공"))
                .andExpect(jsonPath("$.data.length()").value(3)) // 생성된 퀴즈 개수 확인
                .andExpect(jsonPath("$.data[0].question").isNotEmpty())
                .andExpect(jsonPath("$.data[0].option1").isNotEmpty())
                .andExpect(jsonPath("$.data[0].option2").isNotEmpty())
                .andExpect(jsonPath("$.data[0].option3").isNotEmpty())
                .andExpect(jsonPath("$.data[0].correctOption").isNotEmpty());

    }

    @Test
    @DisplayName("PUT /api/quiz/detail/{id} - 상세 퀴즈 수정")
    void updateDetailQuiz() throws Exception {
        //Given
        DetailQuizDto quiz1 = new DetailQuizDto("question1", "option1", "option2", "option3", Option.OPTION1);
        DetailQuizDto quiz2 = new DetailQuizDto("question2", "option1", "option2", "option3", Option.OPTION3);
        DetailQuizDto quiz3 = new DetailQuizDto("question3", "option1", "option2", "option3", Option.OPTION2);

        detailQuizService.saveQuizzes(1L, List.of(quiz1, quiz2, quiz3));
        detailQuizService.saveQuizzes(2L, List.of(quiz1, quiz2, quiz3));

        //When
        ResultActions resultActions = mvc.perform(put("/api/quiz/detail/{id}", 1L)
                .contentType("application/json")
                .content("""
                    {
                        "question": "수정된 질문",
                        "option1": "수정된 옵션1",
                        "option2": "수정된 옵션2",
                        "option3": "수정된 옵션3",
                        "correctOption": "OPTION2"
                    }
                    """)
        ).andDo(print());

        //Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().methodName("updateDetailQuiz"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("상세 퀴즈 수정 성공"))
                .andExpect(jsonPath("$.data.question").value("수정된 질문"))
                .andExpect(jsonPath("$.data.option1").value("수정된 옵션1"))
                .andExpect(jsonPath("$.data.option2").value("수정된 옵션2"))
                .andExpect(jsonPath("$.data.option3").value("수정된 옵션3"))
                .andExpect(jsonPath("$.data.correctOption").value("OPTION2"));
    }
}