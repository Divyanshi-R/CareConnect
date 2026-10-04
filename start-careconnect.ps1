# CareConnect local development startup

$projectRoot = "D:\VSCode\CareConnect"

# Check backend
$backendRunning = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue

if ($backendRunning) {
    Write-Host "Backend is already running on port 8081." -ForegroundColor Yellow
}
else {
    Write-Host "Starting CareConnect backend..." -ForegroundColor Green

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$projectRoot'; mvn.cmd -f backend\pom.xml spring-boot:run"
    )
}

# Check frontend
$frontendRunning = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue

if ($frontendRunning) {
    Write-Host "Frontend is already running on port 5173." -ForegroundColor Yellow
}
else {
    Write-Host "Starting CareConnect frontend..." -ForegroundColor Green

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$projectRoot\frontend'; npm.cmd run dev"
    )
}

Write-Host ""
Write-Host "CareConnect startup check completed." -ForegroundColor Green
Write-Host "Backend:  http://localhost:8081" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Cyan