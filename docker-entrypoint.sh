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

#  Docker entrypoint

ARGS="-server -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10"

if [ ! -z ${FOOM_ARGS+x} ]; then
  ARGS="$ARGS $FOOM_ARGS"
fi

if [ ! -z $1 ]; then
  if [ "$1" == "--debug" ]; then
    ARGS="$ARGS -Dlogging.level.root=DEBUG -Dlogging.level.java.awt=INFO -Dlogging.level.sun.awt=INFO -Dlogging.level.javax.swing=INFO -Dlogging.level.jdk=INFO -Dsun.security.krb5.debug=true -Dsun.security.jgss.debug=true"
  else
    echo "Illegal parameter $1, only '--debug' is allowed"
    exit 1
  fi
fi

exec java $ARGS -cp . -jar winfoom.jar
