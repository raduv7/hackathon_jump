# 🔒 Private Docker Image Deployment Guide

## 🚨 IMPORTANT: Remove Secrets First!

Before publishing ANY image, ensure secrets are not included:

```bash
# Check if secrets are in the image
podman run --rm raduandreivaida/meetscribe-server:latest cat /app/secrets.properties 2>/dev/null || echo "✅ No secrets found"
```

## 🏷️ Private Registry Options

### Option 1: Docker Hub Private Repository (Free)

1. **Create private repository on Docker Hub:**
   - Go to https://hub.docker.com
   - Click "Create Repository"
   - Name: `meetscribe-server`
   - Set to **Private** ✅
   - Create repository

2. **Tag and push your image:**
   ```bash
   # Tag for Docker Hub
   podman tag localhost/2025_09_05_jump_server:latest raduandreivaida/meetscribe-server:latest
   
   # Push to private repository
   podman push raduandreivaida/meetscribe-server:latest
   ```

3. **Pull and run on deployment:**
   ```bash
   # On deployment server
   podman pull raduandreivaida/meetscribe-server:latest
   podman run -d --name meetscribe-server \
     -p 8080:8080 \
     -e GOOGLE_CLIENT_ID="your-id" \
     -e GOOGLE_CLIENT_SECRET="your-secret" \
     -e JWT_SECRET="your-jwt-secret" \
     -e RECALL_API_KEY="your-key" \
     -e OPENAI_API_KEY="your-key" \
     raduandreivaida/meetscribe-server:latest
   ```

### Option 2: GitHub Container Registry (Free)

1. **Create GitHub Personal Access Token:**
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Generate token with `write:packages` permission

2. **Login and push:**
   ```bash
   # Login to GitHub Container Registry
   echo $GITHUB_TOKEN | podman login ghcr.io -u raduandreivaida --password-stdin
   
   # Tag for GitHub
   podman tag localhost/2025_09_05_jump_server:latest ghcr.io/raduandreivaida/meetscribe-server:latest
   
   # Push to private repository
   podman push ghcr.io/raduandreivaida/meetscribe-server:latest
   ```

3. **Make repository private:**
   - Go to your GitHub repository
   - Settings → Packages → meetscribe-server
   - Change visibility to Private

### Option 3: AWS ECR (Amazon Elastic Container Registry)

1. **Create ECR repository:**
   ```bash
   aws ecr create-repository --repository-name meetscribe-server --region us-east-1
   ```

2. **Login and push:**
   ```bash
   # Get login token
   aws ecr get-login-password --region us-east-1 | podman login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
   
   # Tag and push
   podman tag localhost/2025_09_05_jump_server:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/meetscribe-server:latest
   podman push <account-id>.dkr.ecr.us-east-1.amazonaws.com/meetscribe-server:latest
   ```

### Option 4: Google Container Registry (GCR)

1. **Enable GCR API and login:**
   ```bash
   gcloud auth configure-docker
   ```

2. **Tag and push:**
   ```bash
   podman tag localhost/2025_09_05_jump_server:latest gcr.io/your-project-id/meetscribe-server:latest
   podman push gcr.io/your-project-id/meetscribe-server:latest
   ```

## 🛡️ Security Best Practices

### 1. Remove Secrets Before Building

Create a production Dockerfile without secrets:

```dockerfile
# Dockerfile.prod
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew build --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache sqlite
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN mkdir -p /app/data /app/logs && \
    chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
```

### 2. Build Production Image

```bash
# Build without secrets
podman build -f Dockerfile.prod -t meetscribe-server:prod .

# Verify no secrets
podman run --rm meetscribe-server:prod ls -la /app/ | grep secrets || echo "✅ No secrets"
```

### 3. Environment Variables for Secrets

```bash
# Create .env file for deployment
cat > .env << EOF
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
LINKEDIN_CLIENT_ID=your-linkedin-client-id
LINKEDIN_CLIENT_SECRET=your-linkedin-client-secret
FACEBOOK_CLIENT_ID=your-facebook-client-id
FACEBOOK_CLIENT_SECRET=your-facebook-client-secret
JWT_SECRET=your-jwt-secret
RECALL_API_KEY=your-recall-api-key
OPENAI_API_KEY=your-openai-api-key
APP_FRONTEND_URL=https://your-frontend-domain.com
APP_BASE_URL=https://your-api-domain.com
EOF
```

## 🚀 Deployment Commands

### Using Docker Compose with Private Image

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  server:
    image: raduandreivaida/meetscribe-server:latest  # Your private image
    container_name: meetscribe-server
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/meetScribe.sqlite
      - APP_FRONTEND_URL=${APP_FRONTEND_URL}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - LINKEDIN_CLIENT_ID=${LINKEDIN_CLIENT_ID}
      - LINKEDIN_CLIENT_SECRET=${LINKEDIN_CLIENT_SECRET}
      - FACEBOOK_CLIENT_ID=${FACEBOOK_CLIENT_ID}
      - FACEBOOK_CLIENT_SECRET=${FACEBOOK_CLIENT_SECRET}
      - JWT_SECRET=${JWT_SECRET}
      - RECALL_API_KEY=${RECALL_API_KEY}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    restart: unless-stopped
```

### Deploy with Environment Variables

```bash
# Load environment variables
source .env

# Deploy with private image
podman-compose -f docker-compose.prod.yml up -d
```

## 🔍 Verification Steps

1. **Check image doesn't contain secrets:**
   ```bash
   podman run --rm raduandreivaida/meetscribe-server:latest find /app -name "*secret*" -o -name "*.properties" | grep -v application.properties
   ```

2. **Test deployment:**
   ```bash
   # Pull and run with environment variables
   podman pull raduandreivaida/meetscribe-server:latest
   podman run -d --name test-server \
     -p 8081:8080 \
     -e GOOGLE_CLIENT_ID="test" \
     -e JWT_SECRET="test" \
     raduandreivaida/meetscribe-server:latest
   
   # Check if it starts
   sleep 10
   curl http://localhost:8081/actuator/health
   ```

## 📋 Quick Start Checklist

- [ ] Remove secrets from Docker image
- [ ] Choose private registry (Docker Hub recommended)
- [ ] Create private repository
- [ ] Build production image
- [ ] Tag for your registry
- [ ] Push to private repository
- [ ] Set up environment variables
- [ ] Deploy with secrets as environment variables
- [ ] Verify deployment works
- [ ] Monitor for security issues

## 🆘 Emergency: If Secrets Are Exposed

1. **Immediately delete the image from registry**
2. **Rotate all exposed secrets**
3. **Update OAuth configurations**
4. **Regenerate API keys**
5. **Rebuild and redeploy with new secrets**

Remember: **Private doesn't mean secure by default - you still need proper secret management!** 🛡️
