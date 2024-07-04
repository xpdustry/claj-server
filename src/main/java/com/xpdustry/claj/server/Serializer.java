package com.xpdustry.claj.server;

import arc.net.FrameworkMessage;
import arc.net.FrameworkMessage.DiscoverHost;
import arc.net.FrameworkMessage.KeepAlive;
import arc.net.FrameworkMessage.Ping;
import arc.net.FrameworkMessage.RegisterTCP;
import arc.net.FrameworkMessage.RegisterUDP;
import arc.net.NetSerializer;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import arc.util.io.Reads;
import arc.util.io.Writes;

import java.nio.ByteBuffer;

public class Serializer implements NetSerializer {

    private static final byte FRAMEWORK_ID = -2;
    private static final byte LINK_ID = -3;
    private static final ByteBuffer LAST = ByteBuffer.allocate(8192);

    @Override
    public void write(final ByteBuffer buffer, final Object object) {
        if (object instanceof final ByteBuffer raw) {
            buffer.put(raw);
        } else if (object instanceof final FrameworkMessage message) {
            buffer.put(FRAMEWORK_ID);
            writeFramework(buffer, message);
        } else if (object instanceof final String link) {
            buffer.put(LINK_ID);
            Writes.get(new ByteBufferOutput(buffer)).str(link);
        }
    }

    @Override
    public Object read(final ByteBuffer buffer) {
        final int lastPosition = buffer.position();
        final byte id = buffer.get();

        if (id == FRAMEWORK_ID) {
            return readFramework(buffer);
        }
        if (id == LINK_ID) {
            return Reads.get(new ByteBufferInput(buffer)).str();
        }

        LAST.clear();
        LAST.put(buffer.position(lastPosition));
        LAST.limit(buffer.limit() - lastPosition);

        return LAST.position(0);
    }

    private void writeFramework(final ByteBuffer buffer, final FrameworkMessage message) {
        if (message instanceof final Ping ping) {
            buffer.put((byte) 0).putInt(ping.id).put(ping.isReply ? (byte) 1 : 0);
        } else if (message instanceof DiscoverHost) {
            buffer.put((byte) 1);
        } else if (message instanceof KeepAlive) {
            buffer.put((byte) 2);
        } else if (message instanceof final RegisterUDP p) {
            buffer.put((byte) 3).putInt(p.connectionID);
        } else if (message instanceof final RegisterTCP p) {
            buffer.put((byte) 4).putInt(p.connectionID);
        }
    }

    private FrameworkMessage readFramework(final ByteBuffer buffer) {
        final byte id = buffer.get();

        return switch (id) {
            case 0 -> {
                final Ping ping = new Ping();
                ping.id = buffer.getInt();
                ping.isReply = buffer.get() == 1;
                yield ping;
            }
            case 1 -> FrameworkMessage.discoverHost;
            case 2 -> FrameworkMessage.keepAlive;
            case 3 -> {
                final RegisterUDP udpRegistration = new RegisterUDP();
                udpRegistration.connectionID = buffer.getInt();
                yield udpRegistration;
            }
            case 4 -> {
                final RegisterTCP tpcRegistration = new RegisterTCP();
                tpcRegistration.connectionID = buffer.getInt();
                yield tpcRegistration;
            }
            default -> throw new RuntimeException("Unknown framework message!");
        };
    }
}
