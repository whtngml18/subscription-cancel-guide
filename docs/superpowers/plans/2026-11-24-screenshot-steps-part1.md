# 단계 그림 자세히 1부 (Gemini 연결 · 글 프롬프트 · 글쓰기 안내) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI 연결을 Gemini(OpenAI 호환)로 바꾸고, 글에서 그림 정보를 더 많이 뽑는 프롬프트와 개인정보 걸러내기를 넣고, 제보 화면에 글쓰기 안내를 붙인다.

**Architecture:** `FactchatStepParser`를 `OpenAiCompatibleStepParser`로 바꾸고 주소 끝 `/`와 키를 설정으로 뺀다. 시스템 프롬프트는 새 `StepPrompt`가 조각을 이어 만든다(2부에서 [캡처 읽기] 조각 추가). `AiStep`에 `image`·`note` 칸을 더하되 1부에서는 `AiStepsJson`이 항상 null로 만들고, 개인정보 모양 글자를 걸러 낸다. 글쓰기 안내는 `submit.html` + 작은 스크립트, 틀 그대로 제출은 컨트롤러에서 거절.

**Tech Stack:** Spring Boot 4.1.2, Java 21, RestClient, Jackson 3(`tools.jackson`), Thymeleaf, JUnit 5, MockRestServiceServer, MockMvc

**Spec:** `docs/superpowers/specs/2026-11-24-screenshot-steps-design.md` (이 계획은 ①, ③의 글·개인정보·형식 조각, ⑥만)

## Global Constraints

- 기본 주소 `https://generativelanguage.googleapis.com/v1beta/openai`, 요청 경로 `/chat/completions` (`app.ai.trailing-slash: true`일 때만 `/chat/completions/`).
- 키: `AI_API_KEY` → `GEMINI_API_KEY` → `FACTCHAT_API_KEY` 순서. 키 값은 로그·출력 금지.
- 모델은 `AI_MODEL` 설정. 기본값 `gemini-2.5-flash`(Task 4에서 실제 호출로 확인, 안 되면 AI Studio에 보이는 무료 Flash 모델 이름으로 바꾼다).
- 글이 뼈대, 글에 없는 화면·버튼·메뉴 금지. JSON 형식: `{"service":..,"steps":[{"order","screen","target","position","otherMenus","image","note"}]}`. 1부에서 `image`·`note`는 항상 null.
- 개인정보 처리: otherMenus는 그 메뉴만 빼고, note는 null, screen·target에 있으면 그 단계를 뺀다. 남은 단계가 없으면 "AI가 단계를 하나도 만들지 못했어요."
- 안내 문구는 spec ⑥ 그대로. 틀 그대로 제출 오류: "괄호 안을 실제 내용으로 바꿔 주세요."
- 단계마다 사용자 허락(커밋·push·PR·머지). 브랜치 `feat/#39-screenshot-steps`.

## Review Focus

1. DB에 이미 저장된 예전 `aiStepsJson`(image·note 칸 없음)도 그대로 읽혀야 한다 (Task 2 테스트).
2. 개인정보 오탐: "1:1 문의", "2026-11 결제 내역", "고객센터 1588-0000" 같은 메뉴는 지우지 않는다 — 휴대폰(010…)·13자리 이상 카드 번호·이메일만 (Task 2 테스트).
3. 글에 평범한 괄호("(앱)")가 있으면 틀 오류가 나면 안 된다 — 틀의 세 토큰만 검사 (Task 3 테스트).
4. Gemini가 ```json 코드 블록이나 앞뒤 설명을 붙여 답해도 기존 `stripCodeFence`로 읽힌다 (Task 1 테스트 유지).
5. 키 환경변수가 하나도 없으면 앱은 켜지고 AI 버튼만 숨는다 (Task 1 테스트).

---

### Task 1: Gemini 연결 (OpenAiCompatibleStepParser)

**Files:**
- Rename: `src/main/java/com/whtngml/subscription_cancel_guide/ai/FactchatStepParser.java` → `OpenAiCompatibleStepParser.java` (`git mv`)
- Rename: `src/test/java/com/whtngml/subscription_cancel_guide/ai/FactchatStepParserTest.java` → `OpenAiCompatibleStepParserTest.java`
- Modify: `src/main/resources/application.yaml:38-43`
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/ai/AiKeyPropertyTest.java` (새)

