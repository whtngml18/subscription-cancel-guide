# 카테고리 관리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 카테고리를 관리 페이지와 서비스·승인 폼 안에서 만들고, 이름을 고치고, 빈 카테고리를 지울 수 있게 한다. 빈 카테고리는 사용자 화면에서 숨긴다.

**Architecture:** 카테고리 규칙(검증·생성·삭제 가능 여부)은 새 `CategoryService` 한 곳에 둔다. 관리 페이지 컨트롤러와 기존 두 폼 컨트롤러(AdminGuideController, AdminSubmissionController)는 이 서비스를 부른다. 폼의 "새 카테고리"는 `categoryId == 0`으로 표시하고, `GuideAdminService`가 서비스 저장과 같은 트랜잭션에서 카테고리를 만든다.

**Tech Stack:** Spring Boot 4.1.2, Java 21, Thymeleaf, JPA, Bean Validation, JUnit 5, MockMvc

**Spec:** `docs/superpowers/specs/2026-11-23-category-management-design.md`

## Global Constraints

- 이름: 필수, strip 후 30자 이하. 주소(slug): 필수, `^[a-z0-9]+(-[a-z0-9]+)*$`, 30자 이하, 중복 금지. 주소는 만든 뒤 바꾸지 않는다.
- 새 카테고리 표시값: `SubscriptionForm.NEW_CATEGORY_ID = 0L`.
- 서비스가 1개 이상인 카테고리는 지우지 않는다(서버에서 다시 확인).
- 빈 카테고리: 메인 탭·사이트맵 제외, `/category/{slug}` 404.
- 기본 카테고리 6개(순서대로): OTT(ott), 음악(music), 쇼핑 멤버십(shopping), AI·생산성(ai), 배달·생활(life), 기타(etc).
- 문구(그대로 사용): "카테고리 이름을 입력해 주세요." / "카테고리 이름은 30자까지 쓸 수 있어요." / "주소를 입력해 주세요." / "주소는 영어 소문자, 숫자, -로 30자까지 써 주세요." / "이미 있는 주소예요." / "서비스가 있는 카테고리는 지울 수 없어요." / "카테고리를 추가했어요." / "카테고리 이름을 고쳤어요." / "카테고리를 지웠어요."
- 단계마다 사용자 허락(이슈·브랜치·커밋·push·PR·머지). 브랜치 `feat/#이슈번호-category-management`.

## Review Focus

1. 새 카테고리 + 서비스 slug 중복 등 서비스 쪽 오류 → 카테고리도 저장되지 않고, 폼에 새 카테고리 입력값이 남아 있음 (Task 3 테스트로 고정).
2. 주소 앞뒤 공백·대문자(" Delivery ") → 형식 오류로 거절(자동 변환 안 함), 이름은 strip해서 저장 (Task 1 테스트로 고정).
3. 다른 탭에서 서비스를 넣은 뒤 예전 목록 화면의 삭제 버튼 누름 → 지워지지 않고 안내 문구 (Task 2 테스트로 고정).
4. 기존 카테고리를 고른 채 새 카테고리 칸에 값이 남아 있음 → 새 칸은 무시, 카테고리 생성 안 됨 (Task 3 테스트로 고정).
5. 운영 재시작 시 관리자가 지운 기본 카테고리가 되살아나지 않음 (Task 1의 SQL. MySQL 전용이라 자동 테스트 없음 → 리뷰에서 SQL 직접 확인).

---

### Task 1: CategoryService와 기본 데이터

**Files:**
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/dto/CategoryForm.java`
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/service/CategoryService.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/domain/Category.java` (`rename`)
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/repository/SubscriptionRepository.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/repository/CategoryRepository.java`
- Modify: `src/main/resources/data.sql:2-4` (3줄 뒤에 ai, life, etc 추가)
- Modify: `src/main/resources/data-mysql.sql:1-5` (주석과 카테고리 INSERT 교체)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/service/CategoryServiceTest.java` (`@SpringBootTest @Transactional`)

