# 단계 그림에 실제 메뉴 이름 채우기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 단계 그림(휴대폰 도식)을 점 하나 대신 "화면 제목 + 메뉴 5줄 + 누를 줄 강조"로 그린다.

**Architecture:** 줄 배치 규칙은 Java 한 곳(`PhoneRows`)에 두고, `GuideStep`과 `GuideStepForm`이 `getPhoneRows()`로 꺼내 쓴다. Thymeleaf `miniPhone` 조각은 그 줄을 그리기만 한다. AI 응답 JSON에 `otherMenus` 배열을 더하고, 폼으로 바꿀 때 줄바꿈으로 이어 붙인다.

**Tech Stack:** Spring Boot 4.1.2, Java 21, Thymeleaf, JPA(H2 개발 / MySQL 운영, 둘 다 ddl-auto), JUnit 5, MockMvc, site.css

**Spec:** `docs/superpowers/specs/2026-11-23-step-picture-menus-design.md`

## Global Constraints

- `otherMenus` 최대 300자, 한 줄에 메뉴 하나. 메뉴 이름 하나는 최대 50자.
- 목록은 5줄 고정. 강조 자리: `top-left`·`top-right`→1번째, `middle`→3번째, `bottom`→5번째, 빈 값→`middle`(3번째).
- 위치 값 4종(`top-left`, `top-right`, `middle`, `bottom`)은 바꾸지 않는다.
- AI는 글에 실제로 나온 메뉴만 넣는다. 지어내지 않는다. 없으면 `[]`.
- 사용자에게 보이는 문구는 "~요" 말투(기존 폼 오류 문구와 같게).
- 강조 줄은 색 + `>` 표시(색에만 의존 금지). 회색 줄은 `aria-hidden`.
- 그림 대체 텍스트: `{screenName} 화면에서 {targetLabel}을(를) 누르세요`.
- 이슈·브랜치·커밋·PR·머지는 단계마다 사용자 허락. 브랜치 `feat/#이슈번호-step-picture-menus`, 커밋 `feat: ...`.

## Review Focus

1. 메뉴 칸에 빈 줄·앞뒤 공백·Windows 줄바꿈(`\r\n`)이 섞임 → 빈 줄은 무시하고, 이름에 `\r`이 남지 않아야 함 (Task 1 테스트로 고정).
2. otherMenus에 targetLabel과 같은 이름이 있음 → 같은 메뉴가 두 줄로 나오지 않게 건너뜀 (Task 1 테스트로 고정).
3. 이전 형식 AI JSON(DB에 저장된 제보, `otherMenus` 없음) → 오류 없이 빈 목록으로 읽힘 (Task 2 테스트로 고정).
4. AI가 메뉴 이름을 51자 이상 / 합계 300자 초과로 줌 → 실패하지 않고 잘라서 저장 가능한 값이 됨 (Task 2 테스트로 고정).
5. 375px 화면에서 긴 메뉴 이름 → 말줄임, 가로 스크롤 없음 (Task 3 화면 확인 단계).

---

### Task 1: 줄 배치 규칙과 데이터 칸

**Files:**
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/dto/PhoneRows.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/domain/GuideStep.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/dto/GuideStepForm.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/service/GuideAdminService.java:121-150` (생성자·update에 `form.getOtherMenus()` 전달)
- Modify: `src/test/java/com/whtngml/subscription_cancel_guide/dto/GuideSummaryTest.java:22` (생성자 매개변수 추가, `null`)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/dto/PhoneRowsTest.java`

**Interfaces:**
- Produces:
  - `record PhoneRow(String label, boolean target)` — `PhoneRows` 안의 중첩 record. `label == null`이면 회색 줄.
  - `static List<String> PhoneRows.splitMenus(String otherMenus)` — 줄 단위로 나누고 strip, 빈 줄 제거. null → 빈 목록.
  - `static List<PhoneRow> PhoneRows.of(String position, String targetLabel, String otherMenus)` — 항상 5개.
  - `GuideStep`: 필드 `@Column(length = 300) String otherMenus`, 생성자 `GuideStep(Subscription, int stepOrder, String screenName, String targetLabel, String position, String description, String otherMenus)`, `update(int, String, String, String, String, String otherMenus)`, `List<PhoneRows.PhoneRow> getPhoneRows()`.
  - `GuideStepForm`: `@Size(max = 300, message = "다른 메뉴는 300자까지 쓸 수 있어요.") String otherMenus`, `from()`에서 복사, `List<PhoneRows.PhoneRow> getPhoneRows()`.

