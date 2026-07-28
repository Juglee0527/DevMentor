import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getDashboard } from '../api/learning'
import { getApiErrorMessage } from '../api/client'
import type { Dashboard } from '../types/api'

export function DashboardPage() {
  const navigate = useNavigate()
  const userId = Number(localStorage.getItem('devmentorUserId'))
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!userId) {
      navigate('/')
      return
    }
    getDashboard(userId)
      .then(setDashboard)
      .catch((requestError) => setError(getApiErrorMessage(requestError)))
  }, [navigate, userId])

  if (error) {
    return <main className="status-page"><p className="form-error">{error}</p></main>
  }
  if (!dashboard) {
    return <main className="status-page"><p>학습 현황을 불러오는 중입니다.</p></main>
  }

  return (
    <main className="status-page">
      <header className="status-header">
        <Link className="brand" to="/">DevMentor</Link>
        <nav aria-label="학습 메뉴">
          <Link className="active" to="/dashboard">대시보드</Link>
          <Link to="/learning">학습 현황</Link>
          <Link to="/review">평가와 복습</Link>
          {localStorage.getItem('devmentorLastRoomId') && (
            <Link to={`/chat/${localStorage.getItem('devmentorLastRoomId')}`}>대화</Link>
          )}
        </nav>
      </header>

      <section className="dashboard-hero">
        <div>
          <p className="section-label">LEARNING DASHBOARD</p>
          <h1>{dashboard.user.nickname}님의 개발 학습</h1>
          <p>{dashboard.user.currentRole} · 경력 {dashboard.user.careerYears}년</p>
          <strong>{dashboard.user.learningGoal}</strong>
        </div>
        <div className="overall-score" aria-label={`전체 이해도 ${dashboard.overallUnderstandingScore}점`}>
          <strong>{dashboard.overallUnderstandingScore}</strong><span>/ 100</span>
        </div>
      </section>

      <section className="summary-grid" aria-label="학습 요약">
        <article><span>학습한 개념</span><strong>{dashboard.startedConceptCount}</strong><small>/ {dashboard.totalConceptCount}</small></article>
        <article><span>복습 필요</span><strong>{dashboard.reviewTargetCount}</strong><small>개념</small></article>
        <article><span>최근 대화</span><strong>{dashboard.recentChats.length}</strong><small>개</small></article>
      </section>

      <section className="dashboard-section">
        <div className="section-heading"><h2>기술별 이해도</h2><Link to="/learning">전체 보기</Link></div>
        <div className="skill-progress-list">
          {dashboard.skillProgress.map((skill) => (
            <article key={skill.skillCode}>
              <div><strong>{skill.skillName}</strong><span>{skill.startedConceptCount}/{skill.totalConceptCount} 개념</span></div>
              <div className="progress-track"><span style={{ width: `${skill.averageScore}%` }} /></div>
              <b>{skill.averageScore}</b>
            </article>
          ))}
        </div>
      </section>

      <div className="dashboard-columns">
        <section className="dashboard-section">
          <h2>보완할 개념</h2>
          {dashboard.weakConcepts.length === 0 ? (
            <p className="empty-state">대화를 시작하면 부족한 개념이 여기에 표시됩니다.</p>
          ) : dashboard.weakConcepts.map((concept) => (
            <article className="weak-concept" key={`${concept.skillCode}-${concept.conceptCode}`}>
              <span>{concept.skillName}</span>
              <strong>{concept.conceptName}</strong>
              <p>{concept.reason}</p>
            </article>
          ))}
        </section>
        <section className="dashboard-section">
          <h2>최근 대화</h2>
          {dashboard.recentChats.length === 0 ? (
            <p className="empty-state">아직 생성한 대화가 없습니다.</p>
          ) : dashboard.recentChats.map((chat) => (
            <Link className="recent-chat" to={`/chat/${chat.id}`} key={chat.id}>
              <strong>{chat.title}</strong><span>대화 계속하기 →</span>
            </Link>
          ))}
        </section>
      </div>
    </main>
  )
}
