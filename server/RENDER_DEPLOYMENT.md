# 🚀 Render Deployment Guide

## 📋 Prerequisites

- GitHub repository with your code
- Docker Hub account (for private images)
- Render account

## 🔧 Platform Configuration

Your Docker images are now configured for `linux/amd64` platform, which is required for Render deployment.

## 🚀 Deployment Options

### Option 1: Direct Git Deployment (Recommended)

1. **Connect Repository:**
   - Go to [Render Dashboard](https://dashboard.render.com)
   - Click "New +" → "Web Service"
   - Connect your GitHub repository
   - Select the repository and branch

2. **Configure Service:**
   ```
   Name: meetscribe-server
   Environment: Docker
   Dockerfile Path: ./server/Dockerfile.prod
   Docker Context: ./server
   Plan: Starter (Free)
   Region: Oregon (US West)
   ```

3. **Set Environment Variables:**
   ```
   SPRING_PROFILES_ACTIVE=docker
   SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/meetScribe.sqlite
   APP_FRONTEND_URL=https://your-frontend-domain.com
   APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
   APP_BASE_URL=https://your-service-name.onrender.com
   GOOGLE_CLIENT_ID=your-google-client-id
   GOOGLE_CLIENT_SECRET=your-google-client-secret
   LINKEDIN_CLIENT_ID=your-linkedin-client-id
   LINKEDIN_CLIENT_SECRET=your-linkedin-client-secret
   FACEBOOK_CLIENT_ID=your-facebook-client-id
   FACEBOOK_CLIENT_SECRET=your-facebook-client-secret
   JWT_SECRET=your-jwt-secret
   RECALL_API_KEY=your-recall-api-key
   OPENAI_API_KEY=your-openai-api-key
   ```

4. **Deploy:**
   - Click "Create Web Service"
   - Render will build and deploy automatically

### Option 2: Using render.yaml

1. **Use the provided render.yaml:**
   - The `render.yaml` file is already configured
   - Just set the environment variables in Render dashboard

2. **Deploy:**
   - Render will automatically detect and use the `render.yaml` file

### Option 3: Private Docker Image

1. **Build and push private image:**
   ```bash
   cd server
   ./scripts/publish-private.sh
   ```

2. **Deploy from private image:**
   - In Render, select "Private Image" instead of "Git Repository"
   - Image: `raduandreivaida/meetscribe-server:latest`
   - Set environment variables as above

## 🔧 Build Configuration

### Dockerfile.prod Features:
- ✅ `linux/amd64` platform support
- ✅ No secrets included
- ✅ Multi-stage build for optimization
- ✅ Non-root user for security
- ✅ Health checks enabled
- ✅ Production profile activated

### Build Process:
```dockerfile
# Build stage
FROM --platform=linux/amd64 eclipse-temurin:21-jdk-alpine AS builder
# ... build steps

# Runtime stage  
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
# ... runtime configuration
```

## 🌐 URL Configuration

### Backend URL:
- **Development**: `http://localhost:8080`
- **Render**: `https://your-service-name.onrender.com`

### Frontend Configuration:
Update your frontend's `NG_APP_API_BASE_URL`:
```bash
NG_APP_API_BASE_URL=https://your-service-name.onrender.com
```

## 🔐 OAuth Configuration

Update your OAuth provider settings:

### Google OAuth:
- **Authorized JavaScript origins**: `https://your-frontend-domain.com`
- **Authorized redirect URIs**: `https://your-service-name.onrender.com/oauth2/callback`

### LinkedIn OAuth:
- **Authorized redirect URLs**: `https://your-service-name.onrender.com/oauth2/callback`

### Facebook OAuth:
- **Valid OAuth Redirect URIs**: `https://your-service-name.onrender.com/oauth2/callback`

## 📊 Monitoring & Health Checks

### Health Check Endpoint:
```
https://your-service-name.onrender.com/actuator/health
```

### Render Health Checks:
- **Path**: `/actuator/health`
- **Interval**: 30 seconds
- **Timeout**: 10 seconds

## 🚨 Common Issues & Solutions

### Build Failures:
**Problem**: Platform architecture mismatch
**Solution**: Ensure `--platform=linux/amd64` is specified in Dockerfile

### CORS Errors:
**Problem**: Frontend can't access backend
**Solution**: 
1. Check `APP_CORS_ALLOWED_ORIGINS` environment variable
2. Ensure frontend URL is exactly correct
3. Verify HTTPS is used in production

### OAuth Issues:
**Problem**: OAuth redirects fail
**Solution**:
1. Update OAuth provider settings with Render URL
2. Check `APP_BASE_URL` environment variable
3. Ensure redirect URI matches: `https://your-service-name.onrender.com/oauth2/callback`

### Database Issues:
**Problem**: SQLite file not persisting
**Solution**: 
1. Use Render's persistent disk for data
2. Or migrate to PostgreSQL (recommended for production)

## 🔄 Deployment Process

1. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Configure for Render deployment"
   git push origin main
   ```

2. **Deploy on Render:**
   - Connect repository
   - Set environment variables
   - Deploy

3. **Update OAuth Settings:**
   - Update redirect URIs with Render URL
   - Test OAuth flow

4. **Update Frontend:**
   - Set `NG_APP_API_BASE_URL` to Render URL
   - Deploy frontend

## 📋 Deployment Checklist

- [ ] Docker images built for `linux/amd64`
- [ ] Environment variables configured
- [ ] OAuth provider settings updated
- [ ] Frontend API URL updated
- [ ] Health checks working
- [ ] CORS configuration correct
- [ ] Database persistence configured
- [ ] SSL/HTTPS enabled
- [ ] Monitoring set up

## 🎯 Render-Specific Tips

1. **Free Tier Limitations:**
   - Service sleeps after 15 minutes of inactivity
   - Cold start takes ~30 seconds
   - Consider upgrading for production

2. **Environment Variables:**
   - Set all secrets in Render dashboard
   - Use Render's environment variable management

3. **Logs:**
   - Access logs in Render dashboard
   - Use structured logging for better monitoring

4. **Scaling:**
   - Start with Starter plan
   - Upgrade as needed for production

## 🚀 Quick Start Commands

```bash
# Build for Render
cd server
podman build --platform linux/amd64 -f Dockerfile.prod -t meetscribe-server:render .

# Test locally
podman run -d --name test-render \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e GOOGLE_CLIENT_ID="test" \
  -e JWT_SECRET="test" \
  meetscribe-server:render

# Check health
curl http://localhost:8080/actuator/health
```

Your application is now ready for Render deployment! 🎉