**Interfaces:**
- Produces:
  - `CategoryForm` (@Getter @Setter): `name` — `@NotBlank(message="카테고리 이름을 입력해 주세요.") @Size(max=30, message="카테고리 이름은 30자까지 쓸 수 있어요.")`; `slug` — `@NotBlank(message="주소를 입력해 주세요.") @Pattern(regexp="^[a-z0-9]+(-[a-z0-9]+)*$", message=형식 문구) @Size(max=30, message=형식 문구)`.
  - `Category.rename(String name)`
  - `SubscriptionRepository.countByCategory(Category) : long`
  - `CategoryRepository.existsBySlug(String) : boolean`
  - `record CategoryService.CategoryRow(Category category, long serviceCount)`
  - `CategoryService`: `List<CategoryRow> getRows()` (id 순), `Category get(Long id)` (없으면 404 `ResponseStatusException`), `boolean isSlugTaken(String slug)`, `Category create(String name, String slug)` (name strip), `void rename(Long id, String name)` (strip), `boolean delete(Long id)` (서비스가 있으면 지우지 않고 false).

- [ ] **Step 1: 실패하는 테스트 작성** — `CategoryServiceTest`

```java
@Test void 만들면_이름은_strip해서_저장() {
    Category c = categoryService.create("  배달 ", "delivery");
    assertEquals("배달", c.getName());
    assertTrue(categoryService.isSlugTaken("delivery"));
}
@Test void 이름만_바뀌고_주소는_그대로() {
    Category c = categoryService.create("배달", "delivery");
    categoryService.rename(c.getId(), " 배달·생활 ");
    assertEquals("배달·생활", categoryService.get(c.getId()).getName());
    assertEquals("delivery", categoryService.get(c.getId()).getSlug());
}
@Test void 빈_카테고리는_지운다() {
    Category c = categoryService.create("배달", "delivery");
    assertTrue(categoryService.delete(c.getId()));
    assertFalse(categoryService.isSlugTaken("delivery"));
}
@Test void 서비스가_있으면_지우지_않는다() {
    Long ott = categoryRepository.findBySlug("ott").orElseThrow().getId();
    assertFalse(categoryService.delete(ott));
    assertTrue(categoryService.isSlugTaken("ott"));
}
@Test void 목록에_서비스_개수와_기본_6개() {
    List<CategoryService.CategoryRow> rows = categoryService.getRows();
    assertEquals(List.of("ott", "music", "shopping", "ai", "life", "etc"),
            rows.stream().map(r -> r.category().getSlug()).toList());
    assertTrue(rows.get(0).serviceCount() > 0);
    assertEquals(0, rows.get(5).serviceCount());
}
@Test void 주소_형식_검사() {   // Validator로 CategoryForm 검사
    assertFalse(validate("배달", " Delivery ").isEmpty());
    assertFalse(validate("배달", "a--b").isEmpty());
    assertFalse(validate("배달", "a".repeat(31)).isEmpty());
    assertTrue(validate("배달", "food-life2").isEmpty());
}
```
(`validate(name, slug)`는 테스트 안 도우미: `CategoryForm`을 만들어 주입받은 `jakarta.validation.Validator`로 검사.)

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*CategoryServiceTest"` → 컴파일 실패

- [ ] **Step 3: 구현** — Interfaces 그대로. `getRows`는 `categoryRepository.findAll(Sort.by("id"))` 각각에 `countByCategory`. data.sql에 3줄 추가.

- [ ] **Step 4: data-mysql.sql 교체** — 카테고리 3줄과 첫 줄 주석을 아래로 바꾼다(서비스 INSERT가 있으면 그대로 둔다):

```sql
-- 배포용 초기 데이터. 앱이 켜질 때마다 실행된다.
-- 카테고리는 표가 비어 있을 때(첫 배포)만 넣는다. 관리자가 지운 기본 카테고리가 다시 생기지 않게 한다.
INSERT INTO category (name, slug)
SELECT name, slug FROM (
    SELECT 1 AS ord, 'OTT' AS name, 'ott' AS slug
    UNION ALL SELECT 2, '음악', 'music'
    UNION ALL SELECT 3, '쇼핑 멤버십', 'shopping'
    UNION ALL SELECT 4, 'AI·생산성', 'ai'
    UNION ALL SELECT 5, '배달·생활', 'life'
    UNION ALL SELECT 6, '기타', 'etc'
) AS defaults
WHERE NOT EXISTS (SELECT 1 FROM category)
ORDER BY ord;
```

- [ ] **Step 5: 전체 테스트** — `./gradlew test` → PASS. (기존 테스트가 카테고리 3개를 가정했다면 그 테스트를 6개 기준으로 고치고 기록)

- [ ] **Step 6: 커밋** — `feat: 카테고리 서비스와 기본 카테고리 6개`

