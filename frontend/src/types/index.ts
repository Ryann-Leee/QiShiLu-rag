// 类型定义

// 租户
export interface Tenant {
  id: string;
  name: string;
  description?: string;
  isolationLevel: 'COLLECTION' | 'PARTITION' | 'METADATA';
  createdAt: string;
}

// 对话
export interface Conversation {
  id: string;
  tenantId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

// 消息
export interface Message {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  createdAt: string;
  citations?: Citation[];
}

export interface Citation {
  documentId: string;
  documentName: string;
  chunkId: string;
  content: string;
  score: number;
}

// 文档
export interface KnowledgeDocument {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  fileSize: number;
  fileType: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  chunkCount: number;
  createdAt: string;
  updatedAt: string;
}

// 分块元数据
export interface ChunkMetadata {
  id: string;
  documentId: string;
  documentName: string;
  chunkIndex: number;
  content: string;
  tokenCount: number;
  createdAt: string;
}

// 用户画像
export interface UserProfile {
  id: string;
  tenantId: string;
  preferences: Record<string, unknown>;
  tags: string[];
  lastUpdated: string;
}

// 聊天请求
export interface ChatRequest {
  message: string;
  conversationId?: string;
  useRag: boolean;
  useMemory: boolean;
}

// 聊天响应
export interface ChatResponse {
  conversationId: string;
  message: Message;
  memoryContext?: {
    sessionMemory: Message[];
    episodicMemory: string[];
    userProfile?: UserProfile;
  };
}

// 文档上传请求
export interface DocumentUploadRequest {
  file: File;
  description?: string;
  chunkSize?: number;
  chunkOverlap?: number;
}

// 聊天历史响应
export interface ChatHistoryResponse {
  conversations: Conversation[];
  messages: Message[];
}
