#!/bin/bash

# Stop the MeetScribe server
echo "Stopping MeetScribe server..."

# Navigate to server directory (where this script is located)
cd "$(dirname "$0")/.."

podman-compose down

echo "Server stopped!"
