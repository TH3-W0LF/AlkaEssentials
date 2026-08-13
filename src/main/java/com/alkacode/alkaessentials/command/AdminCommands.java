package com.alkacode.alkaessentials.command;

import com.alkacode.alkaessentials.util.ChatUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

import java.util.List;

/** Comandos de admin adicionais: gamemode, fix, speed, tempo/clima global, top e jump. */
public final class AdminCommands extends BaseCommand {

    public AdminCommands(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        switch (name) {
            case "gamemode":
            case "gmc":
            case "gms":
            case "gma":
            case "gmsp":
                return gamemode(sender, label, args);
            case "fix":
            case "fixall":
                return fix(sender, label);
            case "speed":
                return speed(sender, args);
            case "day":
            case "night":
            case "sun":
            case "rain":
                return timeWeather(sender, label, args);
            case "top":
            case "jump":
                return topJump(sender, label);
            default:
                return false;
        }
    }

    // ---------- gamemode ----------

    private boolean gamemode(CommandSender sender, String label, String[] args) {
        if (!requirePerm(sender, "alkassentials.admin.gamemode")) return true;
        GameMode mode = resolveMode(label, args);
        if (mode == null) {
            ChatUtil.send(sender, "<red>Uso: /gamemode <modo> [jogador]");
            return true;
        }
        Player target = resolveTarget(sender, args, isAlias(label) ? 0 : 1);
        if (target == null) return true;
        target.setGameMode(mode);
        String modeName = modeName(mode);
        ChatUtil.send(sender, "<green>Modo de <yellow>" + target.getName() + " <green>alterado para " + modeName);
        if (target != sender) {
            ChatUtil.send(target, "<green>Seu modo de jogo foi alterado para " + modeName);
        }
        return true;
    }

    private GameMode resolveMode(String label, String[] args) {
        return switch (label.toLowerCase()) {
            case "gmc" -> GameMode.CREATIVE;
            case "gms" -> GameMode.SURVIVAL;
            case "gma" -> GameMode.ADVENTURE;
            case "gmsp" -> GameMode.SPECTATOR;
            default -> {
                if (args.length == 0) yield null;
                yield switch (args[0].toLowerCase()) {
                    case "1", "creative", "c" -> GameMode.CREATIVE;
                    case "0", "survival", "s" -> GameMode.SURVIVAL;
                    case "2", "adventure", "a" -> GameMode.ADVENTURE;
                    case "3", "spectator", "sp" -> GameMode.SPECTATOR;
                    default -> null;
                };
            }
        };
    }

    private boolean isAlias(String label) {
        String l = label.toLowerCase();
        return l.equals("gmc") || l.equals("gms") || l.equals("gma") || l.equals("gmsp");
    }

    private String modeName(GameMode mode) {
        return switch (mode) {
            case CREATIVE -> "<aqua>Criativo";
            case SURVIVAL -> "<green>Sobrevivencia";
            case ADVENTURE -> "<yellow>Aventura";
            case SPECTATOR -> "<gray>Espectador";
            default -> mode.name();
        };
    }

    // ---------- fix ----------

