# 화면 개편 1단계 (디자인 기본 + 사용자 화면) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자 화면(홈·카테고리·검색·가이드·제보·완료·오류·오프라인)을 Bootstrap 없이 `site.css` 하나로 다시 만들어, 핵심 정보가 첫 화면에 보이고 단계가 한 번만 나오게 한다. DB는 바꾸지 않는다.

**Architecture:** 공통 조각(`fragments/layout.html`)의 `pageHead`·`noindexHead`가 새 `site.css`와 Pretendard를 불러온다. 관리자 화면은 3단계에서 바꾸므로 새 조각 `adminHead`로 옮겨 Bootstrap을 계속 쓴다. 목록 줄·서비스 아이콘·단계 목록·위치 표시는 각각 작은 조각으로 만들고, 홈 목록 데이터와 가이드 요약 문장은 새 자바 클래스(`GuideListService`, `GuideSummary`)가 만든다.

**Tech Stack:** Spring Boot 4.1.2, Java 21, Thymeleaf, JPA(H2), Spring Security, JUnit 5 + MockMvc(`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`)

**Spec:** `docs/superpowers/specs/2026-11-21-ui-redesign-design.md` (4장, 5장, 8장의 1번, 9장)

## Global Constraints

- 색 변수(`:root`): 배경 `#fff`, 글자 `#1f2328`, 보조 글자 `#59636e`, 선 `#d0d7de`, 강조 `#1f4fa8`
- 난이도 색: 쉬움 `#1a7f37`/`#e7f5ec`, 보통 회색, 어려움 `#b42318`/`#fdecea` (1단계에서는 변수만 정의, 화면에는 아직 안 씀)
- 주의 상자: 글자 `#9a3412`, 배경 `#fff6f1`, 테두리 `#f5c2a8`
- 글꼴: Pretendard `https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css`, 본문 16px, 줄 간격 1.6
- 본문 최대 폭: 사용자 화면 720px
- 조각 이름에 head·header·footer를 쓰지 않는다. 기존 인자 `pageHead(title, description, path)`, `noindexHead(title)`, `siteHeader(showSearch)`는 그대로 둔다.
- 작은 휴대폰 도식 가로 50px, 화면 폭 400px 미만에서는 글 아래로 내려간다.
- DB·엔티티는 바꾸지 않는다. 아직 없는 정보(해지하는 곳, 난이도, 주의할 점, 예상 시간)는 "아직 확인된 정보가 없어요. 알고 계시면 알려 주세요 →" + `/submit?name={서비스 이름}` 링크로 둔다.
- 아이콘은 글자 아이콘만: 브랜드 색 네모 + 서비스 이름 첫 글자. 브랜드 색이 없으면 `#59636e`.
- 바깥 링크는 새 탭 + `rel="noopener noreferrer"`.
- 화면 글자는 모두 `th:text`로 넣는다(`th:utext` 금지).
- 한국어 줄바꿈: `word-break: keep-all`, 긴 낱말은 `overflow-wrap: anywhere`.
- 기존 테스트 12개가 계속 통과해야 한다.
- 커밋 메시지 `feat: ...`, 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. 커밋·push·PR·머지는 매번 사용자 허락을 받는다.

## Review Focus

1. 단계가 0개인 가이드(예시 데이터의 티빙) — 요약 문장·경로·단계 칸이 깨지지 않고 "아직 등록된 해지 단계가 없어요"가 나와야 한다. → Task 3 테스트 `guideWithoutSteps`
2. 브랜드 색이 비어 있는 서비스 — 아이콘이 회색 네모로 나와야 하고 `style="--brand: null"` 같은 글이 들어가면 안 된다. → Task 2 테스트 `iconWithoutBrandColor`
3. 검색어에 `<script>`나 따옴표 — 그대로 실행되지 않고 글자로 보여야 한다. → Task 2 테스트 `searchEscapesKeyword`
4. 자바스크립트가 꺼진 브라우저 — 카테고리 탭이 `/category/{slug}` 링크로 동작해야 한다. → Task 2 테스트 `homeTabsAreLinks`
5. 375px 화면에서 아주 긴 서비스 이름·버튼 이름 — 가로 스크롤이 생기지 않아야 한다(`overflow-wrap: anywhere`, `min-width: 0`). → Task 6 화면 확인에서 50자 이름으로 직접 확인