---

### Task 2: 관리 페이지 `/admin/categories`

**Files:**
- Create: `src/main/java/com/whtngml/subscription_cancel_guide/controller/AdminCategoryController.java`
- Create: `src/main/resources/templates/admin/categories.html`, `admin/category-form.html`
- Modify: `src/main/resources/templates/fragments/layout.html:82-83` (메뉴 "카테고리", `adminMenu == 'categories'`)
- Test: `src/test/java/com/whtngml/subscription_cancel_guide/web/AdminCategoryTest.java`

**Interfaces:**
- Consumes: `CategoryService`, `CategoryForm` (Task 1)
- Produces: GET `/admin/categories` (모델 `rows`, `form`, `adminMenu="categories"`), POST `/admin/categories`, GET·POST `/admin/categories/{id}/edit` (모델 `category`, `form`), POST `/admin/categories/{id}/delete`. 성공은 모두 `redirect:/admin/categories` + flash `message`.

화면은 기존 관리자 화면(guides.html 표, step-form.html 폼 칸 구조·오류 표시)과 같은 클래스·패턴을 쓴다. 삭제 버튼은 `serviceCount == 0`일 때만, 아니면 "서비스 N개" 글자.

- [ ] **Step 1: 실패하는 테스트** — `AdminCategoryTest` (`@SpringBootTest @AutoConfigureMockMvc @Transactional`, 클래스에 `@WithMockUser(roles="ADMIN")`)
  - `listShowsRowsAndAddForm`: GET → 200, 본문에 `"쇼핑 멤버십"`, `"서비스 "`, `action="/admin/categories"` 포함
  - `addsCategory`: POST name=배달, slug=delivery → 3xx, `categoryService.isSlugTaken("delivery")`
  - `rejectsBadOrDuplicateSlug`: slug=`Delivery` → 200 + 형식 문구, slug=`ott` → 200 + `"이미 있는 주소예요."`
  - `renamesKeepingSlug`: POST `/{etc id}/edit` name=그 밖 → 이름 바뀜, slug `etc` 유지
  - `deletesOnlyEmpty`: etc 삭제 → 사라짐. ott 삭제 → 3xx, flash `"서비스가 있는 카테고리는 지울 수 없어요."`, ott 남음
  - `requiresLogin`: `@WithAnonymousUser` GET → 3xx(로그인으로)

- [ ] **Step 2: 실패 확인** — `./gradlew test --tests "*AdminCategoryTest"` → FAIL(404 등)

- [ ] **Step 3: 컨트롤러·템플릿·메뉴 구현** — 중복 검사는 `@Valid` 통과 뒤 `isSlugTaken` → `rejectValue("slug", "duplicate", "이미 있는 주소예요.")`. 오류 시 목록 화면을 다시 그린다(모델 `rows` 포함).

- [ ] **Step 4: 전체 테스트** — `./gradlew test` → PASS

- [ ] **Step 5: 커밋** — `feat: 관리자 카테고리 관리 페이지`

---

### Task 3: 서비스·승인 폼에서 새 카테고리 만들기

