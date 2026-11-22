# 화면 개편 3단계 (관리자 화면 새 디자인) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자 화면 6개를 Bootstrap 없이 `site.css`로 다시 만들고, 목록에서 각 가이드의 빈 정보를 한눈에 보게 한다. 끝나면 Bootstrap·`app.css`·`phone.css`·`fragments/phone.html`을 지운다.

**Architecture:** 관리자 전용 스타일은 `site.css` 아래쪽 "관리자" 구역에 둔다(폭 960px `.wrap-wide`, 표, 구역 폼). 머리글의 제보 대기 배지는 관리자 컨트롤러에만 적용되는 `@ControllerAdvice`가 채운다. 빈 정보 계산은 순수 자바 클래스 `MissingInfo`가 맡아 단위 테스트한다. 컨트롤러 주소와 폼 칸 이름은 바꾸지 않는다.

**Tech Stack:** Spring Boot 4.1.2, Java 21, Thymeleaf, JUnit 5 + MockMvc + `@WithMockUser`

**Spec:** `docs/superpowers/specs/2026-11-21-ui-redesign-design.md` (4장, 7장, 8장의 3번, 9장 접근성)

## Global Constraints

- 사용자 화면과 같은 글꼴·색 변수. 관리자 본문 폭 960px.
- 메뉴: 가이드 관리, 제보 검토(대기 개수 배지), 사이트 보기, 로그아웃.
- 가이드 목록 열: 아이콘·서비스, 카테고리, 난이도, 단계 수, 빈 정보, 확인일, 수정. 빈 정보는 항목 이름을 노란 글자(`#9a6700`)로. 검색창과 "+ 새 서비스".
- 서비스 폼 구역 3개: 기본 정보 / 해지 정보 / 확인. 틀린 칸은 빨간 테두리 + 오류 문구, `aria-invalid`·`aria-describedby="{칸}-error"`.
- 제보 검토 상세: 2단(왼쪽 보낸 내용·AI 단계 / 오른쪽 승인 폼·체크박스·반려), 900px 미만 1단.
- 로그인: 가운데 작은 상자.
- 컨트롤러 주소, 폼 칸 이름(`name`), 체크박스 이름(`importAiSteps`·`importDetails`·`importIcon`)은 그대로.
- 화면 글자는 `th:text`로만, `th:replace`와 `th:if`는 한 태그에 같이 쓰지 않는다.
- 커밋 `feat: ...` + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. 좁은 화면(375px)의 가이드 목록 표 — 페이지 전체가 아니라 표 상자만 가로로 스크롤돼야 한다(`.table-wrap{overflow-x:auto}`). → Task 2 `guidesTableIsWrapped`
2. 삭제 버튼(서비스·단계) — 새 마크업에서도 CSRF 토큰이 들어가 POST가 동작해야 한다. → Task 3 `deleteStepStillWorks`
3. 제보 대기 배지 — 0건이면 배지를 숨긴다. → Task 1 `badgeHiddenWhenNoPending`
4. 승인 폼의 체크박스 기본값 — 새 서비스는 체크, 기존 서비스는 해제가 그대로여야 한다. → Task 4 `approveCheckboxDefaultsKept`
5. 지운 `phone.css`·`app.css`를 서비스 워커·템플릿이 더는 부르지 않아야 한다. → Task 5 `oldCssRemoved`

## 결정 사항

- **목록 검색은 서버에서 이름으로 거른다**(`/admin/guides?q=넷`). 서비스가 수십 개 수준이라 전체를 읽어 자바에서 거른다.
- **빈 정보 항목과 순서:** 단계, 아이콘, 해지하는 곳, 난이도, 예상 시간, 공식 링크, 환불. (주의할 점은 없는 게 정상일 수 있어 넣지 않는다.)
- **2단계 미룬 문제 하나를 같이 고친다:** 새 서비스 승인 화면의 "해지하는 곳·주의할 점 가져오기" 옆에 "체크하면 위에 쓴 값 대신 제보 내용을 써요"를 적는다.

## 파일 구조

