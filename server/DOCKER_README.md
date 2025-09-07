# MeetScribe Server - Docker Setup

This document explains how to run the MeetScribe server using Podman and Podman Compose.

**Note:** All Docker-related files are located in the `server/` directory.

## Prerequisites

- Podman installed
- Podman Compose installed
- Java 21 (for local development)

## Quick Start

### 1. Start the Server

```bash
# Using the convenience script (from server directory)
./scripts/start-server.sh

# Or manually (from server directory)
podman-compose up -d
```

### 2. Check Server Status

```bash
# From server directory
podman-compose ps
```

### 3. View Logs

```bash
# From server directory
podman-compose logs -f
```

### 4. Stop the Server

```bash
# Using the convenience script (from server directory)
./scripts/stop-server.sh

# Or manually (from server directory)
podman-compose down
```

## Configuration

### Environment Variables

The following environment variables can be set in the `docker-compose.yml` file:

- `APP_FRONTEND_URL`: Frontend URL for CORS configuration (default: http://localhost:4200)
- `SPRING_PROFILES_ACTIVE`: Spring profile to use (default: docker)

### Secrets

Create a `secrets.properties` file in the `server/src/main/resources/` directory with your sensitive configuration:

```properties
# OAuth Client IDs and Secrets
spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret
spring.security.oauth2.client.registration.linkedin.client-id=your-linkedin-client-id
spring.security.oauth2.client.registration.linkedin.client-secret=your-linkedin-client-secret
spring.security.oauth2.client.registration.facebook.client-id=your-facebook-client-id
spring.security.oauth2.client.registration.facebook.client-secret=your-facebook-client-secret

# API Keys
app.recall.api-key=your-recall-api-key
app.openai.api-key=your-openai-api-key
app.linkedin.api-key=your-linkedin-api-key

# JWT Secret
app.security.jwt.secret=your-jwt-secret

# Frontend URL
app.frontend-url=http://localhost:4200
```

## Data Persistence

- **Database**: SQLite database is persisted in `./server/data/meetScribe.sqlite`
- **Logs**: Application logs are stored in `./server/logs/`

## Health Checks

The container includes health checks that monitor the application status:

```bash
# Check health status
podman-compose ps

# View health check logs
podman inspect meetscribe-server | grep -A 10 Health
```

## Development

### Rebuild the Container

```bash
# Using the convenience script (from server directory)
./scripts/rebuild-server.sh

# Or manually (from server directory)
podman-compose down
podman-compose up -d --build
```

### Access the Container

```bash
# Execute commands in the running container
podman-compose exec server sh

# View container logs
podman-compose logs server
```

## Troubleshooting

### Common Issues

1. **Port 8080 already in use**: Change the port mapping in `docker-compose.yml`
2. **Permission issues**: Ensure the `server/data` and `server/logs` directories are writable
3. **Database issues**: Check that the SQLite database file is accessible

### Logs

```bash
# View all logs
podman-compose logs

# View logs for specific service
podman-compose logs server

# Follow logs in real-time
podman-compose logs -f server
```

### Clean Up

```bash
# Stop and remove containers
podman-compose down

# Remove images
podman rmi meetscribe-server:latest

# Remove volumes (WARNING: This will delete your database)
podman-compose down -v
```

## Production Considerations

1. **Security**: Ensure secrets are properly managed and not committed to version control
2. **Database**: Consider using a more robust database for production
3. **Monitoring**: Add proper monitoring and alerting
4. **Backup**: Implement regular database backups
5. **SSL/TLS**: Configure HTTPS for production deployments

## API Endpoints

Once the server is running, you can access:

- **Health Check**: http://localhost:8080/actuator/health
- **API Base**: http://localhost:8080/api/
- **OAuth Callbacks**: http://localhost:8080/oauth2/callback
