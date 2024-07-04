package com.xpdustry.claj.server;

import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;

public class Blacklist {

    private static final String ACTIONS_URL = "https://api.github.com/meta";
    private static final Seq<String> ips = new Seq<>();

    private Blacklist() {
        // no-op
    }

    static void refresh() {
        Http.get(ACTIONS_URL, (final Http.HttpResponse result) -> {
            final var json = Jval.read(result.getResultAsString());
            json.get("actions").asArray().each((final Jval element) -> {
                final String ip = element.asString();
                if (ip.charAt(4) != ':') ips.add(ip); // skip IPv6
            });

            Log.info("Added @ GitHub Actions IPs to blacklist.", ips.size);
        }, (final Throwable error) -> Log.err("Failed to fetch GitHub Actions IPs", error));
    }

    public static void add(final String ip) {
        ips.add(ip);
    }

    static boolean contains(final String ip) {
        return ips.contains(ip);
    }

    static void remove(final String ip) {
        ips.remove(ip);
    }

    static void clear() {
        ips.clear();
    }
}
