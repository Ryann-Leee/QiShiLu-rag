#!/usr/bin/env python3
"""
Qwen3-Embedding-0.6B 模型下载脚本
使用 HF Mirror 加速下载

使用方法:
    python download_model.py
"""

import os
import sys

# 设置 HF Mirror
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
os.environ["HF_HUB_ENABLE_HF_TRANSFER"] = "1"

from huggingface_hub import snapshot_download

MODEL_ID = "Qwen/Qwen3-Embedding-0.6B"
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models", "Qwen3-Embedding-0.6B")

def download_model():
    print(f"=" * 50)
    print(f"下载 Qwen3-Embedding-0.6B 模型")
    print(f"=" * 50)
    print(f"模型ID: {MODEL_ID}")
    print(f"保存路径: {MODEL_DIR}")
    print(f"使用镜像: https://hf-mirror.com")
    print()
    
    try:
        snapshot_download(
            repo_id=MODEL_ID,
            local_dir=MODEL_DIR,
            local_dir_use_symlinks=False,
            resume_download=True,
        )
        print()
        print(f"✓ 模型下载完成!")
        print(f"模型路径: {MODEL_DIR}")
        
        # 列出下载的文件
        print()
        print("下载的文件:")
        for f in os.listdir(MODEL_DIR):
            size = os.path.getsize(os.path.join(MODEL_DIR, f))
            size_mb = size / 1024 / 1024
            print(f"  - {f} ({size_mb:.1f} MB)")
        
        return True
        
    except Exception as e:
        print(f"✗ 下载失败: {e}")
        return False

if __name__ == "__main__":
    success = download_model()
    sys.exit(0 if success else 1)
