#!/bin/bash

# Start the MeetScribe server using Podman Compose
echo "Starting MeetScribe server with Podman Compose..."

# Navigate to server directory (where this script is located)
cd "$(dirname "$0")/.."

# Create necessary directories
mkdir -p data logs

# Start the services (with linux/amd64 platform)
podman-compose up -d

echo "Server started! Check status with: podman-compose ps"
echo "View logs with: podman-compose logs -f"
echo "Stop server with: podman-compose down"
