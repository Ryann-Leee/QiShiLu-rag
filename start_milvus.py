#!/usr/bin/env python3
"""
Milvus Lite standalone server for RAG system
Version: 2.5.1
"""

import os
import sys
import time
import signal
import atexit

# Global server manager instance
_server_manager = None

def signal_handler(signum, frame):
    """Handle shutdown signals"""
    print("\nReceived shutdown signal, releasing Milvus Lite...")
    cleanup()
    sys.exit(0)

def cleanup():
    """Cleanup resources on exit"""
    global _server_manager
    if _server_manager:
        try:
            _server_manager.release_all()
            print("Milvus Lite released successfully")
        except Exception as e:
            print(f"Warning: Error during cleanup: {e}")

def start_milvus_lite():
    """Start Milvus Lite server"""
    global _server_manager
    
    print("Starting Milvus Lite server (v2.5.2rc1)...")
    
    # Data directory
    data_dir = os.environ.get('MILVUS_DATA_DIR', '/tmp/milvus_data')
    os.makedirs(data_dir, exist_ok=True)
    
    try:
        from milvus_lite.server_manager import ServerManager
        
        # Create server manager
        _server_manager = ServerManager()
        
        # Start server and get URI
        uri = _server_manager.start_and_get_uri(data_dir)
        
        print(f"Milvus Lite server started successfully")
        print(f"  URI: {uri}")
        print(f"  Data directory: {data_dir}")
        print(f"  Port: 19530")
        
        # Register cleanup handlers
        atexit.register(cleanup)
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
        
        # Keep running
        while True:
            time.sleep(1)
            
    except ImportError:
        print("Error: milvus-lite not installed")
        print("Please run: pip install milvus-lite==2.5.2rc1")
        sys.exit(1)
        
    except Exception as e:
        print(f"Error starting Milvus Lite: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    start_milvus_lite()