---

## 파일 구조

| 파일 | 할 일 |
|---|---|
| Create `static/css/site.css` | 변수, 기본 글꼴, 머리글·바닥글, 버튼, 칩, 알림 상자, 주의 상자, 폼, 목록 줄, 서비스 아이콘, 단계 목록, 작은 휴대폰, 페이지 넘김 |
| Modify `templates/fragments/layout.html` | `pageHead`·`noindexHead`를 site.css로, `adminHead(title)` 추가, 머리글·바닥글 새 모양, `breadcrumb(category, current)` 추가 |
| Create `templates/fragments/ui.html` | `serviceIcon(subscription)`, `serviceRow(subscription, stepCount)`, `missingInfo(name)` |
| Create `templates/fragments/steps.html` | `stepList(steps, color)`: 세로선 번호 목록 + 작은 휴대폰 |
| Modify `templates/admin/*.html` (6개) | `noindexHead` → `adminHead` |
| Create `service/GuideListService.java` | 홈 묶음 목록, 서비스별 단계 수 |
| Create `dto/CategoryGroup.java` | `record CategoryGroup(Category category, List<Subscription> subscriptions)` |
| Create `dto/GuideSummary.java` | 가이드 요약 문장과 경로 |
| Modify `repository/SubscriptionRepository.java`, `GuideStepRepository.java`, `FeedbackRepository.java` | 조회 메서드 추가 |
| Modify `service/FeedbackService.java` | `countFor(Subscription)` |
| Modify `controller/HomeController.java`, `CategoryController.java`, `SearchController.java`, `GuideController.java` | 새 모델 값 |
| Modify `templates/index.html`, `category.html`, `search.html`, `guide.html`, `submit.html`, `submit-done.html` | 새 모양 |
| Create `static/js/home-filter.js` | 홈 카테고리 탭 바로 거르기 |
| Create `templates/error/404.html`, `templates/error/500.html` | 오류 화면 |
| Modify `static/offline.html`, `static/service-worker.js`, `static/manifest.json` | 새 색·새 CSS |
| Test `src/test/.../web/PublicPagesTest.java` | 화면 렌더링 확인(MockMvc) |
| Test `src/test/.../dto/GuideSummaryTest.java` | 요약 문장·경로 |
| Test `src/test/.../web/ErrorPagesTest.java` | 404 화면(실제 서버) |

`app.css`, `phone.css`, `fragments/phone.html`은 관리자 화면이 아직 쓰므로 3단계까지 남긴다.

모든 화면 테스트는 아래 틀을 쓴다(예시 데이터 `data.sql`: 넷플릭스 3단계·`#E50914`, 티빙 0단계, 멜론 3단계).

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // 테스트에서 저장한 서비스·피드백은 끝나면 되돌린다
class PublicPagesTest {
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categoryRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    // 테스트마다 mvc.perform(get(...)).andExpect(status().isOk()).andExpect(content().string(containsString(...)))
}
```

---

### Task 1: 디자인 기본과 공통 조각

**Files:**
- Create: `src/main/resources/static/css/site.css`
- Create: `src/main/resources/templates/fragments/ui.html`, `src/main/resources/templates/fragments/steps.html`
- Modify: `src/main/resources/templates/fragments/layout.html`
- Modify: `src/main/resources/templates/admin/{guides,login,step-form,submission,submissions,subscription-form}.html` (3번째 줄만)
- Modify: `src/main/resources/static/service-worker.js`, `static/manifest.json`, `static/offline.html`
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/web/PublicPagesTest.java`

