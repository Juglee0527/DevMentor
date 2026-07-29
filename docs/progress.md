# DevMentor 개발 진행 기록

## 1. 현재 상태

- 현재 단계: `9~14단계 로컬 오픈 모델 확장 결과`
- 상태: `조건부 완료 - 9~13 완료, 14단계 피드백 기반 완료·LoRA 학습 보류`
- 다음 작업: `동의·검수 데이터 누적 후 적격성 재확인과 운영 품질 재평가`
- 마지막 갱신일: `2026-07-29`

## 2. 단계별 진행률

| 단계 | 상태 | 완료 작업 |
| --- | --- | --- |
| 0. 요구사항과 작업 체계 | 완료 | 제품 요구사항, 개발 로드맵, 진행 기록 |
| 1. 프로젝트 초기화 | 완료 | 구조, PostgreSQL, Spring Boot, React, 공통 API |
| 2. 핵심 데이터 모델 | 완료 | Entity, Repository, 초기 기술·개념, Testcontainers |
| 3. 사용자와 대화 기본 기능 | 완료 | 사용자·대화방·메시지 API와 실제 화면 |
| 4. AI 멘토 연동 | 완료 | Client 경계, OpenAI 구조화 출력, fallback, 대화 화면 |
| 5. 학습 상태 분석 | 완료 | 상태 전이 규칙, AI 분석 반영, 추천 개념 API |
| 6. 대시보드와 학습 현황 | 완료 | 집계 API, 대시보드, 기술·개념 학습 현황 |
| 7. 평가와 복습 | 완료 | 평가 AI 계약, 평가 저장·상태 반영, 복습 화면 |
| 8. 통합 검증과 문서 | 완료 | 전체 테스트, E2E·DB 증거, 실행·설계 문서 정합성 |
| 9. AI 평가 기준 수립 | 완료 | 장비 확인, 36개 고정 평가셋, 품질·성능 게이트 |
| 10. 로컬 오픈 모델 연결 | 완료 | Ollama Client, 구조화 대화·평가, 실패 처리 |
| 11. AI 계약 공통화 | 완료 | 공통 프롬프트, JSON Schema, 내용 파서, 허용 개념 카탈로그 |
| 12. 모델 비교와 확정 | 완료 | 후보 smoke 평가, RAG 개발 기준 모델 선택, 운영 게이트 판정 |
| 13. RAG | 완료 | 검수 문서 검색·주입·출처 표시와 전후 평가 |
| 14. 피드백·LoRA | 보류 | 피드백·동의·철회·적격성 구현, 실제 LoRA 학습 조건 미달 |

## 3. 변경 기록

### 2026-07-29 - 9~14단계 최종 회귀 검증

- Task 9~14 커밋과 `origin/main` 동기화 확인
- 메인 Java 97개, 테스트 Java 14개 소스 컴파일 성공
- 프론트엔드 production build와 lint 성공
- 평가셋·프롬프트·지식 카탈로그·평가 결과 JSON 파싱 성공
- 최근 6개 커밋 범위 `git diff --check` 성공
- 제품 기본 모드는 `fake`, RAG 개발 기준 모델은 `qwen2.5:7b-instruct`로 구분 유지
- Task 14 실제 LoRA 학습은 동의 데이터·운영 품질·관리자 export 조건 미달로 보류

### 2026-07-29 - 14단계 피드백 수집과 LoRA 적격성 게이트

작업 범위:

- AI 답변의 도움됨·수정 필요 피드백과 사용자 수정 답안 저장
- 모델 개선 활용 동의와 삭제·철회 시각 추적
- 질문·생성 답변·모델 tag/version·프롬프트·응답 시간·실패 유형·RAG 문서 ID snapshot
- 피드백 저장·철회·학습 적격성 API와 대화 화면 피드백 UI
- LoRA 학습 전 최소 데이터 게이트와 확인 스크립트

주요 결정:

- 학습 동의 기본값은 false이며 `NOT_HELPFUL` 학습 데이터에는 수정 답안을 필수로 함
- 철회 데이터는 물리 삭제 전 `deletedAt`과 동의 해제로 추적하고 학습 집계에서 제외
- 클라이언트가 모델·출처 메타데이터를 제출하지 않고 생성 시 서버 snapshot을 사용
- 동의 300건, 수정 답안 200건, 분리 평가셋을 모두 충족하기 전 학습 금지
- 인증·관리자 export 경계와 동의 데이터가 없으므로 export·LoRA 학습·어댑터 배포는 구현하거나 실행하지 않음

