#!/usr/bin/env python3
"""
Qwen3-Embedding-0.6B 本地 Embedding 服务

功能:
- 提供 REST API 生成文本向量
- 支持批量文本处理
- 支持 GPU/CPU 自动切换

启动方式:
    python embedding_service.py

API:
    POST /embed
    {
        "texts": ["文本1", "文本2"]  // 单文本或列表
    }
    
    返回:
    {
        "embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...]],
        "model": "Qwen/Qwen3-Embedding-0.6B",
        "dimension": 1024
    }
"""

import os
import sys
import json
import time
import argparse
from typing import List, Union, Optional
from concurrent.futures import ThreadPoolExecutor

import torch
from transformers import AutoTokenizer, AutoModel
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import numpy as np

# 模型配置
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models", "Qwen3-Embedding-0.6B")
DEFAULT_MODEL = "Qwen/Qwen3-Embedding-0.6B"
HF_ENDPOINT = os.environ.get("HF_ENDPOINT", "https://hf-mirror.com")

# 全局变量
app = FastAPI(title="Qwen3-Embedding Service", version="1.0.0")
tokenizer = None
model = None
model_name = None
embedding_dimension = None

# 添加 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def mean_pooling(model_output, attention_mask):
    """Mean Pooling - 考虑 attention mask 的平均池化"""
    token_embeddings = model_output[0]  # 第一个元素是所有 token 的 embedding
    input_mask_expanded = attention_mask.unsqueeze(-1).expand(token_embeddings.size()).float()
    return torch.sum(token_embeddings * input_mask_expanded, 1) / torch.clamp(input_mask_expanded.sum(1), min=1e-9)


def load_model(model_path: str = None):
    """加载模型"""
    global tokenizer, model, model_name, embedding_dimension
    
    # 确定模型路径
    if model_path and os.path.exists(model_path):
        model_dir = model_path
        model_name = os.path.basename(model_path)
    else:
        # 使用 Hugging Face 远程模型
        model_dir = DEFAULT_MODEL
        model_name = DEFAULT_MODEL
        os.environ["HF_ENDPOINT"] = HF_ENDPOINT
    
    print(f"=" * 60)
    print(f"加载 Qwen3-Embedding 模型")
    print(f"=" * 60)
    print(f"模型: {model_name}")
    print(f"路径: {model_dir}")
    
    # 检查设备
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"设备: {device}")
    
    try:
        # 加载 tokenizer
        print("加载 Tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(
            model_dir,
            trust_remote_code=True,
            use_fast=False
        )
        
        # 加载模型
        print("加载 Model...")
        model = AutoModel.from_pretrained(
            model_dir,
            trust_remote_code=True,
            device_map="auto" if device == "cuda" else None
        )
        
        # 设置为评估模式
        model.eval()
        
        # 获取 embedding 维度 (从模型配置或实际测试)
        try:
            embedding_dimension = model.config.hidden_size
        except:
            # 如果无法从配置获取，进行测试推理
            test_input = tokenizer("test", return_tensors="pt", padding=True, truncation=True)
            with torch.no_grad():
                test_output = model(**test_input)
                embedding_dimension = test_output.last_hidden_state.shape[-1]
        
        print(f"Embedding 维度: {embedding_dimension}")
        print(f"模型加载完成!")
        print(f"=" * 60)
        
        return True
        
    except Exception as e:
        print(f"模型加载失败: {e}")
        return False


def encode_texts(texts: List[str], batch_size: int = 32, normalize: bool = True) -> List[List[float]]:
    """将文本编码为向量"""
    global model, tokenizer
    
    if model is None or tokenizer is None:
        raise RuntimeError("模型未加载")
    
    if isinstance(texts, str):
        texts = [texts]
    
    all_embeddings = []
    
    for i in range(0, len(texts), batch_size):
        batch_texts = texts[i:i + batch_size]
        
        # Tokenize
        encoded = tokenizer(
            batch_texts,
            padding=True,
            truncation=True,
            max_length=8192,
            return_tensors="pt"
        )
        
        # 移动到模型设备
        if hasattr(model, 'device'):
            encoded = {k: v.to(model.device) for k, v in encoded.items()}
        
        # 推理
        with torch.no_grad():
            outputs = model(**encoded)
        
        # Mean Pooling
        embeddings = mean_pooling(outputs, encoded["attention_mask"])
        
        # L2 归一化
        if normalize:
            embeddings = torch.nn.functional.normalize(embeddings, p=2, dim=1)
        
        # 转换为列表
        all_embeddings.extend(embeddings.cpu().numpy().tolist())
    
    return all_embeddings


