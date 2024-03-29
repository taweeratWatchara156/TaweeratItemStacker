package com.taweerat.taweeratplugin.commands.events;

import com.taweerat.taweeratplugin.TaweeratItemStacker;
import net.minecraft.world.InteractionHand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPiglin;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class StackItem implements Listener {
    private TaweeratItemStacker instance = TaweeratItemStacker.getInstance();
    private double radius = TaweeratItemStacker.getInstance().getRadius();
    private HashMap<UUID, Long> cd = new HashMap<>();

    @EventHandler
    public void ItemMergeEvent(ItemMergeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void ItemSpawnEvent(ItemSpawnEvent event){
        Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();

        setHologram(item, itemStack.getAmount());

        if(!instance.validMaterial.contains(itemStack.getType())){
            for(Entity e : item.getNearbyEntities(radius, 2, radius)){
                if(e instanceof Item targetItem && targetItem.getItemStack().getType().equals(itemStack.getType())){
                    int amount = item.getFreezeTicks();
                    int targetAmount = targetItem.getFreezeTicks();

                    setHologram(item, amount + targetAmount);
                    targetItem.remove();
                }
            }
        }
    }

    @EventHandler
    public void EntityPickUpitem(EntityPickupItemEvent event){
        event.setCancelled(true);
        Entity e = event.getEntity();
        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();
        int amount = item.getFreezeTicks();

        if(instance.validMaterial.contains(item.getItemStack().getType())){
            event.setCancelled(false);
            return;
        }

        if(amount != -1){
            if(e instanceof Player e2){
                int remain = getInventoryRemain(e2.getInventory(), item.getItemStack(), 36);
                if(remain >= amount){
                    item.setItemStack(new ItemStack(item.getItemStack().getType(), amount));
                    event.setCancelled(false);
                }else {
                    event.setCancelled(true);
                    e2.getInventory().addItem(new ItemStack(item.getItemStack().getType(), remain));
                    e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5f,new Random().nextFloat() * (2.0f - 1.5f) + 1.5f);
                    setHologram(item, amount - remain);
                }
            }else if(e instanceof Piglin e2){
                event.setCancelled(true);
                CraftPiglin craftPiglin = (CraftPiglin) e2;
                net.minecraft.world.entity.monster.piglin.Piglin piglin = craftPiglin.getHandle();
                net.minecraft.world.item.Item item1 = ((CraftItem) item).getHandle().getItem().getItem();
                net.minecraft.world.item.ItemStack itemToAdd = ((CraftItem) item).getHandle().getItem();
                itemToAdd.setCount(1);

                if(!cd.containsKey(piglin.getUUID())){
                    int returnAmount  = amount - 1;

                    e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1f);
                    if(piglin.getOffhandItem().getItem().toString() == "air"){
                        piglin.setItemInHand(InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(item1, 1));
                        if(returnAmount > 0){
                            setHologram(item, returnAmount);
                        }else{
                            item.remove();
                        }
                    }
                }else{
                    long d = System.currentTimeMillis() - cd.get(piglin.getUUID());
                    if(d >= 1000){
                        int returnAmount  = amount - 1;

                        e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1f);
                        if(piglin.getOffhandItem().getItem().toString() == "air"){
                            piglin.setItemInHand(InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(item1, 1));
                            if(returnAmount > 0){
                                setHologram(item, returnAmount);
                            }else{
                                item.remove();
                            }
                        }
                    }
                }
            }else if(e instanceof Villager e2){
                event.setCancelled(true);
                Inventory inventory = e2.getInventory();

                int remainAmount = getInventoryRemain(e2.getInventory(), itemStack, 8);

                if(amount <= remainAmount){
                    ItemStack itemToAdd = new ItemStack(itemStack.getType(), amount);
                    inventory.addItem(itemToAdd);
                    e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1f);
                    item.remove();
                }else{
                    ItemStack itemToAdd = new ItemStack(itemStack.getType(), remainAmount);
                    inventory.addItem(itemToAdd);
                    e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1f);
                    setHologram(item, amount - remainAmount);
                }
            }else if(e instanceof Allay e2){
                event.setCancelled(true);
                Inventory inventory = e2.getInventory();

                int remainAmount = getInventoryRemain(e2.getInventory(), itemStack, 1);

                if(amount <= remainAmount){
                    ItemStack itemToAdd = new ItemStack(itemStack.getType(), amount);
                    inventory.addItem(itemToAdd);
                    e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1f);
                    item.remove();
                }else{
                    ItemStack itemToAdd = new ItemStack(itemStack.getType(), remainAmount);
                    inventory.addItem(itemToAdd);
                    e2.getWorld().playSound(e2, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1f);
                    setHologram(item, amount - remainAmount);
                }
            }
        }
    }

    @EventHandler
    public void piglinCd(PiglinBarterEvent event){
        Piglin e2 = event.getEntity();
        CraftPiglin craftPiglin = (CraftPiglin) e2;
        net.minecraft.world.entity.monster.piglin.Piglin piglin = craftPiglin.getHandle();
        cd.put(piglin.getUUID(), System.currentTimeMillis());
    }

    private void setHologram(Item item, int amount){
        item.setFreezeTicks(amount);

        StringBuilder result = new StringBuilder();
        String[] words = item.getItemStack().getType().toString().toLowerCase().split("_");

        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
            }
        }

        item.setCustomNameVisible(true);
        item.setCustomName((ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "▷ ") +
                ChatColor.GOLD + "" + ChatColor.BOLD + "X" + amount + " " + ChatColor.RESET + result);
    }

    public int getInventoryRemain(Inventory inventory, ItemStack item, int size) {
        int maxStack = item.getMaxStackSize();


        int remain = 0;

        for (int i = 0;i < size;i++){
            ItemStack itemStack = inventory.getItem(i);


            if(itemStack == null){
                remain = remain + maxStack;
            }else if(itemStack.getType().equals(item.getType()) && itemStack.getAmount() < maxStack){
                int a = maxStack - itemStack.getAmount();
                remain += a;
            }
        }

        return remain;
    }
}
