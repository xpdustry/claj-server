package com.xpdustry.claj.server;

import arc.net.ArcNet;
import arc.net.Connection;
import arc.util.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    private static final String[] tags = { "&lc&fb[D]&fr", "&lb&fb[I]&fr", "&ly&fb[W]&fr", "&lr&fb[E]", "" };
    private static final DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static void main(final String[] args) {
        ArcNet.errorHandler = Log::err;
        Log.logger = (final Log.LogLevel level, final String text) -> { // this is how fashionable I am
            final String result = Log.format("&lk&fb[" + dateTime.format(LocalDateTime.now()) + "]&fr " + tags[level.ordinal()] + " " + text + "&fr");
            System.out.println(result);
        };

        final var distributor = new Distributor();
        new Control(distributor);

        if (args.length == 0) {
            Log.err("Need a port as an argument!");
        } else {
            try {
                distributor.run(Integer.parseInt(args[0]));
            } catch (final Exception error) {
                Log.err("Could not to load redirect system", error);
            }
        }
    }

    static String getIP(final Connection connection) {
        return connection.getRemoteAddressTCP().getAddress().getHostAddress();
    }
}
