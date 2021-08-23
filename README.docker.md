# winfoom
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ecovaci/winfoom/blob/master/LICENSE)

# Overview
Winfoom is an HTTP(s) proxy server facade that allows applications to authenticate through the following proxies: 

* NTLM or Kerberos HTTP authenticated proxy
* SOCKS version 4 or 5, with or without authentication
* Proxy Auto Config files - including Mozilla Firefox extension that is not part of original Netscape specification

typically used in corporate environments, without having to deal with the actual handshake.

For more information see [Winfoom project](https://github.com/ecovaci/winfoom)

# How to use
Run this image with the command:
```
docker run -d --name winfoom -p 3129:3129 -p 9999:9999 ecovaci/winfoom
```
Or, if you want to run it in debug mode:

```
docker run -d --name winfoom -p 3129:3129 -p 9999:9999 ecovaci/winfoom --debug
```

If you want to change the startup parameters, use `FOOM_ARGS` environment variable:

```
docker run -d --name winfoom -p 3129:3129 -p 9999:9999 -e FOOM_ARGS='-Dsocket.soTimeout=10 -Dconnection.request.timeout=60' ecovaci/winfoom
```

If you need to share files between the host and the Docker container:

```
 docker run -d --name winfoom -p 3129:3129 -p 9999:9999 -v /tmp/winfoom:/data ecovaci/winfoom
```

The `/data` directory contains the configuration files, logs etc.

After the container starts, use the `foomcli.sh` (or `foomcli.bat`) packed inside `winfoom.zip` (see [Winfoom releases](https://github.com/ecovaci/winfoom/releases)) for management (start, stop, config).

Please read the [Winfoom project's README](https://github.com/ecovaci/winfoom) for details about configuration and management.