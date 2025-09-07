#!/bin/bash

# Rebuild and restart the MeetScribe server
echo "Rebuilding MeetScribe server..."

# Navigate to server directory (where this script is located)
cd "$(dirname "$0")/.."

# Stop existing containers
podman-compose down

# Remove the image to force rebuild
podman rmi meetscribe-server:latest 2>/dev/null || true

# Rebuild and start (with linux/amd64 platform)
podman-compose up -d --build

echo "Server rebuilt and started!"
echo "Check status with: podman-compose ps"
echo "View logs with: podman-compose logs -f"
