# 📰 뉴스 생성 시스템

## 📌 개요
이 시스템은 **진짜 뉴스**와 **가짜 뉴스**를 자동으로 생성·분석·서비스하는 플랫폼입니다.  
스케줄러와 AI 모델을 활용하여 최신 뉴스를 선별하고, 이를 기반으로 창작된 가짜 뉴스를 퀴즈 형태로 제공합니다.
---

## 🛠 기술 스택
- **Backend**: Java 23, Spring Boot 3.x
- **Database**: MySQL 8.x / JPA
- **AI 연동**: Google Gemini API
- **크롤링**: Jsoup
- **외부 API**: 네이버 뉴스 API
- **Scheduler**: Spring @Scheduled
- **비동기 처리**: CompletableFuture, ThreadPoolTaskExecutor

---

## 1️⃣ 진짜 뉴스 생성 과정

1. **스케줄러 트리거**
   - 매일 정해진 시각에 스케줄러가 진짜 뉴스 생성 작업 시작

2. **AI 프롬프트 호출 및 키워드 생성**
   - AI가 `KeywordHistory` 테이블을 참조하여 **정치, 경제, IT, 사회, 문화** 5개 카테고리별 키워드 2개씩 선별
   - 웹 사전 조사 (주요 헤드라인, 예정 발표, 최근 이슈 등)
   - 최근 사용 키워드 제외
   - `KeywordType`(Enum) 기반으로 진행 중이거나 속보성 기사 포함

3. **네이버 API 호출**
   - 각 키워드별로 N건의 기사를 조회
   - 기사 **제목, 요약** 등 메타데이터 수집

4. **부가 정보 크롤링**
   - 원문 URL, 기자명 등 추가 정보 크롤링

5. **AI 뉴스 분석**
   - 비동기 Batch로 AI 뉴스 분석 프로세서 호출
   - 각 뉴스별 카테고리 분류 및 **완성도, 명확성** 등 기준에 따라 점수 부여

6. **최종 뉴스 선정**
   - 카테고리별 상위 N개 뉴스 필터링
   - 5개 카테고리 중 랜덤으로 최고 점수 뉴스 1건을 **오늘의 뉴스**로 선정

---

## 2️⃣ 가짜 뉴스 생성 과정

1. **스케줄러 트리거**
   - 진짜 뉴스 생성 완료 후, 스케줄러가 가짜 뉴스 생성 작업 시작

2. **가짜 뉴스 AI 생성**
   - 비동기 Batch로 AI 생성 프로세서 호출
   - 진짜 뉴스의 **제목**을 기반으로 허구의 내용 창작
   - 원문 문체, 분량 등을 모방

3. **저장 및 퀴즈 생성**
   - 생성 성공 시 DB에 저장
   - 관련 퀴즈(OX형)와 함께 서비스

---

## 3️⃣ 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Scheduler_Real as 스케줄러(진짜 뉴스)
    participant AI_Keyword as AI 키워드 생성
    participant NaverAPI as 네이버 뉴스 API
    participant Crawler as 부가정보 크롤러
    participant AI_Analysis as AI 뉴스 분석
    participant DB as 데이터베이스
    participant Scheduler_Fake as 스케줄러(가짜 뉴스)
    participant AI_Fake as AI 가짜 뉴스 생성
    participant QuizService as 퀴즈 서비스

    Scheduler_Real->>AI_Keyword: 키워드 생성 요청
    AI_Keyword->>NaverAPI: 카테고리별 기사 조회
    NaverAPI-->>AI_Keyword: 기사 메타데이터 반환
    AI_Keyword->>Crawler: 부가 정보 요청
    Crawler-->>AI_Keyword: 원문 URL, 기자명 등 반환
    AI_Keyword->>AI_Analysis: 뉴스 분석 요청
    AI_Analysis-->>DB: 분석 결과 저장
    AI_Analysis-->>Scheduler_Fake: 진짜 뉴스 생성 완료

    Scheduler_Fake->>AI_Fake: 가짜 뉴스 생성 요청
    AI_Fake->>DB: 진짜 뉴스 제목 조회
    AI_Fake->>AI_Fake: 허구 내용 생성 (문체/분량 모방)
    AI_Fake-->>DB: 가짜 뉴스 저장
    DB-->>QuizService: 퀴즈 데이터 전달
    QuizService-->>QuizService: OX 퀴즈 서비스

