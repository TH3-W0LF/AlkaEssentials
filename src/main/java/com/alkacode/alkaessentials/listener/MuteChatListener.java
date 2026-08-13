package com.alkacode.alkaessentials.listener;

import com.alkacode.alkaessentials.command.MuteChatCommand;
import com.alkacode.alkaessentials.util.ChatUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Bloqueia o chat global quando /mutechat esta ativo (salvo quem tem bypass). */
public final class MuteChatListener implements Listener {

    private final MuteChatCommand muteChat;

    public MuteChatListener(MuteChatCommand muteChat) {
        this.muteChat = muteChat;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        if (!muteChat.isMuted()) {
            return;
        }
        if (event.getPlayer().hasPermission("alkassentials.chat.mutechat.bypass")) {
            return;
        }
        event.setCancelled(true);
        ChatUtil.sendKey(event.getPlayer(), "chat-muted");
    }
}
