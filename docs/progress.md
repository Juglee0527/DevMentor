# DevMentor 개발 진행 기록

## 1. 현재 상태

- 현재 단계: `5단계 - 학습 상태 분석`
- 상태: `완료`
- 다음 작업: `6.1 대시보드 집계 API`
- 마지막 갱신일: `2026-07-28`

## 2. 단계별 진행률

| 단계 | 상태 | 완료 작업 |
| --- | --- | --- |
| 0. 요구사항과 작업 체계 | 완료 | 제품 요구사항, 개발 로드맵, 진행 기록 |
| 1. 프로젝트 초기화 | 완료 | 구조, PostgreSQL, Spring Boot, React, 공통 API |
| 2. 핵심 데이터 모델 | 완료 | Entity, Repository, 초기 기술·개념, Testcontainers |
| 3. 사용자와 대화 기본 기능 | 완료 | 사용자·대화방·메시지 API와 실제 화면 |
| 4. AI 멘토 연동 | 완료 | Client 경계, OpenAI 구조화 출력, fallback, 대화 화면 |
| 5. 학습 상태 분석 | 완료 | 상태 전이 규칙, AI 분석 반영, 추천 개념 API |
| 6. 대시보드와 학습 현황 | 대기 | - |
| 7. 평가와 복습 | 대기 | - |
| 8. 통합 검증과 문서 | 대기 | - |

## 3. 변경 기록

### 2026-07-28 - 5단계 학습 상태 분석

작업 범위:

- 개념 감지·지식 공백·정답·오답별 점수와 복습일 규칙 구현
- AI 감지 개념과 지식 공백을 `UserConceptStatus`에 생성·갱신
- 중복 개념, 낮은 신뢰도, 알 수 없는 코드 필터링
- 부족한 개념 추천 조회 API와 정렬 규칙 구현
- AI 답변 메시지와 학습 상태의 원자적 저장

주요 결정:

- 점수 변화는 감지 `+10`, 지식 공백 `-15`, 정답 `+20`, 오답 `-20`으로 고정하고 0~100 보장
- 지식 공백·오답은 `NEEDS_REVIEW`, 80점 이상 정상 근거는 `UNDERSTOOD`, 그 외는 `LEARNING`
- 복습일은 `NEEDS_REVIEW` 1일, `LEARNING` 3일, `UNDERSTOOD` 7일 후
- 같은 응답의 동일 개념은 한 번만 반영하고 지식 공백을 감지보다 우선
- 신뢰도 0.6 미만 감지와 존재하지 않는 코드는 무시하며 코드만 안전하게 경고 로그 기록

검증:

- `backend\gradlew.bat test --rerun-tasks`: 성공, 17개 테스트 통과
- 0·100 점수 경계, 감지, 공백, 정답, 오답, 잘못된 현재 점수 단위 테스트 성공
- PostgreSQL에서 중복·낮은 신뢰도·알 수 없는 코드 처리와 추천 결과 통합 테스트 성공
- 대화 API에서 AI 답변 저장과 JPA 개념 상태 10점 반영·추천 조회 성공

남은 위험 및 다음 작업:

- AI 감지 신뢰도와 점수 증감값은 MVP 초기 규칙이며 실제 사용 데이터로 조정 필요
- 현재 기술·개념 간 선수 관계 모델은 없으므로 추천은 저장된 부족 상태 기준
- 6단계에서 빈 학습 이력과 기술별 집계를 화면에 연결

### 2026-07-28 - 4단계 AI 멘토 연동

작업 범위:

- `AiTutorClient` 경계와 Fake/OpenAI 구현 분리
- 사용자 프로필, 목표, 관심 기술, 개념 상태, 최근 메시지 최대 10개의 AI 문맥 구성
- Responses API JSON Schema 구조화 출력 요청과 DTO 재검증
- JSON 파싱·필드 검증 실패 시 일반 텍스트 fallback
- 사용자 질문, AI 답변, 분석 JSON 저장과 대화 화면 표시
- 확인 질문과 추천 개념 표시

주요 결정:

- 기본 `AI_CLIENT_MODE=fake`로 API Key 없는 로컬·테스트 재현성 확보
- 실제 OpenAI 모드는 `OPENAI_API_KEY`, `OPENAI_MODEL`을 환경변수로 필수 설정
- 외부 네트워크 호출 중 DB 트랜잭션을 유지하지 않도록 사용자 메시지와 AI 메시지를 각각 저장
- AI 연결 실패 시 사용자 질문은 유지하고 AI 답변은 저장하지 않으며 `502` 반환
- 파싱 실패는 서비스 실패로 처리하지 않고 원문을 일반 답변으로 저장
- API Key, 프롬프트 전문, 외부 오류 본문은 로그에서 제외

