# 해지 화면 캡처 2부 구현 계획

> **에이전트 작업 지침**
>
> 구현에는 superpowers:subagent-driven-development 또는 superpowers:executing-plans를 사용한다. 작업별 체크박스를 검증 결과에 맞춰 표시한다. 이슈 → 브랜치 → 구현 → 커밋 → push → PR → 병합을 단계별로 승인받는다.

**목표**는 제보자가 선택적으로 첨부한 해지 화면으로 AI 단계 그림을 보강하고, 관리자가 원문과 캡처를 대조해 검토할 수 있게 하는 것이다.

**구조**는 공개 제보 폼과 AI 미리보기가 같은 서버 이미지 검사·축소 기능을 사용하고, 최종 제보만 축소한 JPEG를 제보 ID에 연결해 저장하는 방식이다. 관리자가 승인하거나 반려하면 같은 트랜잭션에서 지우고, 매일 실행하는 정리 작업이 30일 지난 대기 제보의 캡처를 지운다.

**기술**은 Java 21, Spring Boot, Spring MVC·Security, JPA, H2·MySQL, Thymeleaf, 브라우저 JavaScript, Gemini OpenAI 호환 API다.

**설계 문서** [캡처와 글 안내 설계 ②~⑤](../specs/2026-11-24-screenshot-steps-design.md)

## 공통 조건

- 글의 단계 순서와 버튼 이름을 우선한다. 캡처는 위치·화면 제목·주변 메뉴를 보강한다. 서로 다르면 AI 단계의 note에 짧게 남긴다.
- 사용자가 고른 순서대로 최대 5장, 한 장 최대 5 MiB, HTTP 멀티파트 요청 전체 최대 25 MiB로 검사한다. PNG·JPEG·WebP만 받아 긴 변 1280px 이하의 JPEG로 만든다. 요청 전체 한도에 폼과 멀티파트 경계도 포함된다. 기존 아이콘의 200 KiB 검사는 계속 적용한다.
- 캡처 없이 제보, AI 미리보기 없이 제보, 기존 제보 조회가 모두 가능해야 한다. 기존 제보·AI 요청 제한은 유지한다.
- 원본 파일과 미리보기용 파일은 제보 DB에 저장하지 않는다. 원본·Base64·AI 응답 원문·API 키를 로그에 남기지 않는다.
- API 키는 Windows 사용자 환경변수에서 읽고 실행 프로세스로만 전달한다. 실제 API 확인과 브라우저 검증에는 개인정보 없는 테스트 화면을 쓴다.

## 모델 확인

