# DevMentor

> AI가 당신의 개발 실력을 분석하고, 부족한 부분을 찾아 성장 경로를 제안하는 개인 개발 멘토

DevMentor는 단발성 개발 질문 챗봇이 아니라 대화와 평가 결과를 누적하여 사용자의 이해 수준을 파악하고 다음 학습 경로를 제안하는 서비스입니다.

## 현재 구현 상태

현재 **8단계: 통합 검증과 문서 완성**까지 완료되어 MVP 기능과 검증 문서가 준비되었습니다.

| 영역 | 구현 상태 |
| --- | --- |
| PostgreSQL | Docker Compose, 환경변수, health check |
| Backend | Spring Boot 3.5.6, DB 연결, 공통 응답·예외·CORS, health API |
| Frontend | React, TypeScript, Vite, Router, Axios, 시작 화면 |
| 연결 검증 | 시작 화면에서 백엔드 API 연결 상태 표시 |
| 데이터 모델 | 사용자, 대화, 기술·개념, 학습 상태, 평가 Entity와 Repository |
| 초기 데이터 | 기술 9개, 핵심 개념 19개 |
| 사용자 기능 | 프로필 생성·조회·수정과 관심 기술 |
| 대화 기능 | 대화방 생성·조회·삭제, 사용자 메시지 저장·조회 |
| AI 멘토 | Fake/OpenAI Client 분리, 구조화 답변, 텍스트 fallback, 대화 화면 연동. 로컬 오픈 모델은 계획 수립 완료·미구현 |
| 학습 분석 | 감지 개념·지식 공백 상태 반영, 점수·복습일 규칙, 추천 개념 API |
| 학습 화면 | 전체·기술별 집계 대시보드, 기술·개념별 학습 현황 |
| 평가·복습 | 확인 질문 목록, AI 평가, 피드백·모범 답안, 평가 이력과 상태 반영 |

MVP 기능 구현과 Fake AI 기반 통합 검증을 완료했습니다. 실제 OpenAI live 검증과 운영 전 조치는 [MVP 검증 기록](./docs/verification.md)을 확인해 주세요. API Key 없이 실행하는 로컬 오픈 모델 연동은 [로컬 오픈 모델 개발 계획](./docs/open-model-development-plan.md)의 Task 9부터 진행합니다.

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5.6
- Gradle 8.14.4
- Spring Web, Spring Data JPA, Validation
- PostgreSQL
- JUnit 5

### Frontend

- React 19
- TypeScript
- Vite
- React Router
- Axios

### Infrastructure

- Docker Compose
- PostgreSQL 17

## 프로젝트 구조

```text
DevMentor
├─ backend
│  ├─ gradle/wrapper
│  ├─ src/main
│  ├─ src/test
│  ├─ build.gradle
│  └─ settings.gradle
├─ frontend
│  ├─ src
│  ├─ package.json
│  └─ vite.config.ts
├─ docs
├─ .env.example
├─ .gitignore
├─ docker-compose.yml
└─ README.md
```

## 실행 환경

- Java 21
- Node.js 20.19 이상 또는 22.12 이상
- Docker Desktop

## 환경변수 설정

PowerShell:

```powershell
Copy-Item .env.example .env
```

루트 `.env`는 Docker Compose가 직접 읽고, 백엔드와 프론트엔드도 각각 명시적으로 같은 파일을 불러옵니다. 기본값으로 실행할 때는 파일 내용을 수정하지 않아도 됩니다. 실제 API Key는 `.env`에만 작성하고 Git에 커밋하지 않습니다.

주요 환경변수:

| 변수 | 기본값 | 용도 |
| --- | --- | --- |
| `POSTGRES_DB` | `devmentor` | DB 이름 |
| `POSTGRES_USER` | `devmentor` | DB 사용자 |
| `POSTGRES_PASSWORD` | `devmentor` | DB 비밀번호 |
| `POSTGRES_PORT` | `5432` | 호스트 DB 포트 |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | 프론트엔드 API 주소 |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | 백엔드가 허용하는 프론트엔드 Origin |
| `AI_CLIENT_MODE` | `fake` | `fake`, `ollama`, `openai` AI 구현 선택 |
| `OPENAI_API_KEY` | 빈 값 | OpenAI 모드 인증 Key |
| `OPENAI_MODEL` | 빈 값 | OpenAI 모드에서 사용할 모델 |
| `OPENAI_TIMEOUT_SECONDS` | `30` | OpenAI 응답 제한 시간 |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama API 주소 |
| `OLLAMA_MODEL` | `qwen2.5:7b-instruct` | Ollama에서 실행할 RAG 개발 기준 모델 |
| `OLLAMA_TIMEOUT_SECONDS` | `120` | 로컬 모델 응답 제한 시간 |
| `OLLAMA_MAX_OUTPUT_TOKENS` | `1024` | 로컬 모델의 최대 출력 토큰 |