| 파일 | 할 일 |
|---|---|
| Create `dto/MissingInfo.java` | `static List<String> of(Subscription s, long stepCount)` |
| Create `config/AdminModelAdvice.java` | 관리자 컨트롤러에 `pendingCount` |
| Modify `controller/AdminGuideController.java` | 목록 `q`, `stepCounts`, `missing` |
| Modify `fragments/layout.html` | `adminHead` → site.css, `adminHeader` 새 모양 + 배지 |
| Modify `fragments/steps.html` | `miniPhone(position, color)` 조각을 떼어 내 `stepList`와 관리자 화면이 함께 씀 |
| Modify `fragments/subscription-fields.html` | 구역 3개, 새 오류 표시 |
| Rewrite `admin/*.html` 6개 | site.css 마크업 |
| Modify `static/css/site.css` | 관리자 구역 |
| Delete `static/css/app.css`, `static/css/phone.css`, `templates/fragments/phone.html` | |
| Test `web/AdminPagesTest.java`, `dto/MissingInfoTest.java` | |

---

### Task 1: 관리자 공통 틀 (head·머리글·배지·로그인)

**Files:** Create `config/AdminModelAdvice.java`; Modify `fragments/layout.html`, `static/css/site.css`, `admin/login.html`; Test `web/AdminPagesTest.java`

**Interfaces:**
- Produces: `@ControllerAdvice(assignableTypes = {AdminGuideController.class, AdminSubmissionController.class}) AdminModelAdvice` + `@ModelAttribute("pendingCount") long pendingCount()` (`SubmissionService.countPending()`). 이제 `AdminGuideController.list`의 `pendingCount` 줄은 지운다.
- `adminHead(title)`: noindex + Pretendard + site.css(Bootstrap·app.css·phone.css 없음).
- `adminHeader`: `<header class="site-header admin-header"><div class="wrap-wide">` "관리자"(로고) · 가이드 관리 · 제보 검토 `<span class="badge" th:if="${pendingCount > 0}">3</span>`(`aria-label`이 아니라 보이는 글자 + `visually-hidden` "건 대기") · 사이트 보기 · 로그아웃 버튼(POST 폼). 지금 화면 메뉴는 `aria-current="page"`(모델 값 `adminMenu`: `"guides"`/`"submissions"`, 각 컨트롤러가 넣는다).
- CSS 클래스(이후 Task가 씀): `.wrap-wide`(960px), `.admin-header`, `.badge`, `.table-wrap`, `.admin-table`, `.missing-list`, `.form-section`(fieldset), `.form-grid`(라벨 140px + 입력), `.btn-danger`, `.btn-sm`, `.cols-2`, `.login-box`, `.alert-success`, `.alert-error`
- `login.html`: `<main class="login-box">` 가운데 360px 상자, 제목·오류/로그아웃 알림·아이디·비밀번호·로그인 버튼.

- [ ] **Step 1: 실패하는 테스트** (`@SpringBootTest @AutoConfigureMockMvc @Transactional`)

