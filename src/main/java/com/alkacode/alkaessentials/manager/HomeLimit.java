package com.alkacode.alkaessentials.manager;

import net.luckperms.api.LuckPermsProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Resolve o limite de homes de um jogador: node LuckPerms {@code alkassentials.homes.<n>}
 * (VIPs podem ter mais) somado ao {@code homes.base-limit} do config.yml. Sem LuckPerms,
 * usa apenas config.yml (default-limit / base-limit).
 */
public final class HomeLimit {

    private final JavaPlugin plugin;

    public HomeLimit(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int limitFor(Player player) {
        return limitFor(player.getUniqueId());
    }

    public int limitFor(UUID uuid) {
        int defaultLimit = plugin.getConfig().getInt("homes.default-limit", 3);
        int nodeLimit = 0;
        int base = plugin.getConfig().getInt("homes.base-limit", 0);

        if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                var user = LuckPermsProvider.get().getUserManager().getUser(uuid);
                if (user != null) {
                    for (String perm : user.getCachedData().getPermissionData().getPermissionMap().keySet()) {
                        if (perm.startsWith("alkassentials.homes.")) {
                            try {
                                int value = Integer.parseInt(perm.substring("alkassentials.homes.".length()));
                                nodeLimit = Math.max(nodeLimit, value);
                            } catch (NumberFormatException ignored) {
                                // node malformado - ignora
                            }
                        }
                    }
                }
            } catch (IllegalStateException ignored) {
                // LuckPerms presente mas ainda nao carregado
            }
        }

        return Math.max(defaultLimit, Math.max(nodeLimit, base + nodeLimit));
    }
}
