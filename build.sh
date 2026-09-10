#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

IMAGE_NAME="rtsp-android-builder"
OUTPUT_DIR="output"

echo "==> Building Docker image (this compiles app-debug.apk)..."
docker build -t "$IMAGE_NAME" .

echo "==> Extracting APK from the container..."
mkdir -p "$OUTPUT_DIR"
CONTAINER_ID=$(docker create "$IMAGE_NAME")
docker cp "$CONTAINER_ID:/project/app/build/outputs/apk/debug/app-debug.apk" "$OUTPUT_DIR/app-debug.apk"
docker rm "$CONTAINER_ID" > /dev/null

echo "==> Done: $OUTPUT_DIR/app-debug.apk"
