package de.themoep.namechangesimulator;

/*
 *  NamechangeSimulator Bukkit plugin
 *  Copyright (C) 2018 Max Lee (https://github.com/Phoenix616)
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

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.logging.Level;

public class NMSAuthService extends YggdrasilMinecraftSessionService {

    private final NamechangeSimulator plugin;

    public NMSAuthService(NamechangeSimulator plugin, YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);
        this.plugin = plugin;
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress inetAddress) throws AuthenticationUnavailableException {
        user = super.hasJoinedServer(user, serverId, inetAddress);
        if (user == null) {
            return null;
        }
        plugin.addOriginalName(user.getId(), user.getName());
        String fakeName = plugin.getFakeName(user.getId());
        if (fakeName != null) {
            GameProfile fakeUser = new GameProfile(user.getId(), fakeName);
            fakeUser.getProperties().putAll(user.getProperties());
            plugin.getLogger().log(Level.INFO, "Set name of " + user.getName() + "/" + user.getId() + " to " + fakeUser.getName());
            user = fakeUser;
        }

        Player player = Bukkit.getPlayer(user.getName());
        if (player != null && player.isOnline()) {
            player.kickPlayer("Someone joined with your username " + player.getName() + "!");
        }

        return user;
    }

    public static void setUp(NamechangeSimulator plugin) throws Exception {

        String nmsVersion = plugin.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];

        String sessionAuthVariableName;
        String sessionServiceVariableName;

        switch (nmsVersion) {
            case "v1_9_R1":
            case "v1_10_R1":
                sessionServiceVariableName = "V";
                sessionAuthVariableName = "U";
                break;
            case "v1_8_R3":
            case "v1_11_R1":
            case "v1_12_R1":
                sessionServiceVariableName = "W";
                sessionAuthVariableName = "V";
                break;
            case "v1_13_R1":
            case "v1_13_R2":
                sessionServiceVariableName = "V";
                sessionAuthVariableName = "U";
                break;
            default:
                plugin.getLogger().log(Level.SEVERE, plugin.getName() + " currently does not support spigot version " + plugin.getServer().getVersion());
                plugin.getLogger().log(Level.SEVERE, "This build of " +  plugin.getName() + " only supports Minecraft versions 1.8.8, 1.9, 1.10, 1.11, 1.12 and 1.13");
                plugin.getPluginLoader().disablePlugin(plugin);
                return;
        }

        Method method = Class.forName("net.minecraft.server." + nmsVersion + ".MinecraftServer").getMethod("getServer");

        Object minecraftServer = method.invoke(null);

        Field sessionServiceVariable = minecraftServer.getClass().getSuperclass().getDeclaredField(sessionServiceVariableName);

        sessionServiceVariable.setAccessible(true);

        Field sessionAuthVariable = minecraftServer.getClass().getSuperclass().getDeclaredField(sessionAuthVariableName);

        sessionAuthVariable.setAccessible(true);

        sessionServiceVariable.set(minecraftServer,
                new NMSAuthService(plugin, (YggdrasilAuthenticationService) sessionAuthVariable.get(minecraftServer)));
    }

}