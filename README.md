# 구독 해지 가이드

서비스마다 다른 구독 해지 경로를 한국어로 모아 단계별로 안내하는 반응형 웹사이트입니다. 사용자가 해지 방법을 제보하면 AI가 초안을 단계로 나누고, 관리자가 검토한 뒤 공개합니다.

## 먼저 읽기

- [프로젝트 기획서·개발자 안내](docs/PROJECT-GUIDE.ko.md): 프로젝트 목적, 주요 화면과 기능, 기술 구성, 데이터 흐름, 실행 방법, 현재 상태
- [작업 규칙](AGENTS.md)
- [현재 인계 메모](docs/superpowers/handoff-2026-11-25.md)

## 기술 구성

Java 21 · Spring Boot · Thymeleaf · Spring Data JPA · Spring Security · H2(개발) · MySQL(운영 예정)

## 실행

Windows PowerShell에서 저장소 폴더로 이동한 뒤 실행합니다.

```powershell
$secure = Read-Host '로컬 관리자 비밀번호' -AsSecureString
$env:ADMIN_PASSWORD = [pscredential]::new('admin', $secure).GetNetworkCredential().Password
.\gradlew.bat bootRun
```

기본 로컬 주소는 `http://localhost:8080`입니다. 관리자 비밀번호는 실행할 때 입력하며 저장소에 기본값이 없습니다. 자세한 환경변수와 검증 명령은 [프로젝트 기획서](docs/PROJECT-GUIDE.ko.md#10-windows에서-실행하고-검증하기)를 참고하세요.
