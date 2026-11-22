# 화면 개편 2단계 (새 정보 칸) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서비스에 해지하는 곳·주의할 점·난이도·공식 해지 링크·예상 시간·아이콘 이미지를 저장하고, 관리자 입력·사용자 제보·승인 가져오기·사용자 화면 표시까지 연결한다.

**Architecture:** `Subscription`·`Submission`에 칸을 더하고(해지하는 곳은 `AttributeConverter`로 글 한 칸), 이미지는 새 엔티티 `IconImage`에 따로 둔다. 업로드 검사는 `IconValidator`(매직 바이트) 하나가 맡고, 공개 주소 `/icons/{id}`는 서비스에 연결된 아이콘만 내보낸다. 관리자 화면은 3단계 전이라 Bootstrap 모양 그대로 칸만 더한다.

**Tech Stack:** Spring Boot 4.1.2, Java 21, Thymeleaf, JPA(H2/MySQL), Spring Security, JUnit 5 + MockMvc(`spring-security-test`의 `@WithMockUser`)

**Spec:** `docs/superpowers/specs/2026-11-21-ui-redesign-design.md` (5장 가이드·제보, 6장 전체, 8장의 2번, 9장)

## Global Constraints

- `channels`: `Set<CancelChannel>` (APP, WEB, PHONE, OTHER), DB에는 `"APP,WEB"` 글 한 칸. 항상 enum 순서(APP→WEB→PHONE→OTHER)로 저장, 빈 집합 ↔ `null`.
- `caution` 500자, `officialCancelUrl` 500자 + `http://` 또는 `https://`로 시작, `estimatedMinutes` 1~120, `difficulty` EASY/NORMAL/HARD. 모두 비워 둘 수 있다.
- 화면 표시 글자: APP "앱", WEB "웹", PHONE "전화", OTHER "기타" / EASY "쉬움", NORMAL "보통", HARD "어려움".
- 아이콘: PNG·JPEG·WebP만, 200KB(204800바이트) 이하, 파일 앞부분(매직 바이트)으로 형식 판단. SVG 거부. 오류 문구 "아이콘은 PNG, JPEG, WebP 이미지만 올릴 수 있어요." / "아이콘은 200KB까지 올릴 수 있어요."
- `GET /icons/{id}`: 서비스에 연결된 아이콘만, `Cache-Control: public, max-age=31536000, immutable`. 승인 전 아이콘은 `/admin/icons/{id}`로만.
- 공식 해지 링크는 새 탭 + `rel="noopener noreferrer"`.
- 승인 체크박스: 새 서비스로 승인은 기본 체크, 기존 서비스로 승인은 기본 해제. 해당 정보가 없는 제보에서는 체크박스를 숨긴다.
- AI 프롬프트·검색·피드백·보안 설정은 바꾸지 않는다.
- 화면 글자는 `th:text`로만. `th:replace`와 `th:if`를 한 태그에 같이 쓰지 않는다(조건은 바깥 `th:block`).
- 커밋 `feat: ...` + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. 공식 해지 링크에 `javascript:alert(1)` — 저장되지 않고 오류 문구가 떠야 한다. → Task 3 `rejectsJavascriptUrl`
2. 정적 아이콘 `/icons/favicon-32.png`·`/icons/icon-192.png` — 새 `/icons/{id}` 주소가 생겨도 그대로 나와야 한다(경로를 숫자만 받게 `{id:\d+}`). → Task 2 `staticIconsStillServed`
3. 승인 전 제보의 아이콘 — 공개 주소로는 404. → Task 2 `unlinkedIconIsNotPublic`
4. 확장자만 `.png`로 바꾼 글 파일·SVG — 거부. 200KB를 조금 넘는 파일도 거부(500 오류가 아니라 폼 오류). → Task 2 `IconValidatorTest`, Task 4 `submitRejectsSvgIcon`
5. 기존 서비스로 승인하면서 체크를 안 했을 때 — 기존 해지하는 곳·주의할 점·아이콘이 그대로 남아야 한다. → Task 5 `approveExistingWithoutImportKeepsValues`

