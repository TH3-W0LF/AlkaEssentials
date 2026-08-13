package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** /mutechat - muta/desmuta o chat global. */
public final class MuteChatCommand extends BaseCommand {

    private final AtomicBoolean muted = new AtomicBoolean(false);

    public MuteChatCommand(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePerm(sender, "alkassentials.chat.mutechat")) return true;
        boolean now = muted.getAndSet(!muted.get());
        String name = sender instanceof Player p ? p.getName() : "Console";
        for (Player online : Bukkit.getOnlinePlayers()) {
            ChatUtil.send(online, now
                    ? "<red>O chat foi mutado por <yellow>" + name + "<red>."
                    : "<green>O chat foi desmutado por <yellow>" + name + "<green>.");
        }
        return true;
    }

    public boolean isMuted() {
        return muted.get();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
