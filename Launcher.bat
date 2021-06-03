@ECHO OFF
start "TAS-Editor-Launcher" /MIN cmd /c "java -jar Launcher.jar v0 & if ERRORLEVEL 3 call Launcher-updater.bat"