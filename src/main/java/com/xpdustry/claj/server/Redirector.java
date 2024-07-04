package com.xpdustry.claj.server;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;

/**
 * Contains a host and a client, redirects packets from one to the other.
 * 
 * @author xzxADIxzx
 */
public class Redirector implements NetListener {

    final Connection host;
    Connection client;

    Redirector(final Connection host) {
        this.host = host;
    }

    @Override
    public void disconnected(final Connection connection, final DcReason reason) {
        host.close(DcReason.closed);
        if (client != null) client.close(DcReason.closed);
    }

    @Override
    public void received(final Connection connection, final Object object) {
        final var receiver = connection == host ? client : host;
        if (receiver != null) receiver.sendTCP(object);
    }
}
