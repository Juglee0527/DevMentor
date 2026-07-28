import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Link } from 'react-router-dom'
import { createChatRoom, deleteChatRoom, getChatRooms, getMessages, sendMessage } from '../api/chat'
import { getUser } from '../api/users'
import { getApiErrorMessage } from '../api/client'
import type { AiTutorAnalysis, ChatMessage, ChatRoom, User } from '../types/api'

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
  const [latestAnalysis, setLatestAnalysis] = useState<AiTutorAnalysis | null>(null)

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

  async function handleSend(event: FormEvent) {
    event.preventDefault()
    if (!content.trim()) return
    setSending(true)
    setError('')
    try {
      const exchange = await sendMessage(roomId, userId, content)
      setMessages((current) => [
        ...current,
        exchange.userMessage,
        exchange.assistantMessage,
      ])
      setLatestAnalysis(exchange.analysis)
      setContent('')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSending(false)
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
          {messages.length === 0 && <div className="empty-message"><strong>첫 질문을 남겨보세요.</strong><span>학습 목표와 최근 대화를 바탕으로 AI 멘토가 맞춤 답변을 제공합니다.</span></div>}
          {messages.map((message) => (
            <article className={`message ${message.role.toLowerCase()}`} key={message.id}>
              <span>{message.role === 'USER' ? '나' : 'DevMentor'}</span>
              <p>{message.content}</p>
            </article>
          ))}
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
        </div>
        {error && <p className="form-error chat-error">{error}</p>}
        <form className="message-form" onSubmit={handleSend}>
          <textarea aria-label="메시지" placeholder="개발 질문을 입력하세요." maxLength={10000}
            value={content} onChange={(e) => setContent(e.target.value)} />
          <button type="submit" disabled={sending || !content.trim()}>{sending ? '저장 중' : '메시지 저장'}</button>
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
