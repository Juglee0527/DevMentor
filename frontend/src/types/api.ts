export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface User {
  id: number
  nickname: string
  careerYears: number
  currentRole: string
  learningGoal: string
  interestedSkillCodes: string[]
}

export interface ChatRoom {
  id: number
  userId: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
}

export interface ConceptSignal {
  skillCode: string
  conceptCode: string
  confidence?: number
  reason?: string
}

export interface AiTutorAnalysis {
  answer: string
  detectedConcepts: ConceptSignal[]
  knowledgeGaps: ConceptSignal[]
  followUpQuestion: string | null
  recommendedConcepts: ConceptSignal[]
}

export interface ChatExchange {
  userMessage: ChatMessage
  assistantMessage: ChatMessage
  analysis: AiTutorAnalysis
  structured: boolean
}