**Interfaces:**
- Produces (조각, 다른 Task가 `th:replace`로 씀):
  - `fragments/layout :: adminHead(title)` — noindex + Bootstrap + app.css + phone.css (지금 `noindexHead` 내용 그대로)
  - `fragments/layout :: breadcrumb(category, current)` — `category`는 `Category` 또는 `null`, `current`는 글자 또는 `null`. `<nav aria-label="위치">` 안에 `홈 › 카테고리 › current`, 마지막 항목은 링크 없이 `aria-current="page"`
  - `fragments/ui :: serviceIcon(subscription)` — `<span class="svc-icon" aria-hidden="true" style="--brand: #E50914">넷</span>`; 브랜드 색이 비면 `style` 속성 자체를 빼고 CSS 기본값 `#59636e`
  - `fragments/ui :: serviceRow(subscription, stepCount)` — `<a class="svc-row" href="/guide/{slug}">` 아이콘 + 이름 + 보조 줄. 보조 줄은 `stepCount > 0`이면 `"3단계"`, 0이면 `"단계 준비 중"`
  - `fragments/ui :: missingInfo(name)` — `<p class="missing">아직 확인된 정보가 없어요. <a href="/submit?name=...">알고 계시면 알려 주세요 →</a></p>`
  - `fragments/steps :: stepList(steps, color)` — `steps`는 `GuideStep` 또는 `GuideStepForm` 목록(둘 다 `stepOrder, screenName, targetLabel, position, description`). `<ol class="steps">`, 항목마다 번호 동그라미, 제목 `<h3>{screenName}에서 <b>{targetLabel}</b></h3>`, 설명, 작은 휴대폰 `<div class="mini-phone pos-{position}" aria-hidden="true">`(position 비면 `pos-middle`)
- CSS 클래스 이름(다른 Task가 씀): `.wrap`(720px 본문), `.btn`, `.btn-primary`, `.btn-secondary`, `.chip`, `.chips`, `.notice`, `.caution`, `.summary-box`, `.missing`, `.field`, `.field-error`, `.svc-list`, `.svc-row`, `.svc-icon`, `.tabs`, `.steps`, `.mini-phone`, `.pager`, `.breadcrumb`, `.lead`

- [ ] **Step 1: 실패하는 테스트 작성** — `PublicPagesTest`에 아래 세 개

```java
@Test void userPagesUseSiteCss() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk())
       .andExpect(content().string(containsString("/css/site.css")))
       .andExpect(content().string(containsString("pretendard")))
       .andExpect(content().string(not(containsString("bootstrap"))));
}
@Test void adminPagesKeepBootstrap() throws Exception {
    mvc.perform(get("/admin/login")).andExpect(status().isOk())
       .andExpect(content().string(containsString("bootstrap")));
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*PublicPagesTest"` → `userPagesUseSiteCss` FAIL(bootstrap 있음)

- [ ] **Step 3: `site.css` 작성** — Global Constraints의 값을 `:root` 변수로(`--bg --text --muted --line --accent --easy --easy-bg --hard --hard-bg --caution --caution-bg --caution-line`). 작은 휴대폰: `width:50px; aspect-ratio: 9/16; border:2px solid var(--text); border-radius:8px; position:relative`, 위쪽에 브랜드 색 막대, 누를 곳은 강조색 점(`.pos-top-left` 왼쪽 위, `.pos-top-right` 오른쪽 위, `.pos-middle` 가운데, `.pos-bottom` 아래 가운데). 단계 항목은 `grid-template-columns: 32px 1fr 50px`, `@media (max-width: 399px)`에서 휴대폰이 두 번째 칸 아래로. 단계 목록 세로선은 번호 동그라미 사이를 잇는 `::before`. 포커스는 `:focus-visible { outline: 3px solid var(--accent); outline-offset: 2px }`.

