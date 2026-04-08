'use client';

import { useState } from 'react';
import { Sidebar } from '@/components/layout/Sidebar';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { toast } from 'sonner';
import {
  Database,
  Layers,
  Shield,
  Search,
  FileText,
  Brain,
  User,
  RefreshCw,
  Copy,
  ExternalLink,
} from 'lucide-react';

interface ChunkPreview {
  id: string;
  content: string;
  documentName: string;
  chunkIndex: number;
  tokenCount: number;
}

interface MemoryPreview {
  type: 'session' | 'episodic' | 'profile';
  content: string;
  timestamp: string;
}

export default function KnowledgePage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedChunk, setSelectedChunk] = useState<ChunkPreview | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const stats = {
    totalDocuments: 12,
    totalChunks: 1256,
    memoryEntries: 89,
    lastSync: '2024-01-15 10:30:00',
  };

  const chunks: ChunkPreview[] = [
    {
      id: '1',
      content: 'RAG（检索增强生成）是一种结合检索系统和生成模型的技术，可以提高 AI 回答的准确性和相关性。',
      documentName: 'RAG技术介绍.pdf',
      chunkIndex: 0,
      tokenCount: 120,
    },
    {
      id: '2',
      content: 'Milvus 是一个开源的向量数据库，专门用于存储和检索高维向量数据。',
      documentName: '向量数据库概述.md',
      chunkIndex: 5,
      tokenCount: 98,
    },
    {
      id: '3',
      content: '多租户隔离是企业级 RAG 系统的重要特性，确保不同租户的数据互不影响。',
      documentName: '系统架构设计.docx',
      chunkIndex: 12,
      tokenCount: 145,
    },
  ];

  const memories: MemoryPreview[] = [
    {
      type: 'session',
      content: '用户询问了关于 RAG 系统架构的问题',
      timestamp: '2024-01-15 10:30:00',
    },
    {
      type: 'episodic',
      content: '用户之前上传过向量数据库相关的文档',
      timestamp: '2024-01-14 15:20:00',
    },
    {
      type: 'profile',
      content: '用户偏好简洁的技术回答',
      timestamp: '2024-01-10 09:00:00',
    },
  ];

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      toast.success(`找到 ${chunks.length} 条相关结果`);
    }, 1000);
  };

  const filteredChunks = chunks.filter(
    (chunk) =>
      chunk.content.includes(searchQuery) ||
      chunk.documentName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex h-screen">
      <Sidebar />
      <main className="flex-1 pt-14 lg:pt-0 overflow-hidden">
        <div className="h-full flex flex-col p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold">知识库管理</h1>
              <p className="text-muted-foreground">
                查看和管理知识库内容与记忆
              </p>
            </div>
            <Button variant="outline">
              <RefreshCw className="w-4 h-4 mr-2" />
              同步
            </Button>
          </div>

          <div className="grid gap-4 md:grid-cols-4 mb-6">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium">文档总数</CardTitle>
                <FileText className="w-4 h-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.totalDocuments}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium">分块总数</CardTitle>
                <Layers className="w-4 h-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.totalChunks.toLocaleString()}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium">记忆条目</CardTitle>
                <Brain className="w-4 h-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.memoryEntries}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium">隔离级别</CardTitle>
                <Shield className="w-4 h-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <Badge>METADATA</Badge>
              </CardContent>
            </Card>
          </div>

          <Tabs defaultValue="chunks" className="flex-1">
            <TabsList>
              <TabsTrigger value="chunks">向量分块</TabsTrigger>
              <TabsTrigger value="memories">长期记忆</TabsTrigger>
              <TabsTrigger value="isolation">隔离策略</TabsTrigger>
            </TabsList>

            <TabsContent value="chunks" className="mt-4">
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>向量分块</CardTitle>
                      <CardDescription>
                        存储在 Milvus 中的文档分块，支持语义搜索
                      </CardDescription>
                    </div>
                    <div className="flex gap-2">
                      <div className="relative w-64">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                        <Input
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                          placeholder="搜索分块内容..."
                          className="pl-9"
                        />
                      </div>
                      <Button onClick={handleSearch} disabled={isLoading}>
                        {isLoading ? '搜索中...' : '搜索'}
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <ScrollArea className="h-[400px]">
                    <div className="space-y-4">
                      {filteredChunks.map((chunk) => (
                        <div
                          key={chunk.id}
                          className="p-4 border rounded-lg hover:bg-accent/50 cursor-pointer transition-colors"
                          onClick={() => setSelectedChunk(chunk)}
                        >
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-2">
                              <Badge variant="outline">{chunk.documentName}</Badge>
                              <Badge variant="secondary">
                                Chunk #{chunk.chunkIndex}
                              </Badge>
                            </div>
                            <span className="text-xs text-muted-foreground">
                              {chunk.tokenCount} tokens
                            </span>
                          </div>
                          <p className="text-sm line-clamp-2">{chunk.content}</p>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="memories" className="mt-4">
              <Card>
                <CardHeader>
                  <CardTitle>长期记忆</CardTitle>
                  <CardDescription>
                    三层记忆架构：会话记忆、情景记忆、用户画像
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <ScrollArea className="h-[400px]">
                    <div className="space-y-4">
                      {memories.map((memory, idx) => (
                        <div key={idx} className="p-4 border rounded-lg">
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-2">
                              {memory.type === 'session' && (
                                <>
                                  <Brain className="w-4 h-4 text-blue-500" />
                                  <Badge variant="outline">会话记忆</Badge>
                                </>
                              )}
                              {memory.type === 'episodic' && (
                                <>
                                  <Layers className="w-4 h-4 text-purple-500" />
                                  <Badge variant="outline">情景记忆</Badge>
                                </>
                              )}
                              {memory.type === 'profile' && (
                                <>
                                  <User className="w-4 h-4 text-green-500" />
                                  <Badge variant="outline">用户画像</Badge>
                                </>
                              )}
                            </div>
                            <span className="text-xs text-muted-foreground">
                              {memory.timestamp}
                            </span>
                          </div>
                          <p className="text-sm">{memory.content}</p>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="isolation" className="mt-4">
              <Card>
                <CardHeader>
                  <CardTitle>多租户隔离策略</CardTitle>
                  <CardDescription>
                    当前使用 METADATA 级别隔离，数据安全有保障
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-6">
                    <div className="grid gap-4 md:grid-cols-3">
                      <div className="p-4 border rounded-lg">
                        <div className="flex items-center gap-2 mb-2">
                          <Shield className="w-5 h-5 text-primary" />
                          <h4 className="font-semibold">Collection 级别</h4>
                        </div>
                        <p className="text-sm text-muted-foreground mb-2">
                          每个租户独立 Collection，完全物理隔离
                        </p>
                        <Badge>推荐用于超大规模</Badge>
                      </div>
                      <div className="p-4 border rounded-lg border-primary">
                        <div className="flex items-center gap-2 mb-2">
                          <Shield className="w-5 h-5 text-primary" />
                          <h4 className="font-semibold">Partition 级别</h4>
                        </div>
                        <p className="text-sm text-muted-foreground mb-2">
                          租户共享 Collection，通过 Partition 隔离
                        </p>
                        <Badge variant="default">当前使用</Badge>
                      </div>
                      <div className="p-4 border rounded-lg">
                        <div className="flex items-center gap-2 mb-2">
                          <Shield className="w-5 h-5 text-muted-foreground" />
                          <h4 className="font-semibold">Metadata 级别</h4>
                        </div>
                        <p className="text-sm text-muted-foreground mb-2">
                          所有数据在同一个 Collection，通过 metadata 过滤
                        </p>
                        <Badge variant="secondary">成本最低</Badge>
                      </div>
                    </div>
                    <div className="p-4 bg-muted rounded-lg">
                      <h4 className="font-semibold mb-2">隔离原理</h4>
                      <ul className="text-sm space-y-1 text-muted-foreground">
                        <li>每个请求通过 X-Tenant-ID Header 标识租户</li>
                        <li>Milvus 查询自动注入 tenant_id 过滤条件</li>
                        <li>MySQL 查询通过租户 ID 字段过滤</li>
                        <li>Redis Session 按租户 ID 隔离存储</li>
                      </ul>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </main>

      <Dialog open={!!selectedChunk} onOpenChange={() => setSelectedChunk(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>分块详情</DialogTitle>
            <DialogDescription>
              {selectedChunk?.documentName} - Chunk #{selectedChunk?.chunkIndex}
            </DialogDescription>
          </DialogHeader>
          {selectedChunk && (
            <div className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant="outline">{selectedChunk.tokenCount} tokens</Badge>
              </div>
              <div className="p-4 bg-muted rounded-lg">
                <p className="whitespace-pre-wrap">{selectedChunk.content}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="sm">
                  <Copy className="w-4 h-4 mr-2" />
                  复制内容
                </Button>
                <Button variant="outline" size="sm">
                  <ExternalLink className="w-4 h-4 mr-2" />
                  查看原文
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
