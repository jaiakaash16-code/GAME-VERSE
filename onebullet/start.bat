@echo off
echo 🔫 Starting One Bullet Game...
echo.

REM Check Java
java -version 2>&1 | findstr /i "version" > nul
if errorlevel 1 (
    echo ❌ Java not found. Please install Java 17+
    pause
    exit /b 1
)

echo ✅ Java found

REM Start backend
echo.
echo 🚀 Starting Java backend...
cd backend

REM Check if Maven exists
if not exist "apache-maven-3.9.6" (
    echo 📦 Downloading Maven...
    curl -L -o maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"
    tar xzf maven.tar.gz
    del maven.tar.gz
)

set PATH=%cd%\apache-maven-3.9.6\bin;%PATH%

REM Kill any existing process on port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /F /PID %%a 2>nul

REM Start Spring Boot
start "One Bullet Backend" mvn spring-boot:run

REM Wait for backend to start
echo ⏳ Waiting for backend to start...
timeout /t 15 /nobreak > nul

echo ✅ Backend starting at http://localhost:8080

REM Start frontend
echo.
echo 🌐 Starting frontend server...
cd ..\frontend

REM Kill any existing process on port 3000
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING') do taskkill /F /PID %%a 2>nul

start "One Bullet Frontend" python -m http.server 3000

timeout /t 2 /nobreak > nul
echo ✅ Frontend running at http://localhost:3000

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo 🎮 ONE BULLET is ready!
echo.
echo    Frontend: http://localhost:3000
echo    Backend:  http://localhost:8080
echo.
echo    Close this window or press any key to exit
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

pause > nul
