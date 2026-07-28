import { useEffect, useState } from 'react'
import { getApiHealth } from '../api/health'

type ApiConnectionStatus = 'checking' | 'connected' | 'disconnected'

export function HomePage() {
  const [connectionStatus, setConnectionStatus] =
    useState<ApiConnectionStatus>('checking')

  useEffect(() => {
    let active = true

    getApiHealth()
      .then(() => {
        if (active) {
          setConnectionStatus('connected')
        }
      })
      .catch(() => {
        if (active) {
          setConnectionStatus('disconnected')
        }
      })

    return () => {
      active = false
    }
  }, [])

  const statusLabel = {
    checking: 'API 연결 확인 중',
    connected: 'API 연결됨',
    disconnected: 'API 연결 필요',
  }[connectionStatus]

  return (
    <main className="landing">
      <nav className="navigation" aria-label="주요 탐색">
        <a className="brand" href="/">
          DevMentor
        </a>
        <span className={`connection-status ${connectionStatus}`}>
          <span aria-hidden="true" />
          {statusLabel}
        </span>
      </nav>

      <section className="hero">
        <p className="eyebrow">PERSONAL AI DEVELOPER MENTOR</p>
        <h1>
          질문에서 끝나지 않는
          <br />
          개발 학습을 시작하세요.
        </h1>
        <p className="hero-description">
          DevMentor는 대화 속 이해도를 분석하고 부족한 개념과 다음 학습
          순서를 찾아주는 개인 개발 멘토입니다.
        </p>
        <button type="button" disabled>
          학습 시작하기
          <span aria-hidden="true">→</span>
        </button>
        <p className="phase-note">
          사용자 등록과 실제 학습 기능은 다음 개발 단계에서 연결됩니다.
        </p>
      </section>

      <section className="flow" aria-labelledby="learning-flow-title">
        <div>
          <p className="section-label">HOW IT WORKS</p>
          <h2 id="learning-flow-title">나에게 맞춰지는 학습 흐름</h2>
        </div>
        <ol>
          <li>
            <strong>01</strong>
            <span>개발 질문</span>
          </li>
          <li>
            <strong>02</strong>
            <span>이해 수준 분석</span>
          </li>
          <li>
            <strong>03</strong>
            <span>맞춤 설명</span>
          </li>
          <li>
            <strong>04</strong>
            <span>다음 학습 추천</span>
          </li>
        </ol>
      </section>
    </main>
  )
}