검증:

- `backend\gradlew.bat test`: 성공, 11개 테스트 통과
- 구조화 응답 정상, 일반 텍스트 fallback, 필드 범위 위반 fallback, 외부 HTTP 실패 변환 테스트 성공
- PostgreSQL Testcontainers에서 USER·ASSISTANT 메시지 저장·재조회 성공
- `frontend\npm run build`, `npm run lint`: 성공
- 브라우저에서 사용자 생성 → 질문 전송 → Fake AI 답변·확인 질문 표시 성공
- 새로고침 후 USER·ASSISTANT 메시지 PostgreSQL 재조회 성공
- 브라우저 console error 없음
- 실제 OpenAI API는 자동 테스트에서 호출하지 않음

남은 위험 및 다음 작업:

- 실제 API Key를 사용한 운영 모델 응답 품질·비용·rate limit은 배포 환경에서 별도 확인 필요
- 확인 질문과 추천 개념 카드는 현재 전송 직후 응답에서 표시하며 새로고침 후에는 메시지 본문만 복원
- 5단계에서 신뢰할 수 없는 감지 코드를 검증하고 학습 상태에 반영

### 2026-07-28 - 3단계 사용자와 대화 기본 기능

작업 범위:

- 사용자 프로필 생성·조회·수정과 관심 기술 검증
- 대화방 생성·목록·상세·삭제와 사용자 소유권 검증
- 사용자 메시지 저장·생성 시간순 조회와 빈 메시지 차단
- 프로필 입력 시작 화면과 대화방·메시지 화면 실제 API 연결
- 로컬 사용자·마지막 대화방 컨텍스트 유지

주요 결정:

- 인증이 없는 MVP에서도 모든 대화방·메시지 API에 `userId`를 요구해 소유권 확인
- 메시지 저장 시 대화방 수정 시간을 갱신해 최근 활동순 정렬 유지
- AI 답변은 4단계 범위이므로 3단계 화면에서 사용자 메시지만 저장

검증:

- `backend\gradlew.bat test --no-daemon`: 성공, 7개 테스트 통과
- 사용자 생성 → 대화방 생성 → 메시지 저장·조회 통합 테스트 성공
- 다른 사용자 대화방 접근 404, 빈 메시지 400 검증 성공
- `frontend\npm run build`, `npm run lint`: 성공
- 브라우저에서 사용자·대화방 생성, 메시지 저장, 새로고침 후 재조회 성공
- 브라우저 console error 없음

남은 위험 및 다음 작업:

- 인증 도입 전까지 클라이언트가 전달하는 userId 기반 소유권 검증이라는 MVP 한계가 있음
- 4단계에서 사용자 메시지 저장과 AI 응답 저장의 트랜잭션 정책을 확정해야 함

### 2026-07-28 - 2단계 핵심 데이터 모델

작업 범위:

- User, ChatRoom, ChatMessage, Skill, Concept, UserConceptStatus, Assessment 구현
- 메시지 역할, 개념 난이도, 학습 상태 enum 구현
- 사용자 관심 기술 연결과 주요 unique·index·점수 check constraint 구성
- 7개 Repository와 사용자 소유·정렬·복습 조회 메서드 구현
- 기술 9개와 핵심 개념 19개 누락 보완 초기화 구현
- PostgreSQL Testcontainers 기반 context·Repository 통합 테스트 작성

주요 결정:

- 로컬 스키마는 MVP 기간에 `ddl-auto=update`, 테스트는 `create-drop` 사용
- 운영 배포 전 migration 도구 도입 필요성을 `database-design.md`에 명시
- PostgreSQL 예약어 `CURRENT_ROLE`과 충돌하지 않도록 컬럼명을 `job_role`로 지정
- 초기 데이터는 전체 존재 여부가 아니라 code별 누락 항목만 추가

검증:

- `backend\gradlew.bat test --no-daemon`: 성공, 6개 테스트 통과
- PostgreSQL 17 Testcontainers: 스키마·관계·저장·조회 성공
- 초기 기술 9개, 개념 19개 검증 성공
- `frontend\npm run build`: 성공
- `frontend\npm run lint`: 성공

남은 위험 및 다음 작업:

- 운영 배포 전 Flyway 또는 Liquibase migration 전환 필요
- 3단계에서 DTO Validation과 사용자·대화 소유권 검증을 서비스 계층에 구현

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
