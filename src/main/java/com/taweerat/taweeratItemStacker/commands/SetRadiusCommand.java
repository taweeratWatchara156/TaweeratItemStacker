package com.taweerat.taweeratItemStacker.commands;

import com.taweerat.taweeratItemStacker.TaweeratItemStacker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.joml.Vector3d;

public class SetRadiusCommand implements CommandExecutor {
    public TaweeratItemStacker instance;

    public SetRadiusCommand(TaweeratItemStacker instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player player){
            if(strings.length >= 3){
                try{
                    Vector3d req = new Vector3d(Double.parseDouble(strings[0]), Double.parseDouble(strings[1]), Double.parseDouble(strings[2]));
                    instance.saveRadiusData(req);
                    player.sendMessage("[ TaweeratItemStacker ] radius is set to X: " + strings[0] +  " Y: " + strings[1] +  " Z: " + strings[2] );
                    return true;
                }catch (NumberFormatException e){
                    player.sendMessage("[ TaweeratItemStacker ] " + ChatColor.RED + "arguments is not a number");
                }
            }
        }

        return false;
    }
}
