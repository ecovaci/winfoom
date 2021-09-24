function FindProxyForURL(url, host) {
     if (isResolvable(host)) {
        return "DIRECT";
     } else {
        return "DIRECT";
     }
 }