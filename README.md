# NamechangeSimulator
Bukkit plugin to simulate name changes by changing the username in the Gameprofile that Mojang's AuthLib returns while a player logs in.

This could be used as a nick system as it stores the fake names after logouts and restarts and includes checks for the availability of such name with Mojang. But the main purpose is for testing how other plugins react to namechanges without having to buy/own a lot of acconts (or to have friends).

**Please note that this is an extremely experimental plugin! Use at your own risk!**

## Commands

Aliases: `/namechangesimulator`, `/simulatenamechange`, `/namechange`, `/ns`

 Command                               | Description
---------------------------------------|---------------------------------------------------
 `/ns set [<player/uuid>] <fakename>`  | Set a fake name for yourself or for another player
 `/ns reset [<player/uuid>]`           | Reset your name to your original one
 `/ns list`                            | List all the fake names

## Permissions

Players with operator status have all permissions by default.

Permission                                  | Description
--------------------------------------------|------------------------------------------------------------------------------------
`namechangesimulator.command`               | Use the plugin command
`namechangesimulator.command.set`           | Change names via `/ns set`
`namechangesimulator.command.set.others`    | Set the name of other players
`namechangesimulator.command.set.used`      | Set usernames that are already in use by other Minecraft player
`namechangesimulator.command.reset`         | Reset names with `/ns list`
`namechangesimulator.command.reset.others`  | Reset the name of other players
`namechangesimulator.command.list`          | List all changed names with `/ns list`

## Downloads

Development builds can be downloaded here: https://ci.minebench.de/job/NamechangeSimulator/

## Building

You can build the project with maven. Due to it proxying the YggdrasilMinecraftSessionService from Mojang's AuthLib it needs CraftBukkit or Spigot to build.

## License

The proxy session service is based on the Bukkit version of [AlwaysOnline](https://github.com/johnnywoof/AlwaysOnline) and this plugin is therefore licensed under a compatible license:

```
NamechangeSimulator Bukkit plugin
Copyright (C) 2017 Max Lee (https://github.com/Phoenix616)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
```

The full license text can be found [here](https://github.com/Phoenix616/NamechangeSimulator/blob/master/LICENSE).
