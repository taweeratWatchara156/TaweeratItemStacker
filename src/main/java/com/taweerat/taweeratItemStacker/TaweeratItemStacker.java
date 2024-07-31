package com.taweerat.taweeratItemStacker;

import com.taweerat.taweeratItemStacker.commands.SetRadiusCommand;
import com.taweerat.taweeratItemStacker.listerners.ItemStacker;
import com.taweerat.taweeratItemStacker.model.ExceptionItemToStack;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public final class TaweeratItemStacker extends JavaPlugin {
    public static TaweeratItemStacker instance;
    private final Logger logger = getLogger();
    public Map<String, Integer> stackerData;
    public Map<String, Boolean> piglinState;
    List<Material> exceptionMaterials = new ArrayList<>();
    private FileConfiguration dataConfig;
    private File dataConfigFile;
    BukkitRunnable task;

    @Override
    public void onEnable() {
//        init instance
        instance = this;

        saveDefaultConfig();
        initConfig();
        exceptionMaterials = loadConfig();

//        Stacker data
        saveDefaultDataConfig();
        stackerData = loadDataConfig();
        piglinState = loadPiglinDataConfig();

        logger.info("Hello from Taweerat.");

        getServer().getPluginManager().registerEvents(new ItemStacker(), this);

//        task = new BukkitRunnable() {
//            @Override
//            public void run() {
//                for (World world : getServer().getWorlds()){
//                    for (Entity entity : world.getEntities()){
//                        if(entity.isOnGround()){
//                            if(entity instanceof Item item){
//                                ItemStacker stacker = new ItemStacker();
//                                int amount = stacker.getAmount(item);
//                                if(amount != -1){
//                                    try {
//                                        stacker.stackItem(item, true);
//                                    } catch (IOException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        };
//
//        task.runTaskTimer(this, 200L, 200L);

//        Commands
        Objects.requireNonNull(getCommand("setRadius")).setExecutor(new SetRadiusCommand(this));
    }
    @Override
    public void onDisable() {
        logger.info("Goodbye from Taweerat.");
    }
    public static TaweeratItemStacker getInstance() {
        return instance;
    }

    public void initConfig(){
        List<String> data = new ArrayList<>();

        if(!getConfig().contains("radius")){
            getConfig().set("radius.x", 6D);
            getConfig().set("radius.y", 2D);
            getConfig().set("radius.z", 6D);

            saveConfig();
        }

        if(!getConfig().contains("exceptionMaterials")){
            for (ExceptionItemToStack matString : ExceptionItemToStack.values()){
                data.add(matString.toString());
            }

            for (Material mat : Material.values()){
                if(mat.toString().endsWith("SMITHING_TEMPLATE") || mat.toString().startsWith("MUSIC_DISC") || mat.toString().endsWith("BANNER_PATTERN")){
                    data.add(mat.toString());
                }
            }

            getConfig().set("exceptionMaterials", data);
            saveConfig();
        }
    }

    public List<Material> loadConfig(){
        List<Material> data = new ArrayList<>();

        if(getConfig().contains("exceptionMaterials")){
            for (String s : getConfig().getStringList("exceptionMaterials")){
                data.add(Material.valueOf(s));
            }
        }

        return data;
    }

    public void reloadDataConfig() {
        if (dataConfigFile == null) {
            dataConfigFile = new File(getDataFolder(), "data.yml");
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataConfigFile);
    }

    public void saveDefaultDataConfig(){
        if(dataConfigFile == null){
            dataConfigFile = new File(getDataFolder(), "data.yml");
        }
        if(!dataConfigFile.exists()){
            saveResource("data.yml", false);
        }
    }

    public void saveData(Map<String, Integer> data) throws IOException {
        getDataConfig().set("stacker", null);

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            getDataConfig().set("stacker." + entry.getKey(), entry.getValue());
        }

        getDataConfig().save(dataConfigFile);
    }

    public void saveRadiusData(Vector3d v){
        getConfig().set("radius.x", v.x);
        getConfig().set("radius.y", v.y);
        getConfig().set("radius.z", v.z);

        saveConfig();
    }


    public Vector3d getRadius() {
        return new Vector3d(getConfig().getDouble("radius.x"), getConfig().getDouble("radius.y"), getConfig().getDouble("radius.z"));
    }

    public void savePiglinData(Map<String, Boolean> data) throws IOException {
        getDataConfig().set("piglinState", null);

        for (Map.Entry<String, Boolean> entry : data.entrySet()) {
            getDataConfig().set("piglinState." + entry.getKey(), entry.getValue());
        }

        getDataConfig().save(dataConfigFile);
    }

    public Map<String, Integer> loadDataConfig(){
        Map<String, Integer> data = new HashMap<>();

        if(getDataConfig().contains("stacker")){
            for(String key : Objects.requireNonNull(getDataConfig().getConfigurationSection("stacker")).getKeys(false)){
                data.put(key, getDataConfig().getInt("stacker." + key));
            }
        }

        return data;
    }

    public Map<String, Boolean> loadPiglinDataConfig(){
        Map<String, Boolean> data = new HashMap<>();

        if(getDataConfig().contains("piglinState")){
            for(String key : Objects.requireNonNull(getDataConfig().getConfigurationSection("piglinState")).getKeys(false)){
                data.put(key, getDataConfig().getBoolean("piglinState." + key));
            }
        }

        return data;
    }

    public FileConfiguration getDataConfig() {
        if (dataConfig == null) {
            reloadDataConfig();
        }
        return dataConfig;
    }

    public Map<String, Integer> getStackerData() {
        return stackerData;
    }

    public Map<String, Boolean> getPiglinState() {
        return piglinState;
    }

    public List<Material> getExceptionMaterials() {
        return exceptionMaterials;
    }
}
