package com.raidclient.command;

import com.raidclient.mod.impl.waypoints.WaypointsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandKoth extends CommandBase {

    @Override
    public String getCommandName() {
        return "koth";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/koth <nombre>";
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
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Uso: /koth <nombre> (ej: /koth Citadel)"));
            return;
        }

        String name = "KoTH " + String.join(" ", args);
        Minecraft mc = Minecraft.getMinecraft();
        WaypointsMod mod = WaypointsMod.INSTANCE;

        if (mod == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Raid Client] Mod Waypoints no activo."));
            return;
        }

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        mod.addWaypoint(name, (int) x, (int) y, (int) z, 0xFFFFD700, true);
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[Raid Waypoints] " +
                EnumChatFormatting.YELLOW + "⚔ " + name + " marcado en " +
                String.format("(%.0f, %.0f, %.0f)", x, y, z)));
    }
}
