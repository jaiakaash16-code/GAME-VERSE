@echo off
setlocal
set ROOT=%~dp0
set BACKEND=%ROOT%backend
set FRONTEND=%ROOT%frontend

echo [1/3] Compiling Java backend...
javac -d "%BACKEND%" "%BACKEND%\MirrorWorldServer.java"
if errorlevel 1 (
  echo BUILD FAILED.
  exit /b 1
)
echo OK.

echo [2/3] Verifying frontend files...
if not exist "%FRONTEND%\index.html" (
  echo Missing frontend/index.html
  exit /b 1
)
if not exist "%FRONTEND%\game.js" (
  echo Missing frontend/game.js
  exit /b 1
)
if not exist "%FRONTEND%\style.css" (
  echo Missing frontend/style.css
  exit /b 1
)
echo OK.

echo [3/3] Starting server on http://localhost:8080/
cd /d "%BACKEND%"
java MirrorWorldServer