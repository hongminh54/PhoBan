package com.hongminh54.phoban.gui;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.ItemBuilder;
import com.hongminh54.phoban.utils.ItemUtil;
import com.hongminh54.phoban.utils.Messages;
import me.orineko.pluginspigottools.MethodDefault;
import me.orineko.pluginspigottools.NBTTag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class RewardGui implements Listener {

    public static void open(Player p, String name) {
        Game game = Game.getGame(name);
        FileConfiguration gui = FileManager.getFileConfig(Files.GUI);
        File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
        if (!configFile.exists()) try {
            configFile.createNewFile();
        } catch (Exception ignored) {
        }
        FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(configFile) : game.getConfig();

        Inventory inv = Bukkit.createInventory(null, gui.getInt("RewardGui.Rows") * 9, gui.getString("RewardGui.Title").replace("&", "§").replace("<name>", name));

        HashMap<String, List<String>> replace = new HashMap<>();
        replace.put("<reward_amount>", Collections.singletonList(String.valueOf(room.getInt("RewardAmount", 0))));

        ConfigurationSection contentSection = gui.getConfigurationSection("RewardGui.Content");
        if(contentSection != null) contentSection.getKeys(false).forEach(content -> {
            ItemStack item = ItemBuilder.build(Files.GUI, "RewardGui.Content." + content, replace);

            ItemStack item2 = ItemUtil.copyItem(item);
            if (gui.contains("RewardGui.Content." + content + ".ClickType"))
                item2 = NBTTag.setKey(item2, "RewardGui_ClickType", gui.getString("RewardGui.Content." + content + ".ClickType"));

            for (int slot : gui.getIntegerList("RewardGui.Content." + content + ".Slot")) {
                if (slot >= (gui.getInt("RewardGui.Rows") * 9)) return;

                if (slot <= -1) {
                    for (int i = 0; i < (gui.getInt("RewardGui.Rows") * 9); i++) inv.setItem(i, item2);
                    return;
                }

                inv.setItem(slot, item2);
            }
        });

        for (int reward_slot : gui.getIntegerList("RewardGui.RewardSlot"))
            inv.setItem(reward_slot, new ItemStack(Material.AIR));

        if (room.contains("Reward")) {
            ConfigurationSection section = room.getConfigurationSection("Reward");
            if (section != null) section.getKeys(false).forEach(s -> {
                ItemStack originalItem = room.contains("Reward." + s + ".Command") ? new ItemStack(Material.PAPER) :
                        room.getItemStack("Reward." + s + ".Item", new ItemStack(Material.PAPER));
                ItemStack item = ItemUtil.copyItem(originalItem);
                
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    if (room.contains("Reward." + s + ".Command")) {
                        String command = room.getString("Reward." + s + ".Command");
                        meta.setDisplayName("§f/" + command);
                    }

                    List<String> lores = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                    lores.add("§r");
                    for (String format : gui.getStringList("RewardGui.Format")) {
                        lores.add(format.replace("&", "§").replace("<chance>",
                                String.valueOf(room.getInt("Reward." + s + ".Chance"))));
                    }

                    meta.setLore(lores);
                    item.setItemMeta(meta);
                }

                item = NBTTag.setKey(item, "id_item_reward", s);
                item = NBTTag.setKey(item, "RewardGui_ID", s);

                inv.addItem(item);
            });
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(AEPhoBan.inst(), () -> {
            p.closeInventory();
            p.openInventory(inv);
            viewers.put(p, name);
        });
    }

    private static final HashMap<Player, String> viewers = new HashMap<>();

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (viewers.containsKey(p)) {
            String name = viewers.get(p);
            Game game = Game.getGame(name);
            FileConfiguration gui = FileManager.getFileConfig(Files.GUI);

            if (e.getClickedInventory() == p.getOpenInventory().getBottomInventory()) {
                if (e.isShiftClick()) e.setCancelled(true);
                return;
            }

            if (gui.getIntegerList("RewardGui.RewardSlot").contains(e.getSlot())) {
                ItemStack click = e.getCurrentItem() == null ? null : ItemUtil.copyItem(e.getCurrentItem());
                ItemStack cursor = e.getCursor() == null ? null : ItemUtil.copyItem(e.getCursor());
                
                if ((click == null || click.getType().equals(Material.AIR)) && (cursor != null && !cursor.getType().equals(Material.AIR))) {
                    String id = UUID.randomUUID().toString();
                    File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                    if (!configFile.exists()) try {
                        configFile.createNewFile();
                    } catch (Exception ignored) {
                    }
                    FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(configFile) : game.getConfig();

                    ItemStack itemToSave = ItemUtil.copyItem(cursor);
                    
                    if (itemToSave.getItemMeta() != null) {
                        ItemMeta meta = itemToSave.getItemMeta();
                        itemToSave.setItemMeta(meta);
                    }
                    
                    room.set("Reward." + id + ".Item", itemToSave);
                    room.set("Reward." + id + ".Chance", 100);
                    
                    FileManager.saveFileConfig(room, configFile);

                    editor.put(p, name + ":editchance:" + id);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AEPhoBan.inst(), () -> {
                        p.closeInventory();
                        p.sendMessage(Messages.get("EditChance"));
                    });
                    return;
                }

                if ((click != null && !click.getType().equals(Material.AIR)) && (cursor != null && cursor.getType().equals(Material.AIR))) {
                    e.setCancelled(true);

                    String id = NBTTag.getKey(click, "RewardGui_ID");

                    if (e.isLeftClick()) {
                        editor.put(p, name + ":editchance:" + id);

                        Bukkit.getScheduler().scheduleSyncDelayedTask(AEPhoBan.inst(), () -> {
                            p.closeInventory();
                            p.sendMessage(Messages.get("EditChance"));
                        });
                    }

                    if (e.isRightClick()) {
                        File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        if (!configFile.exists()) try {
                            configFile.createNewFile();
                        } catch (Exception ignored) {
                        }
                        FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(configFile) : game.getConfig();
                        room.set("Reward." + id, null);
                        FileManager.saveFileConfig(room, configFile);

                        p.closeInventory();

                        RewardGui.open(p, name);
                    }
                    return;
                }

                e.setCancelled(true);
                return;
            }

            e.setCancelled(true);

            ItemStack click = e.getCurrentItem();

            if (click == null) return;
            if (click.getType().equals(Material.AIR)) return;

            String clickType = NBTTag.getKey(click, "RewardGui_ClickType");
            if (clickType == null || clickType.isEmpty()) return;

            switch (clickType.toLowerCase()) {
                case "rewardamount": {
                    editor.put(p, name + ":editrewardamount");
                    p.closeInventory();
                    p.sendMessage(Messages.get("EditRewardAmount"));
                    break;
                }

                case "rewardcommand": {
                    String id = UUID.randomUUID().toString();
                    editor.put(p, name + ":addrewardcommand:" + id);
                    p.closeInventory();
                    p.sendMessage(Messages.get("AddRewardCommand"));
                    break;
                }

                case "confirm": {
                    EditorGui.open(p, name);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        viewers.remove(p);
    }

    public static final HashMap<Player, String> editor = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (editor.containsKey(p)) {
            e.setCancelled(true);

            String mess = ChatColor.stripColor(e.getMessage());

            String[] arr = editor.get(p).split(":");
            if (arr.length < 2) {
                editor.remove(p);
                p.sendMessage(Messages.get("Error").replace("<e>", "Lỗi định dạng dữ liệu"));
                return;
            }
            
            String name = arr[0];
            String type = arr[1];
            Game game = Game.getGame(name);

            switch (type.toLowerCase()) {
                case "editchance": {
                    if (arr.length < 3) {
                        editor.remove(p);
                        p.sendMessage(Messages.get("Error").replace("<e>", "Thiếu thông tin ID phần thưởng"));
                        return;
                    }
                    
                    String id = arr[2];

                    try {
                        int chance = Integer.parseInt(mess);

                        File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        if (!configFile.exists()) try {
                            configFile.createNewFile();
                        } catch (Exception ignored) {
                        }
                        FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(configFile) : game.getConfig();
                        room.set("Reward." + id + ".Chance", chance);
                        FileManager.saveFileConfig(room, configFile);

                        boolean checkOpenGui = (arr.length >= 4 && MethodDefault.formatNumber(arr[3], 0) == 2);
                        editor.remove(p);

                        if (!checkOpenGui) {
                            RewardGui.open(p, name);
                        } else {
                            p.sendMessage(Messages.get("AddRewardCommandSuccess"));
                        }
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            RewardGui.open(p, name);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<e>", ex.getMessage()));
                        ex.printStackTrace();
                    }

                    break;
                }

                case "editrewardamount": {
                    try {
                        int amount = Integer.parseInt(mess);

                        File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        if (!configFile.exists()) try {
                            configFile.createNewFile();
                        } catch (Exception ignored) {
                        }
                        FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(configFile) : game.getConfig();
                        room.set("RewardAmount", amount);
                        FileManager.saveFileConfig(room, configFile);

                        editor.remove(p);
                        RewardGui.open(p, name);
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            EditorGui.open(p, name);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<e>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }

                case "addrewardcommand": {
                    if (arr.length < 3) {
                        editor.remove(p);
                        p.sendMessage(Messages.get("Error").replace("<e>", "Thiếu thông tin ID phần thưởng"));
                        return;
                    }
                    
                    String id = arr[2];
                    String command = ChatColor.stripColor(e.getMessage());

                    File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                    if (!configFile.exists()) try {
                        configFile.createNewFile();
                    } catch (Exception ignored) {
                    }
                    FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(configFile) : game.getConfig();
                    room.set("Reward." + id + ".Command", command);
                    FileManager.saveFileConfig(room, configFile);

                    editor.remove(p);
                    editor.put(p, name + ":editchance:" + id);

                    p.sendMessage(Messages.get("EditChance"));
                    break;
                }
            }
        }
    }

}