---

4️⃣ 아키텍처 다이어그램

graph TB
    %% 외부 API
    subgraph "External API"
        NAVER[네이버 뉴스 API]
        GEMINI[Gemini AI]
    end
    
    %% 스케줄러
    subgraph "Scheduler"
        DAILY[일일 뉴스 처리 00시]
        FAKE[가짜뉴스 생성 01시]
    end
    
    %% 핵심 서비스
    subgraph "Main Services"
        COLLECT[뉴스 수집 NewsDataService]
        ANALYZE[뉴스 분석 AnalysisService]
        BATCH[비동기 배치 BatchService]
        GENERATE[가짜뉴스 생성 FakeNewsService]
    end
    
    %% 데이터베이스
    subgraph "Database"
        REAL_DB[(진짜뉴스)]
        FAKE_DB[(가짜뉴스)]
    end
    
    %% 메인 플로우
    DAILY --> |1.시작| COLLECT
    COLLECT -->|2.키워드 생성 요청| GEMINI
    GEMINI -->|3.키워드 반환| COLLECT
    COLLECT -->|4.API 호출| NAVER
    NAVER -->|5.뉴스 데이터| COLLECT
    COLLECT -->|6.크롤링 후 분석 요청| ANALYZE
    ANALYZE -->|7.뉴스 분석 배치| BATCH
    BATCH -->|8.AI 분석| GEMINI
    GEMINI -->|9.분석 결과| ANALYZE
    ANALYZE -->|10.선별된 뉴스| COLLECT
    COLLECT -->|11.저장| REAL_DB
    
    %% 가짜뉴스 플로우
    FAKE -->|12.뉴스 조회| REAL_DB
    REAL_DB -->|13.데이터| GENERATE
    GENERATE -->|14.가짜 뉴스 배치| BATCH
    BATCH -->|15.AI 생성| GEMINI
    GEMINI -->|16.가짜뉴스 반환| GENERATE
    GENERATE -->|17.저장| FAKE_DB
    
    %% 스타일링
    classDef external fill: #e3f2fd, stroke: #1976d2, stroke-width:2px
    classDef scheduler fill: #fff3e0, stroke: #f57c00, stroke-width:2px
    classDef service fill: #f3e5f5, stroke: #7b1fa2, stroke-width:2px
    classDef database fill: #e8f5e8, stroke: #388e3c, stroke-width:2px
    
    class NAVER,GEMINI external
    class DAILY,FAKE scheduler
    class COLLECT,ANALYZE,BATCH,GENERATE service
    class REAL_DB,FAKE_DB database

---

📂 디렉토리 구조
csharp
복사
편집
src
 └── main
     └── java
         ├── domain                # 도메인별 핵심 비즈니스 로직
         │   ├── member            # 회원 관련
         │   │   ├── member
         │   │   └── quizhistory
         │   ├── news               # 뉴스 관련
         │   │   ├── common
         │   │   ├── fake
         │   │   ├── real
         │   │   └── today
         │   └── quiz               # 퀴즈 관련
         │       ├── daily
         │       ├── detail
         │       └── fact
         │
         └── global                 # 전역(공용) 모듈
             ├── ai                 # AI 연동 모듈
             ├── exception          # 예외 처리
             ├── init               # 초기 설정
             ├── security           # 보안 설정
             ├── util               # 유틸리티
             └── etc...             # 기타 공용 모듈
