# 🚀 Frontend-Backend Deployment Guide

## 🌐 Communication Setup

Your deployed frontend needs to communicate with your deployed backend. Here's how to configure it properly.

## 📋 Configuration Overview

### Frontend → Backend Communication
- **Development**: `http://localhost:8080`
- **Production**: `https://your-backend-domain.com:8081`

### Backend CORS Configuration
- **Development**: `http://localhost:4200`
- **Production**: `https://your-frontend-domain.com`

## 🔧 Frontend Configuration

### 1. Environment Variables

Create `.env` file in `web-client/` directory:

```bash
# For development
NG_APP_API_BASE_URL=http://localhost:8080

# For production
NG_APP_API_BASE_URL=https://your-backend-domain.com:8081
```

### 2. Build with Environment Variables

```bash
# Development build
ng build

# Production build with environment variables
NG_APP_API_BASE_URL=https://your-backend-domain.com:8081 ng build --prod
```

### 3. Deployment Platforms

#### Render (Frontend)
1. Set environment variable: `NG_APP_API_BASE_URL=https://your-backend-domain.com:8081`
2. Build command: `ng build --prod`
3. Publish directory: `dist/web-client/browser`

#### Vercel (Frontend)
1. Add environment variable in dashboard
2. Build command: `ng build --prod`
3. Output directory: `dist/web-client/browser`

## 🔧 Backend Configuration

### 1. Environment Variables

Set these environment variables for your backend:

```bash
# Frontend URL for CORS
APP_FRONTEND_URL=https://your-frontend-domain.com
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com

# Backend URL
APP_BASE_URL=https://your-backend-domain.com:8081

# OAuth redirect URLs (will be auto-configured)
# These will be: https://your-backend-domain.com:8081/oauth2/callback
```

### 2. OAuth Provider Configuration

Update your OAuth app settings:

#### Google OAuth
- **Authorized JavaScript origins**: `https://your-frontend-domain.com`
- **Authorized redirect URIs**: `https://your-backend-domain.com:8081/oauth2/callback`

#### LinkedIn OAuth
- **Authorized redirect URLs**: `https://your-backend-domain.com:8081/oauth2/callback`

#### Facebook OAuth
- **Valid OAuth Redirect URIs**: `https://your-backend-domain.com:8081/oauth2/callback`

### 3. Deployment Commands

```bash
# Using docker-compose.prod.yml
podman-compose -f docker-compose.prod.yml up -d

# Or with environment variables
podman run -d --name meetscribe-server \
  -p 8081:8080 \
  -e APP_FRONTEND_URL="https://your-frontend-domain.com" \
  -e APP_CORS_ALLOWED_ORIGINS="https://your-frontend-domain.com" \
  -e APP_BASE_URL="https://your-backend-domain.com:8081" \
  -e GOOGLE_CLIENT_ID="your-google-client-id" \
  -e GOOGLE_CLIENT_SECRET="your-google-client-secret" \
  -e JWT_SECRET="your-jwt-secret" \
  -e RECALL_API_KEY="your-recall-api-key" \
  -e OPENAI_API_KEY="your-openai-api-key" \
  raduandreivaida/meetscribe-server:latest
```

## 🔍 Testing the Connection

### 1. Test Backend Health
```bash
curl https://your-backend-domain.com:8081/actuator/health
```

### 2. Test CORS
```bash
curl -H "Origin: https://your-frontend-domain.com" \
     -H "Access-Control-Request-Method: GET" \
     -H "Access-Control-Request-Headers: X-Requested-With" \
     -X OPTIONS \
     https://your-backend-domain.com:8081/api/events
```

### 3. Test Frontend API Call
Open browser dev tools and check if API calls are successful.

## 🚨 Common Issues & Solutions

### CORS Errors
**Problem**: `Access to fetch at 'https://backend' from origin 'https://frontend' has been blocked by CORS policy`

**Solution**: 
1. Check `APP_CORS_ALLOWED_ORIGINS` environment variable
2. Ensure frontend URL is exactly correct (no trailing slash)
3. Verify backend is running and accessible

### OAuth Redirect Issues
**Problem**: OAuth login redirects to wrong URL

**Solution**:
1. Update OAuth provider settings with correct redirect URI
2. Check `APP_BASE_URL` environment variable
3. Ensure redirect URI matches: `https://your-backend-domain.com:8081/oauth2/callback`

### API Connection Issues
**Problem**: Frontend can't reach backend API

**Solution**:
1. Check `NG_APP_API_BASE_URL` environment variable
2. Verify backend is running on correct port
3. Test backend health endpoint
4. Check firewall/network settings

## 📋 Deployment Checklist

### Frontend
- [ ] Set `NG_APP_API_BASE_URL` environment variable
- [ ] Build with production configuration
- [ ] Deploy to hosting platform
- [ ] Test API connectivity

### Backend
- [ ] Set `APP_FRONTEND_URL` environment variable
- [ ] Set `APP_CORS_ALLOWED_ORIGINS` environment variable
- [ ] Set `APP_BASE_URL` environment variable
- [ ] Update OAuth provider settings
- [ ] Deploy with private Docker image
- [ ] Test health endpoint
- [ ] Test CORS configuration

### OAuth Providers
- [ ] Update Google OAuth settings
- [ ] Update LinkedIn OAuth settings
- [ ] Update Facebook OAuth settings
- [ ] Test OAuth login flow

## 🔗 Example URLs

### Development
- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- API calls: `http://localhost:8080/api/*`

### Production
- Frontend: `https://meetscribe-frontend.onrender.com`
- Backend: `https://meetscribe-backend.onrender.com:8081`
- API calls: `https://meetscribe-backend.onrender.com:8081/api/*`

## 🛡️ Security Notes

1. **HTTPS Only**: Use HTTPS in production
2. **CORS**: Only allow your frontend domain
3. **Environment Variables**: Never commit secrets to git
4. **OAuth**: Use secure redirect URIs
5. **JWT**: Use strong secrets and proper expiration

Remember: **Test thoroughly in development before deploying to production!** 🚀
