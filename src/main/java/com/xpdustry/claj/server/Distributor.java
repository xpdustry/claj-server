package com.xpdustry.claj.server;

import arc.math.Mathf;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage;
import arc.net.NetListener;
import arc.net.Server;
import arc.struct.IntMap;
import arc.struct.IntMap.Entry;
import arc.util.Log;
import arc.util.Ratekeeper;

import java.io.IOException;

/**
 * It is an entry point for clients, distributes their packets to redirectors.
 *
 * @author xzxADIxzx
 */
class Distributor extends Server {

    /**
     * List of all characters that are allowed in a link.
     */
    private static final char[] SYMBOLS = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwYyXxZz".toCharArray();

    /**
     * Limit for packet count sent within 3 sec that will lead to a disconnect. Note: only for clients.
     */
    int spamLimit = 500;

    /**
     * Map containing the connection id and its redirector.
     */
    final IntMap<Room> rooms = new IntMap<>();

    /**
     * Map containing the connection id and its redirector.
     */
    private final IntMap<Redirector> redirectors = new IntMap<>();

    Distributor() {
        super(32768, 8192, new Serializer());
        addListener(new Listener());
    }

    void run(final int port) throws IOException {
        Blacklist.refresh(); // refresh GitHub's ips
        Log.info("Distributor hosted on port @.", port);

        bind(port, port);
        run();
    }

    public class Listener implements NetListener {

        @Override
        public void connected(final Connection connection) {
            if (Blacklist.contains(connection.getRemoteAddressTCP().getAddress().getHostAddress())) {
                connection.close(DcReason.closed);
                return;
            }

            Log.info("Connection @ received!", connection.getID());
            connection.setArbitraryData(new Ratekeeper());
        }

        @Override
        public void disconnected(final Connection connection, final DcReason reason) {
            Log.info("Connection @ lost: @.", connection.getID(), reason);

            var room = rooms.get(connection.getID());
            if (room != null) {
                room.close(); // disconnects all related redirectors
                rooms.remove(connection.getID());
                return;
            }

            final var redirector = redirectors.get(connection.getID());
            if (redirector == null) return;

            redirectors.remove(redirector.host.getID());
            if (redirector.client != null) redirectors.remove(redirector.client.getID());

            // called after deletion to prevent double close
            redirector.disconnected(connection, reason);

            room = find(redirector);
            if (room != null) room.redirectors.remove(redirector);
        }

        @Override
        public void received(final Connection connection, final Object object) {
            final var rate = (Ratekeeper) connection.getArbitraryData();
            if (!rate.allow(3000L, spamLimit)) {
                handlePacketSpam(connection, rate);
                return;
            }

            if (object instanceof FrameworkMessage) return;
            if (object instanceof final String link) {
                if ("new".equals(link)) {
                    createNewLink(connection);
                } else if (link.startsWith("host")) {
                    hostLink(connection, link);
                } else if (link.startsWith("join")) {
                    joinLink(connection, link);
                }

                return;
            }

            final var redirector = redirectors.get(connection.getID());
            if (redirector != null) redirector.received(connection, object);
        }

        private void joinLink(final Connection connection, final String link) {
            final var room = find(link.substring(4));
            if (room == null) {
                connection.close(DcReason.error);
                return;
            }

            final var redirector = room.redirectors.find((final Redirector r) -> r.client == null);
            if (redirector == null) {
                connection.close(DcReason.error); // no empty redirectors
                return;
            }

            redirector.client = connection;
            redirectors.put(connection.getID(), redirector);
            room.sendMessage("new"); // ask to create a new redirector for the future

            Log.info("Connection @ joined to room @.", connection.getID(), room.link);
        }

        private void hostLink(final Connection connection, final String link) {
            final var room = find(link.substring(4));
            if (room == null || !Main.getIP(room.host).equals(Main.getIP(connection))) {
                connection.close(DcReason.error); // kick the connection if it tries to host a redirector without permission
                return;
            }

            final var redirector = new Redirector(connection);
            room.redirectors.add(redirector);
            redirectors.put(connection.getID(), redirector);

            Log.info("Connection @ hosted a redirector in room @.", connection.getID(), room.link);
        }

        private void createNewLink(final Connection connection) {
            final String link;
            link = generateLink();

            connection.sendTCP(link);
            rooms.put(connection.getID(), new Room(link, connection));

            Log.info("Connection @ created a room @.", connection.getID(), link);
        }

        private void handlePacketSpam(final Connection connection, final Ratekeeper rate) {
            rate.occurences = -spamLimit; // reset to prevent message spam

            final var redirector = redirectors.get(connection.getID());
            if (redirector != null && connection == redirector.host) {
                Log.warn("Connection @ spammed with packets but not disconnected due to being a host.", connection.getID());
                return;
            }

            Log.warn("Connection @ disconnected due to packet spam.", connection.getID());
            if (redirector != null) {
                final var room = find(redirector);
                if (room != null) {
                    room.sendMessage("[scarlet]⚠[] Connection closed due to packet spam.");
                    room.redirectors.remove(redirector);
                }
            }

            connection.close(DcReason.closed);
        }

        // region room management

        private String generateLink() {
            final StringBuilder builder = new StringBuilder("CLaJ");
            for (int i = 0; i < 42; i++)
                builder.append(SYMBOLS[Mathf.random(SYMBOLS.length - 1)]);

            return builder.toString();
        }

        private Room find(final String link) {
            for (final Entry<Room> entry : rooms)
                if (entry.value.link.equals(link)) return entry.value;

            return null;
        }

        private Room find(final Redirector redirector) {
            for (final Entry<Room> entry : rooms)
                if (entry.value.redirectors.contains(redirector)) return entry.value;

            return null;
        }

        // endregion
    }
}