---

## 결정 사항 (스펙과 다른 점)

- **아이콘 주소의 `?v=` 대신 "새로 올리면 새 행":** 아이콘을 바꾸면 `IconImage`를 덮어쓰지 않고 새로 저장해 id가 바뀐다. 그래서 주소 `/icons/{id}` 자체가 바뀌고, 목록에서 `icon.id`만 읽으면 되어 이미지 행을 추가로 조회하지 않는다. `updatedAt`은 기록용으로 둔다. 예전 행은 지우지 않는다(제보가 같은 행을 가리킬 수 있음).
- **요약 문장:** 해지하는 곳이 있으면 `"앱·웹에서 3단계로 해지할 수 있어요."`(가운뎃점으로 이음). "앱이나 웹" 같은 조사 처리는 하지 않는다.
- **업로드 한도:** `spring.servlet.multipart.max-file-size: 2MB`, `max-request-size: 3MB`. 200KB 검사는 앱이 하고, 2MB를 넘는 파일은 Spring 기본 오류로 둔다.

## 파일 구조

| 파일 | 할 일 |
|---|---|
| Create `domain/CancelChannel.java`, `domain/Difficulty.java` | enum + `label` |
| Create `domain/ChannelSetConverter.java` | `Set<CancelChannel>` ↔ 글 |
| Create `domain/IconImage.java`, `repository/IconImageRepository.java` | 이미지 행 |
| Create `service/IconValidator.java`, `service/IconValidationException.java`, `service/IconService.java` | 검사·저장 |
| Create `controller/IconController.java` | `/icons/{id}`, `/admin/icons/{id}` |
| Modify `domain/Subscription.java`, `domain/Submission.java` | 새 칸 |
| Modify `dto/SubscriptionForm.java`, `dto/SubmissionForm.java` | 새 칸 + `MultipartFile iconFile` |
| Modify `service/GuideAdminService.java`, `service/SubmissionService.java` | 저장·승인 가져오기 |
| Modify `controller/AdminGuideController.java`, `controller/AdminSubmissionController.java`, `controller/SubmissionController.java` | 업로드 오류 처리, 체크박스 |
| Modify `repository/SubscriptionRepository.java` | `boolean existsByIcon_Id(Long iconId)`, 목록 EntityGraph에 icon은 넣지 않음 |
| Modify `dto/GuideSummary.java` | 해지하는 곳을 문장에 |
| Modify templates `fragments/subscription-fields.html`, `admin/subscription-form.html`, `admin/submission.html`, `submit.html`, `guide.html`, `fragments/ui.html`, `static/css/site.css` | 화면 |
| Modify `resources/application.yaml`, `resources/data.sql` | multipart 한도, 예시 값 |

---

### Task 1: 새 값 타입과 엔티티 칸

**Files:**
- Create: `domain/CancelChannel.java`, `domain/Difficulty.java`, `domain/ChannelSetConverter.java`, `domain/IconImage.java`
- Modify: `domain/Subscription.java`, `domain/Submission.java`, `resources/data.sql`
- Test: `src/test/.../domain/ChannelSetConverterTest.java`

