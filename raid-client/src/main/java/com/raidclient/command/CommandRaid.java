package com.raidclient.command;

import com.raidclient.RaidClient;
import com.raidclient.gui.AutoTextScreen;
import com.raidclient.mod.impl.AutoTextMod;
import com.raidclient.mod.impl.TeamViewMod;
import com.raidclient.mod.impl.autotext.AutoTextMacro;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

public class CommandRaid extends CommandBase {

    @Override
    public String getCommandName() {
        return "raid";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/raid [friend|autotext]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Available for all players without OP
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().displayGuiScreen(RaidClient.INSTANCE.getClickGUI());
            });
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("friend")) {
            TeamViewMod teamView = RaidClient.INSTANCE.getModManager().getMod(TeamViewMod.class);
            if (teamView == null) return;

            if (args.length >= 3 && args[1].equalsIgnoreCase("add")) {
                teamView.addFriend(args[2]);
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Client] " + EnumChatFormatting.GREEN + "Jugador " + args[2] + " añadido a amigos de equipo."));
            } else if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                teamView.removeFriend(args[2]);
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Client] " + EnumChatFormatting.RED + "Jugador " + args[2] + " eliminado de amigos."));
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Client] Amigos registrados: " + EnumChatFormatting.YELLOW + teamView.getFriends()));
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Uso: /raid friend <add|remove|list> [nombre]"));
            }
        } else if (sub.equals("autotext") || sub.equals("macro")) {
            if (args.length == 1) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Minecraft.getMinecraft().displayGuiScreen(new AutoTextScreen());
                });
                return;
            }

            AutoTextMod autoText = RaidClient.INSTANCE.getModManager().getMod(AutoTextMod.class);
            if (autoText == null) return;

            if (args.length >= 4 && args[1].equalsIgnoreCase("add")) {
                try {
                    int key = Keyboard.getKeyIndex(args[2].toUpperCase());
                    if (key == Keyboard.KEY_NONE) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Raid Client] Tecla inválida: " + args[2]));
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
                    }
                    autoText.addMacro(key, sb.toString());
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Client] " + EnumChatFormatting.GREEN + "Macro añadido: [" + Keyboard.getKeyName(key) + "] -> " + sb.toString()));
                } catch (Exception e) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Uso: /raid autotext add <tecla> <comando/mensaje>"));
                }
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Raid Client] Macros configurados (" + autoText.getMacros().size() + "):"));
                for (AutoTextMacro macro : autoText.getMacros()) {
                    String status = macro.isEnabled() ? EnumChatFormatting.GREEN + "ACTIVO" : EnumChatFormatting.RED + "INACTIVO";
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "• [" + macro.getKeyName() + "] -> " + EnumChatFormatting.WHITE + macro.getMessage() + " (" + status + EnumChatFormatting.GRAY + ")"));
                }
            } else {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Minecraft.getMinecraft().displayGuiScreen(new AutoTextScreen());
                });
            }
        } else {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().displayGuiScreen(RaidClient.INSTANCE.getClickGUI());
            });
        }
    }
}
