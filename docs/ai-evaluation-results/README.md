# AI 평가 원본 결과

이 디렉터리는 `scripts/evaluate-ollama.ps1`이 생성한 모델별 원본 결과를 보존합니다.

현재 파일은 Task 12의 smoke 평가입니다. 대표 케이스에서 필수 게이트 위반이 확인되어 전체 36건 실행 전에 중단했습니다.

| 파일 | 범위 | 용도 |
| --- | --- | --- |
| `smoke-qwen2.5-1.5b-instruct.json` | assessment 2건 | 속도 후보 논리 검증 |
| `smoke-qwen3.5-4b.json` | assessment 2건 | 4B 후보 품질·속도 검증 |
| `smoke-qwen2.5-7b-instruct.json` | tutor 2건, assessment 2건 | RAG 개발 기준 모델 선정 |
| `rag-smoke-qwen2.5-7b-instruct.json` | tutor 1건 | JPA/Hibernate RAG 전후 비교 |

모든 결과는 실제 사용자 데이터가 아닌 고정 평가셋 `2026-07-29.1`로 생성했습니다. 결과 파일에는 API Key, 환경변수 값, 실제 대화가 포함되지 않습니다.

재실행 예시:

```powershell
.\scripts\evaluate-ollama.ps1 `
  -Model "qwen2.5:7b-instruct" `
  -TimeoutSeconds 120 `
  -MaxOutputTokens 512
```

RAG 재실행:

```powershell
.\scripts\evaluate-ollama.ps1 `
  -Model "qwen2.5:7b-instruct" `
  -CaseId "TUTOR-012" `
  -Rag `
  -TimeoutSeconds 120 `
  -MaxOutputTokens 512
```
