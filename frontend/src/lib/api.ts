// API 客户端
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

interface ApiOptions {
  tenantId?: string;
}

class ApiClient {
  private tenantId: string = 'default';

  setTenantId(tenantId: string) {
    this.tenantId = tenantId;
  }

  getTenantId(): string {
    return this.tenantId;
  }

  private getHeaders(): HeadersInit {
    return {
      'Content-Type': 'application/json',
      'X-Tenant-ID': this.tenantId,
    };
  }

  // 租户相关
  async getTenants() {
    const res = await fetch(`${API_BASE_URL}/api/tenants`, {
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch tenants');
    return res.json();
  }

  async createTenant(data: { name: string; description?: string; isolationLevel?: string }) {
    const res = await fetch(`${API_BASE_URL}/api/tenants`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to create tenant');
    return res.json();
  }

  async switchTenant(tenantId: string) {
    this.setTenantId(tenantId);
    if (typeof window !== 'undefined') {
      localStorage.setItem('tenantId', tenantId);
    }
  }

  // 对话相关
  async getConversations() {
    const res = await fetch(`${API_BASE_URL}/api/chat/conversations`, {
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch conversations');
    return res.json();
  }

  async createConversation(title?: string) {
    const res = await fetch(`${API_BASE_URL}/api/chat/conversations`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ title }),
    });
    if (!res.ok) throw new Error('Failed to create conversation');
    return res.json();
  }

  async getMessages(conversationId: string) {
    const res = await fetch(`${API_BASE_URL}/api/chat/conversations/${conversationId}/messages`, {
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch messages');
    return res.json();
  }

  async sendMessage(data: { message: string; conversationId?: string; useRag?: boolean; useMemory?: boolean }) {
    const res = await fetch(`${API_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({
        message: data.message,
        conversationId: data.conversationId,
        useRag: data.useRag ?? true,
        useMemory: data.useMemory ?? true,
      }),
    });
    if (!res.ok) throw new Error('Failed to send message');
    return res.json();
  }

  async sendMessageStream(
    data: { message: string; conversationId?: string; useRag?: boolean; useMemory?: boolean },
    onChunk: (text: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/chat/stream`, {
        method: 'POST',
        headers: this.getHeaders(),
        body: JSON.stringify({
          message: data.message,
          conversationId: data.conversationId,
          useRag: data.useRag ?? true,
          useMemory: data.useMemory ?? true,
        }),
      });

      if (!response.ok) throw new Error('Failed to send message');
      if (!response.body) throw new Error('No response body');

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data === '[DONE]') {
              onComplete();
              return;
            }
            try {
              const parsed = JSON.parse(data);
              if (parsed.content) {
                onChunk(parsed.content);
              }
            } catch {
              // 忽略解析错误
            }
          }
        }
      }
      onComplete();
    } catch (error) {
      onError(error instanceof Error ? error : new Error('Unknown error'));
    }
  }

  // 文档相关
  async getDocuments() {
    const res = await fetch(`${API_BASE_URL}/api/documents`, {
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch documents');
    return res.json();
  }

  async uploadDocument(formData: FormData) {
    const res = await fetch(`${API_BASE_URL}/api/documents/upload`, {
      method: 'POST',
      headers: {
        'X-Tenant-ID': this.tenantId,
      },
      body: formData,
    });
    if (!res.ok) throw new Error('Failed to upload document');
    return res.json();
  }

  async deleteDocument(documentId: string) {
    const res = await fetch(`${API_BASE_URL}/api/documents/${documentId}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to delete document');
    return res.json();
  }

  async getDocumentChunks(documentId: string) {
    const res = await fetch(`${API_BASE_URL}/api/documents/${documentId}/chunks`, {
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch chunks');
    return res.json();
  }

  // 搜索相关
  async search(query: string, limit?: number) {
    const res = await fetch(`${API_BASE_URL}/api/search?query=${encodeURIComponent(query)}&limit=${limit || 10}`, {
      headers: this.getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to search');
    return res.json();
  }

  // 健康检查
  async healthCheck() {
    try {
      const res = await fetch(`${API_BASE_URL}/api/health`);
      return res.ok;
    } catch {
      return false;
    }
  }
}

export const api = new ApiClient();

// 初始化租户 ID
if (typeof window !== 'undefined') {
  const savedTenantId = localStorage.getItem('tenantId');
  if (savedTenantId) {
    api.setTenantId(savedTenantId);
  }
}
