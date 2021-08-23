#!/bin/bash
#
# Copyright (c) 2020. Eugen Covaci
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and limitations under the License.
#
#

#  Launcher for Winfoom - Basic Proxy Facade

usage() {
  echo "Usage: launch [arguments]"
  echo "where [arguments] must be any of the following:"
  echo "  --debug             start in debug mode"
  echo "  --systemjre         use the system jre"
}

if [ "$1" == "--help" ]; then
  usage
  exit 0
fi

ARGS="-server -XX:+UseG1GC -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10"

if [ ! -z ${FOOM_ARGS+x} ]; then
  ARGS="$ARGS $FOOM_ARGS"
fi

if [ ! -z $1 ]; then
  if [ "$1" == "--debug" ]; then
    ARGS="$ARGS -Dlogging.level.root=DEBUG -Dlogging.level.java.awt=INFO -Dlogging.level.sun.awt=INFO -Dlogging.level.javax.swing=INFO -Dlogging.level.jdk=INFO -Dsun.security.krb5.debug=true -Dsun.security.jgss.debug=true"
  else
    echo "Invalid command, try 'launch --help' for more information"
    exit 1
  fi
fi

if [ -e out.log ]; then
  echo "Is there another application instance running?"
  echo "If so, you need to close it first; otherwise remove the 'out.log' file and retry"
  exit 2
fi

if [ -e ./jdk/bin/java ]; then
  JAVA_EXE=./jdk/bin/java
fi

if [ -z ${JAVA_EXE+x} ]; then
  JAVA_EXE=java
fi

(
  $JAVA_EXE $ARGS -cp . -jar winfoom.jar >out.log 2>&1
  rm -f out.log
) &

echo "You can check the application log with: \$ tail -n 150 -f ~/.winfoom/logs/winfoom.log"
echo "If application failed to start, you may get the reason with: \$ cat out.log"
echo "Use foomcli script for management (like start, stop etc.)"