**Interfaces:**
- Produces:
  - `enum CancelChannel { APP("앱"), WEB("웹"), PHONE("전화"), OTHER("기타"); String getLabel(); }`
  - `enum Difficulty { EASY("쉬움"), NORMAL("보통"), HARD("어려움"); String getLabel(); }`
  - `@Converter class ChannelSetConverter implements AttributeConverter<Set<CancelChannel>, String>` — 엔티티 쪽 값은 항상 `EnumSet`(빈 집합 포함, null 아님)
  - `@Entity IconImage { Long id; @Lob @Basic(fetch = LAZY) @Column(nullable=false) byte[] data; @Column(nullable=false, length=20) String contentType; @Column(nullable=false) LocalDateTime updatedAt; IconImage(byte[] data, String contentType) }`
  - `Subscription` 칸: `channels`(`@Convert`, `@Column(length=40)`, 기본값 `EnumSet.noneOf`), `caution`(500), `difficulty`(`@Enumerated STRING`, length 10), `officialCancelUrl`(500), `estimatedMinutes`(Integer), `icon`(`@ManyToOne(fetch=LAZY) @JoinColumn(name="icon_id")`)
  - `Subscription.updateDetails(Set<CancelChannel> channels, String caution, Difficulty difficulty, String officialCancelUrl, Integer estimatedMinutes)` / `changeIcon(IconImage icon)` / `changeChannelsAndCaution(Set<CancelChannel> channels, String caution)`
  - `Submission` 칸: `channels`, `caution`, `icon`(같은 규칙). 생성자 `Submission(String serviceName, String rawText, String aiStepsJson, Set<CancelChannel> channels, String caution, IconImage icon)` (기존 3인자 생성자는 지우고 호출부를 바꾼다)
  - `Submission.hasDetails()`: channels가 비어 있지 않거나 caution이 비어 있지 않으면 true

- [ ] **Step 1: 실패하는 테스트**

```java
class ChannelSetConverterTest {
    ChannelSetConverter c = new ChannelSetConverter();
    @Test void emptyIsNull() {
        assertNull(c.convertToDatabaseColumn(EnumSet.noneOf(CancelChannel.class)));
        assertNull(c.convertToDatabaseColumn(null));
        assertEquals(EnumSet.noneOf(CancelChannel.class), c.convertToEntityAttribute(null));
        assertEquals(EnumSet.noneOf(CancelChannel.class), c.convertToEntityAttribute(""));
    }
    @Test void savesInEnumOrder() {
        assertEquals("APP,WEB", c.convertToDatabaseColumn(new LinkedHashSet<>(List.of(CancelChannel.WEB, CancelChannel.APP))));
        assertEquals(EnumSet.of(CancelChannel.APP, CancelChannel.PHONE), c.convertToEntityAttribute("PHONE,APP"));
    }
    @Test void ignoresUnknownNames() {
        assertEquals(EnumSet.of(CancelChannel.WEB), c.convertToEntityAttribute("WEB,FAX"));
    }
}
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*ChannelSetConverterTest"` → 컴파일 실패(클래스 없음)
- [ ] **Step 3: enum·변환기·IconImage·엔티티 칸 구현**, `SubmissionService.submit`의 생성자 호출을 6인자로(새 칸은 아직 `EnumSet.noneOf`, `null`, `null`).
- [ ] **Step 4: `data.sql`** — subscription INSERT 세 줄에 칸 추가:
  - 넷플릭스: channels `'WEB'`, difficulty `'EASY'`, official_cancel_url `'https://www.netflix.com/cancelplan'`, estimated_minutes `3`, caution `'앱스토어나 구글플레이로 결제했다면 그쪽 구독 관리에서 해지해야 해요.'`
  - 티빙: channels `'APP,WEB'`, difficulty `'NORMAL'`, estimated_minutes `5`, 나머지 NULL
  - 멜론: 모두 NULL (빈 정보 표시 확인용)
- [ ] **Step 5: 통과 확인** — `./gradlew test` → 전체 PASS (contextLoads가 data.sql 컬럼 이름까지 확인)
- [ ] **Step 6: 커밋** — `feat: 해지하는 곳·난이도 등 새 정보 칸과 아이콘 엔티티`

---

### Task 2: 아이콘 검사·저장·내보내기

**Files:**
- Create: `service/IconValidator.java`, `service/IconValidationException.java`, `service/IconService.java`, `repository/IconImageRepository.java`, `controller/IconController.java`
- Modify: `repository/SubscriptionRepository.java`, `resources/application.yaml`
- Test: `src/test/.../service/IconValidatorTest.java`, `src/test/.../web/IconControllerTest.java`

