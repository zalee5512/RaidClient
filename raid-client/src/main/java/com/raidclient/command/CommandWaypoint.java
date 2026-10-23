package com.raidclient.command;

import com.raidclient.mod.impl.waypoints.Waypoint;
import com.raidclient.mod.impl.waypoints.WaypointsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;

public class CommandWaypoint extends CommandBase {

    @Override
    public String getCommandName() {
        return "waypoint";
    }

    @Override
    public List<String> getCommandAliases() {
        return java.util.Collections.singletonList("wp");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/waypoint <add|remove|list> [nombre] [x y z]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        Minecraft mc = Minecraft.getMinecraft();
        WaypointsMod mod = WaypointsMod.INSTANCE;

        if (mod == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Raid Waypoints] Mod no inicializado."));
            return;
        }

        if (sub.equals("add")) {
            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Uso: /waypoint add <nombre> [x y z]"));
                return;
            }

            String name = args[1];
            double x = mc.thePlayer.posX;
            double y = mc.thePlayer.posY;
            double z = mc.thePlayer.posZ;

            if (args.length >= 5) {
                try {
                    x = Double.parseDouble(args[2]);
                    y = Double.parseDouble(args[3]);
                    z = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Coordenadas inválidas."));
                    return;
                }
            }

            boolean isKoth = name.toLowerCase().contains("koth") || name.toLowerCase().contains("citadel");
            int color = isKoth ? 0xFFFFD700 : 0xFF00E5FF;

            mod.addWaypoint(name, (int)x, (int)y, (int)z, color, isKoth);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Waypoints] " +
                    EnumChatFormatting.GREEN + "Waypoint '" + name + "' guardado en " +
                    String.format("(%.0f, %.0f, %.0f) para este servidor.", x, y, z)));

        } else if (sub.equals("remove") || sub.equals("del")) {
            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Uso: /waypoint remove <nombre>"));
                return;
            }

            String name = args[1];
            boolean removed = mod.removeWaypoint(name);
            if (removed) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Waypoints] " +
                        EnumChatFormatting.YELLOW + "Waypoint '" + name + "' eliminado."));
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No se encontró el waypoint '" + name + "'."));
            }

        } else if (sub.equals("list")) {
            List<Waypoint> list = mod.getWaypointsForCurrentServer();
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "=== Waypoints en " + mod.getCurrentServerIp() + " ==="));
            if (list.isEmpty()) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "No hay waypoints configurados. Usa /waypoint add <nombre>"));
            } else {
                for (Waypoint w : list) {
                    String prefix = w.isKoth() ? EnumChatFormatting.GOLD + "[KoTH] " : EnumChatFormatting.AQUA + "• ";
                    sender.addChatMessage(new ChatComponentText(prefix + EnumChatFormatting.WHITE + w.getName() +
                            EnumChatFormatting.GRAY + " (" + (int)w.getX() + ", " + (int)w.getY() + ", " + (int)w.getZ() + ")"));
                }
            }
        } else {
            sendHelp(sender);
        }
    }

    private void sendHelp(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "--- Comandos de Waypoints (Lunar Style) ---"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/waypoint add <nombre> " + EnumChatFormatting.GRAY + "- Añade waypoint en tu posición"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/waypoint add <nombre> <x> <y> <z> " + EnumChatFormatting.GRAY + "- Añade en coordenadas"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/waypoint remove <nombre> " + EnumChatFormatting.GRAY + "- Elimina waypoint"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/waypoint list " + EnumChatFormatting.GRAY + "- Lista waypoints del servidor"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/koth <nombre> " + EnumChatFormatting.GRAY + "- Añade un KoTH en tu posición"));
    }
}