```java
@Test @WithMockUser(roles="ADMIN") void adminPagesUseSiteCss()   // /admin/guides, /admin/submissions: "site.css" 포함, "bootstrap" 미포함
@Test void loginPageUsesSiteCss()                                  // /admin/login: "login-box", "bootstrap" 미포함
@Test @WithMockUser(roles="ADMIN") void badgeShowsPendingCount()   // 제보 1건 저장 → /admin/guides: class="badge" 포함, ">1<" 포함
@Test @WithMockUser(roles="ADMIN") void badgeHiddenWhenNoPending() // 대기 제보를 모두 reject → "class=\"badge\"" 미포함
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*AdminPagesTest"`
- [ ] **Step 3: 구현** — 아직 바꾸지 않은 관리자 화면이 Bootstrap 클래스를 쓰더라도 이 Task에서는 head만 바꾼다(모양은 Task 2~4에서).
- [ ] **Step 4: 통과 확인** — `./gradlew test` 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 관리자 공통 틀과 로그인 화면 새 디자인`

---

### Task 2: 가이드 목록 (빈 정보·검색)

**Files:** Create `dto/MissingInfo.java`; Modify `controller/AdminGuideController.java`, `admin/guides.html`; Test `dto/MissingInfoTest.java`, `AdminPagesTest`

**Interfaces:**
- Consumes: `GuideListService.stepCounts()`, `fragments/ui :: serviceIcon`, `difficultyChip`
- Produces: `MissingInfo.of(Subscription s, long stepCount): List<String>` — 순서 "단계"(0개), "아이콘"(icon null), "해지하는 곳"(비어 있음), "난이도", "예상 시간", "공식 링크", "환불"(빈 글자).
- `list(@RequestParam(defaultValue="") String q, Model)`: 이름에 `q`(대소문자 무시, 앞뒤 공백 제거)가 들어간 서비스만, 이름 순. 모델 `subscriptions`, `stepCounts`, `feedbackCounts`, `q`, `adminMenu="guides"`.
- 화면: 제목 "가이드 관리" + 검색 폼(`q`) + "+ 새 서비스". `<div class="table-wrap"><table class="admin-table">` 열은 Global Constraints대로 + 피드백(기존 값 유지). 빈 정보 칸은 `<ul class="missing-list">`에 항목, 없으면 "—". 확인일 `M.d`. 검색 결과 없음 "'{q}'에 맞는 서비스가 없어요."

- [ ] **Step 1: 실패하는 테스트**

```java
// MissingInfoTest — 엔티티는 new Subscription(category, "멜론", "melon", null, null, null)로 만든다
@Test void allMissing()  // stepCount 0 → ["단계","아이콘","해지하는 곳","난이도","예상 시간","공식 링크","환불"]
@Test void nothingMissing() // updateDetails(EnumSet.of(WEB), null, EASY, "https://x", 3), changeIcon(icon), refund "있음", stepCount 3 → []
```
```java
// AdminPagesTest
@Test @WithMockUser(roles="ADMIN") void guidesListShowsMissingInfo() // /admin/guides: "missing-list", 멜론 줄 "난이도" 포함, "단계 수" 머리글
@Test @WithMockUser(roles="ADMIN") void guidesListSearch()           // ?q=넷 → "넷플릭스" 포함, "멜론" 미포함
@Test @WithMockUser(roles="ADMIN") void guidesTableIsWrapped()       // "class=\"table-wrap\"" 포함
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*MissingInfoTest" --tests "*AdminPagesTest"`
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `./gradlew test` 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 관리자 가이드 목록에 빈 정보와 검색`

---

### Task 3: 서비스 폼·단계 폼

**Files:** Modify `fragments/subscription-fields.html`, `fragments/steps.html`, `admin/subscription-form.html`, `admin/step-form.html`, `controller/AdminGuideController.java`(`adminMenu`); Test `AdminPagesTest`

**Interfaces:**
- Produces: `fragments/steps :: miniPhone(position, color)` — 지금 `stepList` 안의 `.mini-phone` div를 이 조각으로 떼고 `stepList`가 호출한다.
- `subscription-fields :: fields`: `<fieldset class="form-section">` 3개. 기본 정보 = 카테고리·이름·slug·브랜드 색·아이콘(현재 아이콘 미리보기), 해지 정보 = 해지하는 곳·난이도·예상 시간·공식 링크·주의할 점·환불, 확인 = 마지막 확인일. 칸마다 `.form-grid` 한 줄(라벨 | 입력 + 도움말/오류). 오류는 1단계 제보 화면과 같은 규칙: `<p class="field-error" id="{칸}-error">` + 입력에 `aria-invalid="true"`, `aria-describedby="{칸}-error"`.
- `subscription-form.html`: 폼 아래 "해지 단계" 표(번호 · 화면에서 [버튼] · `miniPhone` · 수정/삭제), "+ 단계 추가", "제보 원문" 목록, 서비스 삭제(빨간 버튼, 기존 확인창 유지).
- `step-form.html`: 칸 그대로 + 오른쪽에 `miniPhone(${form.position}, ${subscription.brandColor})` 미리보기. 위치 select를 바꾸면 미리보기 클래스가 바뀌는 짧은 인라인 스크립트(`pos-` 클래스 교체).

- [ ] **Step 1: 실패하는 테스트**

```java
@Test @WithMockUser(roles="ADMIN") void serviceFormHasThreeSections()   // /admin/guides/1/edit: "기본 정보","해지 정보","확인" legend, "mini-phone"
@Test @WithMockUser(roles="ADMIN") void serviceFormErrorsLinked()       // slug="잘못" POST → aria-describedby="slug-error", id="slug-error", aria-invalid="true"
@Test @WithMockUser(roles="ADMIN") void stepFormHasPreview()            // /admin/guides/1/steps/new: "mini-phone"
@Test @WithMockUser(roles="ADMIN") void deleteStepStillWorks()          // edit 화면에서 단계 삭제 폼에 _csrf 포함, POST /admin/guides/1/steps/{첫 단계 id}/delete (csrf) → 302, 단계 수 3→2
```

- [ ] **Step 2: 실패 확인** · **Step 3: 구현** · **Step 4: 전체 PASS** · **Step 5: 커밋** — `feat: 관리자 서비스 폼 구역 나누기와 단계 미리보기`

---

### Task 4: 제보 검토 목록·상세

**Files:** Modify `admin/submissions.html`, `admin/submission.html`, `controller/AdminSubmissionController.java`(`adminMenu`); Test `AdminPagesTest`

**Interfaces:**
- 목록: 표(서비스 이름 · 제보 시각 `M월 d일 HH:mm` · 첨부). 첨부는 있는 것만 칩으로 "AI 단계"(`aiStepsJson != null`) · "아이콘"(`icon != null`) · "추가 정보"(`hasDetails()`). 페이지 넘김은 사용자 화면의 `.pager` 모양.
- 상세: `<div class="cols-2">` 왼쪽(보낸 내용 fieldset — 원문, 해지하는 곳, 주의할 점, 아이콘 / AI 단계 fieldset — `stepList(aiSteps, null)`), 오른쪽(기존 서비스로 승인 상자(있을 때) / 새 서비스로 승인 폼 / 반려). 체크박스 이름·기본값은 그대로. 새 서비스 쪽 "해지하는 곳·주의할 점 가져오기" 라벨 아래 도움말 "체크하면 위에 쓴 값 대신 제보 내용을 써요."

- [ ] **Step 1: 실패하는 테스트**

```java
@Test @WithMockUser(roles="ADMIN") void submissionsListShowsAttachments() // 아이콘·channels 있는 제보 저장 → /admin/submissions: "아이콘", "추가 정보" 칩
@Test @WithMockUser(roles="ADMIN") void submissionDetailIsTwoColumns()    // /admin/submissions/{id}: "cols-2", "보낸 내용"
@Test @WithMockUser(roles="ADMIN") void approveCheckboxDefaultsKept()     // 이름 "넷플릭스" 제보(channels·icon 있음): id="importDetails" ... checked, id="importDetailsExisting" 태그에 checked 없음
```
(마지막 테스트는 응답 글에서 `id="importDetailsExisting"`가 들어 있는 `<input ...>` 한 태그를 잘라 `checked`가 없는지 본다.)

- [ ] **Step 2~5** — 실패 확인 → 구현 → 전체 PASS → 커밋 `feat: 관리자 제보 검토 화면 2단 배치`

---

### Task 5: 옛 스타일 지우기와 화면 확인

**Files:** Delete `static/css/app.css`, `static/css/phone.css`, `templates/fragments/phone.html`; Test `AdminPagesTest`

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void oldCssRemoved() // GET /css/phone.css, /css/app.css → 404; 템플릿 폴더 전체에서 "phone.css"·"app.css"·"bootstrap" 문자열이 없는지 Files.walk로 확인
```

- [ ] **Step 2: 실패 확인** · **Step 3: 파일 삭제, 남은 참조 정리** · **Step 4: 전체 PASS**
- [ ] **Step 5: 화면 확인** — 앱을 켜고 관리자 로그인 후 375px·1280px 스크린샷: 로그인, 가이드 목록, 서비스 수정, 단계 추가, 제보 목록, 제보 상세. 375px에서 페이지 가로 스크롤이 없는지(표는 상자 안에서만 스크롤). Tab 키로 메뉴→검색→표의 수정 링크까지 포커스가 보이는지.
- [ ] **Step 6: 커밋** — `feat: Bootstrap과 옛 스타일 파일 제거`