**Interfaces:**
- Consumes: `IconImage`, `Subscription.changeIcon`, `Submission` 6인자 생성자
- Produces:
  - `@Component IconValidator { String detectContentType(byte[] bytes) }` — `"image/png"`, `"image/jpeg"`, `"image/webp"` 중 하나를 돌려주거나 `IconValidationException`(RuntimeException, 메시지는 Global Constraints 문구). 순서: 길이 > 204800 → 크기 오류, 매직 바이트 불일치 → 형식 오류. PNG `89 50 4E 47 0D 0A 1A 0A`, JPEG `FF D8 FF`, WebP 0~3 `RIFF` + 8~11 `WEBP`.
  - `@Service IconService { Optional<IconImage> store(MultipartFile file) }` — `file == null || file.isEmpty()`면 `Optional.empty()`, 아니면 검사 후 새 행 저장. 검사 실패는 `IconValidationException` 그대로 던진다.
  - `SubscriptionRepository.existsByIcon_Id(Long iconId)`
  - `GET /icons/{id:\d+}` → 연결된 아이콘만 200(`Content-Type` = 저장한 형식, `Cache-Control: public, max-age=31536000, immutable`, `X-Content-Type-Options: nosniff`), 아니면 404
  - `GET /admin/icons/{id:\d+}` → 있으면 200(`Cache-Control: no-store`), 없으면 404
- `application.yaml`: `spring.servlet.multipart.max-file-size: 2MB`, `max-request-size: 3MB`

- [ ] **Step 1: 실패하는 테스트** — `IconValidatorTest`

```java
static final byte[] PNG = {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0,0};
static final byte[] JPEG = {(byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0,0};
static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
@Test void acceptsPngJpegWebp() { assertEquals("image/png", v.detectContentType(PNG)); /* jpeg, webp 동일 */ }
@Test void rejectsSvg() { assertThrows(IconValidationException.class, () -> v.detectContentType("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes())); }
@Test void rejectsRenamedText() { assertThrows(IconValidationException.class, () -> v.detectContentType("hello".getBytes())); }
@Test void rejectsOver200KB() {
    byte[] big = Arrays.copyOf(PNG, 204801);
    var e = assertThrows(IconValidationException.class, () -> v.detectContentType(big));
    assertEquals("아이콘은 200KB까지 올릴 수 있어요.", e.getMessage());
}
@Test void accepts200KBExactly() { assertEquals("image/png", v.detectContentType(Arrays.copyOf(PNG, 204800))); }
```

`IconControllerTest`(`@SpringBootTest @AutoConfigureMockMvc @Transactional`): 저장소로 `IconImage`를 저장해 준비

