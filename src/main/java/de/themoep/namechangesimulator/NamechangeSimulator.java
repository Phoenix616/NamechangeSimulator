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
import org.apache.commons.lang.StringUtils;
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
    private BiMap<UUID, String> originalNames = HashBiMap.create();

    private final Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_-]{3,16}$");//The regex to verify usernames;

    @Override
    public void onEnable() {
        getCommand("namechangesimulator").setExecutor(this);
        getLogger().info("Loading fake names from config...");

        getConfig().getDefaults().createSection("names");
        saveDefaultConfig();
        for (String uuidString : getConfig().getConfigurationSection("names").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                fakeNames.put(uuid, getConfig().getString("names." + uuid + ".fake"));
                if (getConfig().contains("names." + uuid + ".name")) {
                    originalNames.put(uuid, getConfig().getString("names." + uuid + ".name"));
                }
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "The configured UUID " + uuidString + " does not match the UUID format! " + e.getMessage());
            }
        }

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
            UUID targetId;
            if (args.length == 1) {
                return false;
            } else if (args.length > 2) {
                if (sender.hasPermission(cmd.getPermission() + ".set.others")) {
                    try {
                        targetId = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException e) {
                        Player target = getServer().getPlayer(args[1]);
                        if (target != null) {
                            targetId = target.getUniqueId();
                        } else {
                            targetId = originalNames.inverse().get(args[1]);
                        }
                        if (targetId == null) {
                            targetId = fakeNames.inverse().get(args[1]);
                        }
                        if (targetId == null) {
                            sender.sendMessage(ChatColor.RED + args[1] + " is neither a name of an online player nor an UUID?");
                            return true;
                        }
                    }
                    name = args[2];
                } else {
                    sender.sendMessage(cmd.getPermissionMessage().replace("<permission>", cmd.getPermission() + ".set.others"));
                    return true;
                }
            } else if (sender instanceof Player) {
                targetId = ((Player) sender).getUniqueId();
                name = args[1];
            } else {
                sender.sendMessage("Please use /" + label + " set <player/uuid> <name> from the console!");
                return true;
            }

            if (!usernamePattern.matcher(name).matches()) {
                sender.sendMessage(ChatColor.RED + "The name " + name + " is not a valid minecraft username!");
                return true;
            }

            if (setName(targetId, name)) {
                sender.sendMessage(ChatColor.GREEN + "Set name of " + getOriginalName(targetId) + " to " + name);
            } else {
                sender.sendMessage(ChatColor.RED + "The name " + name + " is already in use!");
            }

        } else if ("reset".equalsIgnoreCase(args[0])) {
            UUID targetId;
            if (args.length > 1) {
                if (sender.hasPermission(cmd.getPermission() + ".reset.others")) {
                    try {
                        targetId = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException e) {
                        Player target = getServer().getPlayer(args[1]);
                        if (target != null) {
                            targetId = target.getUniqueId();
                        } else {
                            targetId = originalNames.inverse().get(args[1]);
                        }
                        if (targetId == null) {
                            targetId = fakeNames.inverse().get(args[1]);
                        }
                        if (targetId == null) {
                            sender.sendMessage(ChatColor.RED + args[1] + " is neither a name of an online player nor an UUID?");
                            return true;
                        }
                    }
                } else {
                    sender.sendMessage(cmd.getPermissionMessage().replace("<permission>", cmd.getPermission() + ".reset.others"));
                    return true;
                }
            } else if (sender instanceof Player) {
                targetId = ((Player) sender).getUniqueId();
            } else {
                sender.sendMessage("Please use /" + label + " reset <player/uuid> from the console!");
                return true;
            }
            if (resetName(targetId)) {
                sender.sendMessage(ChatColor.GREEN + "Reset name of " + getOriginalName(targetId));
            } else {
                sender.sendMessage(ChatColor.YELLOW + "" + getOriginalName(targetId) + " does not have a changed name?");
            }

        } else if ("list".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.RED + getName() + ChatColor.YELLOW + " " + getDescription().getVersion() + " by " + StringUtils.join(getDescription().getAuthors(), ", "));
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
                    sender.sendMessage(ChatColor.WHITE + "" + getOriginalName(entry.getKey()) + " -> " + entry.getValue());
                } else {
                    sender.sendMessage(ChatColor.GRAY + "" + getOriginalName(entry.getKey()) + " -> " + entry.getValue());
                }
            }
        }


        return true;
    }

    private String getOriginalName(UUID playerId) {
        if (originalNames.containsKey(playerId)) {
            return originalNames.get(playerId);
        }
        return playerId.toString();
    }

    void addOriginalName(UUID playerId, String name) {
        originalNames.put(playerId, name);
        if (getConfig().contains("names." + playerId)) {
            getConfig().set("names." + playerId + ".name", name);
            saveConfig();
        }
    }

    private boolean setName(UUID targetId, String name) {
        if (getServer().getPlayer(name) != null || fakeNames.inverse().containsKey(name) || originalNames.inverse().containsKey(name)) {
            return false;
        }
        fakeNames.put(targetId, name);
        getConfig().set("names." + targetId + ".fake", name);
        if (originalNames.containsKey(targetId)) {
            getConfig().set("names." + targetId + ".name", originalNames.get(targetId));
        }
        saveConfig();
        kickPlayer(targetId, "Please rejoin so that your changed name takes effect!");
        return true;
    }

    private boolean resetName(UUID targetId) {
        String previous = fakeNames.remove(targetId);
        getConfig().set("names." + targetId, null);
        saveConfig();
        if (previous != null) {
            kickPlayer(targetId, "Please rejoin so that your name reset takes effect!");
            return true;
        }
        return false;
    }

    public String getFakeName(UUID playerId) {
        return fakeNames.get(playerId);
    }

    private void kickPlayer(UUID targetId, String message) {
        Player player = getServer().getPlayer(targetId);
        if (player != null) {
            player.kickPlayer(message);
        }
    }
}
