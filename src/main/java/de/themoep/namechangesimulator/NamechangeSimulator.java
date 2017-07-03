package de.themoep.namechangesimulator;

/*
 *  NamechangeSimulator Bukkit plugin
 *  Copyright (C) 2017 Max Lee (https://github.com/Phoenix616)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class NamechangeSimulator extends JavaPlugin {

    private BiMap<UUID, String> fakeNames = HashBiMap.create();

    private final Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_-]{3,16}$");//The regex to verify usernames;

    @Override
    public void onEnable() {
        getCommand("namechangesimulator").setExecutor(this);
        getLogger().info("Setting up NMS authentication service...");
        try {
            NMSAuthService.setUp(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while setting up fake auth services. The plugin might not be compatbile with your server version :(", e);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (!sender.hasPermission(cmd.getPermission() + "." + args[0].toLowerCase())) {
            sender.sendMessage(cmd.getPermissionMessage().replace("<permission>", cmd.getPermission() + "." + args[0].toLowerCase()));
            return true;
        }
        if ("set".equalsIgnoreCase(args[0])) {
            String name;
            Player target;
            if (args.length == 1) {
                return false;
            } else if (args.length > 2) {
                if (sender.hasPermission(cmd.getPermission() + ".set.others")) {
                    target = getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "The player " + args[1] + " was not found online!");
                        return true;
                    }
                    name = args[2];
                } else {
                    sender.sendMessage(cmd.getPermissionMessage().replace("<permission>", cmd.getPermission() + ".set.others"));
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
                name = args[1];
            } else {
                sender.sendMessage("Please use /" + label + " set <player> <name> from the console!");
                return true;
            }

            if (!usernamePattern.matcher(name).matches()) {
                sender.sendMessage(ChatColor.RED + "The name " + name + " is not a valid minecraft username!");
                return true;
            }

            String targetName = target.getName();
            if (setName(target, name)) {
                sender.sendMessage(ChatColor.GREEN + "Set name of " + targetName + " to " + name);
            } else {
                sender.sendMessage(ChatColor.RED + "The name " + name + " is already in use!");
            }

        } else if ("reset".equalsIgnoreCase(args[0])) {
            Player target;
            if (args.length > 1) {
                if (sender.hasPermission(cmd.getPermission() + ".reset.others")) {
                    target = getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "The player " + args[1] + " was not found online!");
                        return true;
                    }
                } else {
                    sender.sendMessage(cmd.getPermissionMessage().replace("<permission>", cmd.getPermission() + ".reset.others"));
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("Please use /" + label + " reset <player> <name> from the console!");
                return true;
            }
            if (resetName(target)) {
                sender.sendMessage(ChatColor.GREEN + "Reset name of " + target.getName());
            } else {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " does not have a changed name?");
            }

        } else if ("list".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "Fake usernames:");

            for (Map.Entry<UUID, String> entry : fakeNames.entrySet().stream().sorted((e1, e2) -> {
                Player p1 = getServer().getPlayer(e1.getKey());
                Player p2 = getServer().getPlayer(e2.getKey());
                if (p1 == null && p2 == null) {
                    return e1.getKey().compareTo(e2.getKey());
                } else if (p1 == null) {
                    return 1;
                } else if (p2 == null) {
                    return -1;
                }
                return p1.getName().compareTo(p2.getName());
            }).collect(Collectors.toList())) {
                Player p = getServer().getPlayer(entry.getKey());
                if (p != null) {
                    sender.sendMessage(ChatColor.WHITE + p.getName() + " -> " + entry.getValue());
                } else {
                    sender.sendMessage(ChatColor.GRAY + "" + entry.getKey() + " -> " + entry.getValue());
                }
            }
        }


        return true;
    }

    private boolean setName(Player target, String name) {
        if (getServer().getPlayer(name) != null || fakeNames.inverse().containsKey(name)) {
            return false;
        }

        fakeNames.put(target.getUniqueId(), name);
        target.kickPlayer("Please rejoin so that your changed name takes effect!");
        return true;
    }

    private boolean resetName(Player target) {
        if (!fakeNames.containsKey(target.getUniqueId())) {
            return false;
        }
        fakeNames.remove(target.getUniqueId());
        target.kickPlayer("Please rejoin so that your name reset takes effect!");
        return true;
    }

    public String getFakeName(UUID playerId) {
        return fakeNames.get(playerId);
    }
}
