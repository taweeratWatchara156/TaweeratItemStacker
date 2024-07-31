package com.taweerat.taweeratItemStacker.listerners;

import com.taweerat.taweeratItemStacker.TaweeratItemStacker;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_21_R1.entity.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemStacker implements Listener {
    private final TaweeratItemStacker instance = TaweeratItemStacker.getInstance();
    Map<String, Integer> stacker = instance.getStackerData();
    Map<String, Boolean> piglinState = instance.getPiglinState();
    List<Material> exceptionMaterials = instance.getExceptionMaterials();

//    item Spawn event
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onItemSpawn(ItemSpawnEvent event) throws IOException {
        Item item = event.getEntity();
        initItemStack(item);

        stackItem(item, false);
    }

//    item despawn event
    @EventHandler
    private void itemDespawnEvent(ItemDespawnEvent event) throws IOException {
        Item item = event.getEntity();

        stacker.remove(item.getUniqueId().toString());
        instance.saveData(stacker);
    }

//    Disable item merge builtin
    @EventHandler
    private void itemMerge(ItemMergeEvent event){
        event.setCancelled(true);
    }

//    Entity PickUp item event
    @EventHandler
    private void onItemPickUp(EntityPickupItemEvent event) throws IOException {
        event.setCancelled(true);
        Item item = event.getItem();
        Entity entity = event.getEntity();
        ItemStack itemStack = item.getItemStack();
        int amount = getAmount(item);
        ItemEntity itemEntity = ((CraftItem) item).getHandle();

        if(isException(item)) {
            if (amount != -1) {
                switch (entity) {
                    case Player player -> {
                        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                        ServerGamePacketListenerImpl listener = serverPlayer.connection;

                        int remainAmount = getRemainingAmount(player.getInventory(), item, 36);

                        if (amount <= remainAmount) {
                            ClientboundTakeItemEntityPacket packet = new ClientboundTakeItemEntityPacket(itemEntity.getId(), serverPlayer.getId(), 1);
                            listener.send(packet);

                            itemEntity.getItem().setCount(amount);
                            serverPlayer.getInventory().add(itemEntity.getItem());

                            removeStack(item);
                        } else {
                            int remain = amount - remainAmount;
                            updateStack(item, remain);

                            itemEntity.getItem().setCount(remainAmount);
                            serverPlayer.getInventory().add(itemEntity.getItem());
                        }
                    }
                    case Piglin piglin -> {
                        net.minecraft.world.entity.monster.piglin.Piglin piglinMc = ((CraftPiglin) piglin).getHandle();
                        if (itemStack.getType().equals(Material.GOLD_INGOT)) {
                            if (amount <= 1) {
                                if (piglinState.containsKey(piglin.getUniqueId().toString()) && piglinState.get(piglin.getUniqueId().toString())) {
                                    piglinState.replace(piglin.getUniqueId().toString(), false);
                                    instance.savePiglinData(piglinState);
                                    if (itemEntity.getOwner() instanceof ServerPlayer serverPlayer) {
                                        ClientboundTakeItemEntityPacket packet = new ClientboundTakeItemEntityPacket(itemEntity.getId(), piglinMc.getId(), 1);
                                        serverPlayer.connection.send(packet);
                                    }

                                    itemEntity.getItem().setCount(amount);
                                    piglinMc.setItemInHand(InteractionHand.OFF_HAND, itemEntity.getItem());

                                    removeStack(item);
                                }
                            } else {
                                if (piglinState.containsKey(piglin.getUniqueId().toString()) && piglinState.get(piglin.getUniqueId().toString())) {
                                    piglinState.replace(piglin.getUniqueId().toString(), false);
                                    instance.savePiglinData(piglinState);
                                    updateStack(item, amount - 1);
                                    itemEntity.getItem().setCount(1);
                                    piglinMc.setItemInHand(InteractionHand.OFF_HAND, itemEntity.getItem());
                                }
                            }
                        }
                    }
                    case Villager villager -> {
                        net.minecraft.world.entity.npc.Villager villagerMc = ((CraftVillager) villager).getHandle();
                        ServerGamePacketListenerImpl listener = null;
                        if (itemEntity.getOwner() instanceof ServerPlayer _serverPlayer) {
                            listener = _serverPlayer.connection;
                        }

                        int remainAmount = getRemainingAmount(villager.getInventory(), item, 8);

                        if (amount <= remainAmount) {
                            ClientboundTakeItemEntityPacket packet = new ClientboundTakeItemEntityPacket(itemEntity.getId(), villagerMc.getId(), 1);
                            if (listener != null) {
                                listener.send(packet);
                            }

                            float stack = (float) amount / itemStack.getMaxStackSize();

                            if (stack <= 1.0f) {
                                itemEntity.getItem().setCount(amount);
                                villagerMc.getInventory().addItem(itemEntity.getItem());
                            } else {
                                itemEntity.getItem().setCount(itemStack.getMaxStackSize());
                                for (int i = 0; i < (int) stack; i++) {
                                    villagerMc.getInventory().addItem(itemEntity.getItem());
                                }

                                if (amount % itemStack.getMaxStackSize() != 0) {
                                    itemEntity.getItem().setCount(amount % itemStack.getMaxStackSize());
                                    villagerMc.getInventory().addItem(itemEntity.getItem());
                                }
                            }

                            removeStack(item);
                        } else {
                            int remain = amount - remainAmount;
                            updateStack(item, remain);

                            float stack = (float) remainAmount / itemStack.getMaxStackSize();
                            if (stack <= 1.0f) {
                                itemEntity.getItem().setCount(remainAmount);
                                villagerMc.getInventory().addItem(itemEntity.getItem());
                            } else {
                                itemEntity.getItem().setCount(itemStack.getMaxStackSize());
                                for (int i = 0; i < (int) stack; i++) {
                                    villagerMc.getInventory().addItem(itemEntity.getItem());
                                }

                                if (remainAmount % itemStack.getMaxStackSize() != 0) {
                                    itemEntity.getItem().setCount(remainAmount % itemStack.getMaxStackSize());
                                    villagerMc.getInventory().addItem(itemEntity.getItem());
                                }
                            }
                        }
                    }
                    case Allay allay -> {
                        net.minecraft.world.entity.animal.allay.Allay allayMc = ((CraftAllay) allay).getHandle();

                        ServerGamePacketListenerImpl listener = null;
                        if (itemEntity.getOwner() instanceof ServerPlayer _serverPlayer) {
                            listener = _serverPlayer.connection;
                        }

                        int remainAmount = getRemainingAmount(allay.getInventory(), item, 1);

                        if (amount <= remainAmount) {
                            ClientboundTakeItemEntityPacket packet = new ClientboundTakeItemEntityPacket(itemEntity.getId(), allayMc.getId(), 1);
                            if (listener != null) {
                                listener.send(packet);
                            }

                            itemEntity.getItem().setCount(amount);
                            allayMc.getInventory().addItem(itemEntity.getItem());

                            removeStack(item);
                        } else {
                            int remain = amount - remainAmount;
                            itemEntity.getItem().setCount(remainAmount);
                            allayMc.getInventory().addItem(itemEntity.getItem());
                            updateStack(item, remain);
                        }
                    }
                    case Fox fox -> {
                        ServerGamePacketListenerImpl listener = null;
                        net.minecraft.world.entity.animal.Fox foxMc = ((CraftFox) fox).getHandle();
                        if (itemEntity.getOwner() instanceof ServerPlayer _serverPlayer) {
                            listener = _serverPlayer.connection;
                        }

                        if (amount > 1) {
                            updateStack(item, amount - 1);
                            itemEntity.getItem().setCount(1);
                            foxMc.setItemSlot(EquipmentSlot.MAINHAND, itemEntity.getItem());
                        } else {
                            ClientboundTakeItemEntityPacket packet = new ClientboundTakeItemEntityPacket(itemEntity.getId(), foxMc.getId(), 1);
                            if (listener != null) {
                                listener.send(packet);
                            }

                            itemEntity.getItem().setCount(1);
                            foxMc.setItemSlot(EquipmentSlot.MAINHAND, itemEntity.getItem());
                            removeStack(item);
                        }
                    }
                    default -> {
                    }
                }
            }
        }else{
            event.setCancelled(false);
        }
    }

//    Piglin barthering event
    @EventHandler
    public void piglinBartheringEvent(PiglinBarterEvent event) {
        Piglin piglin = event.getEntity();
        if(piglinState.containsKey(piglin.getUniqueId().toString())){

            new BukkitRunnable() {
                @Override
                public void run() {
                    piglinState.replace(piglin.getUniqueId().toString(), true);
                    try {
                        instance.savePiglinData(piglinState);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.runTaskLater(instance, 60L); // 60 ticks = 3 seconds
        }
    }

//    init piglin
    @EventHandler
    public void piglinSpawn(EntitySpawnEvent event) throws IOException {
        Entity entity = event.getEntity();

        if(entity instanceof Piglin piglin){
            piglinState.put(piglin.getUniqueId().toString(), true);
            instance.savePiglinData(piglinState);
        }
    }

//    piglin zombified event
    @EventHandler
    public void detectPiglinZombified(EntityTransformEvent event) throws IOException {
        Entity entity = event.getEntity();
        if(entity instanceof CraftPiglin){
            Bukkit.broadcastMessage(entity.getUniqueId().toString());
            if(piglinState.containsKey(entity.getUniqueId().toString())){
                piglinState.remove(entity.getUniqueId().toString());
                instance.savePiglinData(piglinState);
            }
        }
    }

//    piglin death event
    @EventHandler
    public void piglinDeathEvent(EntityDeathEvent event) throws IOException {
        Entity entity = event.getEntity();

        if(entity instanceof Piglin piglin){
            if(piglinState.containsKey(piglin.getUniqueId().toString())){
                piglinState.remove(piglin.getUniqueId().toString());
                instance.savePiglinData(piglinState);
            }
        }
    }

//    Hopper
    @EventHandler
    public void hopperPickUp(InventoryPickupItemEvent event) throws IOException {
        event.setCancelled(true);
        Inventory inventory = event.getInventory();
        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();
        int amount = getAmount(item);

        if(inventory.getType() == InventoryType.HOPPER){
            int remainAmount = getRemainingAmount(inventory, item, 5);

            if(amount != -1){
                if(amount <= remainAmount){
                    float stack = (float) amount / itemStack.getMaxStackSize();

                    if(stack <= 1.0f){
                        inventory.addItem(new ItemStack(itemStack.getType(), amount));
                    }else{
                        for (int i = 0;i < (int) stack;i++){
                            inventory.addItem(new ItemStack(itemStack.getType(), itemStack.getMaxStackSize()));
                        }

                        if(amount % itemStack.getMaxStackSize() != 0){
                            inventory.addItem(new ItemStack(itemStack.getType(), amount % itemStack.getMaxStackSize()));
                        }
                    }

                    removeStack(item);
                }else{
                    int remain = amount - remainAmount;
                    updateStack(item, remain);

                    float stack = (float) remainAmount / itemStack.getMaxStackSize();
                    if(stack <= 1.0f){
                        inventory.addItem(new ItemStack(itemStack.getType(), remainAmount));
                    }else{
                        for (int i = 0;i < (int) stack;i++){
                            inventory.addItem(new ItemStack(itemStack.getType(), itemStack.getMaxStackSize()));
                        }

                        if(remainAmount % itemStack.getMaxStackSize() != 0){
                            inventory.addItem(new ItemStack(itemStack.getType(), remainAmount % itemStack.getMaxStackSize()));
                        }
                    }
                }
            }else{
                inventory.addItem(item.getItemStack());
                item.remove();
            }
        }
    }

//    Debug Stick
    @EventHandler
    private void removeAllItemStack(PlayerInteractEvent event) throws IOException {
        Action action = event.getAction();
        ItemStack itemStack = event.getItem();

        if(event.getPlayer().isSneaking() && action.equals(Action.RIGHT_CLICK_AIR)){
            assert itemStack != null;
            if(itemStack.getType().equals(Material.STICK)){
                stacker = new HashMap<>();
                instance.saveData(stacker);

                piglinState = new HashMap<>();
                instance.savePiglinData(piglinState);
            }
        }
    }

//   on Reload
    @EventHandler
    private void onReload(PlayerCommandPreprocessEvent event) throws IOException {
        String command = event.getMessage();

        if(command.equalsIgnoreCase("/reload confirm")){
            instance.saveData(stacker);
        }
    }

//    Get Entity Nearby

    private Item checkEntity(Item item){
        List<Entity> entityNb = item.getNearbyEntities(instance.getRadius().x(), instance.getRadius().y(), instance.getRadius().z());
        for (Entity entity : entityNb){
            if(entity instanceof Item itemNb && itemNb.getItemStack().getType().equals(item.getItemStack().getType()) && isException(itemNb)){
                return itemNb;
            }
        }

        return null;
    }

//    Stack item

    public boolean stackItem(Item item, boolean effect) throws IOException {
        Item itemNb = checkEntity(item);
        if(itemNb != null){
            int amount = getAmount(item);
            int nbAmount = getAmount(itemNb);

            if(amount != -1 && nbAmount != -1){
                updateStack(item, amount + nbAmount);
                removeStack(itemNb);
                if(effect){
                    item.getWorld().spawnParticle(Particle.EXPLOSION, item.getLocation(), 1, 0, 0, 0, 0.1);
                }

                return true;
            }
        }

        return false;
    }

    //    Get amount of item Stacker
    public int getAmount(Item item){
        if(stacker.containsKey(item.getUniqueId().toString())){
            return stacker.get(item.getUniqueId().toString());
        }

        return -1;
    }

//    remove ItemStacker
    public void removeStack(Item item) throws IOException {
        if(stacker.containsKey(item.getUniqueId().toString())){
            stacker.remove(item.getUniqueId().toString());
            instance.saveData(stacker);
            item.remove();
        }else{
            instance.getLogger().severe("Error occurred at ItemStacker:removeStack() : Key doesn't exists");
        }
    }

//    update ItemStacker
    private void updateStack(Item item, int amount) throws IOException {
        item.setCustomName(null);
        item.setCustomName(stackerFormat(item, amount));

        if(stacker.containsKey(item.getUniqueId().toString())){
            stacker.remove(item.getUniqueId().toString());
            stacker.put(item.getUniqueId().toString(), amount);
        }else{
            stacker.put(item.getUniqueId().toString(), amount);
        }

        instance.saveData(stacker);
    }

//    Init itemStack custom name / add to data map
    private void initItemStack(Item item) throws IOException {
        item.setCustomNameVisible(true);
        item.setCustomName(stackerFormat(item, item.getItemStack().getAmount()));

        if(isException(item)){
            stacker.put(item.getUniqueId().toString(), item.getItemStack().getAmount());
            item.getItemStack().setAmount(1);
            instance.saveData(stacker);
        }
    }

//    Check if item is in exception List
    private boolean isException(Item item){
        return !exceptionMaterials.contains(item.getItemStack().getType());
    }

//    item stacker name format
    private String stackerFormat(Item item, int amount){
        String res;

        if(item.getItemStack().hasItemMeta() && Objects.requireNonNull(item.getItemStack().getItemMeta()).hasDisplayName()){
            res = "" + ChatColor.GOLD + ChatColor.BOLD + "▶ " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + (amount + "x ") + ChatColor.RESET + item.getItemStack().getItemMeta().getDisplayName();
        }else{
            res = "" + ChatColor.GOLD + ChatColor.BOLD + "▶ " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + (amount + "x ") + ChatColor.RESET + item.getName();
        }

        return res;
    }


    private int getRemainingAmount(Inventory inventory, Item item, int storageSize) {
        int maxStack = item.getItemStack().getMaxStackSize();
        int remaining = 0;

        for (int i = 0; i < storageSize; i++) {
            ItemStack itemFound = inventory.getItem(i);
            if (itemFound == null) {
                remaining += maxStack;
            } else if (itemFound.getType().equals(item.getItemStack().getType()) && itemFound.getAmount() != itemFound.getMaxStackSize()) {
                remaining += (itemFound.getMaxStackSize() - itemFound.getAmount());
            }
        }
        return remaining;
    }
}