    private boolean fix(CommandSender sender, String label) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        boolean all = label.equalsIgnoreCase("fixall");
        if (!requirePerm(sender, all ? "alkassentials.admin.fix.all" : "alkassentials.admin.fix")) return true;
        if (all) {
            for (ItemStack item : player.getInventory().getContents()) {
                repair(item);
            }
            ChatUtil.send(player, "<green>Todo o inventario foi reparado.");
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                ChatUtil.send(player, "<red>Segure um item na mao.");
                return true;
            }
            repair(hand);
            ChatUtil.send(player, "<green>Item reparado.");
        }
        return true;
    }

    private void repair(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (item.getItemMeta() instanceof Damageable meta) {
            meta.setDamage(0);
            item.setItemMeta(meta);
        }
    }

    // ---------- speed ----------

    private boolean speed(CommandSender sender, String[] args) {
        if (!requirePerm(sender, "alkassentials.admin.speed")) return true;
        if (args.length < 2) {
            ChatUtil.send(sender, "<red>Uso: /speed <fly|walk> <0-10> [jogador]");
            return true;
        }
        boolean fly;
        if (args[0].equalsIgnoreCase("fly") || args[0].equalsIgnoreCase("f")) {
            fly = true;
        } else if (args[0].equalsIgnoreCase("walk") || args[0].equalsIgnoreCase("w")) {
            fly = false;
        } else {
            ChatUtil.send(sender, "<red>Modo deve ser fly ou walk.");
            return true;
        }
        float speed;
        try {
            speed = Math.max(0, Math.min(10, Float.parseFloat(args[1]))) / 10f;
        } catch (NumberFormatException e) {
            ChatUtil.send(sender, "<red>Valor invalido. Use 0-10.");
            return true;
        }
        Player target = args.length > 2 ? matchPlayer(args[2]) : null;
        if (args.length > 2 && target == null) {
            ChatUtil.send(sender, "<red>Jogador nao encontrado.");
            return true;
        }
        if (target == null) {
            Player self = asPlayer(sender);
            if (self == null) {
                ChatUtil.send(sender, "<red>Especifique um jogador.");
                return true;
            }
            target = self;
        }
        if (fly) target.setFlySpeed(speed); else target.setWalkSpeed(speed);
        String mode = fly ? "voo" : "caminhada";
        ChatUtil.send(target, "<green>Velocidade de " + mode + " definida para <yellow>" + (int) (speed * 10));
        if (target != sender) {
            ChatUtil.send(sender, "<green>Velocidade de " + mode + " de <yellow>" + target.getName() + " <green>ajustada.");
        }
        return true;
    }

    // ---------- time / weather global ----------

    private boolean timeWeather(CommandSender sender, String label, String[] args) {
        if (!requirePerm(sender, "alkassentials.admin." + label.toLowerCase())) return true;
        World world = resolveWorld(sender, args);
        if (world == null) return true;
        switch (label.toLowerCase()) {
            case "day" -> { world.setTime(6000); ChatUtil.send(sender, "<green>Mundo <yellow>" + world.getName() + " <green>definido como dia."); }
            case "night" -> { world.setTime(18000); ChatUtil.send(sender, "<green>Mundo <yellow>" + world.getName() + " <green>definido como noite."); }
            case "sun" -> { world.setStorm(false); world.setThundering(false); ChatUtil.send(sender, "<green>Mundo <yellow>" + world.getName() + " <green>definido como limpo."); }
            case "rain" -> { world.setStorm(true); ChatUtil.send(sender, "<green>Mundo <yellow>" + world.getName() + " <green>definido como chuvoso."); }
            default -> { return false; }
        }
        return true;
    }

    private World resolveWorld(CommandSender sender, String[] args) {
        if (args.length > 0) {
            World w = plugin.getServer().getWorld(args[0]);
            if (w == null) {
                ChatUtil.send(sender, "<red>Mundo nao encontrado.");
                return null;
            }
            return w;
        }
        if (sender instanceof Player p) return p.getWorld();
        ChatUtil.send(sender, "<red>Especifique um mundo.");
        return null;
    }

    // ---------- top / jump ----------

    private boolean topJump(CommandSender sender, String label) {
        Player player = asPlayer(sender);
        if (player == null) return true;
        if (!requirePerm(sender, "alkassentials.admin." + label.toLowerCase())) return true;
        if (label.equalsIgnoreCase("top")) {
            Location loc = player.getLocation();
            int y = player.getWorld().getHighestBlockYAt(loc);
            loc.setY(y + 1);
            player.teleport(loc);
            ChatUtil.send(player, "<green>Teleportado para o topo.");
        } else {
            Block target = null;
            try {
                BlockIterator it = new BlockIterator(player, 100);
                while (it.hasNext()) {
                    Block b = it.next();
                    if (!b.getType().isAir()) {
                        target = b.getRelative(BlockFace.UP);
                        break;
                    }
                }
            } catch (IllegalStateException ignored) {
            }
            if (target == null) {
                ChatUtil.send(player, "<red>Nenhum bloco visivel.");
                return true;
            }
            player.teleport(target.getLocation().add(0.5, 0, 0.5).setDirection(player.getLocation().getDirection()));
            ChatUtil.send(player, "<green>Teleportado.");
        }
        return true;
    }

    // ---------- helpers ----------

    private Player resolveTarget(CommandSender sender, String[] args, int offset) {
        if (args.length > offset) {
            Player t = matchPlayer(args[offset]);
            if (t == null) {
                ChatUtil.send(sender, "<red>Jogador nao encontrado.");
                return null;
            }
            return t;
        }
        if (sender instanceof Player p) return p;
        ChatUtil.send(sender, "<red>Especifique um jogador.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if ((name.equals("gamemode") || name.equals("gm")) && args.length == 1) {
            return List.of("survival", "creative", "adventure", "spectator");
        }
        if (args.length == 1 && (name.equals("speed"))) {
            return List.of("fly", "walk");
        }
        return List.of();
    }
}
