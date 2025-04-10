package com.xpdustry.claj.server;

import arc.net.Connection;
import arc.net.DcReason;
import arc.struct.Seq;
import arc.util.Log;

/**
 * Represents a room containing a host and redirectors.
 * 
 * @author xzxADIxzx
 */
public class Room {

    public String link;

    public Connection host;
    public Seq<Redirector> redirectors = new Seq<>();

    public Room(String link, Connection host) {
        this.link = link;
        this.host = host;

        sendMessage("new"); // there must be at least one empty redirector in the room
        sendMessage(
                "[scarlet][[CLaJ Server]: [yellow]WARNING[white], the scheme-size version of [yellow]CLaJ[] is no longer maintained and will shutdown soon. " +
                "You can get the new version by installing the dedicated [yellow]'claj'[] mod in the mod browser.\n" +
                "[lightgray]After that, if you don't use any of the other features of scheme-size, removing it is recommended.");
        Log.info("Room @ created!", link);
    }

    public void close() {
        // rooms only closes if the host left, so there's no point in disconnecting it again
        redirectors.each(r -> r.disconnected(null, DcReason.closed));

        Log.info("Room @ closed.", link);
    }

    public void sendMessage(String message) {
        host.sendTCP(message);
    }
}
