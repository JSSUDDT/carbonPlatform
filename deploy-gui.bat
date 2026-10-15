@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
start "" powershell.exe -STA -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-gui.ps1"
endlocal