- [ ] **Step 4: `layout.html` 수정**
  - `pageHead`·`noindexHead`: Bootstrap·app.css·phone.css 줄을 Pretendard + `@{/css/site.css}`로 바꾸고 `theme-color`를 `#1f4fa8`로.
  - `adminHead(title)` 추가: 지금 `noindexHead`의 내용 그대로(Bootstrap 포함). 관리자 템플릿 6개의 3번째 줄 `noindexHead` → `adminHead`.
  - `siteHeader(showSearch)`: `<header class="site-header"><div class="wrap">` 사이트 이름 링크, `showSearch`일 때 작은 검색 폼(`/search`, `name="q"`, `th:value="${keyword}"`, `aria-label="서비스 이름 검색"`), "제보하기" 링크.
  - `siteFooter`: 지금 문구 두 줄 그대로, 새 클래스.
  - `adminHeader`는 손대지 않는다.
  - `breadcrumb(category, current)` 추가.

- [ ] **Step 5: `fragments/ui.html`, `fragments/steps.html` 작성** — Interfaces 그대로. 첫 글자는 `${#strings.substring(subscription.name, 0, 1)}`.

- [ ] **Step 6: PWA 파일** — `service-worker.js`: `CACHE_NAME = 'cancel-guide-v2'`, `PRECACHE_URLS`에서 app.css·phone.css를 빼고 `'/css/site.css'` 추가. `manifest.json`: `theme_color` `#1f4fa8`, `background_color` `#ffffff`. `offline.html`: 같은 색 변수를 파일 안 `<style>`에 넣고(오프라인이라 외부 글꼴 없이 `system-ui`), 버튼 색 `#1f4fa8`, 배경 흰색.

- [ ] **Step 7: 테스트 통과 확인** — `./gradlew test` → 전체 PASS(기존 12개 + 새 2개).

- [ ] **Step 8: 커밋(허락 후)**

```bash
git add src/main/resources/static src/main/resources/templates/fragments src/main/resources/templates/admin src/test/java/com/whtngml/subscription_cancel_guide/web
git commit -m "feat: 새 디자인 기본(site.css)과 공통 조각"
```

---

### Task 2: 홈·카테고리·검색 목록

**Files:**
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/service/GuideListService.java`
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/dto/CategoryGroup.java`
- Create: `src/main/resources/static/js/home-filter.js`
- Modify: `repository/SubscriptionRepository.java`, `repository/GuideStepRepository.java`
- Modify: `controller/HomeController.java`, `controller/CategoryController.java`, `controller/SearchController.java`
- Modify: `templates/index.html`, `templates/category.html`, `templates/search.html`
- Test: `PublicPagesTest.java`

**Interfaces:**
- Consumes: `serviceRow(subscription, stepCount)`, `breadcrumb(category, current)`, CSS `.tabs .svc-list .pager`
- Produces:
  - `SubscriptionRepository`: `@EntityGraph(attributePaths = "category") List<Subscription> findAllByOrderByNameAsc();`
  - `GuideStepRepository`: `@Query("select s.subscription.id, count(s) from GuideStep s group by s.subscription.id") List<Object[]> countStepsBySubscription();`
  - `GuideListService.groupedByCategory(): List<CategoryGroup>` — 카테고리 id 순, 서비스가 없는 카테고리는 뺀다, 묶음 안은 이름 순
  - `GuideListService.stepCounts(): Map<Long, Long>` — key 서비스 id, 단계 없는 서비스는 key 없음(화면에서 `${stepCounts[s.id] ?: 0}`)
  - 모델 값: 홈 `categories`(전체, 탭용), `groups`, `stepCounts` / 카테고리·검색 `stepCounts` 추가

- [ ] **Step 1: 실패하는 테스트 작성**

