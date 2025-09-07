#!/bin/bash

# Convenience script to rebuild the server from project root
echo "Rebuilding MeetScribe server..."

cd server
./scripts/rebuild-server.sh
