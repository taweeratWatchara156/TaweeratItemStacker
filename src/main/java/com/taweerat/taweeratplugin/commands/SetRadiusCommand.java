package com.taweerat.taweeratplugin.commands;

import com.taweerat.taweeratplugin.TaweeratItemStacker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SetRadiusCommand implements CommandExecutor {
    TaweeratItemStacker instance = TaweeratItemStacker.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length > 0){
            try{
                double r = Double.parseDouble(strings[0]);
                instance.setRadius(r);
                commandSender.sendMessage("[ " + ChatColor.GOLD + "" + ChatColor.BOLD + "Taweerat Plugin " + ChatColor.RESET + "] " +
                        "Set '" + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + ((int) instance.getRadius()) + ChatColor.RESET + "' to stacker radius!");
            }catch (NumberFormatException e){
                commandSender.sendMessage("[ " + ChatColor.GOLD + "" + ChatColor.BOLD + "Taweerat Plugin " + ChatColor.RESET + "] " +
                        "Cannot set '" + ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + strings[0] + ChatColor.RESET + "' to stacker radius");
            }
        }else{
            commandSender.sendMessage("[ " + ChatColor.GOLD + "" + ChatColor.BOLD + "Taweerat Plugin " + ChatColor.RESET + "] " +
                    "Please enter stacker radius");
        }

        return true;
    }
}