```java
@Test void homeListsAllServicesWithStepCount() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk())
       .andExpect(content().string(containsString("넷플릭스")))
       .andExpect(content().string(containsString("멜론")))
       .andExpect(content().string(containsString("3단계")))
       .andExpect(content().string(containsString("단계 준비 중")))   // 티빙
       .andExpect(content().string(containsString("찾는 서비스가 없나요?")));
}
@Test void homeTabsAreLinks() throws Exception {
    mvc.perform(get("/")).andExpect(content().string(containsString("href=\"/category/music\"")))
       .andExpect(content().string(containsString("/js/home-filter.js")));
}
@Test void categoryPageHasBreadcrumbAndRows() throws Exception {
    mvc.perform(get("/category/ott")).andExpect(status().isOk())
       .andExpect(content().string(containsString("aria-label=\"위치\"")))
       .andExpect(content().string(containsString("href=\"/guide/netflix\"")))
       .andExpect(content().string(containsString("svc-icon")));
}
@Test void searchEscapesKeyword() throws Exception {
    mvc.perform(get("/search").param("q", "<script>x</script>")).andExpect(status().isOk())
       .andExpect(content().string(not(containsString("<script>x</script>"))))
       .andExpect(content().string(containsString("&lt;script&gt;")))
       .andExpect(content().string(containsString("가이드가 아직 없어요")))
       .andExpect(content().string(containsString("/submit?name=")));
}
@Test void iconWithoutBrandColor() throws Exception {
    Category ott = categoryRepository.findBySlug("ott").orElseThrow();
    subscriptionRepository.save(new Subscription(ott, "왓챠", "watcha", null, null, null));
    mvc.perform(get("/category/ott")).andExpect(status().isOk())
       .andExpect(content().string(containsString("href=\"/guide/watcha\"")))
       .andExpect(content().string(not(containsString("--brand: null"))))
       .andExpect(content().string(not(containsString("--brand: ;"))));
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*PublicPagesTest"` → 위 5개 FAIL

- [ ] **Step 3: 저장소 메서드, `CategoryGroup`, `GuideListService` 구현** — `@Service @RequiredArgsConstructor @Transactional(readOnly = true)`, 묶기는 `Collectors.groupingBy(s -> s.getCategory(), LinkedHashMap::new, toList())` 대신 카테고리 목록(`categoryRepository.findAll(Sort.by("id"))`)을 돌며 거른다(Category에 equals가 없으므로 id로 비교).

- [ ] **Step 4: 컨트롤러 3개에 모델 값 추가** — 홈은 `categoryRepository.findAll(Sort.by("id"))`를 `categories`로.

- [ ] **Step 5: 템플릿**
  - `index.html`: 제목 "구독 해지 가이드", 한 줄 소개 "넷플릭스, 멜론 같은 구독 서비스를 어디서 어떻게 해지하는지 단계별로 알려드려요.", 큰 검색 폼, `<nav class="tabs" aria-label="카테고리">`(첫 탭 "전체" `href="/"`, 나머지 `/category/{slug}`, 각 `data-category="{slug}"`), 묶음마다 `<section class="svc-group" data-category="{slug}"><h2>`+`<ul class="svc-list">` 안에 `serviceRow`, 맨 아래 "찾는 서비스가 없나요? <a href="/submit">제보하기 →</a>", `<script src="/js/home-filter.js" defer>`.
  - `category.html`: `breadcrumb(null, ${category.name})`(홈 › OTT), 제목, `svc-list`, 페이지 넘김 `.pager`(이전·"1 / 2"·다음, 없는 쪽은 `aria-disabled="true"`인 `<span>`).
  - `search.html`: 제목 `'키워드' 검색 결과`, 결과 없으면 `.notice` 안에 `'키워드' 가이드가 아직 없어요` + "직접 해지해 보셨다면 방법을 알려 주세요." + `/submit?name={keyword}` 버튼, 있으면 `svc-list`, 같은 `.pager`.

- [ ] **Step 6: `home-filter.js`** — 탭 클릭 시(`data-category`가 있는 탭만) `preventDefault`, 같은 `data-category`의 `.svc-group`만 보이고 나머지는 `hidden`, "전체"는 모두 보임, 누른 탭에 `aria-current="page"`. 수정 키(Ctrl·Cmd·Shift)나 가운데 클릭이면 기본 동작(새 탭)을 막지 않는다.

- [ ] **Step 7: 테스트 통과 확인** — `./gradlew test` → 전체 PASS

