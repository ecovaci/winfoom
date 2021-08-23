package org.kpax.winfoom.proxy;

import org.apache.http.HttpException;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;

import java.io.IOException;
import java.net.Socket;

/**
 * A specialized {@link ClientConnection} for any manual proxy type.
 */
public class ManualProxyClientConnection extends ClientConnection {

    ManualProxyClientConnection(Socket socket,
                                ProxyConfig proxyConfig,
                                SystemConfig systemConfig,
                                ConnectionProcessorSelector connectionProcessorSelector,
                                ProxyInfo manualProxy) throws IOException, HttpException {
        super(socket, proxyConfig, systemConfig, connectionProcessorSelector, manualProxy);
    }

    @Override
    void process() {
        processProxy(manualProxy);
    }
}
