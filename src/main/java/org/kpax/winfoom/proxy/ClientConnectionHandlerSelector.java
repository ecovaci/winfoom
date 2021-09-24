package org.kpax.winfoom.proxy;

import lombok.RequiredArgsConstructor;
import org.kpax.winfoom.config.ProxyConfig;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ClientConnectionHandlerSelector {

    private final ProxyConfig proxyConfig;

    private final ManualClientConnectionHandler manualClientConnectionHandler;

    private final PacClientConnectionHandler pacClientConnectionHandler;

    public ClientConnectionHandler select() {
        return proxyConfig.getProxyType().isPac() ? pacClientConnectionHandler : manualClientConnectionHandler;
    }
}