- [ ] **Step 8: 커밋(허락 후)** — `git commit -m "feat: 홈 전체 목록·카테고리 탭과 목록 화면 새 디자인"`

---

### Task 3: 가이드 화면

**Files:**
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/dto/GuideSummary.java`
- Modify: `repository/FeedbackRepository.java`, `service/FeedbackService.java`, `controller/GuideController.java`
- Modify: `templates/guide.html`
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/dto/GuideSummaryTest.java`, `PublicPagesTest.java`

**Interfaces:**
- Consumes: `breadcrumb`, `serviceIcon`, `missingInfo`, `stepList(steps, color)`; 기존 `FeedbackCount(Long subscriptionId, long helpful, long notHelpful)`
- Produces:
  - `record GuideSummary(String sentence, String route)` + `static GuideSummary of(List<GuideStep> steps)`
    - 단계 n개(n ≥ 1): `sentence = n + "단계로 해지할 수 있어요."`, 0개: `"아직 등록된 해지 단계가 없어요."`
    - `route`: 단계 순서대로 `screenName`을 이어 붙이되 바로 앞과 같으면 한 번만, 끝에 마지막 단계의 `targetLabel`, 구분자 `" › "`. 0개면 `null`
  - `FeedbackRepository`: `long countBySubscription(Subscription s); long countBySubscriptionAndHelpfulTrue(Subscription s);`
  - `FeedbackService.countFor(Subscription s): FeedbackCount`
  - 모델 값 추가: `summary`(GuideSummary), `feedbackCount`(FeedbackCount)

- [ ] **Step 1: 실패하는 테스트 작성** — `GuideSummaryTest`(엔티티는 `new GuideStep(null, 1, "홈", "프로필 아이콘", "top-right", null)`처럼 만든다)

```java
@Test void sentenceAndRoute() {
    var s = GuideSummary.of(List.of(step(1,"홈","프로필"), step(2,"프로필","계정"), step(3,"계정","멤버십 해지")));
    assertEquals("3단계로 해지할 수 있어요.", s.sentence());
    assertEquals("홈 › 프로필 › 계정 › 멤버십 해지", s.route());
}
@Test void repeatedScreenAppearsOnce() {
    var s = GuideSummary.of(List.of(step(1,"설정","더보기"), step(2,"설정","해지")));
    assertEquals("설정 › 해지", s.route());
}
@Test void noSteps() {
    var s = GuideSummary.of(List.of());
    assertEquals("아직 등록된 해지 단계가 없어요.", s.sentence());
    assertNull(s.route());
}
```

`PublicPagesTest`에 추가:

```java
@Test void guideShowsSummaryFirstAndStepsOnce() throws Exception {
    mvc.perform(get("/guide/netflix")).andExpect(status().isOk())
       .andExpect(content().string(containsString("넷플릭스 해지 방법")))
       .andExpect(content().string(containsString("3단계로 해지할 수 있어요.")))
       .andExpect(content().string(containsString("class=\"steps\"")))
       .andExpect(content().string(not(containsString("phone-list"))))
       .andExpect(content().string(containsString("aria-label=\"위치\"")))
       .andExpect(content().string(containsString("/submit?name=")))         // 빈 정보 칸
       .andExpect(content().string(containsString("application/ld+json")));  // SEO 유지
}
@Test void guideWithoutSteps() throws Exception {
    mvc.perform(get("/guide/tving")).andExpect(status().isOk())
       .andExpect(content().string(containsString("아직 등록된 해지 단계가 없어요.")))
       .andExpect(content().string(not(containsString("application/ld+json"))));
}
@Test void guideShowsFeedbackCount() throws Exception {
    mvc.perform(post("/guide/netflix/feedback").param("helpful", "true").with(csrf()));
    mvc.perform(get("/guide/netflix")).andExpect(content().string(containsString("1명 중 1명이 됐다고 했어요")));
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*GuideSummaryTest" --tests "*PublicPagesTest"` → 새 테스트 FAIL

- [ ] **Step 3: `GuideSummary`, 저장소·서비스 메서드, 컨트롤러 모델 값 구현**