**Interfaces:**
- Produces: `OpenAiCompatibleStepParser(AiStepsJson, @Value base-url, api-key, model, @Value("${app.ai.trailing-slash:false}") boolean trailingSlash)`; 테스트 생성자 `OpenAiCompatibleStepParser(RestClient, AiStepsJson, String apiKey, String model, boolean trailingSlash)`. `SYSTEM_PROMPT` 상수는 Task 2에서 `StepPrompt`로 옮긴다(이 Task에서는 그대로).
- 메시지 형식: `ChatMessage(String role, Object content)` — system은 문자열, user는 `List.of(new TextPart("text", "서비스: ...\n해지 방법 글:\n..."))`. `record TextPart(String type, String text)`.

- [ ] **Step 1: 테스트 수정·추가** (`OpenAiCompatibleStepParserTest`)
  - 기존 `URL`을 `https://ai.test/v1beta/openai/chat/completions`로, baseUrl을 `https://ai.test/v1beta/openai`로, 생성자에 `false`.
  - 기존 `jsonPath("$.messages[1].content")` 검사를 `jsonPath("$.messages[1].content[0].type").value("text")`, `jsonPath("$.messages[1].content[0].text").value(containsString("넷플릭스"))`로.
  - `학교_게이트웨이는_끝에_슬래시`: 생성자 `true` → `requestTo(".../chat/completions/")` 한 번 응답 OK.
  - 기존 4개(재요청·두 번 실패·서버 오류·키 없음)는 그대로 통과해야 한다.
- [ ] **Step 2: 키 우선순위 테스트** (`AiKeyPropertyTest`, `@SpringBootTest(properties = {"GEMINI_API_KEY=g-key", "FACTCHAT_API_KEY=f-key"})`)
  - `env.getProperty("app.ai.api-key")` == `"g-key"`.
  - 중첩 클래스 또는 두 번째 테스트 클래스 `@SpringBootTest(properties = {"AI_API_KEY=a-key", "GEMINI_API_KEY=g-key"})` → `"a-key"`.
  - 세 번째: 아무 키도 없음(기본 테스트 컨텍스트) → `StepParser.isAvailable()` false.
- [ ] **Step 3: 실패 확인** — `./gradlew test --tests "*StepParserTest" --tests "*AiKeyPropertyTest"` → 컴파일 실패/FAIL
- [ ] **Step 4: 구현** — `git mv`로 이름 변경, `.uri(trailingSlash ? "/chat/completions/" : "/chat/completions")`. yaml:
  ```yaml
  # 제보 글을 단계로 나누는 AI (OpenAI 호환 주소: 기본 Google Gemini). 키는 저장소에 넣지 않고 환경변수로 넣는다.
  # 키가 없으면 앱은 그대로 켜지고 AI 미리보기 버튼만 숨긴다. 학교 factchat으로 돌아갈 때는 AI_BASE_URL, AI_MODEL, AI_TRAILING_SLASH=true.
  ai:
    base-url: ${AI_BASE_URL:https://generativelanguage.googleapis.com/v1beta/openai}
    api-key: ${AI_API_KEY:${GEMINI_API_KEY:${FACTCHAT_API_KEY:}}}
    model: ${AI_MODEL:gemini-2.5-flash}
    trailing-slash: ${AI_TRAILING_SLASH:false}
  ```
  `application-prod.yaml`에 ai 설정이 있으면 같은 규칙으로 맞춘다.
- [ ] **Step 5: 전체 테스트** — `./gradlew test` → 실패 0
- [ ] **Step 6: 커밋** — `feat: AI 연결을 OpenAI 호환 Gemini 기본으로 바꾸기`

---

### Task 2: 프롬프트 조각과 결과 검사 (image·note·개인정보)