- [ ] **Step 1: 실패하는 테스트 작성** — `PhoneRowsTest`

```java
@Test void 메뉴_나누기는_빈줄과_공백과_CR을_뺀다() {
    assertEquals(List.of("프로필", "알림"), PhoneRows.splitMenus(" 프로필 \r\n\r\n알림\n"));
    assertEquals(List.of(), PhoneRows.splitMenus(null));
}
@Test void 위치별_강조_자리() {
    assertTrue(PhoneRows.of("top-left", "해지", null).get(0).target());
    assertTrue(PhoneRows.of("top-right", "해지", null).get(0).target());
    assertTrue(PhoneRows.of("middle", "해지", null).get(2).target());
    assertTrue(PhoneRows.of("bottom", "해지", null).get(4).target());
    assertTrue(PhoneRows.of(null, "해지", null).get(2).target());
}
@Test void 메뉴가_없으면_강조줄만_있고_나머지는_회색() {
    List<PhoneRows.PhoneRow> rows = PhoneRows.of("middle", "해지", null);
    assertEquals(5, rows.size());
    assertEquals(new PhoneRows.PhoneRow("해지", true), rows.get(2));
    assertNull(rows.get(0).label());
}
@Test void 메뉴는_강조자리를_건너뛰며_위부터_채운다() {
    List<PhoneRows.PhoneRow> rows = PhoneRows.of("middle", "해지", "프로필\n알림");
    assertEquals("프로필", rows.get(0).label());
    assertEquals("알림", rows.get(1).label());
    assertNull(rows.get(3).label());
}
@Test void 메뉴가_많으면_남는건_버린다() {
    List<PhoneRows.PhoneRow> rows = PhoneRows.of("top-left", "해지", "a\nb\nc\nd\ne\nf");
    assertEquals(List.of("해지", "a", "b", "c", "d"), rows.stream().map(PhoneRows.PhoneRow::label).toList());
}
@Test void 누를_메뉴와_같은_이름은_다시_넣지_않는다() {
    List<PhoneRows.PhoneRow> rows = PhoneRows.of("bottom", "해지", "해지\n알림");
    assertEquals(1, rows.stream().filter(r -> "해지".equals(r.label())).count());
    assertEquals("알림", rows.get(0).label());
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*PhoneRowsTest"` → 컴파일 실패(PhoneRows 없음)

- [ ] **Step 3: `PhoneRows` 구현** — 강조 자리 인덱스 계산 → `splitMenus` 결과에서 targetLabel과 같은 이름(strip 후 equals) 제거 → 5칸을 돌며 강조 자리면 target 줄, 아니면 다음 메뉴 또는 `null`.

- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS

- [ ] **Step 5: `GuideStep`·`GuideStepForm`·`GuideAdminService`·`GuideSummaryTest`에 `otherMenus` 반영** (Interfaces 그대로). `getPhoneRows()`는 `PhoneRows.of(position, targetLabel, otherMenus)`를 돌려준다.

- [ ] **Step 6: 전체 테스트** — `./gradlew test` → 모두 PASS

- [ ] **Step 7: 커밋**

```bash
git add src/main/java src/test/java
git commit -m "feat: 단계에 다른 메뉴 칸과 그림 줄 배치 규칙 추가"
```

---

### Task 2: AI가 다른 메뉴를 함께 돌려주기