- [ ] **Step 4: `guide.html`** — 스펙 5장 "가이드" 순서대로:
  1. `breadcrumb(${subscription.category}, ${subscription.name})`
  2. `serviceIcon` + `<h1>{이름} 해지 방법</h1>`
  3. `<p class="lead">` `summary.sentence`
  4. `.chips`: 단계 수(`3단계`), 확인일(`2026.11.12 확인`), 출처(`source.label`). 난이도·해지하는 곳·예상 시간 칩은 1단계에서 넣지 않는다.
  5. 공식 해지 링크 버튼: 1단계에는 데이터가 없으므로 넣지 않는다(2단계).
  6. `.summary-box`: "경로" `summary.route`(없으면 줄 생략), "환불" `refundInfo`(없으면 `missingInfo`)
  7. "해지하는 곳 · 주의할 점" 줄: `missingInfo(${subscription.name})`
  8. `<h2>해지 단계</h2>` + `stepList(${steps}, ${subscription.brandColor})`, 비면 `summary.sentence`
  9. 피드백 `id="feedback"`: 지금 폼 그대로(버튼 글자 "됐어요"/"달라요", 값 `true`/`false`), `feedbackCount.helpful + notHelpful > 0`이면 `"{합}명 중 {helpful}명이 됐다고 했어요"`
  - 맨 아래 JSON-LD 블록은 그대로 둔다.

- [ ] **Step 5: 테스트 통과 확인** — `./gradlew test` → 전체 PASS

- [ ] **Step 6: 커밋(허락 후)** — `git commit -m "feat: 가이드 화면 요약 우선·단계 한 번만 보이게 개편"`

---

### Task 4: 제보·제보 완료 화면

**Files:**
- Modify: `templates/submit.html`, `templates/submit-done.html`
- Test: `PublicPagesTest.java`

**Interfaces:**
- Consumes: `stepList(steps, color)` (AI 미리보기 `aiSteps`는 `List<GuideStepForm>`, color는 `null`), CSS `.field .field-error .notice .btn`
- Produces: 없음. 폼 칸 id(`serviceName`, `rawText`, `aiStepsJson`)와 `formaction=/submit/preview`, 글을 고치면 AI 단계를 버리는 스크립트(`aiPreview`, `aiStepsJson` id 사용)는 그대로 둔다.

- [ ] **Step 1: 실패하는 테스트 작성**

```java
@Test void submitErrorsAreLinkedToFields() throws Exception {
    mvc.perform(post("/submit").param("serviceName", "").param("rawText", "짧음").with(csrf()))
       .andExpect(status().isOk())
       .andExpect(content().string(containsString("aria-describedby=\"serviceName-error\"")))
       .andExpect(content().string(containsString("id=\"serviceName-error\"")))
       .andExpect(content().string(containsString("aria-invalid=\"true\"")));
}
@Test void submitDoneOffersNextActions() throws Exception {
    mvc.perform(get("/submit/done")).andExpect(content().string(containsString("다른 서비스 제보")));
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*PublicPagesTest"` → 2개 FAIL

- [ ] **Step 3: `submit.html`** — 칸마다 `<div class="field">` label + 입력 + 도움말(`id="rawText-help"`) + 오류(`id="{칸}-error"`, `th:if="${#fields.hasErrors(...)}"`). 오류가 있을 때만 입력에 `aria-invalid="true"`와 `aria-describedby="{칸}-error"`(rawText는 `"rawText-error rawText-help"`), 없으면 rawText만 `aria-describedby="rawText-help"`. AI 오류는 `.notice`(`role="alert"`), 미리보기는 `<section id="aiPreview">` 안에 `stepList(${aiSteps}, null)`. 버튼 두 개는 `.btn-primary`·`.btn-secondary`. "더 알려 주실 수 있나요?" 접는 칸은 데이터 칸이 생기는 2단계에서 넣는다.

