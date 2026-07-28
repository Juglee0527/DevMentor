# DevMentor 아키텍처

## 1. 현재 범위

이 문서는 1단계에서 실제 구현된 실행 골격을 설명합니다. 사용자, 대화, 학습 상태, AI 연동 구조는 해당 개발 단계에서 코드와 함께 확장합니다.

## 2. 시스템 구성

```text
Browser
  │  http://localhost:5173
  ▼
React + TypeScript + Vite
  │  Axios /api
  ▼
Spring Boot :8080
  │  Spring Data JPA
  ▼
PostgreSQL :5432
```

### Frontend

- `BrowserRouter`가 화면 경로를 관리합니다.
- Axios 공통 인스턴스가 `VITE_API_BASE_URL`을 사용합니다.
- 시작 화면은 `/api/health`를 호출하여 백엔드 연결 여부를 표시합니다.
- 사용자 기능이 없는 현재 단계에서는 학습 시작 버튼을 비활성화합니다.

### Backend

- `HealthController`가 실행 상태를 제공합니다.
- `ApiResponse`가 성공·실패 응답 형태를 통일합니다.
- `GlobalExceptionHandler`가 검증, 미존재 리소스, 예상하지 못한 오류를 변환합니다.
- 예상하지 못한 내부 예외는 서버에 기록하되 상세 메시지를 클라이언트에 노출하지 않습니다.
- CORS는 `FRONTEND_ORIGIN` 한 개만 허용합니다.

### Database

- Docker Compose가 PostgreSQL 17 컨테이너와 영속 volume을 관리합니다.
- `pg_isready` 기반 health check로 연결 준비 여부를 판단합니다.
- 현재 Entity는 없으며 2단계에서 데이터 모델과 스키마를 추가합니다.

## 3. 현재 요청 흐름

```text
시작 화면 로드
→ getApiHealth()
→ GET /api/health
→ ApiResponse<HealthResponse>
→ 화면에 API 연결 상태 표시
```

## 4. 설정 경계

- DB 연결값은 환경변수로 재정의할 수 있습니다.
- 프론트엔드 API 주소는 `VITE_API_BASE_URL`로 관리합니다.
- 백엔드 CORS Origin은 `FRONTEND_ORIGIN`으로 관리합니다.
- API Key와 모델 설정은 파일에 고정하지 않습니다.

## 5. 다음 변경 예정

2단계에서 다음 내용이 추가됩니다.

- 핵심 Entity와 Repository
- Entity 관계와 DB 제약조건
- 초기 기술·개념 데이터
- PostgreSQL Testcontainers 통합 테스트

