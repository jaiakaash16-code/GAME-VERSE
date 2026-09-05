#!/bin/bash

echo "🔫 Starting One Bullet Game..."
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+"
    exit 1
fi

echo "✅ Java found: $(java -version 2>&1 | head -1)"

# Start backend
echo ""
echo "🚀 Starting Java backend..."
cd backend

# Check if Maven exists, if not use the downloaded one
if ! command -v mvn &> /dev/null; then
    if [ -d "apache-maven-3.9.6" ]; then
        export PATH="$(pwd)/apache-maven-3.9.6/bin:$PATH"
    else
        echo "📦 Downloading Maven..."
        curl -L -o maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"
        tar xzf maven.tar.gz
        rm maven.tar.gz
        export PATH="$(pwd)/apache-maven-3.9.6/bin:$PATH"
    fi
fi

# Kill any existing process on port 8080
netstat -ano | grep :8080 | head -1 | awk '{print $5}' | cut -d'/' -f1 | xargs -r taskkill //F //PID 2>/dev/null

# Start Spring Boot
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 15

# Test backend
if curl -s http://localhost:8080/api/players -X POST -H "Content-Type: application/json" -d '{"username":"test"}' > /dev/null 2>&1; then
    echo "✅ Backend running at http://localhost:8080"
else
    echo "⚠️  Backend may still be starting..."
fi

# Start frontend
echo ""
echo "🌐 Starting frontend server..."
cd ../frontend

# Kill any existing process on port 3000
netstat -ano | grep :3000 | head -1 | awk '{print $5}' | cut -d'/' -f1 | xargs -r taskkill //F //PID 2>/dev/null

python -m http.server 3000 &
FRONTEND_PID=$!

sleep 2
echo "✅ Frontend running at http://localhost:3000"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎮 ONE BULLET is ready!"
echo ""
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080"
echo ""
echo "   Press Ctrl+C to stop both servers"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Wait for user to press Ctrl+C
trap "echo ''; echo '🛑 Shutting down...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM
wait
