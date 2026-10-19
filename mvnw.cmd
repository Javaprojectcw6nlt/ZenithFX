@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "MAVEN_HOME=%USERPROFILE%\.m2\zenithfx-maven\apache-maven-%MAVEN_VERSION%"
if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runmaven
echo Maven is not installed globally. Downloading Apache Maven %MAVEN_VERSION% for this project...
if not exist "%USERPROFILE%\.m2\zenithfx-maven" mkdir "%USERPROFILE%\.m2\zenithfx-maven"
set "ZIPFILE=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ZIPFILE%'"
if errorlevel 1 (
  echo Download failed. Check your internet connection, then try again.
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIPFILE%' -DestinationPath '%USERPROFILE%\.m2\zenithfx-maven' -Force"
if errorlevel 1 exit /b 1
:runmaven
call "%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
