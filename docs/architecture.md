# DevMentor 아키텍처

## 1. 현재 범위

이 문서는 DevMentor MVP와 이후 로컬 오픈 모델·RAG 확장 구조를 설명합니다.

## 2. 시스템 구성

```text
Browser
  │  React + TypeScript + Axios
  ▼
Spring Boot :8080
  ├─ 사용자·대화 서비스
  ├─ AI 문맥 구성
  ├─ 검수 지식 검색
  ├─ 학습 상태 규칙·분석
  ├─ 대시보드·학습 현황 조회
  ├─ 확인 질문 평가·복습
  └─ AiTutorClient
       ├─ FakeAiTutorClient (기본 로컬·테스트)
       ├─ OllamaAiTutorClient (로컬 오픈 모델)
       └─ OpenAiTutorClient (Responses API)
  │
  ▼
PostgreSQL :5432
```

### Frontend

- 시작 화면에서 사용자와 첫 대화방을 생성합니다.
- 대화 화면은 사용자 질문과 AI 답변을 실제 API로 저장·조회합니다.
- 최신 구조화 응답의 확인 질문, 추천 개념, RAG 근거 문서를 별도 카드로 표시합니다.
- Axios 공통 인스턴스가 `VITE_API_BASE_URL`을 사용합니다.

### Backend

- 사용자·대화 서비스가 입력과 리소스 소유권을 검증합니다.
- `AiContextService`가 사용자 프로필, 관심 기술, 기존 개념 상태, 최근 메시지 최대 10개, 허용 개념 카탈로그와 관련 검수 문서를 읽습니다.
- `KnowledgeRetrievalService`는 활성 `PUBLIC` 문서만 대상으로 명시적 키워드와 토큰 점수를 계산하고 임계값 이상 최대 3건만 반환합니다.
- 지식 문서 안의 문장은 시스템 지시가 아닌 데이터로 취급하며, 응답 API는 모델에 제공한 문서 ID·제목·버전·원문 URL을 별도로 반환합니다.
- `AiTutorClient`가 외부 AI 경계를 정의하고 Fake, OpenAI, Ollama 구현을 분리합니다.
- OpenAI 구현은 JDK `HttpClient`로 Responses API를 호출하며 JSON Schema 구조화 출력을 요청합니다.
- Ollama 구현은 별도 JDK `HttpClient`로 로컬 Chat API를 호출합니다.
- 시스템 지시문, tutor/assessment JSON Schema, 구조화 내용 파싱은 모델 구현체와 무관한 공통 컴포넌트입니다.
- 외부 응답은 Jackson 파싱 후 Bean Validation과 평가 논리 일관성 규칙으로 다시 검증합니다.
- 파싱·필드 검증 실패는 일반 텍스트 답변으로 전환하고, 연결·HTTP 오류는 `502`로 변환합니다.
- `LearningProgressPolicy`가 AI 값과 분리된 고정 규칙으로 점수·상태·복습일을 계산합니다.
- 감지 신뢰도 0.6 미만, 알 수 없는 코드, 한 응답의 중복 개념은 상태 오염을 막기 위해 필터링합니다.
- 학습 현황은 기술·개념 한 번, 사용자 상태 한 번의 fetch 조회 후 메모리에서 결합합니다.
- 대시보드는 같은 학습 현황 결과로 전체·기술별 평균과 보완 개념을 계산하고 최근 대화만 추가 조회합니다.
- 복습 조회는 최근 AI 메시지의 검증된 분석 JSON과 사용자 개념 상태를 결합해 미제출 확인 질문을 구성합니다.
- 평가는 `AiTutorClient.assess` 경계 뒤에서 Fake/OpenAI 구현을 공유하고 구조화 응답만 허용합니다.
- API Key, 프롬프트, 외부 오류 본문은 로그에 남기지 않습니다.
- AI 답변 메시지에는 provider, model tag·version, 프롬프트 버전, 응답 시간, fallback 유형, RAG 문서 ID를 함께 저장합니다.
- 사용자가 남긴 도움 여부·수정 답안·학습 동의는 별도 `AiFeedback` snapshot으로 저장하고 철회된 데이터는 학습 집계에서 제외합니다.

### Database

- PostgreSQL 17에 사용자, 대화, 메시지, 기술·개념, 학습 상태, 평가 Entity를 저장합니다.
- AI 답변 본문은 `ChatMessage.content`, 검증된 분석 DTO는 `analysis_json` text 컬럼에 JSON으로 저장합니다.
- 로컬은 `ddl-auto=update`, 테스트는 `create-drop`이며 운영 전 migration 도구가 필요합니다.

## 3. 메시지 처리 흐름과 트랜잭션

