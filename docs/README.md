# DevMentor 문서

이 디렉터리는 DevMentor의 요구사항, 개발 순서, 진행 상태를 관리하는 기준 문서입니다.

## 문서 구성

| 문서 | 목적 | 갱신 시점 |
| --- | --- | --- |
| [product-requirements.md](./product-requirements.md) | 제품 목표, MVP 범위, 완료 조건 정의 | 요구사항이 변경될 때 |
| [development-roadmap.md](./development-roadmap.md) | Codex에 하나씩 지시할 개발 작업 목차 | 작업 범위나 순서가 변경될 때 |
| [open-model-development-plan.md](./open-model-development-plan.md) | 로컬 오픈 모델 연동, 평가, RAG, 선택적 파인튜닝 계획 | AI 실행 구조나 단계별 기준이 변경될 때 |
| [progress.md](./progress.md) | 완료 내역, 검증 결과, 다음 작업 기록 | 각 개발 작업이 끝날 때 |
| `architecture.md` | 시스템 구조와 주요 데이터 흐름 | 1단계에서 생성 |
| `api-spec.md` | API 계약과 요청·응답 예시 | API 구현 시 생성·갱신 |
| `database-design.md` | Entity, 관계, 컬럼, enum 정의 | 2단계에서 생성 |
| [verification.md](./verification.md) | MVP 자동·브라우저·DB 검증 증거와 제한사항 | 통합 검증 시 갱신 |

설계 문서는 실제 구현과 함께 갱신하여 문서와 코드가 어긋나지 않게 관리합니다.

## 문서 운영 원칙

1. 현재 작업은 [development-roadmap.md](./development-roadmap.md)의 작업 ID 하나를 기준으로 진행합니다.
2. 작업 시작 전 범위, 제외 범위, 완료 조건을 확인합니다.
3. 구현 중 결정된 내용은 관련 설계 문서에 반영합니다.
4. 작업 종료 후 [progress.md](./progress.md)에 변경 파일, 검증 결과, 남은 위험을 기록합니다.
5. 구현되지 않은 기능은 완료로 표시하지 않습니다.
6. MVP 제외 기능은 현재 구조에 미리 구현하지 않습니다.
7. 로컬 오픈 모델 작업은 [open-model-development-plan.md](./open-model-development-plan.md)의 착수 조건과 중단 기준을 따릅니다.

## Codex 작업 요청 예시

```text
docs/development-roadmap.md의 1.1 프로젝트 구조 생성을 진행해 주세요.
해당 작업의 완료 조건까지만 구현하고, 완료 후 docs/progress.md를 갱신해 주세요.
```
