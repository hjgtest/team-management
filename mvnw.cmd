@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script (simplified)
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties
set MAVEN_HOME_DIR=%USERPROFILE%\.m2\wrapper\apache-maven-3.8.8

@REM Check if Maven is already downloaded
if exist "%MAVEN_HOME_DIR%\bin\mvn.cmd" goto runMaven

echo Maven not found locally, downloading...

@REM Read distribution URL from properties
set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.8/apache-maven-3.8.8-bin.zip
set WRAPPER_DIR=%USERPROFILE%\.m2\wrapper
set ZIP_FILE=%WRAPPER_DIR%\apache-maven-3.8.8-bin.zip

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

@REM Download using PowerShell
echo Downloading Maven 3.8.8 ...
powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%' }"

if not exist "%ZIP_FILE%" (
    echo [ERROR] Failed to download Maven. Please check your network.
    echo         URL: %DOWNLOAD_URL%
    exit /b 1
)

@REM Extract using PowerShell
echo Extracting Maven...
powershell -Command "& { Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%WRAPPER_DIR%' -Force }"

if not exist "%MAVEN_HOME_DIR%\bin\mvn.cmd" (
    echo [ERROR] Failed to extract Maven.
    exit /b 1
)

echo Maven 3.8.8 installed to %MAVEN_HOME_DIR%
del "%ZIP_FILE%" 2>nul
echo.

:runMaven
set MAVEN_CMD=%MAVEN_HOME_DIR%\bin\mvn.cmd
set JAVA_EXE=java

@REM Find java.exe
if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

@REM Run Maven with all passed arguments
"%MAVEN_CMD%" %*

exit /b %ERRORLEVEL%