기본 `fake` 모드는 API Key 없이 결정적인 멘토 답변을 반환하므로 로컬 개발과 테스트에 사용합니다. 실제 OpenAI API를 사용하려면 `.env`에서 `AI_CLIENT_MODE=openai`, `OPENAI_API_KEY`, `OPENAI_MODEL`을 모두 설정합니다.

로컬 오픈 모델을 사용하려면 먼저 [Ollama for Windows](https://docs.ollama.com/windows)를 설치하고 모델을 준비합니다.

```powershell
ollama pull qwen2.5:7b-instruct
ollama list
```

그다음 `.env`를 다음과 같이 변경합니다.

```dotenv
AI_CLIENT_MODE=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b-instruct
OLLAMA_TIMEOUT_SECONDS=120
OLLAMA_MAX_OUTPUT_TOKENS=1024
```

Ollama가 실행되지 않았거나 지정 모델이 설치되지 않으면 백엔드는 안전한 `502` 응답을 반환합니다. `qwen2.5:7b-instruct`는 Task 12 비교에서 한국어와 구조화 출력이 가장 안정적이어서 RAG 개발 기준 모델로 고정했지만, 정확성 필수 게이트는 통과하지 못했습니다. 따라서 제품 기본값과 자동 테스트·CI는 계속 `fake` 모드를 사용하고 `ollama` 모드는 명시적으로 선택할 때만 활성화합니다.

## 실행 방법

### 1. PostgreSQL

저장소 루트에서 실행합니다.

```powershell
docker compose up -d postgres
docker compose ps
```

`devmentor-postgres` 상태가 `healthy`이면 준비된 것입니다.

종료:

```powershell
docker compose stop postgres
```

데이터까지 제거하는 `docker compose down -v`는 저장된 DB 데이터를 삭제하므로 의도한 경우에만 사용합니다.

### 2. Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

상태 확인:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

### 3. Frontend

새 PowerShell에서 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 엽니다.

Vite는 다른 서비스가 5173 포트를 사용 중이면 명확히 실패하도록 설정되어 있습니다. 다른 포트가 필요하면 프론트엔드 포트와 백엔드의 `FRONTEND_ORIGIN`을 같은 주소로 변경해야 합니다.

## 테스트와 빌드

Backend:

```powershell
cd backend
.\gradlew.bat test
```

Frontend:

```powershell
cd frontend
npm run build
npm run lint
```

## API 문서

- 현재 API: [docs/api-spec.md](./docs/api-spec.md)
- 현재 구조: [docs/architecture.md](./docs/architecture.md)
- 제품 요구사항: [docs/product-requirements.md](./docs/product-requirements.md)
- 개발 진행 기록: [docs/progress.md](./docs/progress.md)
- MVP 검증 결과: [docs/verification.md](./docs/verification.md)
- 로컬 오픈 모델 개발 계획: [docs/open-model-development-plan.md](./docs/open-model-development-plan.md)

## MVP 개발 순서

1. ~~실행 가능한 프로젝트 골격~~
2. ~~핵심 데이터 모델~~
3. ~~사용자와 대화 기본 기능~~
4. ~~AI 멘토 연동~~
5. ~~학습 상태 분석~~
6. ~~대시보드와 학습 현황~~
7. ~~평가와 복습~~
8. ~~통합 검증과 문서 완성~~

면접 모드, GitHub Repository 분석, RAG, 음성 기능 등은 MVP 이후 백로그입니다.

## MVP 이후 AI 개발 순서

1. 하드웨어 확인과 DevMentor 평가 기준 수립
2. Ollama 기반 로컬 오픈 모델 연결
3. 모델별 프롬프트·응답 스키마 공통화
4. DevMentor 평가셋으로 모델 비교와 기본 모델 확정
5. 지식 부족이 확인된 경우에만 RAG 도입
6. 검수 데이터와 개선 필요가 충분한 경우에만 LoRA 파인튜닝

상세 범위, 예외 처리, 완료 조건과 중단 기준은 [로컬 오픈 모델 개발 계획](./docs/open-model-development-plan.md)을 따릅니다.
