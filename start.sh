#!/bin/bash
DIR="/opt/sit/carbonPlatform"
cd "$DIR" || exit 1
echo "Stopping existing carbon processes..."
pkill -f 'carbon-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
sleep 3
pkill -9 -f 'carbon-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
sleep 2

echo "Starting carbon in background..."
nohup java -Duser.timezone=Asia/Taipei -jar "$DIR/carbon-0.0.1-SNAPSHOT.jar" --spring.profiles.active=sit > "$DIR/logs/console.log" 2>&1 &
PID=$!
echo $PID > "$DIR/carbon.pid"
echo "Carbon started with PID $PID"
