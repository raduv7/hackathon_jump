#!/bin/bash

# Script to publish MeetScribe server as private Docker image
# This script ensures no secrets are included in the published image

set -e  # Exit on any error

echo "🔒 Publishing MeetScribe Server as Private Image"
echo "================================================"

# Configuration
REGISTRY_USER="raduandreivaida"
IMAGE_NAME="meetscribe-server"
TAG="latest"
FULL_IMAGE_NAME="${REGISTRY_USER}/${IMAGE_NAME}:${TAG}"

# Check if we're in the right directory
if [ ! -f "Dockerfile.prod" ]; then
    echo "❌ Error: Dockerfile.prod not found. Run this script from the server directory."
    exit 1
fi

echo "📋 Configuration:"
echo "  Registry: Docker Hub (docker.io)"
echo "  Image: ${FULL_IMAGE_NAME}"
echo "  Dockerfile: Dockerfile.prod (no secrets)"
echo ""

# Step 1: Build production image (no secrets) for linux/amd64
echo "🔨 Building production image for linux/amd64 platform..."
podman build --platform linux/amd64 -f Dockerfile.prod -t ${IMAGE_NAME}:prod .

# Step 2: Verify no secrets in image
echo "🔍 Verifying no secrets in image..."
SECRETS_FOUND=$(podman run --rm ${IMAGE_NAME}:prod find /app -name "*secret*" -o -name "secrets.properties" 2>/dev/null | wc -l)
if [ "$SECRETS_FOUND" -gt 0 ]; then
    echo "❌ Error: Secrets found in image! Aborting."
    podman run --rm ${IMAGE_NAME}:prod find /app -name "*secret*" -o -name "secrets.properties"
    exit 1
else
    echo "✅ No secrets found in image"
fi

# Step 3: Tag for registry
echo "🏷️  Tagging image for registry..."
podman tag ${IMAGE_NAME}:prod ${FULL_IMAGE_NAME}

# Step 4: Login to Docker Hub
echo "🔐 Logging in to Docker Hub..."
if ! podman login docker.io; then
    echo "❌ Error: Failed to login to Docker Hub"
    echo "   Make sure you have a Docker Hub account and valid credentials"
    exit 1
fi

# Step 5: Push to registry
echo "📤 Pushing image to Docker Hub..."
if podman push ${FULL_IMAGE_NAME}; then
    echo "✅ Successfully pushed ${FULL_IMAGE_NAME}"
else
    echo "❌ Error: Failed to push image"
    exit 1
fi

echo ""
echo "🎉 Image published successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Go to https://hub.docker.com/r/${REGISTRY_USER}/${IMAGE_NAME}"
echo "2. Make sure the repository is set to PRIVATE"
echo "3. Use the following commands to deploy:"
echo ""
echo "   # Pull the image"
echo "   podman pull ${FULL_IMAGE_NAME}"
echo ""
echo "   # Run with environment variables"
echo "   podman run -d --name meetscribe-server \\"
echo "     -p 8080:8080 \\"
echo "     -e GOOGLE_CLIENT_ID=\"your-google-client-id\" \\"
echo "     -e GOOGLE_CLIENT_SECRET=\"your-google-client-secret\" \\"
echo "     -e JWT_SECRET=\"your-jwt-secret\" \\"
echo "     -e RECALL_API_KEY=\"your-recall-api-key\" \\"
echo "     -e OPENAI_API_KEY=\"your-openai-api-key\" \\"
echo "     ${FULL_IMAGE_NAME}"
echo ""
echo "   # Or use docker-compose.prod.yml"
echo "   podman-compose -f docker-compose.prod.yml up -d"
echo ""
echo "⚠️  Remember: Set all secrets as environment variables!"