**Files:**
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/ai/StepPrompt.java`
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/ai/PersonalInfo.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/dto/AiStep.java` (칸 추가)
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/ai/AiStepsJson.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/ai/OpenAiCompatibleStepParser.java` (`SYSTEM_PROMPT` → `StepPrompt.system()`)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/ai/StepPromptTest.java`, `PersonalInfoTest.java`, `AiStepsJsonTest.java`

**Interfaces:**
- Consumes: Task 1의 파서.
- Produces:
  - `AiStep(int order, String screen, String target, String position, List<String> otherMenus, Integer image, String note)` — `toForm()`은 image·note를 쓰지 않는다.
  - `final class StepPrompt { static String system(); }` — 조각 상수 `ROLE`, `PRINCIPLES`, `FROM_TEXT`, `PERSONAL_INFO`, `FORMAT`, `EXAMPLE`를 빈 줄로 이어 붙인다. 문구는 spec ③의 "사진이 없을 때" 프롬프트(원칙 1~3, 글에서 뽑기 4~7, 개인정보 8, 형식 9~10, 예시)를 그대로.
  - `final class PersonalInfo { static boolean contains(String text); }` — 이메일 `[\w.+-]+@[\w-]+(\.[\w-]+)+`, 휴대폰 `01[016789][-. ]?\d{3,4}[-. ]?\d{4}`, 카드 숫자(공백·- 제외) 13~19자리 연속 `\d(?:[ -]?\d){12,18}`.
  - `AiStepsJson.read(String json)`: 각 단계 image=null, note=null(1부). screen·target이 `PersonalInfo.contains`면 그 단계 제외, otherMenus는 해당 메뉴 제외. 남은 단계 0개면 기존 "AI가 단계를 하나도 만들지 못했어요." 번호는 남은 단계로 1부터.

- [ ] **Step 1: 실패하는 테스트**
  - `StepPromptTest.글_우선과_지어내기_금지_위치표_예시가_들어간다`: `system()`에 `"사용자 글에 있는 내용만"`, `"절대 만들지 마"`, `"오른쪽 위/우측 상단 → top-right"`, `"카드 번호"`, `"\"image\":null"`, `"구매 및 멤버십"` 포함, `"캡처"` 미포함.
  - `PersonalInfoTest`: true — `"me@test.com"`, `"010-1234-5678"`, `"01012345678"`, `"1234 5678 9012 3456"`; false — `"1:1 문의"`, `"2026-11 결제 내역"`, `"고객센터 1588-0000"`, `"멤버십 해지"`.
  - `AiStepsJsonTest.image와_note는_1부에서_null`: 응답에 `"image":2,"note":"다름"` → 읽은 값 둘 다 null.
  - `AiStepsJsonTest.예전_JSON도_읽는다`: image·note 칸 없는 JSON → 정상, null.
  - `AiStepsJsonTest.개인정보_메뉴는_빼고_단계는_뺀다`: 단계1 otherMenus `["계정","me@test.com"]` → `["계정"]`; 단계2 target `"010-1234-5678"` → 제외되고 결과 1단계, order 1.
  - `AiStepsJsonTest.모두_개인정보면_거절`: 유일한 단계 screen이 이메일 → `StepParseException`.
  - 기존 `new AiStep(...)` 호출은 끝에 `null, null` 추가.
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*StepPromptTest" --tests "*PersonalInfoTest" --tests "*AiStepsJsonTest"` → FAIL
- [ ] **Step 3: 구현** (Interfaces 그대로). 파서의 system 메시지는 `StepPrompt.system()`.
- [ ] **Step 4: 전체 테스트** — `./gradlew test` → 실패 0
- [ ] **Step 5: 커밋** — `feat: 글에서 그림 정보를 더 뽑는 프롬프트와 개인정보 걸러내기`

---

### Task 3: 제보 화면 글쓰기 안내

**Files:**
- Modify: `src/main/resources/templates/submit.html:24-33`
- Create: `src/main/resources/static/js/submit-guide.js`
- Modify: `src/main/resources/static/css/site.css` (안내 상자·예시 비교 몇 줄)
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/dto/SubmissionForm.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/controller/SubmissionController.java` (preview·submit)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/web/PublicPagesTest.java`

