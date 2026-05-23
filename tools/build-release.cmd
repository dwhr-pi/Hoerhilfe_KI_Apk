@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "JAVA_EXE=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
set "ANDROID_SDK_ROOT=C:\Users\danie\AppData\Local\Android\Sdk"
set "ANDROID_HOME=C:\Users\danie\AppData\Local\Android\Sdk"

call "%~dp0gradle-8.7\bin\gradle.bat" --no-daemon --offline assembleRelease

echo.
echo Erwartete Ausgabe:
echo   app\build\outputs\apk\release\
endlocal
