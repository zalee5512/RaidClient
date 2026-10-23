package com.raidclient.command;

import com.raidclient.gui.AutoTextScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class CommandAutoText extends CommandBase {

    @Override
    public String getCommandName() {
        return "autotext";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/autotext";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Available for all players
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft.getMinecraft().displayGuiScreen(new AutoTextScreen());
        });
    }
}