@app.get("/")
def root():
    """服务健康检查"""
    return {
        "status": "ok",
        "model": model_name,
        "dimension": embedding_dimension,
        "device": "cuda" if torch.cuda.is_available() else "cpu"
    }


@app.get("/health")
def health():
    """健康检查"""
    return {"status": "healthy"}


@app.post("/embed")
def embed(request: dict):
    """
    文本嵌入接口
    
    Request Body:
    {
        "texts": ["文本1", "文本2"]  // 单文本或列表
    }
    
    Response:
    {
        "embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...]],
        "model": "Qwen/Qwen3-Embedding-0.6B",
        "dimension": 1024
    }
    """
    global model, embedding_dimension
    
    if model is None:
        raise HTTPException(status_code=500, detail="模型未加载")
    
    try:
        texts = request.get("texts")
        if texts is None:
            raise HTTPException(status_code=400, detail="缺少 'texts' 参数")
        
        if isinstance(texts, str):
            texts = [texts]
        
        if not isinstance(texts, list):
            raise HTTPException(status_code=400, detail="'texts' 必须是字符串或字符串列表")
        
        if len(texts) == 0:
            raise HTTPException(status_code=400, detail="'texts' 不能为空")
        
        # 编码
        start_time = time.time()
        embeddings = encode_texts(texts)
        elapsed = time.time() - start_time
        
        return {
            "embeddings": embeddings,
            "model": model_name,
            "dimension": embedding_dimension,
            "count": len(embeddings),
            "elapsed_ms": round(elapsed * 1000, 2)
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embed_batch")
def embed_batch(request: dict):
    """
    批量嵌入接口 (支持大批量)
    
    Request Body:
    {
        "texts": ["文本1", "文本2", ...],
        "batch_size": 32
    }
    """
    try:
        texts = request.get("texts", [])
        batch_size = request.get("batch_size", 32)
        
        if not texts:
            raise HTTPException(status_code=400, detail="'texts' 不能为空")
        
        all_embeddings = []
        total = len(texts)
        
        for i in range(0, total, batch_size):
            batch = texts[i:i + batch_size]
            embeddings = encode_texts(batch)
            all_embeddings.extend(embeddings)
        
        return {
            "embeddings": all_embeddings,
            "model": model_name,
            "dimension": embedding_dimension,
            "count": len(all_embeddings),
            "batches": (total + batch_size - 1) // batch_size
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/similarity")
def compute_similarity(request: dict):
    """
    计算文本相似度
    
    Request Body:
    {
        "text1": "文本1",
        "text2": "文本2"
    }
    """
    try:
        text1 = request.get("text1")
        text2 = request.get("text2")
        
        if not text1 or not text2:
            raise HTTPException(status_code=400, detail="需要 text1 和 text2")
        
        emb1 = encode_texts([text1])[0]
        emb2 = encode_texts([text2])[0]
        
        # 计算余弦相似度
        similarity = np.dot(emb1, emb2)
        
        return {
            "similarity": float(similarity),
            "text1_length": len(text1),
            "text2_length": len(text2)
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def main():
    parser = argparse.ArgumentParser(description="Qwen3-Embedding-0.6B 本地服务")
    parser.add_argument("--model", "-m", type=str, default=None,
                        help="模型路径或 Hugging Face 模型 ID")
    parser.add_argument("--port", "-p", type=int, default=5001,
                        help="服务端口 (默认: 5001)")
    parser.add_argument("--host", type=str, default="0.0.0.0",
                        help="服务地址 (默认: 0.0.0.0)")
    args = parser.parse_args()
    
    # 加载模型
    if not load_model(args.model):
        print("模型加载失败，退出")
        sys.exit(1)
    
    # 启动服务
    print()
    print(f"启动 Embedding 服务...")
    print(f"地址: http://{args.host}:{args.port}")
    print(f"API 文档: http://{args.host}:{args.port}/docs")
    print()
    
    uvicorn.run(
        app,
        host=args.host,
        port=args.port,
        log_level="info"
    )


if __name__ == "__main__":
    main()
