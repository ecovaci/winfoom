package org.kpax.winfoom.proxy;

import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.pac.PacScriptEvaluator;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;

import java.net.Socket;

/**
 * A specialized {@link ClientConnection} for PAC proxy type.
 */
public class PacClientConnection extends ClientConnection {

    PacClientConnection(Socket socket,
                        ProxyConfig proxyConfig,
                        SystemConfig systemConfig,
                        ConnectionProcessorSelector connectionProcessorSelector,
                        PacScriptEvaluator pacScriptEvaluator) throws Exception {
        super(socket, proxyConfig, systemConfig, connectionProcessorSelector, pacScriptEvaluator);
    }

    @Override
    void process() {
        while (proxyInfoIterator.hasNext()) {
            if (processProxy(proxyInfoIterator.next())) {
                break;
            }
        }
    }
}
