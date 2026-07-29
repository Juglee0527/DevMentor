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
  sources: KnowledgeSource[]
}

export interface KnowledgeSource {
  id: string
  title: string
  sourceUrl: string
  version: string
}

export type LearningState =
  | 'NOT_STARTED'
  | 'LEARNING'
  | 'UNDERSTOOD'
  | 'NEEDS_REVIEW'

export interface SkillProgress {
  skillCode: string
  skillName: string
  averageScore: number
  startedConceptCount: number
  totalConceptCount: number
}

export interface WeakConcept {
  skillCode: string
  skillName: string
  conceptCode: string
  conceptName: string
  understandingScore: number
  learningStatus: LearningState
  reason: string | null
}

export interface Dashboard {
  user: {
    id: number
    nickname: string
    careerYears: number
    currentRole: string
    learningGoal: string
  }
  overallUnderstandingScore: number
  totalConceptCount: number
  startedConceptCount: number
  reviewTargetCount: number
  skillProgress: SkillProgress[]
  weakConcepts: WeakConcept[]
  recentChats: ChatRoom[]
}

export interface ConceptLearningStatus {
  conceptCode: string
  conceptName: string
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  understandingScore: number
  learningStatus: LearningState
  assessmentReason: string | null
  lastStudiedAt: string | null
  nextReviewAt: string | null
}

export interface SkillLearningStatus {
  skillCode: string
  skillName: string
  averageScore: number
  concepts: ConceptLearningStatus[]
}

export interface LearningStatusOverview {
  skills: SkillLearningStatus[]
}

export interface ReviewTarget {
  chatMessageId: number
  skillCode: string
  skillName: string
  conceptCode: string
  conceptName: string
  question: string
  understandingScore: number
  learningStatus: LearningState
}

export interface AssessmentResult {
  id: number
  chatMessageId: number
  skillCode: string
  conceptCode: string
  conceptName: string
  question: string
  userAnswer: string
  score: number
  correct: boolean
  feedback: string
  correctAnswer: string
  reviewRequired: boolean
  createdAt: string
}