- [ ] **Step 4: `submit-done.html`** — "제보해 주셔서 고마워요!", 안내 문구 그대로, 버튼 "홈으로"(`/`)와 "다른 서비스 제보"(`/submit`).

- [ ] **Step 5: 테스트 통과 확인** — `./gradlew test` → 전체 PASS

- [ ] **Step 6: 커밋(허락 후)** — `git commit -m "feat: 제보 화면 새 디자인과 오류 문구 연결"`

---

### Task 5: 404·500 오류 화면

**Files:**
- Create: `src/main/resources/templates/error/404.html`, `src/main/resources/templates/error/500.html`
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/web/ErrorPagesTest.java`

**Interfaces:**
- Consumes: `noindexHead(title)`, `siteHeader(false)`, `siteFooter`
- Produces: 없음. Spring Boot 기본 오류 처리가 `error/{상태코드}` 템플릿을 자동으로 고른다(컨트롤러 추가 없음).

- [ ] **Step 1: 실패하는 테스트 작성** — MockMvc는 오류 화면으로 넘기지 않으므로 실제 서버를 띄운다.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorPagesTest {
    @LocalServerPort int port;

    @Test void unknownGuideShowsFriendly404() throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/guide/no-such-service"))
                .header("Accept", "text/html").build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("찾는 화면이 없어요"));
        assertTrue(res.body().contains("action=\"/search\""));
        assertFalse(res.body().contains("Whitelabel"));
    }
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*ErrorPagesTest"` → FAIL(Whitelabel)

- [ ] **Step 3: 템플릿 작성**
  - `404.html`: 제목 "찾는 화면이 없어요 | 구독 해지 가이드", `<h1>찾는 화면이 없어요</h1>`, "주소가 바뀌었거나 아직 없는 가이드예요.", 검색 폼(`/search`), "홈으로" 버튼.
  - `500.html`: `<h1>잠시 문제가 생겼어요</h1>`, "잠시 뒤 다시 시도해 주세요.", "홈으로" 버튼. 오류 내용(예외 메시지)은 보여 주지 않는다.

- [ ] **Step 4: 테스트 통과 확인** — `./gradlew test` → 전체 PASS

- [ ] **Step 5: 커밋(허락 후)** — `git commit -m "feat: 404·500 오류 화면"`

---

### Task 6: 화면 확인과 마무리

**Files:** 고칠 곳이 나오면 해당 템플릿·`site.css`

- [ ] **Step 1: 앱 실행** — `./gradlew bootRun`, http://localhost:8080

- [ ] **Step 2: 375px·1280px 스크린샷** — 홈, `/category/ott`, `/search?q=없는서비스`, `/guide/netflix`, `/guide/tving`, `/submit`(오류 상태 포함), `/submit/done`, `/guide/no-such`. 확인할 것: 가이드 첫 화면에 요약 문장·칩·요약 상자가 보임, 375px에서 단계 하나가 한 화면 안, 가로 스크롤 없음(`document.documentElement.scrollWidth <= innerWidth`).

- [ ] **Step 3: 긴 이름 확인(Review Focus 5)** — 관리자 화면에서 50자 서비스 이름과 50자 버튼 이름 단계를 하나 만들고, 375px 홈·가이드에서 가로 스크롤이 없는지 본 뒤 지운다.

- [ ] **Step 4: 접근성** — Tab 키로 머리글 → 검색 → 탭 → 목록 → 피드백 버튼까지 이동하며 포커스 테두리가 보이는지, 보조 글자 `#59636e`/흰 배경 대비가 4.5:1 이상인지(약 6.2:1) 확인.

- [ ] **Step 5: 관리자 화면이 그대로인지** — 로그인 후 가이드 목록·서비스 수정·제보 상세가 예전 모양(Bootstrap)으로 깨지지 않고 열리는지.

- [ ] **Step 6: 전체 테스트** — `./gradlew test` → 전체 PASS, 결과 개수 보고

- [ ] **Step 7: 고친 것이 있으면 커밋(허락 후)** — `git commit -m "fix: 화면 확인 중 발견한 배치 수정"`