병합 커밋은 `a07322c`다. 개인정보가 없는 576×1280 JPEG 테스트 화면 2장을 Google의 OpenAI 호환 `/chat/completions`에 순서대로 `image_url` 데이터 URL로 전송했다. `gemini-3.5-flash-lite`가 두 화면의 제목·버튼 이름·위치·주변 메뉴·첨부 순서를 모두 맞혔고, 성공 요청은 약 2.0초였다. [Google OpenAI 호환 API](https://ai.google.dev/gemini-api/docs/openai)

| 순서 | 화면 | 버튼 | 위치 | 주변 메뉴 |
| --- | --- | --- | --- | --- |
| 1 | 계정 도구 | 구독 관리 | 오른쪽 위 | 이용 내역 |
| 2 | 멤버십 설정 | 자동 갱신 끄기 | 아래쪽 | 요금제 보기 |

이 검증은 대표 화면 두 장과 이미지 전송 형식을 확인했다. 앱의 프롬프트, 글·이미지 불일치 처리, 다섯 장 입력은 아래 검증에 포함한다.

## 리뷰에서 볼 입력과 상태

- 파일 시그니처와 확장자가 다른 이미지, 중간에 잘린 이미지, 해상도가 지나치게 큰 이미지는 검사에서 거절한다.
- 미리보기를 기다리는 중 글이나 파일을 수정하거나 요청 순서가 뒤바뀌면 이전 응답이 최신 화면과 숨은 JSON을 덮어쓰지 않는다.
- 브라우저 스크립트 없이 파일을 보냈거나 폼이 다시 표시되면 다시 선택할 수 있고, 이전 첨부의 번호가 다른 파일을 가리키지 않는다.
- 승인 저장이 실패하거나 정리 작업과 검토가 겹쳐도 제보 상태와 캡처 보관 상태가 일치하고, 이미 없어진 이미지 요청은 404다.
- 관리자 URL을 주소창에 직접 입력하거나 이전 링크를 재사용해도 비로그인·비관리자에게 캡처 바이트를 보내지 않는다.

## 1. 캡처 검사와 정규화

**파일**

`build.gradle`, `service/IconValidator.java`, 새 `service/ImageSignatures.java`, `service/SubmissionScreenshotProcessor.java`, `service/ScreenshotValidationException.java`, 테스트 `service/SubmissionScreenshotProcessorTest.java`. Java 경로는 기존 `com.whtngml.subscription_cancel_guide` 패키지 아래다.

- [ ] PNG·JPEG·WebP 정상 샘플의 JPEG 출력, 긴 변 1280px 및 원래 비율, 투명 배경, 선택 순서, 0장·5장·6장, 5 MiB 경계, 확장자 위조·잘린 파일·큰 픽셀 크기를 테스트한다. 실패한 테스트를 `gradlew.bat test --no-daemon --tests '*SubmissionScreenshotProcessorTest'`로 확인한다.
- [ ] `SubmissionScreenshotProcessor.process(List<MultipartFile>) → List<byte[]>`를 구현한다. 빈 파일 입력은 제외하고 파일 개수와 크기를 먼저 검사한다. 기존 아이콘 검사에서 시그니처 판별 부분을 재사용한다. 헤더로 크기를 읽고 큰 픽셀 크기는 전체 디코딩 전에 거절한다. WebP는 TwelveMonkeys ImageIO WebP 리더를 사용한다. 실제 디코드한 뒤 투명 영역에는 흰색을 채우고 긴 변을 1280px 이하로 줄여 메타데이터 없이 JPEG로 인코딩한다. [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys)
- [ ] 프로세서 테스트와 `IconValidatorTest`를 실행한다. 승인된 커밋 단계에서 위 파일만 커밋한다.

## 2. Gemini 입력과 AI 응답 검증

**파일**

`ai/StepParser.java`, `ai/OpenAiCompatibleStepParser.java`, `ai/StepPrompt.java`, `ai/AiStepsJson.java` 및 기존 대응 테스트.

- [ ] 가짜 API 서버 테스트에 캡처 0·1·5장 요청을 추가한다. 첫 조각은 text, 다음 조각은 선택 순서와 같은 JPEG URL인지 본다. 프롬프트에는 캡처가 있을 때만 캡처 읽기·글 우선·불일치 note 규칙이 포함돼야 한다. 응답 단위 테스트는 캡처 번호 0, 음수, 장수 초과, 0장일 때 image·note, 100자 제한, 개인정보가 들어간 note, 메뉴 4개 제한을 확인한다.
- [ ] `StepParser.parse(String serviceName, String rawText, List<byte[]> screenshots)`와 기존 2인자 메서드의 빈 목록 위임을 추가한다. `StepPrompt.system(boolean hasScreenshots)`로 기존 조각을 조립한다. 이미지가 없으면 현재 요청을 유지한다. 이미지 번호는 1부터 시작한다. `AiStepsJson.read(String json, int screenshotCount)`에서 범위 밖 image는 null, 개인정보가 들어간 note는 null로 한다. 공백 note는 null로 두고 길이는 100자로 제한한다. 캡처가 한 장도 없으면 image·note를 비운다. 기존 화면·버튼·메뉴 개인정보 검사와 메뉴 전체 길이 제한을 재사용한다. 오류 로그에는 상태 코드나 안전한 고정 문구를 남긴다.
- [ ] `gradlew.bat test --no-daemon --tests '*OpenAiCompatibleStepParserTest' --tests '*StepPromptTest' --tests '*AiStepsJsonTest'`를 실행한다. 최종 앱 검증에서 버튼명이 서로 다른 글·캡처는 글의 버튼과 관리자 경고로 이어지는지 확인한다. 승인된 커밋 단계에서 위 파일만 커밋한다.

## 3. 저장, 보관 기간, 관리자 이미지 주소

**파일**

새 `domain/SubmissionScreenshot.java`, `repository/SubmissionScreenshotRepository.java`, `service/SubmissionScreenshotCleanup.java`, 기존 `service/SubmissionService.java`, `repository/SubmissionRepository.java`, `controller/AdminSubmissionController.java`, `SubscriptionCancelGuideApplication.java`, 테스트 `SubmissionApproveTest`, 새 `service/SubmissionScreenshotLifecycleTest.java`, `web/AdminPagesTest.java`.

- [ ] 저장 순서와 JPEG 바이트, 한 제보의 번호 중복 금지, 승인 두 경로·반려 시 삭제, 승인 롤백, 30일 경계, 만료 후 재호출, 비로그인·권한 없는 사용자 차단을 테스트한다. 유효한 관리자 GET은 JPEG와 `Cache-Control: no-store`를 반환하고, 다른 제보의 번호나 이미 삭제된 번호는 404인지 확인한다.
- [ ] `SubmissionScreenshot`의 필드는 `id, submission, imageOrder, data, contentType`으로 둔다. (제보 ID, 순서)에 고유 제약을 두고 저장소에서는 번호와 바이트 조회를 분리한다. MySQL 컬럼은 기존 아이콘의 LOB 매핑 방식을 참고해 축소 JPEG 최대 크기를 담을 수 있게 한다. H2 테스트와 MySQL용 스키마 매핑에서 타입을 확인한다.
- [ ] `SubmissionService.submit(SubmissionForm, IconImage, List<byte[]>) → Long`의 한 트랜잭션에서 제보와 캡처를 저장한다. 제출 JSON은 실제 저장할 캡처 장수로 다시 검증한다. 관리자 승인·반려는 대기 상태를 트랜잭션 안에서 잠그고 이미지 삭제를 함께 처리한다. 정리 작업은 매일 실행하며, 기준 시각을 인자로 받는 서비스 메서드에서 30일 경과한 대기 제보의 캡처만 삭제한다. 관리자 이미지 GET은 기존 `/admin/**` 권한 규칙을 사용한다.
- [ ] 관리자가 그림을 가져올 때 기존 `AiStep.toForm()`을 이용해 공개 `GuideStep`에 image·note가 들어가지 않는지 확인한다. `gradlew.bat test --no-daemon --tests '*SubmissionApproveTest' --tests '*SubmissionScreenshotLifecycleTest' --tests '*AdminPagesTest'`를 실행한다. 승인된 커밋 단계에서 위 파일만 커밋한다.

## 4. 제보 화면과 미리보기 연결

**파일**

`dto/SubmissionForm.java`, `controller/SubmissionController.java`, `config/UploadLimitAdvice.java`, `application.yaml`, `templates/submit.html`, `static/js/submit-guide.js`, `static/css/site.css`, `static/service-worker.js`, 테스트 `PublicPagesTest`, `UploadLimitTest`.

- [ ] 멀티파트 테스트로 `screenshots` 순서, 잘못된 업로드의 필드 오류, 전체 요청 한도 초과 후 제보 화면 안내, 관리자 아이콘 오류 문구, 미리보기 DB 쓰기 0건, AI 장애 시 캡처와 원문 제출을 확인한다.
- [ ] 해지 방법 칸 아래에 선택형 다중 파일 입력을 둔다. 설계의 순서·전체 화면·개인정보 가리기·Gemini 전송 안내를 적용하고, 30일 보관과 검토 뒤 삭제가 사이트의 캡처 보관에 관한 문구임을 분명히 한다. 사진을 고른 순서대로 번호와 삭제 버튼을 표시한다. 브라우저가 파일 목록을 유지하거나 편집할 수 없으면 기본 입력을 사용한다.
- [ ] `/submit/preview`와 `/submit` 모두 같은 프로세서를 호출한다. 최종 제출에서는 캡처를 검증한 뒤 기존 아이콘 저장을 진행한다. 스크립트가 있는 미리보기는 기존 멀티파트 폼을 fetch로 보내고 결과와 오류를 화면 일부에 반영해 선택한 파일을 유지한다. 글이나 캡처를 바꿀 때 결과와 숨은 JSON을 지우고, 수정 이전 요청의 응답은 무시한다. 전체 페이지 응답으로 파일 선택을 잃는 경우 다시 선택하라는 문구를 보여주고, 캡처 기반 미리보기 JSON은 새 선택에 넘기지 않는다.
- [ ] 멀티파트 한도를 5 MiB/25 MiB로 설정한다. 기존 초과 업로드 안내는 제보 캡처와 관리자 아이콘 경로에서 각각 맞는 문구를 보여준다. CSS·JS 변경을 배포할 때 서비스 워커를 `cancel-guide-v4`로 올린다.
- [ ] `gradlew.bat test --no-daemon --tests '*PublicPagesTest' --tests '*UploadLimitTest'`를 실행한다. 브라우저에서는 2장 선택, 하나 제거, 미리보기, 파일 선택 변경 중 응답 도착, 기본 파일 입력, 폼 오류 후 재선택을 확인한다. 승인된 커밋 단계에서 위 파일만 커밋한다.

## 5. 관리자 검토 화면과 통합 확인

**파일**

`controller/AdminSubmissionController.java`, `service/SubmissionService.java`, `templates/admin/submission.html`, `static/css/site.css`, 테스트 `AdminPagesTest`, `SubmissionApproveTest`.

- [ ] 기존 리뷰 페이지 테스트를 확장해 번호가 붙은 캡처, 단계별 이미지 링크, note 표시와 HTML 이스케이프, 캡처 없이 만든 옛 제보, 삭제 후 남은 AI JSON이 깨진 링크를 만들지 않는지 확인한다.
- [ ] 관리자용 모델에는 기존 `GuideStepForm` 그림과 `AiStep`의 image·note를 함께 넘긴다. 저장된 JSON을 읽을 때는 원래 검사한 슬롯 범위 1~5와 note를 유지하고, 실제 링크는 현재 존재하는 순서 번호와 대조해서 만든다. 캡처 목록은 순서 번호만 조회한다. 상세 화면 원문 아래에는 썸네일과 새 탭 링크를 두고, 단계 그림 옆에는 일치하는 캡처를 두며, 없으면 ‘캡처 없음’을 표시한다. note가 있으면 ‘캡처와 다름 ⚠’을 표시한다. 검토 뒤 지워진다는 안내를 포함한다. 좁은 화면에서는 세로로 배치한다.
- [ ] `gradlew.bat test --no-daemon` 전체가 통과하는지 확인한다. 로컬에서 가상 캡처로 제보→미리보기→제출→관리자 대조→승인/반려→이미지 404를 확인한다. 데스크톱과 375px 화면을 캡처하고 서버를 종료한다. 승인된 커밋 단계에서 위 파일만 커밋한다.

## GitHub 작업 순서

설계의 ‘이슈 하나, PR 두 개’에 따라 2부 범위가 포함된 기존 이슈 #39에서 이어 간다. 브랜치 생성, 구현, 커밋, push, PR, 병합에 각각 승인을 받는다.

## 구현 및 검증 기록

- 이슈 #39를 이어 쓰고, `feat/#39-screenshot-steps-part2`에서 구현과 검증을 마쳤다. 이 계획과 2부 코드를 로컬 커밋에 포함한다.
- `gradlew.bat test --no-daemon` 150개 통과. 실제 웹 서버에서 스크립트 없는 멀티파트의 파일 크기 초과 안내, WebP 변환, JPEG 방향 메타데이터 반영, 만료 정리, 관리자 권한과 삭제를 검증했다.
- 최종 프롬프트를 실제 Gemini에 보내 글에 적힌 ‘구독 취소’를 유지하고, 캡처의 ‘자동 갱신 끄기’와 다른 점을 기록하는 것을 확인했다.
- Playwright와 로컬 가짜 AI 서버로 업로드·삭제·미리보기·제출·관리자 대조·반려 후 404를 확인했다. 데스크톱과 375px 제보·관리자 화면을 `output/playwright/screenshot-steps-part2/`에 저장했다.
- MySQL BLOB 크기 매핑은 [Hibernate MySQLDialect 소스](https://github.com/hibernate/hibernate-orm/blob/7.4/hibernate-core/src/main/java/org/hibernate/dialect/MySQLDialect.java)에서 확인했다. 실제 MySQL 서버 테스트는 하지 않았다.
