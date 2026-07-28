import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { getApiHealth } from '../api/health'
import { createUser, type UserPayload } from '../api/users'
import { createChatRoom } from '../api/chat'
import { getApiErrorMessage } from '../api/client'

const SKILLS = ['JAVA', 'SPRING', 'SPRING_BOOT', 'JPA', 'DATABASE', 'REACT', 'GIT', 'DOCKER']

export function HomePage() {
  const navigate = useNavigate()
  const [apiConnected, setApiConnected] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState<UserPayload>({
    nickname: '',
    careerYears: 0,
    currentRole: '',
    learningGoal: '',
    interestedSkillCodes: [],
  })

  useEffect(() => {
    getApiHealth().then(() => setApiConnected(true)).catch(() => setApiConnected(false))
  }, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const user = await createUser(form)
      const room = await createChatRoom(user.id, `${user.nickname}님의 첫 학습`)
      localStorage.setItem('devmentorUserId', String(user.id))
      localStorage.setItem('devmentorLastRoomId', String(room.id))
      navigate(`/chat/${room.id}`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  function toggleSkill(code: string) {
    setForm((current) => ({
      ...current,
      interestedSkillCodes: current.interestedSkillCodes.includes(code)
        ? current.interestedSkillCodes.filter((skill) => skill !== code)
        : [...current.interestedSkillCodes, code],
    }))
  }

  const savedRoomId = localStorage.getItem('devmentorLastRoomId')

  return (
    <main className="landing">
      <nav className="navigation" aria-label="주요 탐색">
        <a className="brand" href="/">DevMentor</a>
        <span className={`connection-status ${apiConnected ? 'connected' : 'disconnected'}`}>
          <span aria-hidden="true" />
          {apiConnected ? 'API 연결됨' : 'API 연결 필요'}
        </span>
      </nav>

      <section className="hero">
        <p className="eyebrow">PERSONAL AI DEVELOPER MENTOR</p>
        <h1>질문에서 끝나지 않는<br />개발 학습을 시작하세요.</h1>
        <p className="hero-description">
          대화 속 이해도를 분석하고 부족한 개념과 다음 학습 순서를 찾아주는 개인 개발 멘토입니다.
        </p>
        <div className="hero-actions">
          <button type="button" onClick={() => setShowForm((visible) => !visible)}>
            새 학습 시작하기 <span aria-hidden="true">→</span>
          </button>
          {savedRoomId && (
            <button className="secondary-button" type="button" onClick={() => navigate(`/chat/${savedRoomId}`)}>
              이전 학습 계속하기
            </button>
          )}
        </div>
      </section>

      {showForm && (
        <section className="profile-section" aria-labelledby="profile-title">
          <div>
            <p className="section-label">YOUR PROFILE</p>
            <h2 id="profile-title">멘토가 이해할 수 있게<br />나를 알려주세요.</h2>
          </div>
          <form className="profile-form" onSubmit={handleSubmit}>
            <label>닉네임<input required maxLength={50} value={form.nickname}
              onChange={(e) => setForm({ ...form, nickname: e.target.value })} /></label>
            <label>개발 경력(년)<input required min={0} max={60} type="number" value={form.careerYears}
              onChange={(e) => setForm({ ...form, careerYears: Number(e.target.value) })} /></label>
            <label>현재 역할<input required maxLength={100} value={form.currentRole}
              onChange={(e) => setForm({ ...form, currentRole: e.target.value })} /></label>
            <label>학습 목표<input required maxLength={200} value={form.learningGoal}
              onChange={(e) => setForm({ ...form, learningGoal: e.target.value })} /></label>
            <fieldset>
              <legend>관심 기술</legend>
              <div className="skill-options">
                {SKILLS.map((skill) => (
                  <label key={skill}>
                    <input type="checkbox" checked={form.interestedSkillCodes.includes(skill)}
                      onChange={() => toggleSkill(skill)} />{skill.replace('_', ' ')}
                  </label>
                ))}
              </div>
            </fieldset>
            {error && <p className="form-error">{error}</p>}
            <button type="submit" disabled={submitting || !apiConnected}>
              {submitting ? '시작하는 중...' : '프로필 저장하고 시작하기'}
            </button>
          </form>
        </section>
      )}

      <section className="flow" aria-labelledby="learning-flow-title">
        <div><p className="section-label">HOW IT WORKS</p><h2 id="learning-flow-title">나에게 맞춰지는 학습 흐름</h2></div>
        <ol>{['개발 질문', '이해 수준 분석', '맞춤 설명', '다음 학습 추천'].map((label, index) => (
          <li key={label}><strong>0{index + 1}</strong><span>{label}</span></li>
        ))}</ol>
      </section>
    </main>
  )
}
