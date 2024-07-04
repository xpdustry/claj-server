package com.xpdustry.claj.server;

import arc.struct.IntMap;
import arc.util.CommandHandler;
import arc.util.CommandHandler.Command;
import arc.util.CommandHandler.CommandResponse;
import arc.util.CommandHandler.ResponseType;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Threads;

import java.util.Scanner;

class Control {

    private final CommandHandler handler = new CommandHandler("");
    private final Distributor distributor;

    Control(final Distributor distributor) {
        this.distributor = distributor;
        registerCommands();

        Threads.daemon("Application Control", () -> {
            try (final Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNext()) handleCommand(scanner.nextLine());
            }
        });
    }

    private void handleCommand(final String command) {
        final CommandResponse response = handler.handleMessage(command);

        if (response.type == ResponseType.unknownCommand) {
            final String closest = handler.getCommandList().map((Command cmd) -> cmd.text).min((String cmd) -> Strings.levenshtein(cmd, command));
            Log.err("Command not found. Did you mean @?", closest);
        } else if (response.type != ResponseType.noCommand && response.type != ResponseType.valid)
            Log.err("Too @ command arguments.", response.type == ResponseType.fewArguments ? "few" : "many");
    }

    private void registerCommands() {
        handler.register("help", "Display the command list.", (final String[] args) -> {
            Log.info("Commands:");
            handler.getCommandList().each((Command command) -> Log.info("  &b&lb@@&fr - @",
                    command.text, command.paramText.isEmpty() ? "" : " &lc&fi" + command.paramText, command.description));
        });

        handler.register("list", "Displays all current rooms.", (final String[] args) -> {
            Log.info("Rooms:");
            distributor.rooms.forEach((final IntMap.Entry<Room> entry) -> {
                Log.info("  &b&lbRoom @&fr", entry.value.link);
                entry.value.redirectors.each((final Redirector r) -> {
                    Log.info("    [H] &b&lbConnection @&fr - @", r.host.getID(), Main.getIP(r.host));
                    if (r.client == null) return;
                    Log.info("    [C] &b&lbConnection @&fr - @", r.client.getID(), Main.getIP(r.client));
                });
            });
        });

        handler.register("limit", "[amount]", "Sets spam packet limit.", (final String[] args) -> {
            if (args.length == 0)
                Log.info("Current limit - @ packets per 3 seconds.", distributor.spamLimit);
            else {
                distributor.spamLimit = Strings.parseInt(args[0], 300);
                Log.info("Packet spam limit set to @ packets per 3 seconds.", distributor.spamLimit);
            }
        });

        handler.register("ban", "<IP>", "Adds the IP to blacklist.", (final String[] args) -> {
            Blacklist.add(args[0]);
            Log.info("IP @ has been blacklisted.", args[0]);
        });

        handler.register("unban", "<IP>", "Removes the IP from blacklist.", (final String[] args) -> {
            Blacklist.remove(args[0]);
            Log.info("IP @ has been removed from blacklist.", args[0]);
        });

        handler.register("refresh", "Unbans all IPs and refresh GitHub Actions IPs.", (final String[] args) -> {
            Blacklist.clear();
            Blacklist.refresh();
        });

        handler.register("exit", "Stop hosting distributor and exit the application.", (final String[] args) -> {
            distributor.rooms.forEach((IntMap.Entry<Room> entry) -> entry.value.sendMessage("[scarlet]⚠[] The server is shutting down.\nTry to reconnect in a minute."));

            Log.info("Shutting down the application.");
            distributor.stop();
        });
    }
}