```java
@Test void linkedIconIsPublicAndCached()     // 넷플릭스에 changeIcon → GET /icons/{id}: 200, content-type image/png, Cache-Control에 "immutable"
@Test void unlinkedIconIsNotPublic()         // 제보에만 연결 → GET /icons/{id}: 404
@Test @WithMockUser(roles = "ADMIN") void adminCanSeeUnlinkedIcon()  // GET /admin/icons/{id}: 200
@Test void staticIconsStillServed()          // GET /icons/favicon-32.png, /icons/icon-192.png: 200
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*IconValidatorTest" --tests "*IconControllerTest"` → 실패
- [ ] **Step 3: 구현** (위 Interfaces 그대로)
- [ ] **Step 4: 통과 확인** — `./gradlew test` → 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 아이콘 이미지 검사와 /icons 주소`

---

### Task 3: 관리자 서비스 폼

**Files:**
- Modify: `dto/SubscriptionForm.java`, `service/GuideAdminService.java`, `controller/AdminGuideController.java`
- Modify: `templates/fragments/subscription-fields.html`, `templates/admin/subscription-form.html`
- Test: `src/test/.../web/AdminGuideFormTest.java`

**Interfaces:**
- Consumes: `IconService.store`, `IconValidationException`, `Subscription.updateDetails/changeIcon`
- Produces:
  - `SubscriptionForm` 칸: `Set<CancelChannel> channels = EnumSet.noneOf(...)`, `@Size(max=500) caution`, `Difficulty difficulty`, `@Size(max=500) @Pattern(regexp="^(https?://\\S+)?$", message="공식 해지 링크는 https://로 시작하는 주소로 써 주세요.") officialCancelUrl`, `@Min(1) @Max(120) Integer estimatedMinutes`(문구 "예상 시간은 1분에서 120분 사이로 써 주세요."), `MultipartFile iconFile`. `from(Subscription)`이 새 칸도 채운다(iconFile 제외).
  - `GuideAdminService.createSubscription(SubscriptionForm form, IconImage icon)` / `updateSubscription(Long id, SubscriptionForm form, IconImage icon)` — `icon == null`이면 아이콘을 바꾸지 않는다. 빈 글자(caution, url)는 `null`로 저장.
  - 컨트롤러: 저장 전에 `iconService.store(form.getIconFile())`, `IconValidationException`이면 `bindingResult.rejectValue("iconFile", "icon", e.getMessage())` 후 폼 다시 보여 주기. `approveAsNew`(Task 5)도 같은 순서를 쓴다 → 비공개 헬퍼 대신 `IconService`가 예외를 던지는 구조 그대로 둔다.
- 화면: `<form ... enctype="multipart/form-data">`. 칸: 해지하는 곳 체크박스 4개(`th:field="*{channels}"`, 값은 enum 이름, 글자는 label), 주의할 점 textarea, 난이도 select("선택 안 함" + 3개), 공식 해지 링크 input(type=url), 예상 시간 input(number 1~120, 단위 "분"), 아이콘 file input(`accept="image/png,image/jpeg,image/webp"`). 수정 화면에서 아이콘이 있으면 `<img src="/admin/icons/{id}" width="48" alt="현재 아이콘">`.

- [ ] **Step 1: 실패하는 테스트** (`@WithMockUser(roles="ADMIN")`, `multipart("/admin/guides/{id}/edit", netflixId)` + 기존 필수 칸 + `.with(csrf())`)

```java
@Test void savesNewDetails()            // channels=APP,PHONE, caution="주의", difficulty=HARD, officialCancelUrl="https://ex.com/cancel", estimatedMinutes=10
                                        // → 302, 다시 읽은 Subscription 값이 모두 같고 channels == EnumSet.of(APP, PHONE)
@Test void rejectsJavascriptUrl()        // officialCancelUrl="javascript:alert(1)" → 200, "공식 해지 링크는 https://로 시작" 포함, DB 값 그대로
@Test void rejectsMinutesOutOfRange()    // estimatedMinutes=0 → 200, "1분에서 120분" 포함
@Test void uploadsIcon()                 // iconFile=PNG 바이트(MockMultipartFile "iconFile","a.png","image/png") → subscription.getIcon() != null
@Test void rejectsSvgIconOnAdminForm()   // iconFile=svg 바이트 → 200, "PNG, JPEG, WebP 이미지만" 포함, icon 그대로 null
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*AdminGuideFormTest"`
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `./gradlew test` → 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 관리자 서비스 폼에 새 정보 칸과 아이콘 업로드`

---

### Task 4: 사용자 제보 화면의 추가 정보

**Files:**
- Modify: `dto/SubmissionForm.java`, `service/SubmissionService.java`, `controller/SubmissionController.java`, `templates/submit.html`, `static/css/site.css`
- Test: `PublicPagesTest.java`

**Interfaces:**
- Consumes: `IconService.store`, `Submission` 6인자 생성자
- Produces:
  - `SubmissionForm` 칸: `Set<CancelChannel> channels = EnumSet.noneOf(...)`, `@Size(max=500, message="주의할 점은 500자까지 쓸 수 있어요.") caution`, `MultipartFile iconFile`
  - `SubmissionService.submit(SubmissionForm form, IconImage icon)` — 빈 caution은 `null`
  - 컨트롤러 `submit`: 검증 오류가 없을 때 `iconService.store` → 실패하면 `rejectValue("iconFile", ...)`, 폼 다시. `preview`는 아이콘을 다루지 않는다.
- 화면: `<form enctype="multipart/form-data">`, 해지 방법 칸 아래 `<details class="more">`(오류가 channels·caution·iconFile에 있으면 `open`) `<summary>더 알려 주실 수 있나요? (선택)</summary>` 안에 해지하는 곳 체크박스 4개(`<fieldset><legend>해지하는 곳</legend>`), 주의할 점 textarea(`maxlength=500`), 아이콘 file input + 도움말 "PNG, JPEG, WebP · 200KB까지". AI 미리보기 뒤(`aiSteps != null`)에는 도움말 아래 "미리보기를 하면 고른 아이콘이 지워져요. 다시 골라 주세요." 추가. 오류 문구 연결은 1단계와 같은 `aria-describedby` 규칙(`iconFile-error`, `caution-error`).

- [ ] **Step 1: 실패하는 테스트**

```java
@Test void submitSavesDetails()       // multipart("/submit"): serviceName="왓챠", rawText 10자 이상, channels=APP&channels=WEB, caution="스토어 결제는 스토어에서", iconFile=PNG
                                      // → 302 /submit/done, 마지막 Submission: channels==EnumSet.of(APP,WEB), caution 같음, icon != null
