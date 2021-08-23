:: Launcher for Winfoom - Basic Proxy Facade
::
:: Copyright (c) 2020. Eugen Covaci
:: Licensed under the Apache License, Version 2.0 (the "License");
::  you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::  http://www.apache.org/licenses/LICENSE-2.0
:: Unless required by applicable law or agreed to in writing, software
::  distributed under the License is distributed on an "AS IS" BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
::  See the License for the specific language governing permissions and limitations under the License.
::
::

@echo off

setlocal EnableDelayedExpansion

if "%1"=="--help" goto usage

set ARGS=-server -XX:+UseG1GC -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10

if defined FOOM_ARGS set ARGS=%FOOM_ARGS% %ARGS%

for %%a in (%*) do (
    if not "%%a"=="--debug" if not "%%a"=="--gui" (
		@echo Invalid command, try 'launch --help' for more information
		exit /B 1;
	)
	if "%%a"=="--debug" (
		set ARGS=!ARGS! -Dlogging.level.root=DEBUG -Dlogging.level.java.awt=INFO -Dlogging.level.sun.awt=INFO -Dlogging.level.javax.swing=INFO -Dlogging.level.jdk=INFO
	)
	if "%%a"=="--gui" (
		set ARGS=!ARGS! -Dswing.aatext=true -Dspring.profiles.active=gui
	)
)

if exist out.log (
del /F out.log
)

if exist out.log (
    @echo Is there another application's instance running?
	exit /B 2
)

if exist .\jdk\bin\javaw.exe (
set JAVA_EXE=.\jdk\bin\javaw
) else (
set JAVA_EXE=javaw
)

start /B %JAVA_EXE% %ARGS% -cp . -jar winfoom.jar > out.log 2>&1

@echo You can check the application log with the command: type "%userprofile%\.winfoom\logs\winfoom.log"
@echo If application is not launched in GUI mode, use foomcli script for management
@echo If application failed to start, you may get the reason with the command: type out.log

exit /B %ERRORLEVEL%

:usage
@echo Usage: launch [arguments]
@echo where [arguments] must be any of the following:
@echo    --debug             start in debug mode
@echo    --gui               start with graphical user interface