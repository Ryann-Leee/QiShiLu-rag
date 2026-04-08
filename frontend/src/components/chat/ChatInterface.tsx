'use client';

import { useState, useRef, useEffect } from 'react';
import { useApp } from '@/stores/AppContext';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import {
  Send,
  Loader2,
  Plus,
  MessageSquare,
  Trash2,
  Copy,
  CheckCheck,
} from 'lucide-react';
import type { Message } from '@/types';

function formatTime(dateString: string) {
  const date = new Date(dateString);
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

function MessageItem({ message }: { message: Message }) {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === 'user';

  const handleCopy = async () => {
    await navigator.clipboard.writeText(message.content);
    setCopied(true);
    toast.success('已复制');
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={`flex gap-3 ${isUser ? 'flex-row-reverse' : ''}`}>
      <Avatar className="w-8 h-8">
        {isUser ? (
          <>
            <AvatarFallback className="bg-blue-500 text-white">U</AvatarFallback>
          </>
        ) : (
          <>
            <AvatarFallback className="bg-purple-500 text-white">AI</AvatarFallback>
          </>
        )}
      </Avatar>
      <div className={`flex flex-col gap-1 max-w-[70%] ${isUser ? 'items-end' : ''}`}>
        <div
          className={`rounded-lg px-4 py-2 ${
            isUser
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted'
          }`}
        >
          <p className="whitespace-pre-wrap text-sm">{message.content}</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span>{formatTime(message.createdAt)}</span>
          {!isUser && (
            <Button
              variant="ghost"
              size="icon"
              className="w-6 h-6"
              onClick={handleCopy}
            >
              {copied ? (
                <CheckCheck className="w-3 h-3" />
              ) : (
                <Copy className="w-3 h-3" />
              )}
            </Button>
          )}
        </div>
        {message.citations && message.citations.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1">
            {message.citations.map((citation, idx) => (
              <Badge key={idx} variant="outline" className="text-xs">
                {citation.documentName}
              </Badge>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ChatList({
  conversations,
  currentConversation,
  onSelect,
  onDelete,
  onNew,
}: {
  conversations: { id: string; title: string; updatedAt: string }[];
  currentConversation: { id: string } | null;
  onSelect: (id: string) => void;
  onDelete: (id: string) => void;
  onNew: () => void;
}) {
  return (
    <div className="w-64 border-r bg-muted/30 flex flex-col">
      <div className="p-4 border-b">
        <Button onClick={onNew} className="w-full gap-2">
          <Plus className="w-4 h-4" />
          新对话
        </Button>
      </div>
      <ScrollArea className="flex-1">
        <div className="p-2">
          {conversations.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <MessageSquare className="w-8 h-8 mx-auto mb-2 opacity-50" />
              <p className="text-sm">暂无对话记录</p>
            </div>
          ) : (
            conversations.map((conv) => (
              <div
                key={conv.id}
                className={`group flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer mb-1 ${
                  currentConversation?.id === conv.id
                    ? 'bg-accent'
                    : 'hover:bg-accent/50'
                }`}
                onClick={() => onSelect(conv.id)}
              >
                <MessageSquare className="w-4 h-4 flex-shrink-0" />
                <span className="flex-1 truncate text-sm">{conv.title || '新对话'}</span>
                <Button
                  variant="ghost"
                  size="icon"
                  className="w-6 h-6 opacity-0 group-hover:opacity-100"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(conv.id);
                  }}
                >
                  <Trash2 className="w-3 h-3" />
                </Button>
              </div>
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}

export function ChatInterface() {
  const {
    messages,
    isLoading,
    isStreaming,
    streamingContent,
    conversations,
    currentConversation,
    setCurrentConversation,
    sendMessage,
    createConversation,
  } = useApp();
  
  const [input, setInput] = useState('');
  const [useRag, setUseRag] = useState(true);
  const [useMemory, setUseMemory] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingContent]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isStreaming) return;

    const message = input.trim();
    setInput('');

    try {
      await sendMessage(message, useRag, useMemory);
    } catch (error) {
      toast.error('发送消息失败');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div className="flex flex-1 h-full">
      {/* Chat List */}
      <ChatList
        conversations={conversations}
        currentConversation={currentConversation}
        onSelect={(id) => {
          const conv = conversations.find((c) => c.id === id);
          if (conv) setCurrentConversation(conv);
        }}
        onDelete={(id) => {
          toast.info('删除功能开发中');
        }}
        onNew={async () => {
          const conv = await createConversation();
          setCurrentConversation(conv);
        }}
      />

      {/* Chat Area */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <div className="h-14 border-b px-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="font-semibold">
              {currentConversation?.title || '新对话'}
            </h2>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Switch
                id="use-rag"
                checked={useRag}
                onCheckedChange={setUseRag}
              />
              <Label htmlFor="use-rag" className="text-sm cursor-pointer">
                RAG 检索
              </Label>
            </div>
            <div className="flex items-center gap-2">
              <Switch
                id="use-memory"
                checked={useMemory}
                onCheckedChange={setUseMemory}
              />
              <Label htmlFor="use-memory" className="text-sm cursor-pointer">
                长期记忆
              </Label>
            </div>
          </div>
        </div>

        {/* Messages */}
        <ScrollArea className="flex-1 p-4">
          <div className="max-w-3xl mx-auto space-y-6">
            {messages.length === 0 && !streamingContent && (
              <div className="flex flex-col items-center justify-center h-full text-center text-muted-foreground">
                <MessageSquare className="w-16 h-16 mb-4 opacity-20" />
                <h3 className="text-lg font-medium mb-2">开始新对话</h3>
                <p className="text-sm max-w-md">
                  输入你的问题，AI 将基于知识库中的内容为你解答。支持 RAG 检索和长期记忆功能。
                </p>
              </div>
            )}
            
            {messages.map((message) => (
              <MessageItem key={message.id} message={message} />
            ))}

            {isStreaming && streamingContent && (
              <MessageItem
                message={{
                  id: 'streaming',
                  conversationId: currentConversation?.id || '',
                  role: 'assistant',
                  content: streamingContent,
                  createdAt: new Date().toISOString(),
                }}
              />
            )}

            {isLoading && !isStreaming && (
              <div className="flex items-center gap-2 text-muted-foreground">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span className="text-sm">思考中...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        </ScrollArea>

        {/* Input */}
        <div className="border-t p-4">
          <form onSubmit={handleSubmit} className="max-w-3xl mx-auto">
            <div className="flex gap-2">
              <Input
                ref={inputRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="输入消息... (Shift+Enter 换行)"
                disabled={isStreaming}
                className="flex-1"
              />
              <Button type="submit" disabled={!input.trim() || isStreaming}>
                {isStreaming ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Send className="w-4 h-4" />
                )}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