@Test void submitRejectsSvgIcon()     // iconFile=svg → 200, "PNG, JPEG, WebP 이미지만", aria-describedby="iconFile-error", 제보 개수 그대로
@Test void submitPageHasMoreSection() // GET /submit → "더 알려 주실 수 있나요?", enctype="multipart/form-data"
```

(`SubmissionRepository`를 테스트에 주입. 개수 비교는 `count()` 앞뒤로.)

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*PublicPagesTest"`
- [ ] **Step 3: 구현** — `.more` 스타일: 테두리 상자, summary 굵게, 열면 안쪽 여백 16px. 체크박스는 한 줄에 여러 개(`display:flex; gap:12px; flex-wrap:wrap`).
- [ ] **Step 4: 통과 확인** — `./gradlew test` → 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 제보 화면에 해지하는 곳·주의할 점·아이콘 추가`

---

### Task 5: 승인할 때 가져오기

**Files:**
- Modify: `service/SubmissionService.java`, `controller/AdminSubmissionController.java`, `templates/admin/submission.html`
- Test: `src/test/.../service/SubmissionApproveTest.java`

**Interfaces:**
- Consumes: `Submission.hasDetails()`, `Subscription.changeChannelsAndCaution/changeIcon`, `IconService.store`
- Produces:
  - `approveAsNew(Long submissionId, SubscriptionForm form, IconImage uploadedIcon, boolean importAiSteps, boolean importDetails, boolean importIcon)` — 관리자가 폼에서 올린 아이콘(`uploadedIcon`)이 있으면 그것이 우선, 없고 `importIcon`이면 제보 아이콘.
  - `approveToExisting(Long submissionId, boolean importAiSteps, boolean importDetails, boolean importIcon)`
  - 가져오기 규칙: `importDetails`이고 제보에 값이 있을 때만 바꾼다 — channels가 비어 있지 않으면 channels를, caution이 비어 있지 않으면 caution을(따로따로). `importIcon`이고 제보 아이콘이 있으면 `changeIcon(submission.getIcon())`.
  - 컨트롤러: `@RequestParam(defaultValue="false") boolean importDetails, importIcon` 추가, approve 폼은 `enctype="multipart/form-data"`.
- 화면(`admin/submission.html`, Bootstrap 유지): 제보 내용 카드에 "해지하는 곳: 앱 · 웹", "주의할 점", 아이콘 `<img src="/admin/icons/{id}" width="48" alt="제보한 아이콘">`(있을 때만). 체크박스 두 개를 기존 AI 체크박스 옆에 — 새 서비스: `importDetails`·`importIcon` `checked`, 기존 서비스: 체크 없음. `submission.hasDetails()`가 false면 importDetails 숨김, `submission.icon == null`이면 importIcon 숨김.

- [ ] **Step 1: 실패하는 테스트** (`@SpringBootTest @Transactional`, 서비스 직접 호출. 제보는 `submissionRepository.save(new Submission("넷플릭스", "설정에서 해지해요. 열 글자 이상", null, EnumSet.of(APP), "스토어 결제 주의", icon))`)

```java
@Test void approveExistingWithImportCopiesValues()      // importDetails=true, importIcon=true → 넷플릭스 channels==EnumSet.of(APP), caution=="스토어 결제 주의", icon==제보 아이콘
@Test void approveExistingWithoutImportKeepsValues()    // false,false → 넷플릭스 channels==EnumSet.of(WEB), caution·icon 그대로
@Test void importSkipsEmptyFields()                     // 제보 caution null, channels APP → importDetails=true여도 넷플릭스 caution 그대로
@Test void approveAsNewImportsDetailsAndIcon()          // "왓챠" 제보, 폼 slug="watcha", uploadedIcon=null, 모두 true → 새 서비스에 channels·caution·icon
@Test void uploadedIconWinsOverSubmissionIcon()         // uploadedIcon=다른 IconImage, importIcon=true → 새 서비스 icon == uploadedIcon
```

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*SubmissionApproveTest"`
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `./gradlew test` → 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 제보 승인 때 해지하는 곳·주의할 점·아이콘 가져오기`

---

### Task 6: 사용자 화면에 새 정보 보여 주기

**Files:**
- Modify: `dto/GuideSummary.java`, `controller/GuideController.java`, `templates/fragments/ui.html`, `templates/guide.html`, `static/css/site.css`
- Test: `GuideSummaryTest.java`, `PublicPagesTest.java`

**Interfaces:**
- Consumes: `Subscription` 새 칸, `CancelChannel/Difficulty.getLabel()`
- Produces:
  - `GuideSummary.of(List<GuideStep> steps, Set<CancelChannel> channels)` — 기존 `of(steps)`는 지우고 호출부를 바꾼다. 문장: 단계 n≥1 + channels 있음 → `"{label·label}에서 n단계로 해지할 수 있어요."`, channels 없음 → 지금 문장 그대로. 단계 0개는 channels와 상관없이 지금 문장.
  - `GuideSummary.channelText(Set<CancelChannel>)`(static, 빈 집합이면 `null`) — `"앱·웹"`. 목록 줄에서도 쓴다(`${T(...GuideSummary).channelText(subscription.channels)}`).
  - `serviceIcon(subscription)`: `subscription.icon != null`이면 `<img class="svc-icon" src="/icons/{icon.id}" alt="" width="40" height="40" loading="lazy">`, 아니면 지금 글자 아이콘. `svc-icon-lg` 클래스는 호출하는 쪽에서 바깥 래퍼로 크기를 정한다.
  - `serviceRow` 보조 줄: `[channelText] · [n단계 | 단계 준비 중] · [약 N분]`(없는 칸은 빼고 ` · `로 이음) + 난이도가 있으면 오른쪽에 `<span class="chip chip-easy|chip-normal|chip-hard">쉬움</span>`
- `guide.html` 순서(스펙 5장): 칩 = 난이도(색) · 해지하는 곳 · 단계 수 · `약 N분` · 확인일 · 출처. 공식 링크 버튼 `<a class="btn btn-primary btn-lg" href="..." target="_blank" rel="noopener noreferrer">공식 해지 페이지 열기 ↗</a>`(있을 때만). 요약 상자: 경로 / 환불 / 해지하는 곳(값 또는 missingInfo). 요약 상자 아래 `<aside class="caution"><strong>주의할 점</strong> …</aside>`(있을 때만). 난이도·예상 시간이 없으면 칩을 빼고 요약 상자에 "난이도·예상 시간" 줄로 missingInfo 하나. 빈 정보 링크가 여러 개면 `missingInfo`에 `<span class="visually-hidden">` 로 항목 이름을 붙여 화면 낭독기에서 구분되게 한다(`missingInfo(name, field)`로 인자 추가).
- CSS: `.chip-easy{color:var(--easy);background:var(--easy-bg);font-weight:600}`, `.chip-hard{color:var(--hard);background:var(--hard-bg);font-weight:600}`, `.chip-normal`은 기본 칩. `img.svc-icon{object-fit:cover;background:var(--bg-soft)}`.

- [ ] **Step 1: 실패하는 테스트**

`GuideSummaryTest`:
```java
@Test void sentenceWithChannels() {
    assertEquals("앱·웹에서 3단계로 해지할 수 있어요.", GuideSummary.of(threeSteps, EnumSet.of(CancelChannel.WEB, CancelChannel.APP)).sentence());
}
@Test void noStepsIgnoresChannels() {
    assertEquals("아직 등록된 해지 단계가 없어요.", GuideSummary.of(List.of(), EnumSet.of(CancelChannel.APP)).sentence());
}
```
(기존 테스트 세 개는 `of(steps, EnumSet.noneOf(CancelChannel.class))`로 바꾼다.)

`PublicPagesTest`:
```java
@Test void guideShowsNewDetails()      // /guide/netflix: "웹에서 3단계로 해지할 수 있어요.", "쉬움", "chip-easy", "약 3분",
                                       // "공식 해지 페이지 열기", "rel=\"noopener noreferrer\"", "앱스토어나 구글플레이로 결제했다면"
