@echo off
chcp 65001 >nul

echo Stopping existing containers...
docker-compose down -v
docker system prune -f

echo Starting infrastructure services...
docker-compose up -d redis elasticsearch nacos

echo Waiting for infrastructure to be ready...
timeout /t 60 /nobreak >nul

echo Building project...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Starting microservices...
docker-compose up -d gym-auth gym-gateway gym-server

echo Services started successfully!
echo Gateway: http://localhost:8888
echo Auth: http://localhost:8081
echo Server: http://localhost:8080
echo Nacos: http://localhost:8848/nacos

docker-compose ps

pause