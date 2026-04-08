'use client';

import { useState } from 'react';
import { Sidebar } from '@/components/layout/Sidebar';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { useApp } from '@/stores/AppContext';
import { toast } from 'sonner';
import {
  Building2,
  Plus,
  Settings,
  Database,
  Brain,
  Shield,
  Users,
  Key,
  Globe,
  Server,
  CheckCircle,
} from 'lucide-react';

export default function SettingsPage() {
  const { tenants, currentTenant, setCurrentTenant, loadTenants } = useApp();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newTenantName, setNewTenantName] = useState('');
  const [newTenantDesc, setNewTenantDesc] = useState('');
  const [newTenantLevel, setNewTenantLevel] = useState('METADATA');

  const handleCreateTenant = async () => {
    if (!newTenantName.trim()) {
      toast.error('请输入租户名称');
      return;
    }
    toast.success('租户创建功能开发中');
    setIsCreateOpen(false);
    setNewTenantName('');
    setNewTenantDesc('');
  };

  return (
    <div className="flex h-screen">
      <Sidebar />
      <main className="flex-1 pt-14 lg:pt-0 overflow-hidden">
        <div className="h-full flex flex-col p-6">
          <div className="mb-6">
            <h1 className="text-2xl font-bold">系统设置</h1>
            <p className="text-muted-foreground">
              配置系统参数和管理多租户
            </p>
          </div>

          <Tabs defaultValue="tenant" className="flex-1">
            <TabsList>
              <TabsTrigger value="tenant">租户管理</TabsTrigger>
              <TabsTrigger value="system">系统配置</TabsTrigger>
              <TabsTrigger value="llm">LLM 配置</TabsTrigger>
              <TabsTrigger value="milvus">Milvus 配置</TabsTrigger>
            </TabsList>

            {/* Tenant Management */}
            <TabsContent value="tenant" className="mt-4">
              <div className="grid gap-6 lg:grid-cols-2">
                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <div>
                        <CardTitle>租户列表</CardTitle>
                        <CardDescription>
                          管理多租户配置
                        </CardDescription>
                      </div>
                      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
                        <DialogTrigger asChild>
                          <Button>
                            <Plus className="w-4 h-4 mr-2" />
                            新建租户
                          </Button>
                        </DialogTrigger>
                        <DialogContent>
                          <DialogHeader>
                            <DialogTitle>创建新租户</DialogTitle>
                            <DialogDescription>
                              输入租户信息创建新租户
                            </DialogDescription>
                          </DialogHeader>
                          <div className="space-y-4 py-4">
                            <div>
                              <Label htmlFor="name">租户名称</Label>
                              <Input
                                id="name"
                                value={newTenantName}
                                onChange={(e) => setNewTenantName(e.target.value)}
                                placeholder="输入租户名称"
                                className="mt-1"
                              />
                            </div>
                            <div>
                              <Label htmlFor="desc">描述（可选）</Label>
                              <Input
                                id="desc"
                                value={newTenantDesc}
                                onChange={(e) => setNewTenantDesc(e.target.value)}
                                placeholder="简要描述"
                                className="mt-1"
                              />
                            </div>
                            <div>
                              <Label htmlFor="level">隔离级别</Label>
                              <Select value={newTenantLevel} onValueChange={setNewTenantLevel}>
                                <SelectTrigger className="mt-1">
                                  <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="COLLECTION">Collection 级别</SelectItem>
                                  <SelectItem value="PARTITION">Partition 级别</SelectItem>
                                  <SelectItem value="METADATA">Metadata 级别</SelectItem>
                                </SelectContent>
                              </Select>
                            </div>
                          </div>
                          <DialogFooter>
                            <Button variant="outline" onClick={() => setIsCreateOpen(false)}>
                              取消
                            </Button>
                            <Button onClick={handleCreateTenant}>创建</Button>
                          </DialogFooter>
                        </DialogContent>
                      </Dialog>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <ScrollArea className="h-[400px]">
                      <div className="space-y-4">
                        {tenants.map((tenant) => (
                          <div
                            key={tenant.id}
                            className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                              currentTenant?.id === tenant.id
                                ? 'border-primary bg-primary/5'
                                : 'hover:bg-accent/50'
                            }`}
                            onClick={() => setCurrentTenant(tenant)}
                          >
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-3">
                                <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                                  <Building2 className="w-5 h-5 text-primary" />
                                </div>
                                <div>
                                  <div className="flex items-center gap-2">
                                    <h4 className="font-semibold">{tenant.name}</h4>
                                    {currentTenant?.id === tenant.id && (
                                      <CheckCircle className="w-4 h-4 text-primary" />
                                    )}
                                  </div>
                                  <p className="text-sm text-muted-foreground">
                                    {tenant.description || '暂无描述'}
                                  </p>
                                </div>
                              </div>
                              <Badge>{tenant.isolationLevel}</Badge>
                            </div>
                          </div>
                        ))}
                        {tenants.length === 0 && (
                          <div className="text-center py-8">
                            <Users className="w-12 h-12 mx-auto text-muted-foreground/50 mb-4" />
                            <p className="text-muted-foreground">暂无租户</p>
                            <Button variant="link" onClick={() => setIsCreateOpen(true)}>
                              创建第一个租户
                            </Button>
                          </div>
                        )}
                      </div>
                    </ScrollArea>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>当前租户详情</CardTitle>
                    <CardDescription>
                      {currentTenant?.name || '选择一个租户查看详情'}
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    {currentTenant ? (
                      <div className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                          <div>
                            <Label className="text-muted-foreground">租户 ID</Label>
                            <p className="font-mono text-sm mt-1">{currentTenant.id}</p>
                          </div>
                          <div>
                            <Label className="text-muted-foreground">隔离级别</Label>
                            <Badge variant="outline" className="mt-1">
                              {currentTenant.isolationLevel}
                            </Badge>
                          </div>
                        </div>
                        <Separator />
                        <div>
                          <Label className="text-muted-foreground">创建时间</Label>
                          <p className="text-sm mt-1">
                            {new Date(currentTenant.createdAt).toLocaleString()}
                          </p>
                        </div>
                        <Separator />
                        <div className="flex gap-2">
                          <Button variant="outline" className="flex-1">
                            编辑
                          </Button>
                          <Button variant="destructive" className="flex-1">
                            删除
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <div className="text-center py-8 text-muted-foreground">
                        <Building2 className="w-12 h-12 mx-auto mb-4 opacity-50" />
                        <p>请从左侧选择一个租户</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            {/* System Config */}
            <TabsContent value="system" className="mt-4">
              <div className="grid gap-6 lg:grid-cols-2">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Server className="w-5 h-5" />
                      服务配置
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <Label>API 地址</Label>
                      <Input
                        defaultValue="http://localhost:8080"
                        className="mt-1"
                        disabled
                      />
                    </div>
                    <div>
                      <Label>Milvus 地址</Label>
                      <Input
                        defaultValue="localhost:19530"
                        className="mt-1"
                        disabled
                      />
                    </div>
                    <div>
                      <Label>MySQL 地址</Label>
                      <Input
                        defaultValue="localhost:3306"
                        className="mt-1"
                        disabled
                      />
                    </div>
                    <div>
                      <Label>Redis 地址</Label>
                      <Input
                        defaultValue="localhost:6379"
                        className="mt-1"
                        disabled
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Brain className="w-5 h-5" />
                      记忆配置
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <Label>会话记忆窗口</Label>
                      <Input
                        defaultValue="10"
                        className="mt-1"
                        disabled
                      />
                      <p className="text-xs text-muted-foreground mt-1">
                        最近 N 条消息作为上下文
                      </p>
                    </div>
                    <div>
                      <Label>情景记忆召回数</Label>
                      <Input
                        defaultValue="5"
                        className="mt-1"
                        disabled
                      />
                    </div>
                    <div>
                      <Label>Token 限制</Label>
                      <Input
                        defaultValue="8000"
                        className="mt-1"
                        disabled
                      />
                    </div>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            {/* LLM Config */}
            <TabsContent value="llm" className="mt-4">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Key className="w-5 h-5" />
                    LLM 配置
                  </CardTitle>
                  <CardDescription>
                    配置大语言模型 API
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <Label>API Provider</Label>
                      <Select defaultValue="openai">
                        <SelectTrigger className="mt-1">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="openai">OpenAI</SelectItem>
                          <SelectItem value="azure">Azure OpenAI</SelectItem>
                          <SelectItem value="claude">Claude</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <Label>Model</Label>
                      <Select defaultValue="gpt-4">
                        <SelectTrigger className="mt-1">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="gpt-4">GPT-4</SelectItem>
                          <SelectItem value="gpt-4-turbo">GPT-4 Turbo</SelectItem>
                          <SelectItem value="gpt-3.5">GPT-3.5 Turbo</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <div>
                    <Label>API Key</Label>
                    <Input
                      type="password"
                      defaultValue="sk-..."
                      className="mt-1"
                      disabled
                    />
                  </div>
                  <div>
                    <Label>Base URL (可选)</Label>
                    <Input
                      placeholder="https://api.openai.com/v1"
                      className="mt-1"
                      disabled
                    />
                  </div>
                  <Separator />
                  <div className="flex items-center gap-4">
                    <Button>保存配置</Button>
                    <Button variant="outline">测试连接</Button>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Milvus Config */}
            <TabsContent value="milvus" className="mt-4">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Database className="w-5 h-5" />
                    Milvus 向量数据库配置
                  </CardTitle>
                  <CardDescription>
                    配置 Milvus 连接和向量检索参数
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <Label>Milvus Host</Label>
                      <Input defaultValue="localhost" className="mt-1" disabled />
                    </div>
                    <div>
                      <Label>Milvus Port</Label>
                      <Input defaultValue="19530" className="mt-1" disabled />
                    </div>
                  </div>
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <Label>向量维度</Label>
                      <Input defaultValue="1536" className="mt-1" disabled />
                    </div>
                    <div>
                      <Label>度量类型</Label>
                      <Select defaultValue="cosine">
                        <SelectTrigger className="mt-1">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="cosine">COSINE</SelectItem>
                          <SelectItem value="ip">IP</SelectItem>
                          <SelectItem value="l2">L2</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <Label>Dense Top K</Label>
                      <Input defaultValue="20" className="mt-1" disabled />
                    </div>
                    <div>
                      <Label>Sparse Top K</Label>
                      <Input defaultValue="20" className="mt-1" disabled />
                    </div>
                  </div>
                  <div>
                    <Label>RRF K 值</Label>
                    <Input defaultValue="60" className="mt-1" disabled />
                    <p className="text-xs text-muted-foreground mt-1">
                      Reciprocal Rank Fusion 参数
                    </p>
                  </div>
                  <Separator />
                  <div className="flex items-center gap-4">
                    <Button>保存配置</Button>
                    <Button variant="outline">测试连接</Button>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </main>
    </div>
  );
}
