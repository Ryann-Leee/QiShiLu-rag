'use client';

import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { api } from '@/lib/api';
import type { Tenant, Conversation, Message, KnowledgeDocument } from '@/types';

interface AppState {
  // 租户
  tenants: Tenant[];
  currentTenant: Tenant | null;
  setCurrentTenant: (tenant: Tenant) => void;
  loadTenants: () => Promise<void>;
  
  // 对话
  conversations: Conversation[];
  currentConversation: Conversation | null;
  messages: Message[];
  isLoading: boolean;
  isStreaming: boolean;
  streamingContent: string;
  setCurrentConversation: (conversation: Conversation | null) => void;
  loadConversations: () => Promise<void>;
  loadMessages: (conversationId: string) => Promise<void>;
  sendMessage: (content: string, useRag?: boolean, useMemory?: boolean) => Promise<void>;
  createConversation: (title?: string) => Promise<Conversation>;
  
  // 文档
  documents: KnowledgeDocument[];
  loadDocuments: () => Promise<void>;
  uploadDocument: (file: File, description?: string) => Promise<void>;
  deleteDocument: (documentId: string) => Promise<void>;
}

const AppContext = createContext<AppState | null>(null);

export function AppProvider({ children }: { children: React.ReactNode }) {
  // 租户状态
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [currentTenant, setCurrentTenantState] = useState<Tenant | null>(null);
  
  // 对话状态
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [currentConversation, setCurrentConversationState] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingContent, setStreamingContent] = useState('');
  
  // 文档状态
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);

  // 加载租户列表
  const loadTenants = useCallback(async () => {
    try {
      const data = await api.getTenants();
      setTenants(data.tenants || []);
      if (data.tenants?.length > 0 && !currentTenant) {
        setCurrentTenantState(data.tenants[0]);
        api.setTenantId(data.tenants[0].id);
      }
    } catch (error) {
      console.error('Failed to load tenants:', error);
    }
  }, [currentTenant]);

  // 设置当前租户
  const setCurrentTenant = useCallback((tenant: Tenant) => {
    setCurrentTenantState(tenant);
    api.setTenantId(tenant.id);
    if (typeof window !== 'undefined') {
      localStorage.setItem('tenantId', tenant.id);
    }
    // 切换租户后重新加载数据
    setConversations([]);
    setMessages([]);
    setDocuments([]);
  }, []);

  // 加载对话列表
  const loadConversations = useCallback(async () => {
    try {
      const data = await api.getConversations();
      setConversations(data.conversations || []);
    } catch (error) {
      console.error('Failed to load conversations:', error);
    }
  }, []);

  // 加载消息
  const loadMessages = useCallback(async (conversationId: string) => {
    try {
      setIsLoading(true);
      const data = await api.getMessages(conversationId);
      setMessages(data.messages || []);
    } catch (error) {
      console.error('Failed to load messages:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 创建对话
  const createConversation = useCallback(async (title?: string): Promise<Conversation> => {
    const conversation = await api.createConversation(title);
    await loadConversations();
    setCurrentConversationState(conversation);
    setMessages([]);
    return conversation;
  }, [loadConversations]);

  // 设置当前对话
  const setCurrentConversation = useCallback((conversation: Conversation | null) => {
    setCurrentConversationState(conversation);
    if (conversation) {
      loadMessages(conversation.id);
    } else {
      setMessages([]);
    }
  }, [loadMessages]);

  // 发送消息（流式）
  const sendMessage = useCallback(async (
    content: string,
    useRag = true,
    useMemory = true
  ) => {
    if (isStreaming) return;

    const conversationId = currentConversation?.id;
    
    // 如果没有当前对话，先创建一个
    let convId = conversationId;
    if (!convId) {
      const newConv = await createConversation();
      convId = newConv.id;
    }

    // 添加用户消息
    const userMessage: Message = {
      id: `temp-${Date.now()}`,
      conversationId: convId,
      role: 'user',
      content,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, userMessage]);

    // 开始流式响应
    setIsStreaming(true);
    setStreamingContent('');

    return new Promise<void>((resolve, reject) => {
      api.sendMessageStream(
        { message: content, conversationId: convId, useRag, useMemory },
        (chunk) => {
          setStreamingContent(prev => prev + chunk);
        },
        () => {
          // 完成
          const assistantMessage: Message = {
            id: `temp-${Date.now() + 1}`,
            conversationId: convId!,
            role: 'assistant',
            content: streamingContent + (streamingContent ? '' : ''),
            createdAt: new Date().toISOString(),
          };
          setMessages(prev => [...prev.filter(m => m.id !== userMessage.id), userMessage, assistantMessage]);
          setIsStreaming(false);
          setStreamingContent('');
          loadConversations();
          resolve();
        },
        (error) => {
          setIsStreaming(false);
          setStreamingContent('');
          reject(error);
        }
      );
    });
  }, [currentConversation, isStreaming, createConversation, loadConversations]);

  // 加载文档列表
  const loadDocuments = useCallback(async () => {
    try {
      const data = await api.getDocuments();
      setDocuments(data.documents || []);
    } catch (error) {
      console.error('Failed to load documents:', error);
    }
  }, []);

  // 上传文档
  const uploadDocument = useCallback(async (file: File, description?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (description) {
      formData.append('description', description);
    }
    await api.uploadDocument(formData);
    await loadDocuments();
  }, [loadDocuments]);

  // 删除文档
  const deleteDocument = useCallback(async (documentId: string) => {
    await api.deleteDocument(documentId);
    await loadDocuments();
  }, [loadDocuments]);

  // 初始化
  useEffect(() => {
    loadTenants();
  }, [loadTenants]);

  const value: AppState = {
    tenants,
    currentTenant,
    setCurrentTenant,
    loadTenants,
    conversations,
    currentConversation,
    messages,
    isLoading,
    isStreaming,
    streamingContent,
    setCurrentConversation,
    loadConversations,
    loadMessages,
    sendMessage,
    createConversation,
    documents,
    loadDocuments,
    uploadDocument,
    deleteDocument,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
}
