# DevMentor 제품 요구사항

## 1. 문제 이해

DevMentor는 단발성 개발 질문 챗봇이 아니라, 대화와 평가 결과를 누적하여 사용자의 이해 수준과 지식 공백을 파악하고 다음 학습 경로를 제안하는 AI 기반 개발 멘토입니다.

핵심 사용자 흐름은 다음과 같습니다.

```text
개발 질문
→ 기존 학습 상태 조회
→ 사용자 수준에 맞는 AI 설명
→ 부족한 개념 감지 및 저장
→ 다음 학습 개념 추천
→ 확인 질문과 답변 평가
→ 학습 상태 갱신
```

제품 문구:

> AI가 당신의 개발 실력을 분석하고, 부족한 부분을 찾아 성장 경로를 제안하는 개인 개발 멘토

## 2. MVP 목표

MVP는 다음 질문에 답할 수 있어야 합니다.

1. 사용자가 현재 알고 있는 개발 지식은 무엇인가?
2. 잘못 이해하거나 선행 지식이 부족한 개념은 무엇인가?
3. 사용자 수준에 맞는 설명 난이도는 어느 정도인가?
4. 다음에 학습하거나 복습할 개념은 무엇인가?
5. 확인 질문에 대한 답변 결과가 학습 상태에 반영되었는가?

## 3. 사용자와 주요 화면

초기 MVP는 인증 없이 한 명의 기본 사용자 또는 닉네임 기반 생성 사용자를 대상으로 합니다.

| 경로 | 책임 |
| --- | --- |
| `/` | 사용자 시작 및 프로필 입력 |
| `/dashboard` | 전체·기술별 이해도, 부족한 개념, 추천 학습, 최근 대화 |
| `/chat/:chatRoomId` | 대화, 확인 질문, 추천 개념, 현재 학습 상태 |
| `/learning` | 기술·개념별 점수와 상태 조회 |
| `/review` | 복습 대상 조회, 답변 제출, AI 평가 확인 |

화면은 Mock 데이터 전시용으로 만들지 않으며 실제 백엔드 API와 연결합니다. 초기에는 차트 라이브러리 없이 CSS 진행률 막대를 사용합니다.

## 4. MVP 기능 범위

### 4.1 사용자

- 닉네임, 개발 경력, 현재 역할, 학습 목표 관리
- 관심 기술 관리 방식은 사용자 API 설계 전에 확정
- 복잡한 회원가입, OAuth, 권한 체계 제외

### 4.2 대화

- 사용자별 여러 대화방 생성·조회·삭제
- `USER`, `ASSISTANT`, `SYSTEM` 역할의 메시지 저장
- 메시지를 생성 시간순으로 조회
- 다른 사용자의 대화방 접근 차단

### 4.3 AI 멘토

- 사용자 프로필, 학습 목표, 관련 개념 상태, 최근 메시지 최대 10개를 문맥으로 사용
- 맞춤 설명, 감지 기술, 지식 공백, 확인 질문, 추천 개념을 구조화된 응답으로 반환
- 구조화 응답을 백엔드 DTO로 검증
- JSON 파싱 실패 시 일반 텍스트 답변으로 안전하게 처리
- AI 호출 구현은 `AiTutorClient` 인터페이스 뒤로 격리

### 4.4 기술과 개념

- 기술과 세부 개념을 분리
- 최소 기술: Java, Spring, Spring Boot, JPA, Database, Redis, React, Git, Docker
- 개념 난이도: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- 초기 데이터는 `data.sql` 또는 명시적인 초기화 코드 중 구현 시점에 한 방식을 선택

### 4.5 학습 상태

- 상태: `NOT_STARTED`, `LEARNING`, `UNDERSTOOD`, `NEEDS_REVIEW`
- 이해도 점수 범위: 0~100
- AI 판단 근거, 마지막 학습일, 다음 복습일 저장
- 초기에는 서비스 계층의 단순하고 테스트 가능한 규칙으로 점수와 상태 갱신

### 4.6 평가와 복습

- AI 설명 뒤 확인 질문 제공
- 사용자 답변을 AI가 점수, 정오답, 피드백, 모범 답안, 복습 필요 여부로 평가
- 평가 결과 저장 후 사용자 개념 상태에 반영

## 5. 기술 기준

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.x, Gradle, Spring Web, Spring Data JPA, Validation, Lombok |
| Database | PostgreSQL |
| Frontend | React, TypeScript, Vite, React Router, Axios |
| AI | 공식 OpenAI API, 환경변수 기반 모델 설정 |
| Test | JUnit 5, Testcontainers, Fake AI client |
| Infrastructure | Docker Compose, GitHub |

