import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getLearningStatus } from '../api/learning'
import { getApiErrorMessage } from '../api/client'
import type { LearningState, LearningStatusOverview } from '../types/api'

const STATUS_LABELS: Record<LearningState, string> = {
  NOT_STARTED: '미학습',
  LEARNING: '학습 중',
  UNDERSTOOD: '이해함',
  NEEDS_REVIEW: '복습 필요',
}

export function LearningPage() {
  const navigate = useNavigate()
  const userId = Number(localStorage.getItem('devmentorUserId'))
  const [overview, setOverview] = useState<LearningStatusOverview | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!userId) {
      navigate('/')
      return
    }
    getLearningStatus(userId)
      .then(setOverview)
      .catch((requestError) => setError(getApiErrorMessage(requestError)))
  }, [navigate, userId])

  return (
    <main className="status-page">
      <header className="status-header">
        <Link className="brand" to="/">DevMentor</Link>
        <nav aria-label="학습 메뉴">
          <Link to="/dashboard">대시보드</Link>
          <Link className="active" to="/learning">학습 현황</Link>
          {localStorage.getItem('devmentorLastRoomId') && (
            <Link to={`/chat/${localStorage.getItem('devmentorLastRoomId')}`}>대화</Link>
          )}
        </nav>
      </header>
      <section className="learning-title">
        <p className="section-label">LEARNING PROGRESS</p>
        <h1>기술과 개념별 학습 현황</h1>
        <p>대화와 평가를 통해 갱신된 이해도와 다음 복습 시점을 확인하세요.</p>
      </section>
      {error && <p className="form-error">{error}</p>}
      {!error && !overview && <p>학습 현황을 불러오는 중입니다.</p>}
      <section className="learning-skill-list">
        {overview?.skills.map((skill) => (
          <article className="learning-skill" key={skill.skillCode}>
            <header>
              <div><h2>{skill.skillName}</h2><span>{skill.concepts.length}개 개념</span></div>
              <strong>{skill.averageScore}점</strong>
            </header>
            <div className="progress-track"><span style={{ width: `${skill.averageScore}%` }} /></div>
            <div className="concept-table">
              {skill.concepts.map((concept) => (
                <div key={concept.conceptCode}>
                  <div><strong>{concept.conceptName}</strong><small>{concept.difficulty}</small></div>
                  <span className={`state-badge ${concept.learningStatus.toLowerCase()}`}>
                    {STATUS_LABELS[concept.learningStatus]}
                  </span>
                  <b>{concept.understandingScore}</b>
                </div>
              ))}
            </div>
          </article>
        ))}
      </section>
    </main>
  )
}
