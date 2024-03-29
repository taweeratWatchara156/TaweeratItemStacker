package com.taweerat.taweeratplugin;

import com.taweerat.taweeratplugin.commands.SetRadiusCommand;
import com.taweerat.taweeratplugin.commands.events.StackItem;
import com.taweerat.taweeratplugin.commands.ref.ValidMaterialToStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class TaweeratItemStacker extends JavaPlugin {
    private static TaweeratItemStacker instance;
    public ArrayList<Material> validMaterial = new ArrayList<>();
    private List<String> cv;

    @Override
    public void onEnable() {
        instance = this;

        //config
        saveDefaultConfig();
        cv = getConfig().getStringList("exceptionMaterial");

        //commands
        getCommand("setradius").setExecutor(new SetRadiusCommand());

        //listener
        getServer().getPluginManager().registerEvents(new StackItem(), this);

        //initialize list
        for (ValidMaterialToStack value : ValidMaterialToStack.values()){
            try{
                Material mat = Material.valueOf(value.toString());
                validMaterial.add(mat);
            }catch (IllegalArgumentException e){
                getLogger().severe(e.getMessage());
            }
        }

        for (String s : cv){
            try{
                Material mat = Material.valueOf(s);
                if(!validMaterial.contains(mat)) validMaterial.add(mat);
            }catch (IllegalArgumentException e){
                getLogger().severe(e.getMessage());
            }
        }
    }

    public double getRadius(){
        return getInstance().getConfig().getDouble("radius");
    }

    public void setRadius(double radius){
        getInstance().getConfig().set("radius", radius);
        getInstance().saveConfig();
    }

    public static TaweeratItemStacker getInstance() {
        return instance;
    }
}
