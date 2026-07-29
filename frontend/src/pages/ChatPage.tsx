import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Link } from 'react-router-dom'
import { createChatRoom, deleteChatRoom, getChatRooms, getMessages, sendMessage } from '../api/chat'
import { getUser } from '../api/users'
import { getApiErrorMessage } from '../api/client'
import { submitAiFeedback } from '../api/feedback'
import type { AiFeedbackRating, AiTutorAnalysis, ChatMessage, ChatRoom, KnowledgeSource, User } from '../types/api'

function getVisibleMessageContent(message: ChatMessage): string {
  if (message.role !== 'ASSISTANT') {
    return message.content
  }

  try {
    const parsedContent: unknown = JSON.parse(message.content)
    if (typeof parsedContent === 'object' && parsedContent !== null) {
      const answer = (parsedContent as { answer?: unknown }).answer
      if (typeof answer === 'string' && answer.trim()) {
        return answer
      }
    }
  } catch {
    return message.content
  }

  return message.content
}

export function ChatPage() {
  const navigate = useNavigate()
  const { chatRoomId } = useParams()
  const userId = Number(localStorage.getItem('devmentorUserId'))
  const roomId = Number(chatRoomId)
  const [user, setUser] = useState<User | null>(null)
  const [rooms, setRooms] = useState<ChatRoom[]>([])
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [content, setContent] = useState('')
  const [newTitle, setNewTitle] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const [pendingQuestion, setPendingQuestion] = useState('')
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [latestAnalysis, setLatestAnalysis] = useState<AiTutorAnalysis | null>(null)
  const [latestSources, setLatestSources] = useState<KnowledgeSource[]>([])
  const [latestAssistantMessageId, setLatestAssistantMessageId] = useState<number | null>(null)
  const [feedbackRating, setFeedbackRating] = useState<AiFeedbackRating | null>(null)
  const [correctedAnswer, setCorrectedAnswer] = useState('')
  const [trainingConsent, setTrainingConsent] = useState(false)
  const [feedbackStatus, setFeedbackStatus] = useState('')
  const messageEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!userId || !roomId) {
      navigate('/')
      return
    }
    Promise.all([getUser(userId), getChatRooms(userId), getMessages(roomId, userId)])
      .then(([profile, roomList, messageList]) => {
        setUser(profile)
        setRooms(roomList)
        setMessages(messageList)
      })
      .catch((requestError) => setError(getApiErrorMessage(requestError)))
  }, [navigate, roomId, userId])

  useEffect(() => {
    if (!sending) return

    const startedAt = Date.now()
    const intervalId = window.setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000))
    }, 1_000)

    return () => window.clearInterval(intervalId)
  }, [sending])

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending, latestAnalysis])

  async function handleSend(event: FormEvent) {
    event.preventDefault()
    if (sending || !content.trim()) return
    const question = content.trim()
    setElapsedSeconds(0)
    setPendingQuestion(question)
    setContent('')
    setSending(true)
    setError('')
    setLatestAnalysis(null)
    setLatestSources([])
    setLatestAssistantMessageId(null)
    setFeedbackRating(null)
    setCorrectedAnswer('')
    setTrainingConsent(false)
    setFeedbackStatus('')
    try {
      const exchange = await sendMessage(roomId, userId, question)
      setMessages((current) => [
        ...current,
        exchange.userMessage,
        exchange.assistantMessage,
      ])
      setLatestAnalysis(exchange.analysis)
      setLatestSources(exchange.sources)
      setLatestAssistantMessageId(exchange.assistantMessage.id)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
      setContent((current) => current || question)
    } finally {
      setPendingQuestion('')
      setSending(false)
    }
  }

  function handleMessageKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) {
      return
    }

    event.preventDefault()
    if (!sending && content.trim()) {
      event.currentTarget.form?.requestSubmit()
    }
  }

  async function handleFeedbackSubmit(event: FormEvent) {
    event.preventDefault()
    if (!latestAssistantMessageId || !feedbackRating) return
    setError('')
    try {
      await submitAiFeedback(
        userId,
        latestAssistantMessageId,
        feedbackRating,
        correctedAnswer,
        trainingConsent,
      )
      setFeedbackStatus('피드백을 저장했습니다.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  async function handleCreateRoom(event: FormEvent) {
    event.preventDefault()
    if (!newTitle.trim()) return
    try {
      const room = await createChatRoom(userId, newTitle)
      localStorage.setItem('devmentorLastRoomId', String(room.id))
      setNewTitle('')
      navigate(`/chat/${room.id}`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  async function handleDeleteRoom() {
    await deleteChatRoom(roomId, userId)
    const remaining = rooms.filter((room) => room.id !== roomId)
    if (remaining.length > 0) {
      navigate(`/chat/${remaining[0].id}`)
    } else {
      const room = await createChatRoom(userId, '새 학습')
      navigate(`/chat/${room.id}`)
    }
  }

  return (
    <main className="chat-layout">
      <aside className="chat-sidebar">
        <a className="brand inverse" href="/">DevMentor</a>
        <form className="new-room-form" onSubmit={handleCreateRoom}>
          <input aria-label="새 대화방 제목" placeholder="새 학습 주제" value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)} />
          <button type="submit">추가</button>
        </form>
        <nav aria-label="대화방 목록">
          {rooms.map((room) => (
            <button className={room.id === roomId ? 'active' : ''} key={room.id}
              onClick={() => navigate(`/chat/${room.id}`)}>{room.title}</button>
          ))}
        </nav>
      </aside>

      <section className="conversation">
        <header>
          <div><p>LEARNING CONVERSATION</p><h1>{rooms.find((room) => room.id === roomId)?.title ?? '학습 대화'}</h1></div>
          <button className="delete-room" type="button" onClick={handleDeleteRoom}>대화방 삭제</button>
        </header>
        <div className="message-list" aria-live="polite">
          {messages.length === 0 && !pendingQuestion && <div className="empty-message"><strong>첫 질문을 남겨보세요.</strong><span>학습 목표와 최근 대화를 바탕으로 AI 멘토가 맞춤 답변을 제공합니다.</span></div>}
          {messages.map((message) => (
            <article className={`message ${message.role.toLowerCase()}`} key={message.id}>
              <span>{message.role === 'USER' ? '나' : 'DevMentor'}</span>
              <p>{getVisibleMessageContent(message)}</p>
            </article>
          ))}
          {pendingQuestion && (
            <article className="message user pending-message">
              <span>나</span>
              <p>{pendingQuestion}</p>
            </article>
          )}
          {sending && (
            <article className="message assistant message-loading" role="status">
              <span>DevMentor</span>
              <div className="loading-bubble">
                <span className="loading-spinner" aria-hidden="true" />
                <div>
                  <strong>답변을 준비하고 있습니다.</strong>
                  <small>
                    로컬 AI가 질문을 분석 중입니다.
                    {elapsedSeconds > 0 && ` ${elapsedSeconds}초 경과`}
                  </small>
                </div>
              </div>
            </article>
          )}
          {latestAnalysis?.followUpQuestion && (
            <section className="mentor-insight">
              <span>확인 질문</span>
              <p>{latestAnalysis.followUpQuestion}</p>
            </section>
          )}
          {latestAnalysis && latestAnalysis.recommendedConcepts.length > 0 && (
            <section className="mentor-insight">
              <span>추천 개념</span>
              <ul>
                {latestAnalysis.recommendedConcepts.map((concept) => (
                  <li key={`${concept.skillCode}-${concept.conceptCode}`}>
                    <strong>{concept.skillCode} · {concept.conceptCode}</strong>
                    {concept.reason && <p>{concept.reason}</p>}
                  </li>
                ))}
              </ul>
            </section>
          )}
          {latestSources.length > 0 && (
            <section className="mentor-insight">
              <span>답변 근거</span>
              <ul>
                {latestSources.map((source) => (
                  <li key={source.id}>
                    <a href={source.sourceUrl} target="_blank" rel="noreferrer">
                      <strong>{source.title}</strong>
                    </a>
                    <p>{source.id} · {source.version}</p>
                  </li>
                ))}
              </ul>
            </section>
          )}
          {latestAssistantMessageId && (
            <form className="mentor-insight" onSubmit={handleFeedbackSubmit}>
              <span>이 답변은 도움이 되었나요?</span>
              <div>
                <button type="button" onClick={() => setFeedbackRating('HELPFUL')}>
                  도움됨
                </button>
                <button type="button" onClick={() => setFeedbackRating('NOT_HELPFUL')}>
                  수정 필요
                </button>
              </div>
              {feedbackRating === 'NOT_HELPFUL' && (
                <textarea
                  aria-label="수정 답안"
                  placeholder="더 나은 답안을 적어 주세요."
                  maxLength={20000}
                  value={correctedAnswer}
                  onChange={(event) => setCorrectedAnswer(event.target.value)}
                />
              )}
              <label>
                <input
                  type="checkbox"
                  checked={trainingConsent}
                  onChange={(event) => setTrainingConsent(event.target.checked)}
                />
                이 질문·답변·수정 내용을 모델 개선 데이터로 사용하는 데 동의합니다.
              </label>
              <button type="submit" disabled={!feedbackRating}>피드백 저장</button>
              {feedbackStatus && <p>{feedbackStatus}</p>}
            </form>
          )}
          <div ref={messageEndRef} />
        </div>
        {error && <p className="form-error chat-error">{error}</p>}
        <form className="message-form" onSubmit={handleSend}>
          <textarea aria-label="메시지" placeholder="개발 질문을 입력하세요." maxLength={10000}
            value={content} onChange={(e) => setContent(e.target.value)}
            onKeyDown={handleMessageKeyDown} />
          <button type="submit" disabled={sending || !content.trim()}>
            {sending ? '답변 대기 중' : '메시지 저장'}
          </button>
        </form>
      </section>

      <aside className="profile-panel">
        <nav className="profile-navigation" aria-label="학습 메뉴">
          <Link to="/dashboard">대시보드</Link>
          <Link to="/learning">학습 현황</Link>
          <Link to="/review">평가와 복습</Link>
        </nav>
        <p className="section-label">LEARNER</p>
        <h2>{user?.nickname ?? '사용자'}</h2>
        <dl>
          <div><dt>현재 역할</dt><dd>{user?.currentRole}</dd></div>
          <div><dt>학습 목표</dt><dd>{user?.learningGoal}</dd></div>
          <div><dt>관심 기술</dt><dd>{user?.interestedSkillCodes.join(', ') || '미선택'}</dd></div>
        </dl>
      </aside>
    </main>
  )
}
