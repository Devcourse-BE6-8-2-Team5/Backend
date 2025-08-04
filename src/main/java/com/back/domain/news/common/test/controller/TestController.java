package com.back.domain.news.common.test.controller;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.common.service.KeywordCleanupService;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.common.service.AnalysisNewsService;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.service.FakeNewsService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.service.AdminNewsService;
import com.back.domain.news.real.service.NewsDataService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final KeywordGenerationService keywordGenerationService;
    private final KeywordCleanupService keywordCleanupService;
    private final FakeNewsService fakeNewsService;
    private final RealNewsService realNewsService;
    private final NewsDataService newsDataService;
    private final AnalysisNewsService analysisNewsService;
    private final AdminNewsService adminNewsService;

    @GetMapping("/keywords")
    public KeywordGenerationResDto testKeywords() {
        return keywordGenerationService.generateTodaysKeywords();
    }


    //     뉴스 배치 프로세서
    @GetMapping("/process")
    public RsData<List<RealNewsDto>> newsProcess() {
        try {
//            adminNewsService.dailyNewsProcess();

            //   속보랑 기타키워드 추가
            List<String> newsKeywords = List.of("주가", "AI");

            List<NaverNewsDto> newsKeywordsAfterAdd = newsDataService.collectMetaDataFromNaver(newsKeywords);

            List<RealNewsDto> newsAfterCrwal = newsDataService.createRealNewsDtoByCrawl(newsKeywordsAfterAdd);

            List<AnalyzedNewsDto> newsAfterFilter = analysisNewsService.filterAndScoreNews(newsAfterCrwal);

            List<RealNewsDto> selectedNews = newsDataService.selectNewsByScore(newsAfterFilter);

            List<RealNewsDto> savedNews = newsDataService.saveAllRealNews(selectedNews);
//            return RsData.of(200, "뉴스 프로세스 성공", null); // savedNews
            return RsData.of(200, "성공", savedNews);

        } catch (Exception e) {
            return RsData.of(500, "실패: " + e.getMessage());
        }
    }


    //-1 -> 내일 이전(모든 키워드 삭제)
    // 0 -> 오늘 이전(어제 키워드까지 삭제)
    @GetMapping("/cleanup/{days}")
    public RsData<String> testCleanup(@PathVariable int days) {
        log.debug("testCleanup days: {}", days);

        try {
            keywordCleanupService.adminCleanup(days);
            return new RsData<>(200, "Cleanup successful", null);
        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage());
            return new RsData<>(500, "Cleanup failed", null);
        }
    }

    //뉴스 생성 (for test)

    @GetMapping("/create")
    public RsData<List<RealNewsDto>> createRealNews(@RequestParam String query) {
        List<RealNewsDto> realNewsList = newsDataService.createRealNewsDto(query);

        if (realNewsList.isEmpty()) {
            return RsData.of(404, String.format("'%s' 검색어로 뉴스를 찾을 수 없습니다", query));
        }

        return RsData.of(200, String.format("뉴스 %d건 생성 완료",realNewsList.size()), realNewsList);
    }

    @GetMapping("/createtoday")
    public RsData<List<RealNewsDto>> getCreateToday() {

        List<RealNewsDto> realNewsDtos = realNewsService.getRealNewsListCreatedToday();

        if (realNewsDtos == null || realNewsDtos.isEmpty()) {
            return RsData.of(400, "실제 뉴스 목록이 비어있습니다.");
        }

        for( RealNewsDto realNewsDto : realNewsDtos) {
            log.info("실제 뉴스 ID: {}, 제목: {}", realNewsDto.id(), realNewsDto.title(), realNewsDto.content());


        }
        return RsData.of(200, "가짜 뉴스 생성 성공", realNewsDtos);

    }

    @GetMapping("/create/fake")
    public RsData<List<FakeNewsDto>> testCreateFake() {

//
//        RealNews news3 = RealNews.builder()
//                .title("네이버·업스테이지·SKT·NC AI·LG AI연, '국대 AI' 만든다")
//                .content("네이버클라우드, 업스테이지, SK텔레콤, NC AI, LG AI연구원이 정부의 전폭적인 지원 속에서 우리나라를 대표할 인공지능(AI) 파운데이션 모델을 개발할 주체들로 선정됐다. 과학기술정보통신부는 4일 '독자 AI 파운데이션 모델 프로젝트'에 참여한 15개 팀을 대상으로 서면 및 발표 평가를 진행해 이들 5개 정예 팀으로 압축했다고 밝혔다.\n\n서면 평가를 통과한 10개 팀을 대상으로 지난달 진행된 발표 평가에서는 AI 모델 개발 경험과 기술력, 개발 목표 및 전략, 사회 파급효과 및 기여 계획 등을 중점적으로 평가했다.\n\n네이버클라우드 컨소시엄에는 네이버, 트웰브랩스, 서울대 산학협력단, 한국과학기술원(KAIST), 포항공과대 산학협력단, 고려대 산학협력단, 한양대 산학협력단이 참여했다. 이 컨소시엄은 범국민 AI 접근성 확대와 산업 분야 확산을 목표로 텍스트·이미지·오디오·영상 등 이종 데이터의 통합 이해·생성 등이 가능한 단일 모델(옴니 파운데이션 모델) 구축을 내세웠다.\n\n네이버클라우드 컨소시엄은 옴니 파운데이션 모델 기반 전 국민 AI 서비스 플랫폼을 운영하며 전 국민 체험형 AI 서비스를 제공하는 한편 개방형 플랫폼인 'AI 에이전트 마켓플레이스'를 통해 누구나 AI 에이전트를 개발·등록·유통할 수 있도록 지원한다.\n\n업스테이지 컨소시엄에는 노타, 래블업, 플리토, 뷰노, 마키나락스, 로앤컴퍼니, 오케스트로, 데이원컴퍼니, 올거나이즈코리아, 금융결제원, 서강대 산학협력단, 한국과학기술원이 참여했다. 지속 가능한 국내 AI 생태계와 기술 주도권 확보를 목표로 글로벌 선도 수준의 AI 파운데이션 모델 '솔라 WBL(월드 베스트 거대언어모델)'을 개발하며 3년간 대국민 AI 서비스를 제공해 사용자 수 1천만명 이상 달성을 목표로 세웠다.\n\n통신사인 SK텔레콤 정예 팀에는 크래프톤, 포티투닷, 리벨리온, 라이너, 셀렉트스타, 서울대 산학협력단, 한국과학기술원이 참여했다. 생성형 AI의 모태가 된 트랜스포머 기술을 고도화한 '포스트-트랜스포머 AI 모델'로 K-AI 서비스를 구현한다는 목표로, 누구나 활용할 수 있는 범용 AI 에이전트 등 기업 대 고객(B2C) 서비스, 제조·자동차·게임·로봇 등 분야의 기업 대 기업(B2B) 서비스를 발굴, 확산한다.\n\nNC AI 컨소시엄에는 고려대, 서울대, 연세대, 한국과학기술원, 한국전자통신연구원, AI웍스, 포스코DX, 롯데이노베이트, HL로보틱스, 인터엑스, 미디어젠, 문화방송, NHN이 참여했다. 글로벌 최고 성능의 200B(매개변수 2천억개) 규모 AI 모델과 이에 기반한 멀티모달 인지·생성 모델 패키지 및 산업 특화 파운데이션 모델을 개발하며 특정 분야에서 운영, 자동화, 최적화 등이 가능한 '도메인 옵스 플랫폼'을 구축한다.\n\n이 컨소시엄은 다양한 추론 프레임워크·포맷 지원을 통해 파운데이션 모델을 확산하고 멀티모달 인지·생성 기술을 정부 서비스에도 연계할 계획이다.\n\n글로벌 최고 수준의 AI 모델 'K-엑사원' 개발을 천명한 LG AI연구원 컨소시엄에는 LG유플러스, LG CNS, 슈퍼브AI, 퓨리오사AI, 프렌들리AI, 이스트소프트, 이스트에이드, 한글과컴퓨터, 뤼튼테크놀로지스가 참여했다. LG AI연구원 컨소시엄은 전문성·범용성을 모두 갖춘 고성능 AI 파운데이션 모델을 개발하고 다양한 산업 현장의 AI 전환(AX) 등을 가속한다는 목표를 세웠다.\n\n선발된 5개 정예 팀은 국가기록원, 국사편찬위원회, 통계청, 특허청, 방송사 등 기관 데이터를 공동 구매하거나 개별 구축할 수 있다. 아울러 순차적으로 1천576억원 규모 그래픽처리장치(GPU) 등 정부 예산으로 확보한 컴퓨팅 자원이 지원된다.\n\nAI 인재의 경우 5곳 중 업스테이지 컨소시엄만 지원을 희망함에 따라 유치하고자 하는 해외 우수 연구자(팀)의 인건비, 연구비 등 필요 비용을 정부가 매칭 지원한다.\n\n정부는 사업비 심의·조정 단계 등을 거쳐 5개 정예 팀의 사업 범위, 지원 내용 등을 확정해 이달 초 협약을 맺을 예정이다. 올해 말까지 이들이 개발·확보한 AI 파운데이션 모델 등을 기반으로 12월 말 1차 단계 평가를 거쳐 지원 대상을 4곳으로 줄인다.\n\n배경훈 과기정통부 장관은 '본 프로젝트의 담대한 도전은 이제 시작이자 '모두의 AI' 출발점이 될 것이다. 대한민국 AI 기업·기관들의 도약, 소버린 AI 생태계 확장을 정부가 적극 뒷받침하겠다'고 말했다.")
//                .description("정부 '독자 AI 파운데이션 모델 프로젝트' 5개 컨소시엄 선정 GPU·데이터·인재 지원…연말 1차 평가 등 거쳐 단계적 압축 네이버클라우드, 업스테이지, SK텔레콤, NC AI, LG AI연구원이 정부의 전폭적인 지원 속에서...")
//                .link("https://n.news.naver.com/mnews/article/001/0015547564?sid=105")
//                .imgUrl("https://imgnews.pstatic.net/image/056/2025/08/04/0012002121_001_20250804142513825.jpg?type=w860")
//                .originCreatedDate(LocalDateTime.parse("2025-08-04T14:47:00"))
//                .createdDate(LocalDateTime.parse("2025-08-05T00:01:33"))
//                .mediaName("KBS")
//                .journalist("이형관 기자")
//                .originalNewsUrl("https://news.kbs.co.kr/news/pc/view/view.do?ncd=8321045&ref=A")
//                .newsCategory(NewsCategory.IT)
//                .build();
//
//        RealNews news4 = RealNews.builder()
//                .title("'K-AI' 간판 단 5개팀 면면 보니…'외연 확장성'에 방점")
//                .content("과학기술정보통신부는 4일 '독자 AI 파운데이션 모델' 프로젝트를 이끌 국가대표 최정예 다섯 팀을 확정했다. 포털·클라우드의 강자 네이버클라우드, 1000억 파라미터급 AI 모델 '엑사원(EXAONE)'을 앞세운 LG AI연구원, 890만 실사용자 '에이닷'을 보유한 SK텔레콤, 14년 AI 연구개발(R&D) 내공의 AI 전문기업 NC AI와 대기업군 속 유일한 스타트업 업스테이지가 그 주인공이다.\n\n포털·통신·게임·제조·스타트업까지 각기 다른 DNA를 지닌 이들이 '프롬 스크래치' 모델 개발과 오픈소스 개방을 내걸고, 글로벌 빅테크가 선점한 AI 패권 판도를 뒤흔들 새 판짜기에 나선다.\n\n네이버클라우드, LG AI연구원, SK텔레콤은 오랜 기간 글로벌 빅테크와 경쟁해온 대기업 계열사로서 풍부한 개발 경험과 안정적인 인프라를 바탕으로 선정됐다. 네이버클라우드는 모회사 네이버의 검색, 클라우드, AI 서비스 경험을 집약해 '범국민 AI 접근성 확대'라는 차별화된 비전을 제시했다.\n\n특히 텍스트·음성·이미지·비디오를 통합 처리하는 옴니 파운데이션 모델 구축과 함께 'AI 에이전트 마켓플레이스' 운영 계획은 AI 기술의 민주화를 통한 전국민적 확산 전략으로 평가받았다. 서울대·한국과학기술원·포항공대·고려대·한양대 등 국내 최고 대학들과의 산학협력 네트워크도 강력한 경쟁력이다.\n\nLG AI연구원은 지난 7월 공개한 '엑사원 4.0(EXAONE 4.0)'의 성과가 결정적 요인으로 작용했다. 글로벌 AI 성능 분석 전문기관 '아티피셜 어낼리시스'의 인텔리전스 지수 평가에서 한국 모델 기준 1위, 오픈 웨이트 모델 기준 글로벌 4위를 기록하며 기술력을 입증했다.\n\nLG그룹 계열사인 LG유플러스, LG CNS와 슈퍼브 AI, 퓨리오사 AI 등 글로벌 역량을 갖춘 10개 기업이 참여하는 컨소시엄은 AI 반도체부터 서비스까지 아우르는 풀스택 생태계 구축 의지를 보여준다.\n\nSK텔레콤은 실제 서비스로 검증된 AI 경험을 강점으로 인정받았다. 이 회사는 2018년부터 자체 LLM인 A.X(에이닷 엑스) 개발을 시작해 올해 GPT-4o와 견줄 수 있는 성능의 A.X 4.0 모델과 프롬 스크래치 방식의 A.X 3.1 모델을 순차 공개했다.\n\n890만 실사용자를 보유한 '에이닷'과 정확한 정보 제공에 특화된 '라이너' 등 고객 친화적 AI 에이전트를 성공적으로 운영해 온 경험이 경쟁력으로 부각됐다. 이와 함께 SK텔레콤은 반도체부터 서비스까지 독자 기술 기반 풀스택 AI 구현을 목표로 리벨리온의 국산 신경망처리장치(NPU) 'ATOM-Max' 최적화 기술까지 확보했다.\n\n서울대, KAIST, 위스콘신 메디슨 등 국내외 석학들과의 원천기술 연구 협력과 함께 자체 슈퍼컴퓨터 '타이탄(TITAN)'에서 축적한 AI 모델 학습 노하우도 주요 선정 요인으로 작용했다.\n\n업스테이지는 5개 선정팀 중 유일한 AI 전문 스타트업 주관사로 주목받고 있다. 대기업 중심의 AI 생태계에서 스타트업이 국가 프로젝트를 주도한다는 것은 이례적인 일이기 때문이다.\n\n업스테이지의 핵심 경쟁력은 'Solar WBL' 모델을 통한 글로벌 프런티어 수준의 기술력이다. 자체 아키텍처와 학습 알고리즘을 새롭게 설계·구현하고, 모델 사이즈(1000억~3000억 파라미터), 언어(한·영·일·동남아), 멀티모달, 산업별 특화 등으로 점진적 확장을 추진한다는 구체적 로드맵을 제시했다.\n\n3년간 1000만 이상 사용자 확보라는 구체적이고 야심찬 목표와 함께 법률·제조·국방·의료·금융 등 전방위 B2B(기업 간 거래) 서비스 확산 계획도 주목된다. 래블업(GPU 분할 가상화), 노타 AI(모델 학습 및 경량화 최적화), 플리토(데이터 전처리 및 평가) 등과의 기술적 시너지와 KAIST, 서강대학교 등 학계와의 연구 협력도 강점이다.\n\n뷰노(의료), 마키나락스(제조·국방), 로앤컴퍼니(법률) 등 각 분야 선도 기업들과의 산업별 확산 계획은 스타트업 특유의 민첩성과 전문성을 보여준다.\n\n게임업계에서 유일하게 컨소시엄을 이끄는 NC AI의 선정은 14년간 축적한 AI 연구 노하우의 결실로 평가된다. NC AI는 게임 명가 엔씨소프트의 AI 자회사다.\n\n2011년부터 시작된 장기적 R&D 투자와 올해 2월 독립 후 6개월 만에 이룬 이번 국가대표 AI 선정은 게임 특화 AI 기술의 산업 확장 가능성을 입증했다. NC AI의 'VARCO(바르코) LLM'은 2023년 8월 국내 거대언어모델(LLM) 최초로 AWS 플랫폼 등재, 2024년 9월 로직코리아 벤치마크 동급 모델 1위 기록 등 검증된 성과를 보유하고 있다.\n\n최근 공개한 'VARCO Vision 2.0'은 140억과 17억 파라미터의 작은 규모임에도 글로벌 동급 SOTA 멀티모달 모델을 뛰어넘는 성능과 온디바이스 AI 환경 지원으로 차별성을 갖췄다.\n\n특히 총 54개 기관이 참여하는 컨소시엄 구성이 가장 큰 강점이다. ETRI, KAIST, 주요 대학과 포스코DX, 롯데이노베이트 등 40개 수요기업까지 포함한 이 연합체는 대한민국 AI 주권 확보라는 거대한 비전을 제시했다.\n\n글로벌 최고 성능 200B급 독자 LLM 개발과 '도메인옵스' 플랫폼 구축을 통한 산업별 맞춤형 AI 전환 지원 계획도 구체적이다.\n\nSK텔레콤 컨소시엄에 참여하는 게임사 크래프톤의 역할도 독특하다. 차세대 멀티모달 모델 아키텍처 설계 및 학습 알고리즘 연구를 기반으로 게임 도메인 특화 AI 솔루션을 개발할 예정이다.\n\n아울러 AI NPC(게임 속 캐릭터) 및 스토리 엔진 등 게임 콘텐츠 API(응용 프로그래밍 인터페이스) 개발을 담당한다. 크래프톤은 이미 인생시뮬레이션 게임 '인조이(inZOI)'에서 엔비디아와 공동 개발한 게임 특화 소형언어모델(SLM) 기반 CPC(플레이어와 상호 작용할 수 있는 캐릭터)를 선보이며 게임 AI 기술력을 입증했다.\n\n크래프톤 관계자는 '독자적인 AI 기술력을 바탕으로 게임을 비롯한 다양한 산업 분야로의 적용 가능성을 확장하며 한국형 독자 AI 파운데이션 모델 개발에 기여할 것'이라고 밝혔다．")
//                .description("인공지능(AI) 활용이 일상화된 시대에서 대한민국의 디지털 주권을 지키기 위한 프로젝트가 본격 가동한다. 과학기술정보통신부는 4일 '독자 AI 파운데이션 모델' 프로젝트를 이끌 국가대표 최정예 다섯 팀을...")
//                .link("https://n.news.naver.com/mnews/article/001/0015547564?sid=105")
//                .imgUrl("https://n.news.naver.com/mnews/article/003/0013403292?sid=105")
//                .originCreatedDate(LocalDateTime.parse("2025-08-04T14:47:00"))
//                .createdDate(LocalDateTime.parse("2025-08-05T00:01:33"))
//                .mediaName("KBS")
//                .journalist("이형관 기자")
//                .originalNewsUrl("https://news.kbs.co.kr/news/pc/view/view.do?ncd=8321045&ref=A")
//                .newsCategory(NewsCategory.IT)
//                .build();
//
//        realNewsRepository.save(news3);
//        realNewsRepository.save(news4);
//

        List<RealNewsDto> realNewsDtos = realNewsService.getRealNewsListCreatedToday();

        if (realNewsDtos == null || realNewsDtos.isEmpty()) {
            return RsData.of(400, "실제 뉴스 목록이 비어있습니다.");
        }

        for( RealNewsDto realNewsDto : realNewsDtos) {
            log.info("실제 뉴스 ID: {}, 제목: {}", realNewsDto.id(), realNewsDto.title());
        }
        try {
            List<FakeNewsDto> fakeNewsDtos = fakeNewsService.generateAndSaveAllFakeNews(realNewsDtos);

            return RsData.of(200, "가짜 뉴스 생성 성공", fakeNewsDtos);
        } catch (Exception e) {
            return RsData.of(500, "가짜 뉴스 생성 실패: " + e.getMessage());
        }
    }

    @GetMapping("/fake/{id}")
    public FakeNewsDto testGetFakeNews(@PathVariable Long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID는 1 이상의 숫자여야 합니다.");
        }

        return fakeNewsService.getFakeNewsByRealNewsId(id);
    }
    @GetMapping("/fetch")
    public RsData<List<NaverNewsDto>> testFetchNews(@RequestParam String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return RsData.of(400, "검색어가 비어있습니다.");
            }
            // 네이버 API 호출
            CompletableFuture<List<NaverNewsDto>> data = newsDataService.fetchNews(query);
            List<NaverNewsDto> news= data.get();

            return RsData.of(200, "네이버 뉴스 조회 성공", news);
        } catch (Exception e) {
            log.error("네이버 뉴스 조회 실패: {}", e.getMessage());
            return RsData.of(500, "네이버 뉴스 조회 실패: " + e.getMessage());
        }
    }

    private final RealNewsRepository realNewsRepository;
}

