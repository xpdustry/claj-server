package com.xpdustry.claj.server;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.struct.Seq;
import arc.util.Log;

/**
 * Represents a room containing a host and redirectors.
 * 
 * @author xzxADIxzx
 */
class Room {

    public final String link;

    final Connection host;
    public final Seq<Redirector> redirectors = new Seq<>();

    Room(final String link, final Connection host) {
        this.link = link;
        this.host = host;

        sendMessage("new"); // there must be at least one empty redirector in the room
        sendMessage("Hello, it's me, [#0096FF]xzxADIxzx#7729[], the creator of CLaJ."); // some contact info
        sendMessage("I just want to say that if you have any problems, you can always message me on Discord.");

        Log.info("Room @ created!", link);
    }

    void close() {
        // rooms only closes if the host left, so there's no point in disconnecting it again
        redirectors.each((final NetListener r) -> r.disconnected(null, DcReason.closed));

        Log.info("Room @ closed.", link);
    }

    public void sendMessage(final String message) {
        host.sendTCP(message);
    }
}