@Test void guideWithoutDetailsShowsMissing() // /guide/melon: "주의할 점" 상자 없음(class="caution" 미포함), "공식 해지 페이지 열기" 미포함, "아직 확인된 정보가 없어요" 포함
@Test void homeRowShowsDetails()       // /: "웹 · 3단계 · 약 3분", "앱·웹 · 단계 준비 중 · 약 5분"
@Test void uploadedIconShownOnHome()   // 넷플릭스에 IconImage 연결 후 /: "<img class=\"svc-icon\" src=\"/icons/" 포함
```
1단계 테스트 중 `guideShowsSummaryFirstAndStepsOnce`의 `"3단계로 해지할 수 있어요."`는 넷플릭스 문장이 바뀌므로 `"웹에서 3단계로 해지할 수 있어요."`로 고친다.

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*GuideSummaryTest" --tests "*PublicPagesTest"`
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `./gradlew test` → 전체 PASS
- [ ] **Step 5: 커밋** — `feat: 가이드·목록에 난이도·해지하는 곳·예상 시간·공식 링크·아이콘 표시`

---

### Task 7: 화면 확인

- [ ] **Step 1:** `./gradlew bootRun` 후 375px·1280px 스크린샷: 홈, `/guide/netflix`(칩·버튼·주의 상자), `/guide/melon`(빈 정보), `/submit`(더 알려 주실 수 있나요 펼침), 관리자 서비스 수정·제보 상세.
- [ ] **Step 2: 실제 흐름** — 관리자 화면에서 넷플릭스에 PNG 아이콘을 올리고 홈·가이드에 보이는지, 제보(체크박스·주의할 점·아이콘) → 관리자 상세에서 아이콘이 보이고 공개 주소는 404인지 → 승인 후 공개 주소 200인지.
- [ ] **Step 3: 1단계에서 미룬 확인** — 관리자 화면에서 50자 서비스 이름·50자 버튼 이름 단계를 만들어 375px 홈·가이드에서 가로 스크롤이 없는지(`scrollWidth <= innerWidth`) 보고 지운다.
- [ ] **Step 4:** `./gradlew test` 전체 PASS, 개수 보고. 고친 것이 있으면 `fix: ...` 커밋.