백엔드 기본 패키지는 `com.devmentor`이며 `common`, `user`, `chat`, `learning`, `skill`, `assessment`, `ai` 도메인으로 구성합니다. 각 도메인 내부 계층은 실제 필요가 생길 때만 추가합니다.

## 6. 핵심 데이터

| Entity | 주요 책임 |
| --- | --- |
| `User` | 프로필과 학습 목표 |
| `ChatRoom` | 사용자별 학습 대화 |
| `ChatMessage` | 발신자, 내용, AI 분석 결과 |
| `Skill` | 상위 기술 분류 |
| `Concept` | 기술별 학습 개념과 난이도 |
| `UserConceptStatus` | 사용자별 점수, 상태, 학습·복습 시점 |
| `Assessment` | 확인 질문과 사용자 답변 평가 결과 |

정확한 관계, 제약조건, 컬럼 타입은 2단계 구현 시 `database-design.md`에 확정합니다.

## 7. API 원칙

- 기본 경로: `/api`
- 공통 응답은 `success`, `message`, `data` 구조 사용
- 입력값은 Bean Validation으로 검증
- `@RestControllerAdvice`로 예외를 일관되게 변환
- 내부 예외 메시지와 stack trace를 클라이언트에 노출하지 않음
- 사용자 소유 리소스 접근 시 소유권 검증
- AI 실패와 AI JSON 파싱 실패를 구분하여 처리

세부 계약은 구현과 함께 `api-spec.md`에서 관리합니다.

## 8. 설정과 보안

- `.env`, `application-local.yml`은 Git에서 제외
- `OPENAI_API_KEY`, `OPENAI_MODEL`을 환경변수로 사용
- 모델명과 API Key를 코드에 고정하지 않음
- 로컬 CORS 허용 대상은 `http://localhost:5173`
- PostgreSQL 포트는 환경변수로 변경 가능하게 구성

## 9. MVP 제외 범위

다음 기능은 향후 백로그이며 MVP 코드에 선행 구조를 만들지 않습니다.

- GitHub·소셜 로그인
- GitHub Repository 분석과 AI 코드 리뷰
- 면접 전용 모드
- 결제, 조직, 관리자, 고급 권한
- 음성 대화, 모바일 앱
- 코드 실행 서버, WebSocket
- 벡터 데이터베이스, RAG, 복잡한 지식 그래프
- Redis, Kubernetes, 마이크로서비스

현재 README에 소개된 면접 모드와 Repository 분석은 제품 비전으로만 유지하며 MVP 완료 조건에는 포함하지 않습니다.

## 10. MVP 완료 조건

- [x] 사용자를 생성·조회·수정할 수 있다.
- [x] 사용자의 대화방을 생성·조회·삭제할 수 있다.
- [x] 개발 질문을 AI에 전달하고 답변을 화면에 표시할 수 있다.
- [x] 대화 내용이 PostgreSQL에 저장된다.
- [x] 감지 기술과 부족한 개념이 학습 상태에 반영된다.
- [x] 기술별 이해도와 다음 추천 학습을 확인할 수 있다.
- [x] 확인 질문에 답하고 AI 평가를 받을 수 있다.
- [x] 평가 결과가 학습 상태와 복습 대상에 반영된다.
- [x] 프론트엔드와 백엔드가 실제 API로 연결된다.
- [x] Docker Compose로 PostgreSQL을 실행할 수 있다.
- [x] 주요 서비스 규칙에 단위 테스트가 있다.
- [x] 주요 저장·조회 흐름에 PostgreSQL 통합 테스트가 있다.
- [x] README만으로 로컬 실행과 테스트가 가능하다.

## 11. 확정된 MVP 결정

1. 관심 기술은 `User`와 `Skill`의 다대다 연결로 저장합니다.
2. 인증 없는 MVP는 닉네임 기반 사용자를 화면에서 생성합니다.
3. 초기 기술·개념은 누락 항목 보완 initializer, 로컬 스키마는 `ddl-auto=update`를 사용합니다.
4. 실제 AI는 OpenAI Responses API의 JSON Schema 구조화 출력으로 연동합니다.
5. AI 분석 DTO는 `ChatMessage.analysisJson` text 컬럼에 JSON 문자열로 저장합니다.
6. 점수 변화는 감지 `+10`, 지식 공백 `-15`, 평가 정답 `+20`, 오답 `-20`이며 0~100으로 제한합니다.

운영 배포 전 migration 도구와 인증·인가 도입은 [검증 기록](./verification.md)의 제한사항으로 관리합니다.
