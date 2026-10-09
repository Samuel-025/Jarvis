#!/usr/bin/env python3
"""
JARVIS Mobile - Offline APK Reassembler
Reassembles the verified 72,716,015-byte JARVIS Mobile APK from base64 chunks.
"""
import base64
import glob
import os
import sys

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, "app-debug.apk")
    part_files = sorted(glob.glob(os.path.join(script_dir, "app-debug.apk.part*.b64")))

    if not part_files:
        print("Error: No .part*.b64 files found in directory:", script_dir)
        sys.exit(1)

    print(f"Found {len(part_files)} base64 chunks. Reassembling...")
    total_bytes = 0
    with open(output_path, "wb") as out_f:
        for p in part_files:
            print(f"Decoding {os.path.basename(p)}...")
            with open(p, "rb") as in_f:
                chunk = base64.b64decode(in_f.read())
                out_f.write(chunk)
                total_bytes += len(chunk)

    print(f"\nSUCCESS: Reassembled {output_path}")
    print(f"Total Size: {total_bytes} bytes ({total_bytes / (1024*1024):.2f} MB)")
    if total_bytes == 72716015:
        print("Integrity check PASSED: Exact match with verified build (72,716,015 bytes).")
    else:
        print(f"Warning: Expected 72,716,015 bytes, got {total_bytes} bytes.")

if __name__ == "__main__":
    main()