**Interfaces:**
- Produces: `SubmissionForm.TEMPLATE_TOKENS = List.of("(어디쯤)", "(버튼 이름)", "(해지 버튼 이름)")`, `boolean hasTemplateLeft()` (rawText에 토큰 하나라도 있으면 true). 컨트롤러 preview·submit 둘 다 `rawText`에 오류가 없고 `hasTemplateLeft()`면 `rejectValue("rawText", "template", "괄호 안을 실제 내용으로 바꿔 주세요.")`.

화면: textarea 위 `<div class="write-guide" id="writeGuide">` — 제목·1~4번 안내·좋은 예/아쉬운 예(spec ⑥ 문구 그대로), 버튼 `<button type="button" id="fillTemplate" class="btn btn-secondary btn-sm">예시 넣어 보기</button>`(`hidden` 속성으로 시작, 스크립트가 보이게 함). placeholder는 좋은 예 문장. 도움말 아래 `<p class="field-help" id="rawTextCount" aria-live="polite"></p>`. 스크립트: 버튼 클릭 시 textarea가 비었을 때만 spec ⑥ 틀을 넣고 focus, input마다 `"N / 1000"`.

- [ ] **Step 1: 실패하는 테스트** (`PublicPagesTest`)
  - `submitShowsWriteGuide`: GET `/submit` → `"이렇게 적어 주시면 그림이 정확해져요"`, `"설정에서 해지하면 돼요."`, `id="fillTemplate"`, `/js/submit-guide.js` 포함.
  - `rejectsTemplateLeft`: POST `/submit` serviceName=왓챠, rawText=`"앱 / 웹:\n1. (어디쯤) '(버튼 이름)'을 눌러요."` → 200, `"괄호 안을 실제 내용으로 바꿔 주세요."`, 제보 저장 안 됨(`submissionRepository.count()` 변화 없음).
  - `allowsNormalParentheses`: rawText=`"앱(아이폰)에서 오른쪽 위 프로필을 눌러요."` → 3xx(기존 성공 흐름과 같은 결과).
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*PublicPagesTest"` → FAIL
- [ ] **Step 3: 구현** (위 Interfaces·화면 설명대로)
- [ ] **Step 4: 전체 테스트** — `./gradlew test` → 실패 0
- [ ] **Step 5: 화면 확인** — 앱 실행, `/submit` 데스크톱·375px 스크린샷(안내 상자, 예시 버튼으로 틀 채움, 글자 수), 가로 스크롤 없음. 끝나면 앱 종료.
- [ ] **Step 6: 커밋** — `feat: 제보 화면에 글쓰기 안내와 예시 틀`

---

### Task 4: 실제 Gemini 확인 (사용자가 `GEMINI_API_KEY`를 넣은 뒤)

**Files:** 없음 (모델 이름이 틀리면 `application.yaml`의 기본값만 수정)

- [ ] **Step 1: 키 확인** — `powershell [Environment]::GetEnvironmentVariable('GEMINI_API_KEY','User')`가 비어 있지 않은지만 확인(값 출력 금지). 없으면 사용자에게 요청하고 멈춘다.
- [ ] **Step 2: 앱 실행 후 미리보기 3번** — 키를 export하고 bootRun, python urllib로 `/submit/preview`에 글 3개(짧은 글 "설정에서 해지", 안내대로 쓴 좋은 예, 위치·메뉴 많은 글) 전송, 화면에 나온 단계(화면·버튼·위치·다른 메뉴) 기록. 404/400 모델 오류면 AI Studio 무료 Flash 모델 이름으로 기본값을 바꾸고 다시.
- [ ] **Step 3: 보고** — 3개 결과를 표로 사용자에게 보여 주고, 글에 없는 메뉴가 생겼는지 확인. 앱 종료.
- [ ] **Step 4: (모델 이름을 바꿨다면) 커밋** — `fix: Gemini 기본 모델 이름`
