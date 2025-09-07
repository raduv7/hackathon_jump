#!/bin/bash

# Convenience script to stop the server from project root
echo "Stopping MeetScribe server..."

cd server
./scripts/stop-server.sh
