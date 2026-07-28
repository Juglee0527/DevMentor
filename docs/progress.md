# DevMentor 개발 진행 기록

## 1. 현재 상태

- 현재 단계: `1단계 - 실행 가능한 프로젝트 골격`
- 상태: `완료`
- 다음 작업: `2.1 사용자·대화 Entity`
- 마지막 갱신일: `2026-07-28`

## 2. 단계별 진행률

| 단계 | 상태 | 완료 작업 |
| --- | --- | --- |
| 0. 요구사항과 작업 체계 | 완료 | 제품 요구사항, 개발 로드맵, 진행 기록 |
| 1. 프로젝트 초기화 | 완료 | 구조, PostgreSQL, Spring Boot, React, 공통 API |
| 2. 핵심 데이터 모델 | 대기 | - |
| 3. 사용자와 대화 기본 기능 | 대기 | - |
| 4. AI 멘토 연동 | 대기 | - |
| 5. 학습 상태 분석 | 대기 | - |
| 6. 대시보드와 학습 현황 | 대기 | - |
| 7. 평가와 복습 | 대기 | - |
| 8. 통합 검증과 문서 | 대기 | - |

## 3. 변경 기록

### 2026-07-28 - 1단계 실행 가능한 프로젝트 골격

작업 범위:

- 루트 환경변수, Git 제외 규칙, Docker Compose 구성
- Spring Boot 3.5.6과 Gradle Wrapper 초기화
- PostgreSQL 연결, 공통 응답, 예외 처리, CORS, health API 구현
- React, TypeScript, Vite, Router, Axios 초기화
- 백엔드 연결 상태를 표시하는 시작 화면 구현
- 현재 구현에 맞게 README, 아키텍처, API 문서 갱신

변경 파일:

- 루트: `.gitignore`, `.env.example`, `docker-compose.yml`, `README.md`
- 백엔드: `backend/build.gradle`, Gradle Wrapper, `backend/src`
- 프론트엔드: `frontend/package.json`, `frontend/package-lock.json`, `frontend/src`
- 문서: `docs/architecture.md`, `docs/api-spec.md`, `docs/development-roadmap.md`, `docs/progress.md`

주요 결정:

- 공통 응답은 불필요한 상속 구조 없이 generic Java record로 구현
- 전역 예외 처리에서 내부 오류 상세를 고정 메시지로 치환
- 루트 `.env` 하나를 Docker Compose, Spring Boot, Vite가 공유하도록 명시적으로 구성
- Vite가 5173 충돌 시 다른 포트로 자동 이동하지 않도록 `strictPort` 적용
- 시작 화면은 Mock 학습 데이터 대신 실제 health API 연결 상태만 표시
- DB Entity와 Testcontainers는 데이터 모델을 정의하는 2단계에서 구현

검증:

- `docker compose config --quiet`: 성공
- `docker compose up -d postgres`: 성공
- `docker compose ps`: PostgreSQL `healthy`
- `backend\gradlew.bat test`: 성공, 5개 테스트 통과
- `frontend\npm run build`: 성공
- `frontend\npm run lint`: 성공
- 브라우저 `http://127.0.0.1:5174`: 시작 화면 렌더링 및 `API 연결됨` 확인
- `GET http://127.0.0.1:8080/api/health`: `success=true`, `status=UP`

남은 위험 및 다음 작업:

- 로컬의 5173 포트가 다른 프로젝트에서 사용 중이었으며 기본 실행 시 해당 프로세스를 먼저 종료하거나 CORS와 프론트엔드 포트를 함께 변경해야 함
- `npm audit`은 React Router의 RSC 서버 기능 관련 high advisory 2건을 보고함. 현재 앱은 브라우저 SPA이며 해당 RSC 기능을 사용하지 않지만, 수정 버전 공개 여부를 다음 프론트엔드 작업에서 다시 확인해야 함
- 2단계에서 DB migration 방식과 Entity 제약조건을 먼저 확정해야 함

### 2026-07-28 - 문서 관리 체계 생성

작업 범위:

- 첨부된 개발 요청 프롬프트를 MVP 요구사항으로 정리
- 하나씩 개발 지시할 수 있도록 0~8단계와 세부 작업 ID 정의
- 각 작업의 완료 조건과 검증 기준 정의
- 문서별 책임과 갱신 규칙 정의

생성 파일:

- `docs/README.md`
- `docs/product-requirements.md`
- `docs/development-roadmap.md`
- `docs/progress.md`

주요 결정:

- 면접 모드, GitHub Repository 분석, RAG는 MVP가 아닌 향후 백로그로 분류
- `architecture.md`, `api-spec.md`, `database-design.md`는 빈 문서로 선행 생성하지 않고 해당 구현 단계에서 코드와 함께 작성
- 한 번의 개발 요청은 로드맵의 작업 ID 하나를 기본 범위로 사용

검증:

- 첨부 프롬프트의 목표, 기능 범위, 기술 스택, 제외 범위, 완료 조건을 문서에 반영
- 기존 README와 요구사항의 범위 충돌을 식별하고 문서에 명시
- 코드 변경이 없어 빌드와 테스트는 실행하지 않음

남은 위험 및 결정사항:

- 관심 기술 저장 방식
- MVP 사용자 생성 방식
- DB migration 방식
- OpenAI 공식 API 연동과 구조화 출력 방식
- 학습 점수와 상태 전이의 구체적인 수치

## 4. 작업 종료 시 기록 양식

아래 양식을 복사하여 최신 변경 기록 위에 추가합니다.

```md
### YYYY-MM-DD - 작업 ID와 제목

작업 범위:

- 

변경 파일:

- 

주요 결정:

- 

검증:

- 실행 명령:
- 결과:

남은 위험 및 다음 작업:

- 
```