**Files:**
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/dto/SubscriptionForm.java`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/service/CategoryService.java` (`checkChoice`)
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/service/GuideAdminService.java:49-70,158-161`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/controller/AdminGuideController.java:78,106,226-229` (checkCategory 제거 → `categoryService.checkChoice`)
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/controller/AdminSubmissionController.java:81-83`
- Modify: `src/main/resources/templates/fragments/subscription-fields.html:14-23` (+ 짧은 토글 스크립트)
- Test: `AdminGuideFormTest.java`, `SubmissionApproveTest.java` 또는 새 `AdminCategoryChoiceTest.java`

**Interfaces:**
- Consumes: `CategoryService.isSlugTaken/create`, `CategoryForm` 제약 (Task 1)
- Produces:
  - `SubscriptionForm.NEW_CATEGORY_ID = 0L`, 필드 `String newCategoryName`, `String newCategorySlug` (애너테이션 없음), `boolean isNewCategory()`.
  - `CategoryService.checkChoice(SubscriptionForm form, BindingResult result)`: `categoryId`에 이미 오류가 있으면 건너뜀. 새 카테고리면 두 값으로 `CategoryForm`을 만들어 `Validator`로 검사해 오류를 `newCategoryName`/`newCategorySlug` 칸에 옮기고, 통과하면 주소 중복 검사. 아니면 `existsById` 실패 시 기존 문구 "없는 카테고리예요. 다시 골라 주세요."
  - `GuideAdminService.getCategory(SubscriptionForm)`: 새 카테고리면 `categoryService.create(...)`, 아니면 기존 조회. create/updateSubscription이 이걸 쓴다(같은 트랜잭션).

조각: select 맨 아래 `<option value="0">+ 새 카테고리</option>`. 그 아래 `<div id="newCategoryFields">`에 이름(maxlength 30)·주소(maxlength 30, placeholder "delivery", 도움말 "영어 소문자, 숫자, -. 만든 뒤에는 바꿀 수 없어요.") 칸과 오류 표시. 스크립트는 select 값이 "0"일 때만 이 div를 보이게 한다(처음 그릴 때도 같은 규칙, 그래서 오류로 돌아와도 보임). 스크립트가 없으면 div는 그대로 보인다.

- [ ] **Step 1: 실패하는 테스트**
  - 서비스 추가 폼(기존 새 서비스 POST 경로, `AdminGuideFormTest`의 multipart 패턴)에서 `categoryId=0, newCategoryName=배달, newCategorySlug=delivery` → 3xx, 카테고리 delivery 생성, 서비스의 카테고리 slug가 delivery
  - `categoryId=0`, `newCategorySlug=ott` → 200 + "이미 있는 주소예요.", 서비스 생성 안 됨, 본문에 입력한 이름 "배달" 남음
  - `categoryId=0`, 서비스 slug가 이미 있는 값(`netflix`) + 올바른 새 카테고리 → 200, 카테고리 delivery 생성 안 됨 (Review Focus 1)
  - `categoryId=<ott id>`, newCategoryName/slug 채움 → 저장되고 delivery 카테고리 없음 (Review Focus 4)
  - 제보 승인(`/admin/submissions/{id}/approve`, `SubmissionApproveTest` 준비 방식)에서 새 카테고리로 승인 → 카테고리와 서비스 생성
  - 폼 GET 본문에 `value="0"` 옵션과 `id="newCategoryFields"` 포함

- [ ] **Step 2: 실패 확인** — 해당 테스트 → FAIL

- [ ] **Step 3: 구현** (Interfaces 그대로). 두 컨트롤러의 기존 카테고리 검사 코드는 지우고 `checkChoice` 한 줄로.

- [ ] **Step 4: 전체 테스트** — `./gradlew test` → PASS

- [ ] **Step 5: 커밋** — `feat: 서비스·승인 폼에서 새 카테고리 만들기`

---

### Task 4: 빈 카테고리 숨기기

**Files:**
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/controller/HomeController.java:20` (탭 = `groups`의 카테고리)
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/controller/CategoryController.java:36-37`
- Modify: `src/main/java/com/whtngml/subscription_cancel_guide/controller/SeoController.java:34-36`
- Test: `PublicPagesTest.java`

**Interfaces:**
- Consumes: `SubscriptionRepository.countByCategory` (Task 1), `GuideListService.groupedByCategory()` (기존, 이미 빈 묶음 제외)

- [ ] **Step 1: 실패하는 테스트** (`PublicPagesTest`)
  - `emptyCategoryHidden`: GET `/` 본문에 `"/category/etc"` 없음, `"/category/ott"` 있음. GET `/category/etc` → 404. GET `/sitemap.xml` 본문에 `/category/etc` 없음
  - `categoryAppearsWithService`: etc 카테고리에 서비스 하나 저장 → `/` 본문에 `"/category/etc"`, `/category/etc` 200

- [ ] **Step 2: 실패 확인** → FAIL

- [ ] **Step 3: 구현** — Home: `categories` 모델을 `groups.stream().map(CategoryGroup::category)`로. CategoryController: `countByCategory == 0`이면 404. Seo: `countByCategory > 0`만.

- [ ] **Step 4: 전체 테스트** — `./gradlew test` → PASS

- [ ] **Step 5: 화면 확인** — 앱 실행, 관리자 카테고리 페이지(추가·오류·삭제 버튼 유무), 서비스 폼에서 "+ 새 카테고리" 선택 시 칸 표시·오류 후 유지, 메인 탭에 빈 카테고리 없음. 데스크톱과 375px 스크린샷. 끝나면 앱 종료.

- [ ] **Step 6: 커밋** — `feat: 서비스가 없는 카테고리는 사용자 화면에서 숨기기`