**Files:**
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/dto/AiStep.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/ai/AiStepsJson.java:47-56`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/ai/FactchatStepParser.java:26-36` (SYSTEM_PROMPT)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/ai/AiStepsJsonTest.java` (기존 `new AiStep(...)` 호출에 `List.of()` 추가)

**Interfaces:**
- Consumes: `GuideStepForm.setOtherMenus(String)` (Task 1)
- Produces: `record AiStep(int order, String screen, String target, String position, List<String> otherMenus)`; `toForm()`이 `String.join("\n", otherMenus)`를 `otherMenus`에 넣는다(빈 목록이면 `null`).

- [ ] **Step 1: 실패하는 테스트 작성** — `AiStepsJsonTest`에 추가

```java
@Test void otherMenus를_읽는다() {
    AiStepsResult r = json.read("""
        {"service":"넷플릭스","steps":[{"order":1,"screen":"설정","target":"멤버십 해지","position":"bottom","otherMenus":[" 프로필 ","","알림"]}]}""");
    assertEquals(List.of("프로필", "알림"), r.steps().get(0).otherMenus());
}
@Test void otherMenus가_없거나_null이면_빈목록() {
    assertEquals(List.of(), json.read("""
        {"steps":[{"order":1,"screen":"설정","target":"해지","position":"middle"}]}""").steps().get(0).otherMenus());
    assertEquals(List.of(), json.read("""
        {"steps":[{"order":1,"screen":"설정","target":"해지","position":"middle","otherMenus":null}]}""").steps().get(0).otherMenus());
}
@Test void 긴_메뉴는_50자로_자르고_합계_300자를_넘지_않는다() {
    String long60 = "가".repeat(60);
    String menus = IntStream.range(0, 10).mapToObj(i -> "\"" + long60 + "\"").collect(Collectors.joining(","));
    AiStep step = json.read("{\"steps\":[{\"order\":1,\"screen\":\"설정\",\"target\":\"해지\",\"position\":\"middle\",\"otherMenus\":[" + menus + "]}]}").steps().get(0);
    assertEquals(50, step.otherMenus().get(0).length());
    assertTrue(String.join("\n", step.otherMenus()).length() <= 300);
}
@Test void toForm은_줄바꿈으로_잇는다() {
    assertEquals("프로필\n알림", new AiStep(1, "설정", "해지", "middle", List.of("프로필", "알림")).toForm().getOtherMenus());
    assertNull(new AiStep(1, "설정", "해지", "middle", List.of()).toForm().getOtherMenus());
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*AiStepsJsonTest"` → 컴파일 실패

- [ ] **Step 3: 구현** — `AiStep`에 칸 추가. `AiStepsJson.read`에서 `cleanMenus(List<String>)`: null→빈 목록, strip, 빈 값 제거, 50자 자르기, `\n`으로 이었을 때 300자를 넘기 직전까지만 담기. 형식 오류로 실패시키지 않는다.

- [ ] **Step 4: 프롬프트 수정** — 규칙에 한 줄 추가하고 JSON 예시에 칸 추가:

```
5. otherMenus는 그 화면에 target 말고 보이는 다른 메뉴 이름 목록이야. 글에 실제로 나온 이름만 넣고 지어내지 마. 없으면 [].
...
{"service":"서비스 이름","steps":[{"order":1,"screen":"화면 이름","target":"누를 버튼","position":"top-right","otherMenus":["다른 메뉴"]}]}
```
(기존 5·6번은 6·7번으로)

- [ ] **Step 5: 전체 테스트** — `./gradlew test` → 모두 PASS (`FactchatStepParserTest` 포함)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java src/test/java
git commit -m "feat: AI 단계 JSON에 다른 메뉴 목록 추가"
```

---

### Task 3: 그림 그리기와 관리자 폼

**Files:**
- Modify: `src/main/resources/templates/fragments/steps.html` (`miniPhone` 조각)
- Modify: `src/main/resources/templates/admin/step-form.html` (textarea, 미리보기 스크립트)
- Modify: `src/main/resources/templates/admin/subscription-form.html:60` (조각 호출)
- Modify: `src/main/resources/static/css/site.css:579-615` (`.mini-phone` 규칙 교체)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/web/AdminGuideFormTest.java`, `PublicPagesTest.java`

**Interfaces:**
- Consumes: `getPhoneRows()`, `getScreenName()`, `getTargetLabel()` (GuideStep/GuideStepForm, Task 1)
- Produces: 조각 `miniPhone(step, color)` — 매개변수가 `position`에서 `step`으로 바뀐다. 호출부 4곳(`stepList` 안, step-form 미리보기, subscription-form 표)을 모두 바꾼다.

조각 마크업 (클래스 이름 고정, CSS와 스크립트가 이걸 쓴다):

```html
<div th:fragment="miniPhone(step, color)" class="mini-phone" role="img"
     th:attr="aria-label=${step.screenName} + ' 화면에서 ' + ${step.targetLabel} + '을(를) 누르세요'"
     th:style="${#strings.isEmpty(color)} ? null : '--brand: ' + ${color}">
    <span class="phone-title" aria-hidden="true" th:text="${step.screenName}">화면</span>
    <ul class="phone-rows" aria-hidden="true">
        <li th:each="row : ${step.phoneRows}"
            th:class="${row.target} ? 'row row-target' : (${row.label} == null ? 'row row-blank' : 'row')"
            th:text="${row.target} ? '> ' + ${row.label} : ${row.label}"></li>
    </ul>
</div>
```

- [ ] **Step 1: 실패하는 테스트 작성**
  - `AdminGuideFormTest`: 단계 생성 POST에 `otherMenus=프로필\n알림` → 저장된 `GuideStep.getOtherMenus()`가 `"프로필\n알림"`. 301자 → 200 응답 + 본문에 `"다른 메뉴는 300자까지 쓸 수 있어요."`, 저장 안 됨.
  - `PublicPagesTest`: otherMenus `"프로필"`, target `"멤버십 해지"`인 단계가 있는 가이드 페이지 → 본문에 `"row-target"`, `"프로필"`, `"화면에서 멤버십 해지을(를) 누르세요"` 포함.

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*AdminGuideFormTest" --tests "*PublicPagesTest"` → 새 테스트 FAIL

- [ ] **Step 3: 조각·호출부 수정** (위 마크업 그대로)

- [ ] **Step 4: step-form에 textarea 추가** — 설명 칸 위에 기존 칸과 같은 구조로: `id="otherMenus"`, 라벨 "이 화면에 보이는 다른 메뉴 (한 줄에 하나)", `rows="4" maxlength="300"`, 안내 `<p class="muted">모르면 비워 두세요. 그림에 회색 줄로 나와요.</p>`, 오류 `id="otherMenus-error"`.

- [ ] **Step 5: 미리보기 스크립트 교체** — `screenName`, `targetLabel`, `position`, `otherMenus` 칸의 `input` 이벤트마다 `.phone-title` 글자와 `.phone-rows`의 5개 `li`를 다시 채운다. 배치 규칙은 `PhoneRows.of`와 똑같이(강조 자리 0/0/2/4, 빈 값 2, 같은 이름 건너뛰기, 빈 줄 무시). 글자는 `textContent`로만 넣는다(innerHTML 금지).

- [ ] **Step 6: CSS** — `.mini-phone`을 폭 120px 세로 휴대폰으로, `.phone-title` 굵게 작은 글씨, `.row` 높이 18px·`white-space: nowrap; overflow: hidden; text-overflow: ellipsis`, `.row-target`은 `background: var(--brand, <기존 강조색>)` + 흰 글씨 + 굵게, `.row-blank`는 회색 막대(글자 없음). `.pos-*`·`.tap` 규칙 삭제.

- [ ] **Step 7: 전체 테스트** — `./gradlew test` → 모두 PASS

- [ ] **Step 8: 화면 확인** — 앱 실행 → 가이드 페이지와 관리자 단계 폼을 데스크톱·375px로 스크린샷(375px는 메모리의 iframe 방법). 확인: 강조 줄 자리, 회색 줄, 50자 메뉴 말줄임, 가로 스크롤 없음, 폼에서 입력하면 미리보기가 바로 바뀜. 확인 뒤 앱 종료.

- [ ] **Step 9: 커밋**

```bash
git add src/main/resources src/test/java
git commit -m "feat: 단계 그림을 메뉴 목록과 강조 줄로 그리기"
```
