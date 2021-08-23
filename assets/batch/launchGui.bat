:: Launch the Winfoom - Basic Proxy Facade in graphical mode
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

setlocal EnableExtensions

set ARGS=-server -XX:+UseG1GC -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10 -Dswing.aatext=true -Dspring.profiles.active=gui

if defined FOOM_ARGS set ARGS=%FOOM_ARGS% %ARGS%

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

exit /B %ERRORLEVEL%

