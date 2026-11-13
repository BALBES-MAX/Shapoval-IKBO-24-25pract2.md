@echo off
if not exist out mkdir out
javac -encoding UTF-8 -d out src\ru\mirea\depvis\*.java
if %errorlevel% neq 0 exit /b %errorlevel%
set CONFIG=%1
if "%CONFIG%"=="" set CONFIG=config.csv
java -cp out ru.mirea.depvis.Main %CONFIG%
