import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getAssessments, getReviewTargets, submitAssessment } from '../api/assessments'
import { getApiErrorMessage } from '../api/client'
import type { AssessmentResult, ReviewTarget } from '../types/api'

export function ReviewPage() {
  const navigate = useNavigate()
  const userId = Number(localStorage.getItem('devmentorUserId'))
  const [targets, setTargets] = useState<ReviewTarget[]>([])
  const [assessments, setAssessments] = useState<AssessmentResult[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [results, setResults] = useState<Record<number, AssessmentResult>>({})
  const [submittingId, setSubmittingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!userId) {
      navigate('/')
      return
    }
    Promise.all([getReviewTargets(userId), getAssessments(userId)])
      .then(([reviewTargets, history]) => {
        setTargets(reviewTargets)
        setAssessments(history)
      })
      .catch((requestError) => setError(getApiErrorMessage(requestError)))
      .finally(() => setLoading(false))
  }, [navigate, userId])

  async function handleSubmit(event: FormEvent, target: ReviewTarget) {
    event.preventDefault()
    const answer = answers[target.chatMessageId]?.trim()
    if (!answer) return
    setSubmittingId(target.chatMessageId)
    setError('')
    try {
      const result = await submitAssessment(target, userId, answer)
      setResults((current) => ({ ...current, [target.chatMessageId]: result }))
      setAssessments((current) => [result, ...current])
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSubmittingId(null)
    }
  }

  return (
    <main className="status-page">
      <header className="status-header">
        <Link className="brand" to="/">DevMentor</Link>
        <nav aria-label="학습 메뉴">
          <Link to="/dashboard">대시보드</Link>
          <Link to="/learning">학습 현황</Link>
          <Link className="active" to="/review">평가와 복습</Link>
          {localStorage.getItem('devmentorLastRoomId') && (
            <Link to={`/chat/${localStorage.getItem('devmentorLastRoomId')}`}>대화</Link>
          )}
        </nav>
      </header>
      <section className="learning-title">
        <p className="section-label">ASSESSMENT & REVIEW</p>
        <h1>확인 질문으로 이해도를 점검하세요.</h1>
        <p>자신의 말로 답하고 피드백과 모범 답안을 확인할 수 있습니다.</p>
      </section>
      {error && <p className="form-error">{error}</p>}
      {loading && <p>복습 대상을 불러오는 중입니다.</p>}
      {!loading && targets.length === 0 && (
        <p className="empty-state">현재 답변할 확인 질문이 없습니다. AI 멘토와 새 개념을 대화해 보세요.</p>
      )}
      <section className="review-list">
        {targets.map((target) => {
          const result = results[target.chatMessageId]
          return (
            <article className="review-card" key={`${target.chatMessageId}-${target.conceptCode}`}>
              <div className="review-meta">
                <span>{target.skillName}</span>
                <strong>{target.conceptName}</strong>
                <b>현재 {target.understandingScore}점</b>
              </div>
              <h2>{target.question}</h2>
              <form onSubmit={(event) => handleSubmit(event, target)}>
                <textarea
                  aria-label={`${target.conceptName} 답변`}
                  maxLength={10000}
                  placeholder="본인의 말로 답변해 보세요."
                  value={answers[target.chatMessageId] ?? ''}
                  onChange={(event) => setAnswers((current) => ({
                    ...current,
                    [target.chatMessageId]: event.target.value,
                  }))}
                  disabled={Boolean(result)}
                />
                <button
                  type="submit"
                  disabled={Boolean(result) || submittingId === target.chatMessageId
                    || !(answers[target.chatMessageId]?.trim())}
                >
                  {submittingId === target.chatMessageId ? '평가 중...' : '답변 평가받기'}
                </button>
              </form>
              {result && (
                <section className={`assessment-result ${result.correct ? 'correct' : 'incorrect'}`}>
                  <strong>{result.score}점 · {result.correct ? '정답' : '보완 필요'}</strong>
                  <p>{result.feedback}</p>
                  <div><span>모범 답안</span><p>{result.correctAnswer}</p></div>
                </section>
              )}
            </article>
          )
        })}
      </section>
      <section className="assessment-history">
        <h2>최근 평가</h2>
        {assessments.length === 0 ? (
          <p className="empty-state">아직 저장된 평가가 없습니다.</p>
        ) : assessments.slice(0, 10).map((assessment) => (
          <article key={assessment.id}>
            <div><strong>{assessment.conceptName}</strong><span>{assessment.score}점</span></div>
            <p>{assessment.feedback}</p>
          </article>
        ))}
      </section>
    </main>
  )
}