```text
소유권 확인·AI 문맥 조회
→ USER 메시지 별도 트랜잭션 저장
→ 검수 지식 검색과 AI 문맥 구성
→ AiTutorClient 호출
→ 구조화 응답 검증 또는 텍스트 fallback
→ ASSISTANT 메시지·분석 JSON·개념 상태를 하나의 트랜잭션으로 저장
→ 사용자·AI 메시지와 분석 응답 반환
```

외부 네트워크 호출 중 DB 트랜잭션을 오래 유지하지 않기 위해 사용자 질문 저장과 AI 결과 저장을 분리합니다. AI 연결 실패 시 사용자 질문은 남고 AI 답변은 없습니다. AI 응답을 받은 뒤에는 ASSISTANT 메시지와 학습 상태가 함께 성공하거나 함께 롤백됩니다. 사용자는 같은 질문을 다시 보낼 수 있으며, 자동 재시도나 요청 중복 제거는 현재 MVP 범위에 포함하지 않습니다.

평가도 같은 원칙을 사용합니다. 평가 대상과 중복 여부를 읽은 뒤 트랜잭션 없이 AI를 호출하고, 평가 응답을 받은 뒤 `Assessment` 저장과 `UserConceptStatus` 갱신만 하나의 쓰기 트랜잭션으로 처리합니다. DB 유일성 제약과 서비스 검증으로 같은 사용자·AI 메시지·개념의 중복 제출을 막습니다.

## 4. AI 설정 경계

- `AI_CLIENT_MODE=fake`가 기본이며 API Key 없이 로컬·테스트에서 동작합니다.
- `AI_CLIENT_MODE=ollama`는 로컬 Ollama의 `/api/chat`을 호출하고 API Key를 사용하지 않습니다.
- 실제 호출은 `AI_CLIENT_MODE=openai`와 비어 있지 않은 `OPENAI_API_KEY`, `OPENAI_MODEL`이 모두 필요합니다.
- 모델명과 API Key는 코드에 고정하지 않습니다.
- `OPENAI_TIMEOUT_SECONDS`로 전체 요청 제한 시간을 설정합니다.
- `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `OLLAMA_TIMEOUT_SECONDS`, `OLLAMA_MAX_OUTPUT_TOKENS`로 로컬 실행기를 설정합니다.
- 멘토 답변은 숨겨진 장시간 추론을 피하기 위해 `think=false`, 결정적 구조화 출력을 위해 `temperature=0`을 사용합니다.
- OpenAI 응답 본문에서 `output_text`를 찾아 구조화 DTO로 변환합니다.
- Ollama 응답 본문의 `message.content`를 같은 DTO로 검증하고, tutor 일반 텍스트는 기존 fallback 정책을 적용합니다.
- OpenAI Responses API와 Ollama Chat API의 envelope가 다르므로 전송·파서 구현은 분리합니다.
- 선택한 모드에서 정확히 하나의 `AiTutorClient`만 조건부 Bean으로 활성화됩니다.
- `AI_RAG_ENABLED`, `AI_RAG_MAX_DOCUMENTS`로 검수 문서 검색 사용 여부와 최대 문서 수를 설정합니다.

## 5. RAG 경계와 선택 근거

현재 카탈로그는 애플리케이션이 소유하고 검수한 공개 문서 13건입니다. 이 규모에서는 임베딩 모델, pgvector, 문서 쓰기 API를 추가하면 운영·권한·재색인 복잡도만 늘어나므로 도입하지 않았습니다. 관련도 임계값이 있는 결정적 검색으로 먼저 품질 효과를 확인합니다.

사용자·프로젝트 문서는 현재 입력받지 않으므로 사용자 간 문서 노출 경로가 없습니다. 향후 비공개 문서를 추가할 때는 저장·검색 전에 사용자와 프로젝트 소유권 모델을 먼저 설계해야 하며, 그때 PostgreSQL full-text 또는 pgvector를 재평가합니다.

Task 13 실측에서 JPA/Hibernate 정의와 허용 개념 선택은 개선됐지만 7B 응답 시간이 116.1초까지 증가하고 모델이 긴 문서 ID를 정확히 복사하지 못했습니다. 따라서 RAG 기능 구현과 근거 품질 개선은 확인했지만 운영 기본 모델은 계속 승인하지 않습니다.

## 6. 피드백과 LoRA 안전 경계

피드백 저장은 모델 학습 실행과 분리합니다. `TrainingEligibilityResponse`가 동의 피드백 300건, 수정 답안 200건, 분리 평가셋 준비를 모두 통과하기 전에는 데이터 export와 LoRA 학습을 시작하지 않습니다.

현재 인증·관리자 권한과 충분한 동의 데이터가 없으므로 학습 데이터 export API를 제공하지 않습니다. 적격성 통과 후에도 개인정보 제거, train/validation/test 분리, 기반 모델·어댑터 버전 고정, Task 9 평가셋 회귀를 별도 승인해야 합니다.
