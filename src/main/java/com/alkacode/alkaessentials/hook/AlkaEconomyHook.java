package com.alkacode.alkaessentials.hook;

import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Bukkit;

import java.util.Locale;
import java.util.UUID;

/** Ponte pro AlkaEconomy - usada so pra cobrar teleporte em warps de jogador pagos
 * (mesmo padrao de import direto + compileOnly + getPlugin()!=null do AlkaVips'
 * AlkaEconomyHook - AlkaEconomy continua softdepend em runtime). */
public final class AlkaEconomyHook {

    private final EconomyManager economyManager;

    private AlkaEconomyHook(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public static AlkaEconomyHook resolve() {
        if (Bukkit.getPluginManager().getPlugin("AlkaEconomy") == null) {
            return null;
        }
        AlkaEconomyPlugin plugin = (AlkaEconomyPlugin) Bukkit.getPluginManager().getPlugin("AlkaEconomy");
        return new AlkaEconomyHook(plugin.getEconomyManager());
    }

    public boolean isValidCurrency(String currency) {
        return resolveCurrency(currency) != null;
    }

    public boolean has(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        return id != null && economyManager.has(uuid, id, amount);
    }

    public boolean withdraw(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        if (id == null || !economyManager.has(uuid, id, amount)) {
            return false;
        }
        economyManager.removeBalance(uuid, id, amount);
        return true;
    }

    public void deposit(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        if (id != null) {
            economyManager.addBalance(uuid, id, amount);
        }
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }

    private String resolveCurrency(String currency) {
        if (currency == null) {
            return null;
        }
        String id = currency.toLowerCase(Locale.ROOT);
        return economyManager.isValidCurrency(id) ? id : null;
    }
}
