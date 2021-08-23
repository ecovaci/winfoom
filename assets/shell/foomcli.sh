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

#  Client for Winfoom - Basic Proxy Facade API
#  It requires 'curl' tool

if [ -z "$1" ]; then
  echo "Invalid command, try 'foomcli --help' for more information"
  exit 1
fi

usage() {
  echo "Usage: foomctl [command] [arguments]"
  echo "It manages winfoom application"
  echo "Note:  It requires 'curl' to be available!"
  echo
  echo "[command] must be one of the following:"
  echo "start                         - start the local proxy facade"
  echo "stop                          - stop the local proxy facade"
  echo "status                        - get the current status of the local proxy facade"
  echo "shutdown                      - shutdown the application"
  echo "validate                      - test the local proxy facade configuration"
  echo "config                        - print the current configuration"
  echo "config -f [json_filepath]     - apply the proxy configuration, where the [json_filepath] is"
  echo "                              the path to the JSON file containing the configuration to be applied"
  echo "config -d [json_content]      - apply the proxy configuration, where the [json_content] is"
  echo "                              the JSON object containing the configuration to be applied"
  echo "config -t [proxy_type]        - change the proxy type, where [proxy_type] can be"
  echo "                              one of: direct, http, pac, socks4, socks5"
  echo "settings                      - print the current settings"
  echo "settings -f [json_filepath]   - apply the proxy settings, where the [json_filepath] is"
  echo "                              the path to the JSON file containing the settings to be applied"
  echo "settings -d [json_content]    - apply the proxy settings, where the [json_content] is"
  echo "                              the JSON object containing the settings to be applied"
  echo
  echo "Note: When proxyType is http, then the value of httpAuthProtocol - the remote proxy protocol -"
  echo "      must be set (allowed values, one of: NTLM, KERBEROS, BASIC)"
}

if [ "$1" == "--help" ]; then
  usage
  exit 0
fi

if [[ "$1" != "start" && "$1" != "stop" && "$1" != "status" && "$1" != "validate" && "$1" != "shutdown" && "$1" != "test" && "$1" != "config"  && "$1" != "settings" ]]; then
  echo "Invalid command, try 'foomcli --help' for more information"
  exit 1
fi

if [ "$1" == "config" ]; then
  if [[ "$2" != "-f" && "$2" != "-t" && "$2" != "-d" && ! -z "$2" ]]; then
    echo "Invalid option '$2', try 'foomcli --help' for more information"
    exit 1
  fi
  if [[ ("$2" == "-f" || "$2" == "-d" || "$2" == "-t") && -z "$3" ]]; then
    echo "Missing option's value, try 'foomcli --help' for more information"
    exit 1
  fi
fi

if [ "$1" == "settings" ]; then
  if [[ "$2" != "-f" && "$2" != "-d" && ! -z "$2" ]]; then
    echo "Invalid option '$2', try 'foomcli --help' for more information"
    exit 1
  fi
  if [[ ("$2" == "-f" || "$2" == "-d") && -z "$3" ]]; then
    echo "Missing option's value, try 'foomcli --help' for more information"
    exit 1
  fi
fi

CTL_USER="admin:winfoom"

if [ -z ${FOOM_LOCATION+x} ]; then FOOM_LOCATION=localhost:9999; fi

if [ "$1" == "config" ]; then
  if [ "$2" == "-f" ]; then
    curl -w '\n' -X POST -H "Content-Type: application/json" -d @"$3" --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  elif [ "$2" == "-t" ]; then
    curl -w '\n' -X POST -H "Content-Type: application/json" -d "{\"proxyType\": \"${3^^}\"}" --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  elif [ "$2" == "-d" ]; then
    curl -w '\n' -X POST -H "Content-Type: application/json" -d "$3" --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  else
    curl -w '\n' --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  fi
elif [ "$1" == "settings" ]; then
  if [ "$2" == "-f" ]; then
    curl -w '\n' -X POST -H "Content-Type: application/json" -d @"$3" --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  elif [ "$2" == "-d" ]; then
    curl -w '\n' -X POST -H "Content-Type: application/json" -d "$3" --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  else
    curl -w '\n' --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
  fi
else
  if [ "$1" == "validate" ]; then echo "It may take some time, please be pacient ..."; fi
  curl -w '\n' --user "$CTL_USER" http://$FOOM_LOCATION/"$1"
fi

exitCode=$?
if [ $exitCode -eq 7 ]; then
  echo "Is the application running?"
elif [ $exitCode -ne 0 ]; then
  echo "Error code $exitCode"
fi
exit $exitCode
