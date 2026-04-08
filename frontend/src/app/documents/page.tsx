'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { useApp } from '@/stores/AppContext';
import { Sidebar } from '@/components/layout/Sidebar';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import {
  Upload,
  FileText,
  File,
  Trash2,
  RefreshCw,
  CheckCircle,
  XCircle,
  Clock,
  Loader2,
  Search,
} from 'lucide-react';
import type { KnowledgeDocument } from '@/types';

function formatFileSize(bytes: number) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function StatusBadge({ status }: { status: KnowledgeDocument['status'] }) {
  const config = {
    PENDING: { icon: Clock, label: '等待中', color: 'bg-yellow-500' },
    PROCESSING: { icon: Loader2, label: '处理中', color: 'bg-blue-500' },
    COMPLETED: { icon: CheckCircle, label: '已完成', color: 'bg-green-500' },
    FAILED: { icon: XCircle, label: '失败', color: 'bg-red-500' },
  };
  const { icon: Icon, label, color } = config[status];
  return (
    <Badge variant="outline" className={`${color} text-white border-0`}>
      <Icon className="w-3 h-3 mr-1" />
      {label}
    </Badge>
  );
}

function DocumentCard({
  document,
  onDelete,
}: {
  document: KnowledgeDocument;
  onDelete: (id: string) => void;
}) {
  return (
    <Card className="hover:bg-accent/50 transition-colors">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
              <FileText className="w-5 h-5 text-primary" />
            </div>
            <div>
              <CardTitle className="text-base">{document.name}</CardTitle>
              <CardDescription className="text-xs">
                {formatFileSize(document.fileSize)}
              </CardDescription>
            </div>
          </div>
          <StatusBadge status={document.status} />
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>{formatDate(document.createdAt)}</span>
          <div className="flex items-center gap-2">
            <Badge variant="secondary">
              {document.chunkCount} 个分块
            </Badge>
            <Button
              variant="ghost"
              size="icon"
              className="w-8 h-8 text-destructive"
              onClick={() => onDelete(document.id)}
            >
              <Trash2 className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default function DocumentsPage() {
  const { documents, loadDocuments, uploadDocument, deleteDocument } = useApp();
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [description, setDescription] = useState('');

  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      if (acceptedFiles.length === 0) return;

      setIsUploading(true);
      setUploadProgress(0);

      // 模拟上传进度
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => Math.min(prev + 10, 90));
      }, 200);

      try {
        for (const file of acceptedFiles) {
          await uploadDocument(file, description);
        }
        setUploadProgress(100);
        toast.success(`成功上传 ${acceptedFiles.length} 个文件`);
        setDescription('');
      } catch (error) {
        toast.error('上传失败');
      } finally {
        clearInterval(progressInterval);
        setTimeout(() => {
          setIsUploading(false);
          setUploadProgress(0);
        }, 500);
      }
    },
    [uploadDocument, description]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/plain': ['.txt'],
      'application/pdf': ['.pdf'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': [
        '.docx',
      ],
      'text/markdown': ['.md'],
    },
    disabled: isUploading,
  });

  const filteredDocuments = documents.filter((doc) =>
    doc.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex h-screen">
      <Sidebar />
      <main className="flex-1 pt-14 lg:pt-0 overflow-hidden">
        <div className="h-full flex flex-col p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold">文档管理</h1>
              <p className="text-muted-foreground">
                上传和管理知识库文档
              </p>
            </div>
            <Button onClick={() => loadDocuments()}>
              <RefreshCw className="w-4 h-4 mr-2" />
              刷新
            </Button>
          </div>

          {/* Upload Area */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>上传文档</CardTitle>
              <CardDescription>
                支持 TXT、PDF、DOCX、MD 格式，单文件不超过 50MB
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div
                {...getRootProps()}
                className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
                  isDragActive
                    ? 'border-primary bg-primary/5'
                    : 'border-muted-foreground/25 hover:border-primary'
                } ${isUploading ? 'pointer-events-none opacity-50' : ''}`}
              >
                <input {...getInputProps()} />
                {isUploading ? (
                  <div className="space-y-4">
                    <Loader2 className="w-12 h-12 mx-auto animate-spin text-primary" />
                    <p className="text-muted-foreground">上传中...</p>
                    <Progress value={uploadProgress} className="max-w-md mx-auto" />
                  </div>
                ) : (
                  <div className="space-y-4">
                    <Upload className="w-12 h-12 mx-auto text-muted-foreground" />
                    {isDragActive ? (
                      <p className="text-primary">松开以上传文件</p>
                    ) : (
                      <>
                        <p className="text-muted-foreground">
                          拖拽文件到此处，或点击选择文件
                        </p>
                        <p className="text-xs text-muted-foreground">
                          支持多个文件
                        </p>
                      </>
                    )}
                  </div>
                )}
              </div>
              <div className="mt-4">
                <Label htmlFor="description">文档描述（可选）</Label>
                <Input
                  id="description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="简要描述文档内容..."
                  className="mt-1"
                />
              </div>
            </CardContent>
          </Card>

          {/* Document List */}
          <Card className="flex-1">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>已上传文档</CardTitle>
                  <CardDescription>
                    共 {filteredDocuments.length} 个文档
                  </CardDescription>
                </div>
                <div className="relative w-64">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  <Input
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="搜索文档..."
                    className="pl-9"
                  />
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <ScrollArea className="h-[calc(100vh-400px)]">
                {filteredDocuments.length === 0 ? (
                  <div className="text-center py-12">
                    <File className="w-12 h-12 mx-auto text-muted-foreground/50 mb-4" />
                    <p className="text-muted-foreground">暂无文档</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      上传第一个文档开始构建知识库
                    </p>
                  </div>
                ) : (
                  <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {filteredDocuments.map((doc) => (
                      <DocumentCard
                        key={doc.id}
                        document={doc}
                        onDelete={(id) => {
                          deleteDocument(id);
                          toast.success('文档已删除');
                        }}
                      />
                    ))}
                  </div>
                )}
              </ScrollArea>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