검증:

- 메인 Java 소스 133개 class 생성 성공
- 피드백 snapshot·철회와 적격성 게이트 테스트 소스 class 생성 성공
- 프론트엔드 build와 lint 성공
- 데이터 없음 상태에서 적격성 응답이 학습을 차단하도록 테스트 작성
- Gradle 전체 테스트는 기존 오프라인 plugin artifact 제약으로 실행하지 않음

남은 위험 및 다음 작업:

- 실제 DB 기동 후 피드백 API·DDL 생성 E2E는 로컬/CI에서 재검증 필요
- 인증과 관리자 권한 없이 학습 데이터 export를 추가할 수 없음
- 실제 동의·검수 데이터가 누적되기 전 LoRA 학습은 계속 보류
- 7B 기본 모델과 RAG도 운영 품질·속도 게이트 미통과 상태

### 2026-07-29 - 13단계 검수 문서 기반 RAG

작업 범위:

- 앱 소유 `PUBLIC` 기술 검수 문서 13건 추가
- 활성 문서만 대상으로 한 결정적 관련도 검색과 최대 3건 제한
- 검색 문서를 AI 문맥에 추가하고 문서 안의 명령을 데이터로 격리
- 실제 제공한 문서 ID·제목·버전·원문 URL을 대화 API와 화면에 표시
- RAG 전후 고정 사례 평가와 원본 결과 저장

주요 결정:

- 현재 문서 규모에서는 임베딩 모델과 pgvector를 추가하지 않고 단순 검색으로 효과부터 검증
- 검색 결과 없음은 기존 일반 모델 흐름으로 처리
- 같은 개념의 활성 문서가 둘 이상이면 상충 문서로 보고 시작 단계에서 거부
- 사용자·프로젝트 문서 입력은 권한 모델이 없으므로 현재 범위에서 제외
- 모델이 생성한 인용 문자열을 신뢰하지 않고 서버가 실제 제공한 출처를 별도 반환

검증:

- JPA/Hibernate 사례에서 허용 개념이 `JPA_HIBERNATE_RELATION`으로 교정됨
- 필수 핵심 용어 `명세`, `구현` 포함 확인
- 메인 Java 소스 122개 class 생성 성공
- 검색 정상·무관 질문·비활성 문서·RAG 비활성·상충 문서 테스트 소스 컴파일 성공
- 프론트엔드 build와 lint 성공
- 지식 카탈로그·프롬프트·평가 결과 JSON 파싱 성공

남은 위험 및 다음 작업:

- 7B RAG 응답이 116.1초로 성능 기준 미달
- 모델이 긴 출처 ID를 정확히 복사하지 못해 자동 인용 검사가 실패
- 운영 기본 모델과 RAG 운영 적용은 계속 보류
- Task 14에서는 데이터 수집 동의, 삭제 추적, 학습 적격성부터 구현하고 데이터 없이 LoRA 학습을 실행하지 않음

### 2026-07-29 - 12단계 모델 비교와 기준 모델 확정

작업 범위:

- 고정 평가셋을 사용하는 Ollama 평가 runner 구현
- PowerShell 5.1 한글 프롬프트 로딩과 HTTP UTF-8 전송 명시
- tutor·assessment JSON Schema, 허용 개념 코드, 한국어, 평가 논리 자동 검사
- Qwen2.5 1.5B/7B, Qwen3.5 4B, Granite 3.2 2B 대표 케이스 비교
- 모델별 원본 결과와 라이선스·실행 장치·응답 시간 기록

주요 결정:

- `qwen2.5:7b-instruct`를 다음 RAG 개발의 재현 가능한 기준 모델로 고정
- 대표 케이스에서 필수 게이트가 실패한 후보는 전체 36건 평가 전에 중단
- 기준 모델도 기술 정확성 게이트를 통과하지 못했으므로 운영 기본 모델로 승인하지 않음
- 제품, 자동 테스트, CI의 기본 `AI_CLIENT_MODE`는 `fake` 유지

검증:

- Qwen2.5 1.5B assessment 2/2 요청·Schema·한국어 성공, 자동 통과 0/2
- Qwen3.5 4B assessment 2/2 요청·Schema·한국어 성공, 자동 통과 0/2
- Qwen2.5 7B tutor/assessment 4/4 요청·Schema·한국어 성공, 자동 통과 0/4
- Granite 3.2 2B tutor smoke는 180초 내 2건을 완료하지 못해 성능 기준 중단
- Qwen2.5 7B Ollama 실행 약 5.1GB, CPU 47% / GPU 53% 확인
- 평가 원본 JSON 저장과 `git diff --check` 확인

남은 위험 및 다음 작업:

- Git branch 정의 오류와 무관 개념 추천처럼 근거 지식 부족이 반복됨
- 현재 장비에서 7B tutor 응답이 60초에 근접해 비스트리밍 UX가 느림
- Task 13에서 검수 문서 검색과 출처를 연결하고 동일 사례를 재평가
- 운영 기본 모델 승인은 RAG 이후 별도 게이트 통과가 필요

### 2026-07-29 - 11단계 AI 계약 공통화

작업 범위:

- tutor·assessment 시스템 지시문을 `AiPromptPolicy`로 공통화
- tutor·assessment JSON Schema 생성을 `AiResponseSchemaFactory`로 공통화
- DTO 검증과 평가 논리 검증을 `AiStructuredContentParser`로 공통화
- OpenAI/Ollama parser에는 각 API envelope의 content 추출 책임만 유지
- `AiTutorRequest`에 DB의 허용 기술·개념 카탈로그 추가
- 프롬프트에서 허용된 코드 조합만 사용하고 사용자 입력을 데이터로 취급하도록 명시
- score·correct·reviewRequired 모순 응답 거부

주요 결정:

- 서로 다른 API envelope를 하나의 조건문 기반 전송 구현으로 합치지 않음
- 프롬프트와 응답 도메인 계약만 공유하고 모델별 전송 차이는 명시적으로 유지
- 19개인 현재 개념 카탈로그는 매 AI 요청에 포함하며, 대규모 카탈로그 최적화는 현재 범위에서 제외
- 알 수 없는 코드에 대한 기존 학습 분석 방어 로직도 유지

검증:

- 메인 Java 소스 82개 `javac` 컴파일 및 class 생성 성공
- 공통 Schema 필수 필드, 허용 개념 프롬프트, 평가 논리 모순 테스트 작성
- OpenAI/Ollama 기존 Client 테스트를 공통 계약 생성자에 맞게 갱신
- Gradle은 plugin artifact의 오프라인 해석 제약으로 실행하지 못함
- 별도 test `javac`는 Windows sandbox JAR ACL 문제로 완료하지 못해 테스트 소스를 정적 검토
- Client별 시스템 지시문·Schema 생성 중복 제거 검색 확인
- `git diff --check` 성공

남은 위험 및 다음 작업:

- 실제 4B 모델은 한국어·평가 정확성·응답시간 기준에 미달
- Task 12 평가 runner와 결과 저장을 구현하고 작은 모델 후보를 비교
- 실제 로컬/CI 환경에서 Gradle 전체 테스트 재실행 필요

### 2026-07-29 - 10단계 Ollama 로컬 모델 연결

작업 범위:

- Windows Ollama `0.32.5` 설치와 로컬 `/api/chat` 연결
- `OllamaAiTutorClient`, 전용 JDK transport, 응답 parser 추가
- tutor와 assessment JSON Schema 요청·응답 변환
- `fake`, `ollama`, `openai` 조건부 Client 분리
- base URL, 모델, timeout, 최대 출력 토큰 환경변수화
- thinking 비활성화와 결정적 출력을 위한 temperature 0 적용
- 서버 실패, 빈 응답, 잘못된 JSON, DTO 검증 실패 처리
- 로컬 실행 절차와 현재 아키텍처 문서화

주요 결정:

- OpenAI `/responses` envelope와 Ollama `/api/chat` envelope를 억지로 통합하지 않고 전송·파서를 분리
- tutor의 일반 텍스트 fallback은 유지하고 assessment 파싱 실패는 오류로 처리
- 모델 서버 응답 본문과 내부 주소를 사용자 오류 메시지에 노출하지 않음
- 실제 모델 품질과 기본 모델 확정은 연결 완료와 구분해 Task 12에서 판정

검증:

- `qwen3.5:4b` tutor: `think=false`, 출력 제한 적용 시 유효 JSON Schema와 허용 코드 반환
- `qwen3.5:4b` assessment: 유효 JSON Schema 반환
- Ollama 실행 장치: CPU/GPU 혼합 추론 확인
- transport stub 기반 tutor·assessment·fallback·외부 실패·필수 설정 테스트 작성
- Gradle 실행은 Spring Boot plugin artifact가 오프라인 캐시에 없어 환경 제약으로 중단
- 생성자 호출, import, 조건부 Bean, 설정 키, DTO 제약 정적 검토
- `git diff --check` 성공

남은 위험 및 다음 작업:

- qwen3.5 4B의 한국어 준수와 평가 정확도가 품질 기준에 미달
- OpenAI/Ollama에 시스템 지시문과 JSON Schema 중복 존재
- Task 11에서 프롬프트·스키마를 공통화하고 허용 개념 목록을 명시적으로 제공
- Task 12에서 더 작은 모델을 포함한 실제 평가로 기본 모델 확정

### 2026-07-29 - 9단계 AI 평가 기준 수립

작업 범위:

- 개발 장비 CPU, RAM, GPU와 Ollama 설치 여부 확인
- DevMentor 기술 9개·개념 19개를 포함한 고정 평가셋 작성
- tutor 24개, assessment 12개로 정상·오답·부분 답변·보안 케이스 구성
- 구조화 출력, 개념 코드, 평가 논리, 응답 시간의 통과·탈락 기준 정의
- 현재 장비에 맞는 초기 모델 후보와 미확정 사항 기록

주요 결정:

- 약 4GB Intel Arc A350M과 16GB RAM을 고려해 `qwen3:4b-instruct` Q4_K_M을 첫 연결 후보로 사용
- 8B급 모델은 품질 비교 후보이며 GPU 적재 가능성을 가정하지 않음
- 모델 선택은 일반 벤치마크가 아니라 고정 평가셋과 블라인드 수동 검토로 결정
- 실제 사용자 대화나 소스코드는 평가셋에 포함하지 않음

검증:

- `ai-evaluation-dataset.json` JSON 파싱 성공
- 평가 케이스 36개와 고유 ID 확인
- 허용 기술·개념 코드 19개가 초기 데이터와 일치
- 로컬 Markdown 링크 검사 성공
- `git diff --check` 성공

남은 위험 및 다음 작업:

- Ollama가 설치되어 있지 않아 실제 모델 실행 결과는 아직 없음
- Intel Arc GPU 사용은 Ollama Vulkan 실험 기능을 활성화한 뒤 실제 로그로 확인 필요
- Task 10에서 Ollama 설치, 구조화 대화·평가 연동과 실패 처리를 구현

### 2026-07-28 - 8단계 통합 검증과 문서 완성

작업 범위:

- 백엔드 전체 테스트와 PostgreSQL Testcontainers 재실행
- 프론트엔드 TypeScript 빌드·lint 재실행
- 사용자 생성부터 오답 평가·복습 상태까지 브라우저 전체 흐름 검증
- 브라우저 결과와 PostgreSQL 사용자·메시지·상태·평가 행 직접 대조
- MVP 완료 조건과 미확정 결정을 실제 구현 상태로 갱신
- `verification.md`에 명령, 결과, 제한사항 기록

주요 결정:

- Fake AI 기반 검증과 실제 OpenAI live 검증을 구분해 기록
- 실제 모바일 뷰포트 미적용을 숨기지 않고 CSS·빌드 검증과 별도 실제 기기 확인 대상으로 구분
- MVP 제외 기능은 완료 조건에 추가하지 않음

검증:

- `docker compose config --quiet`: 성공
- `docker compose ps`: PostgreSQL 17 `healthy`
- `backend\gradlew.bat test --rerun-tasks`: 성공, 22개 테스트 통과
- `frontend\npm run build`, `npm run lint`: 성공
- 브라우저 전체 흐름과 console warning·error 없음 확인
- DB: 대화방 1, USER/ASSISTANT 각 1, 상태 1, 평가 1 저장 확인
- 오답 평가 40점과 `NEEDS_REVIEW`, `reviewRequired=true` 화면·DB 일치
- 최종 E2E 임시 사용자와 연관 데이터 삭제 확인

남은 위험 및 다음 작업:

- 실제 OpenAI live 품질·비용·rate limit
- 운영 migration과 인증·인가
- 실제 모바일 기기 렌더링

### 2026-07-28 - 7단계 평가와 복습

작업 범위:

- `AiTutorClient.assess` 평가 경계와 Fake/OpenAI 구조화 평가 구현
- 미제출 확인 질문 기반 복습 대상 API
- 답변 제출, 평가 저장, 평가 이력 API
- 정답·오답의 학습 상태 반영과 다음 복습일 갱신
- 평가·피드백·모범 답안·최근 이력 화면
- 동일 질문 중복 제출 방지

주요 결정:

- 평가 정답 기준은 70점이며 AI의 점수와 `correct` 불일치는 외부 응답 오류로 처리
- 오답인데 `reviewRequired=false`인 응답도 외부 응답 오류로 처리
- 실제 평가 저장과 상태 갱신은 하나의 트랜잭션, AI 호출 중에는 DB 트랜잭션을 열지 않음
- 중복 기준은 사용자·원본 AI 메시지·개념 조합이며 서비스 검증과 DB unique 제약을 함께 사용
- 구조화 평가 파싱 실패에는 일반 텍스트 fallback을 사용하지 않음

검증:

- `backend\gradlew.bat test`: 성공, 22개 테스트 통과
- 평가 0·100점 경계 성공, 101점 거부 성공
- 오답 40점 → 0점 `NEEDS_REVIEW`, 새 질문 정답 90점 → 30점 `LEARNING` 반영 성공
- 평가 저장·조회, 중복 제출 409, 빈 답변 400 통합 테스트 성공
- `frontend\npm run build`, `npm run lint`: 성공
- 브라우저에서 AI 질문 → 복습 대상 → 정답 제출 → 90점·피드백·모범 답안 표시 성공
- 새로고침 후 평가 이력 유지와 학습 현황 30점 반영 확인
- 브라우저 console warning·error 없음

남은 위험 및 다음 작업:

- 실제 모델 평가 품질과 비용·rate limit은 배포 환경에서 별도 검증 필요
- 같은 개념의 제출 완료 질문은 재사용하지 않으며 새 평가에는 새 AI 대화가 필요
- 8단계에서 전체 MVP 흐름과 실행 문서를 한 번 더 검증

### 2026-07-28 - 6단계 대시보드와 학습 현황

작업 범위:

- 사용자 전체·기술별 이해도와 학습·복습 개수 집계
- 보완 개념과 최근 대화 조회
- 기술 9개·개념 19개의 학습 상태 조회 API
- 대시보드와 학습 현황 React 화면 및 CSS 진행률
- 대화·대시보드·학습 현황 간 탐색
- 모바일 1열 기본 레이아웃

주요 결정:

- 전체 이해도는 학습을 시작한 개념만 평균하고 빈 이력은 0점으로 반환
- 기술 평균은 해당 기술의 전체 개념을 분모로 사용해 미학습 범위를 함께 표현
- 사용자 상태가 없는 개념은 저장하지 않고 조회 DTO에서 `NOT_STARTED`, 0점으로 구성
- 기술·개념과 사용자 상태를 각각 fetch 조회한 뒤 메모리에서 결합해 N+1 방지
- 보완 개념은 `NEEDS_REVIEW` 우선, 낮은 점수 순으로 최대 5개 표시

검증:

- `backend\gradlew.bat test`: 성공, 19개 테스트 통과
- 빈 사용자 9개 기술·19개 개념 `NOT_STARTED` 응답 성공
- 저장 이력의 전체 점수, 기술 점수, 보완 개념, 최근 대화 집계 성공
- 대시보드 조회 SQL 4개 이하 검증
- `frontend\npm run build`, `npm run lint`: 성공
- 브라우저에서 질문 → 전체 이해도 10점 → JPA 학습 중 10점 표시 확인
- 대시보드 1/19 학습 개념, 보완 개념, 최근 대화와 학습 현황 9개 기술 표시 확인
- 브라우저 console warning·error 없음

남은 위험 및 다음 작업:

- 기술 평균이 전체 개념 기준이라는 정책은 사용자 피드백에 따라 조정 가능
- 복습 화면과 실제 답변 평가는 7단계 범위
- 날짜 표시는 학습 현황 API에 포함되지만 현재 목록 UI는 상태와 점수에 집중

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
