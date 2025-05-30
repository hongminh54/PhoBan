package com.hongminh54.phoban.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.ItemBuilder;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;

import me.orineko.pluginspigottools.NBTTag;

public class EditorGui implements Listener {

    public static void open(Player p, String name) {
        FileConfiguration gui = FileManager.getFileConfig(Files.GUI);
        Game game = Game.getGame(name);

        /*File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
        if (!configFile.exists()) try {
            configFile.createNewFile();
        } catch (Exception ignored) {
        }*/
        me.orineko.pluginspigottools.FileManager fileManager =
                new me.orineko.pluginspigottools.FileManager(name+".yml", AEPhoBan.inst());
        fileManager.createFolder("room");
        fileManager.createFile();
        fileManager.reload();
        if(fileManager.getFile() == null) return;
        FileConfiguration room = game == null ? YamlConfiguration.loadConfiguration(fileManager.getFile()) : game.getConfig();

        Inventory inv = Bukkit.createInventory(null, gui.getInt("EditorGui.Rows") * 9, gui.getString("EditorGui.Title", "").replace("&", "§").replace("<name>", name));

        HashMap<String, List<String>> replace = new HashMap<>();
        replace.put("<max_players>", Collections.singletonList(String.valueOf(room.getInt("Player", 0))));
        replace.put("<prefix>", Collections.singletonList(room.getString("Prefix", "").replace("&", "§")));
        replace.put("<time>", Collections.singletonList(String.valueOf(room.getInt("Time", 0))));
        
        // Hiển thị trạng thái khóa
        boolean isLocked = room.getBoolean("Locked", false);
        replace.put("<locked_status>", Collections.singletonList(isLocked ? FileManager.getFileConfig(Files.GUI).getString("EditorGui.LockedStatus", "&cĐã khóa").replace("&", "§") 
                                                                      : FileManager.getFileConfig(Files.GUI).getString("EditorGui.UnlockedStatus", "&aMở khóa").replace("&", "§")));

        for (int i = 1; i <= Game.maxStage; i++) {
            List<String> lores = new ArrayList<>();
            if (room.contains("Mob" + i)) {
                for (String s : room.getConfigurationSection("Mob" + i).getKeys(false)) {
                    String type = room.getString("Mob" + i + "." + s + ".Type", "null");
                    int amount = room.getInt("Mob" + i + "." + s + ".Amount", 0);

                    String format = gui.getString("EditorGui.MobLoreFormat", "").replace("&", "§").replace("<mobs>", type).replace("<amount>", String.valueOf(amount));

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(format).append("§f, §r");
                    for (String child : room.getConfigurationSection("Mob" + i + "." + s).getKeys(false)) {
                        if (child.equalsIgnoreCase("Location") || child.equalsIgnoreCase("Type") || child.equalsIgnoreCase("Amount") || child.equalsIgnoreCase("Time")) {
                            continue;
                        }

                        String childType = room.getString("Mob" + i + "." + s + "." + child + ".Type");
                        int childAmount = room.getInt("Mob" + i + "." + s + "." + child + ".Amount");
                        String childFormat = gui.getString("EditorGui.SecondaryMobLoreFormat", "&fx<amount> &e<mobs>").replace("&", "§").replace("<mobs>", childType).replace("<amount>", childAmount + "");
                        stringBuilder.append(childFormat).append("§f, §r");
                    }
                    String result = stringBuilder.toString();
                    lores.add(result.substring(0, result.length() - 6));
                }
            }
            replace.put("<mob" + i + ">", lores);
        }

        replace.put("<boss_type>", Collections.singletonList(room.getString("Boss.Type", "null")));
        replace.put("<boss_amount>", Collections.singletonList(String.valueOf(room.getInt("Boss.Amount", 0))));

        Location spawn = (Location) room.get("Spawn");
        replace.put("<spawn>", Collections.singletonList(spawn == null ? "" : spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ() + "," + spawn.getWorld().getName()));

        replace.put("<type>", Arrays.asList(room.getString("Type"), ""));

        ConfigurationSection section = gui.getConfigurationSection("EditorGui.Content");
        if (section != null) section.getKeys(false).forEach(content -> {

            ItemStack item = ItemBuilder.build(Files.GUI, "EditorGui.Content." + content, replace);

            if (gui.contains("EditorGui.Content." + content + ".ClickType")) {
                String clickType = gui.getString("EditorGui.Content." + content + ".ClickType");
                if (clickType != null && clickType.contains("EditMob")) {
                    String mob = clickType.replace("Edit", "");
                    if (isEdited(name, mob))
                        item = ItemBuilder.build(Files.GUI, "EditorGui.Content." + content + ".Edited", replace);
                }
                if (clickType != null && clickType.equalsIgnoreCase("EditBoss")) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.DURABILITY, 10, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                        item.setItemMeta(meta);
                    }
                }
            }

            /*NBTItem nbt = new NBTItem(item);
            if(gui.contains("EditorGui.Content." + content + ".ClickType")) nbt.setString("EditorGui_ClickType", gui.getString("EditorGui.Content." + content + ".ClickType"));*/

            ItemStack item2 = item.clone();
            if (gui.contains("EditorGui.Content." + content + ".ClickType")) {
                item2 = NBTTag.setKey(item2, "EditorGui_ClickType", gui.getString("EditorGui.Content." + content + ".ClickType"));
            }

            for (int slot : gui.getIntegerList("EditorGui.Content." + content + ".Slot")) {
                if (slot >= (gui.getInt("EditorGui.Rows") * 9)) continue;

                if (slot <= -1) {
                    for (int i = 0; i < (gui.getInt("EditorGui.Rows") * 9); i++) inv.setItem(i, item2);
                    break;
                }

                inv.setItem(slot, item2);
            }
        });

        Bukkit.getScheduler().scheduleSyncDelayedTask(AEPhoBan.inst(), () -> {
            p.closeInventory();
            p.openInventory(inv);
            viewers.put(p, name);
            
            // Tự động khóa phòng khi mở để chỉnh sửa
            FileConfiguration config = room;
            config.set("Locked", true);
            FileManager.saveFileConfig(config, fileManager.getFile());
            
            if (game != null) {
                game.setLocked(true);
                p.sendMessage(Messages.get("RoomLockedAfterEdit"));
            }
        });
    }


    private static final HashMap<Player, String> viewers = new HashMap<>();

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (viewers.containsKey(p)) {
            e.setCancelled(true);
            String room = viewers.get(p);

            ItemStack click = e.getCurrentItem();

            if (click == null) return;
            if (click.getType().equals(Material.AIR)) return;

            /*NBTItem nbt = new NBTItem(click);
            if(!nbt.hasKey("EditorGui_ClickType")) return;

            String clicktype = nbt.getString("EditorGui_ClickType");*/
            String keyClick = NBTTag.getKey(click, "EditorGui_ClickType");
            if (keyClick == null || keyClick.isEmpty()) return;

            for (int i = 1; i <= Game.maxStage; i++) {
                if (keyClick.equalsIgnoreCase("editmob" + i)) {
                    if (e.isRightClick()) {
                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + room + ".yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                        if (config.contains("Mob" + i)) {
                            AtomicReference<String> toRemove = new AtomicReference<>("");
                            ConfigurationSection section = config.getConfigurationSection("Mob" + i);
                            if (section != null) section.getKeys(false).forEach(toRemove::set);

                            if (!toRemove.get().isEmpty()) {
                                config.set("Mob" + i + "." + toRemove, null);
                                FileManager.saveFileConfig(config, file);
                                p.sendMessage(Messages.get("RemoveMob"));

                                Game game = Game.getGame(room);
                                if (game != null) game.setConfig(config);

                                EditorGui.open(p, room);
                            }
                        }
                    } else {
                        p.closeInventory();
                        p.getInventory().addItem(superultrablazerod(room, "Mob" + i));
                        p.sendMessage(Messages.get("EditMob_Step1"));
                    }
                    break;
                }
            }

            switch (keyClick.toLowerCase()) {
                case "editplayer": {
                    editor.put(p, room + ":editplayer");
                    p.closeInventory();
                    p.sendMessage(Messages.get("EditPlayer"));
                    return;
                }

                case "editprefix": {
                    editor.put(p, room + ":editprefix");
                    p.closeInventory();
                    p.sendMessage(Messages.get("EditPrefix"));
                    return;
                }

                case "edittime": {
                    editor.put(p, room + ":edittime");
                    p.closeInventory();
                    p.sendMessage(Messages.get("EditTime"));
                    return;
                }

                case "editreward": {
                    p.closeInventory();
                    RewardGui.open(p, room);
                    return;
                }

                case "editboss": {
                    p.closeInventory();
                    p.getInventory().addItem(superultrablazerod(room, "Boss"));
                    p.sendMessage(Messages.get("EditBoss_Step1"));
                    return;
                }

                case "editspawn": {
                    p.closeInventory();
                    Location loc = p.getLocation();

                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + room + ".yml");
                    FileConfiguration rooms = YamlConfiguration.loadConfiguration(file);
                    rooms.set("Spawn", loc);
                    FileManager.saveFileConfig(rooms, file);

                    try {
                        Game game = Game.getGame(room);
                        if (game != null) game.setSpawn(loc);
                    } catch (Exception ignored) {

                    }

                    EditorGui.open(p, room);
                    return;
                }

                case "edittype": {
                    editor.put(p, room + ":edittype");
                    p.closeInventory();
                    p.sendMessage(Messages.get("EditType"));
                    return;
                }

                case "deleteroom": {
                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + room + ".yml");
                    file.delete();
                    Game.deleteRoom(room);

                    p.closeInventory();
                    p.sendMessage(Messages.get("DeleteRoom"));
                    return;
                }

                case "confirm": {
                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + room + ".yml");
                    FileConfiguration rooms = YamlConfiguration.loadConfiguration(file);
                    
                    if (rooms.contains("Prefix")) {
                        String prefix = rooms.getString("Prefix", "");
                        prefix = prefix.replace("§", "&");
                        rooms.set("Prefix", prefix);
                        FileManager.saveFileConfig(rooms, file);
                    }
                    
                    if (!Game.canJoin(rooms)) {
                        p.sendMessage(Messages.get("RoomNotConfig"));
                        return;
                    }
                    Game.load(room, rooms, file);
                    p.closeInventory();
                    p.sendMessage(Messages.get("ConfigDone"));

                    String type = rooms.getString("Type");

                    String result = containsPhobanType(type);
                    if (result.equals("deo-co-con-cac-gi-o-day-het")) {
                        FileConfiguration phoban = FileManager.getFileConfig(Files.PHOBAN);
                        FileConfiguration gui = FileManager.getFileConfig(Files.GUI);

                        phoban.set(type + "Room.Type", type);
                        phoban.set(type + "Room.Priority", 100);
                        phoban.set(type + "Room.ID", gui.getString("ChooseTypeGui.TypeFormat.ID"));
                        phoban.set(type + "Room.Name", gui.getString("ChooseTypeGui.TypeFormat.Name"));
                        phoban.set(type + "Room.Lore", gui.getStringList("ChooseTypeGui.TypeFormat.Lore"));

                        FileManager.saveFileConfig(phoban, Files.PHOBAN);
                    }

                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        viewers.remove(p);
    }


    private static final HashMap<Player, String> editor = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (editor.containsKey(p)) {
            e.setCancelled(true);

            String type = editor.get(p).split(":")[1];
            String name = editor.get(p).split(":")[0];
            String mess = ChatColor.stripColor(e.getMessage());

            if (mess.equalsIgnoreCase("cancel") || mess.equalsIgnoreCase("complete")) {
                if (!(type.toLowerCase().contains("editmob") || type.toLowerCase().contains("editboss"))) {
                    clearBlazeRod(p);
                    editor.remove(p);
                    EditorGui.open(p, name);
                    return;
                }
            }

            Game game = Game.getGame(name);

            for (int i = 1; i <= Game.maxStage; i++) {
                if (type.equalsIgnoreCase("editmob" + i + "_control")) {
                    String id = editor.get(p).split(":")[2];
                    switch (mess.toLowerCase()) {
                        case "add": {
                            editor.remove(p);
                            editor.put(p, name + ":editmob" + i + "_type:" + id + ":no-parent");

                            p.sendMessage(Messages.get("EditMob_Step3"));
                            break;
                        }
                        case "remove": {
                            File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                            FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                            room.set("Mob" + i, null);
                            FileManager.saveFileConfig(room, file);

                            p.sendMessage(Messages.get("RemoveMob"));

                            EditorGui.open(p, name);
                            break;
                        }
                        case "exit": {
                            File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                            FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                            room.set("Mob" + i + "." + id, null);
                            FileManager.saveFileConfig(room, file);

                            EditorGui.open(p, name);
                            break;
                        }
                        default: {
                            p.sendMessage(Messages.get("EditMob_Step2"));
                            break;
                        }
                    }
                    break;
                }
                if (type.equalsIgnoreCase("editmob" + i + "_type")) {
                    String id = editor.get(p).split(":")[2];
                    String parent = editor.get(p).split(":")[3];
                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                    FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                    String path = "";
                    if (parent.equalsIgnoreCase("no-parent")) path = "Mob" + i + "." + id;
                    else path = "Mob" + i + "." + parent + "." + id;
                    room.set(path + ".Type", mess);
                    FileManager.saveFileConfig(room, file);

                    editor.remove(p);
                    editor.put(p, name + ":editmob" + i + "_amount:" + id + ":" + parent);

                    p.sendMessage(Messages.get("EditMob_Step4"));
                    break;
                }
                if (type.equalsIgnoreCase("editmob" + i + "_amount")) {
                    try {
                        int amount = Integer.parseInt(mess);

                        String id = editor.get(p).split(":")[2];
                        String parent = editor.get(p).split(":")[3];
                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                        String path = "";
                        if (parent.equalsIgnoreCase("no-parent")) path = "Mob" + i + "." + id;
                        else path = "Mob" + i + "." + parent + "." + id;
                        room.set(path + ".Amount", amount);
                        FileManager.saveFileConfig(room, file);

                        editor.remove(p);
                        if (parent.equalsIgnoreCase("no-parent")) {
                            editor.put(p, name + ":editmob" + i + "_time:" + id + ":" + parent);
                            p.sendMessage(Messages.get("EditMob_Step5"));
                        } else {
                            editor.put(p, name + ":waiting-new-mob");
                            p.getInventory().addItem(superultrablazerod(name, "Mob" + i, parent));
                            p.sendMessage(Messages.get("AddNewMob"));
                        }
                    } catch (Exception ex) {
                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<error>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }
                if (type.equalsIgnoreCase("editmob" + i + "_time")) {
                    try {
                        int time = Integer.parseInt(mess);

                        String id = editor.get(p).split(":")[2];
                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                        room.set("Mob" + i + "." + id + ".Time", time);
                        FileManager.saveFileConfig(room, file);
                        if (game != null) game.setConfig(room);

                        editor.remove(p);
                        editor.put(p, name + ":waiting-new-mob");

                        p.getInventory().addItem(superultrablazerod(name, "Mob" + i, id));
                        p.sendMessage(Messages.get("AddNewMob"));
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            EditorGui.open(p, name);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<error>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }
            }

            switch (type.toLowerCase()) {
                case "waiting-new-mob": {
                    p.sendMessage(Messages.get("AddNewMob"));
                    break;
                }
                case "editplayer": {
                    try {
                        int player = (int) Long.parseLong(mess);

                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                        room.set("Player", player);
                        FileManager.saveFileConfig(room, file);

                        try {
                            game.setMaxPlayer(player);
                        } catch (Exception ignored) {

                        }

                        editor.remove(p);

                        new BukkitRunnable() {
                            public void run() {
                                EditorGui.open(p, name);
                            }
                        }.runTaskLater(AEPhoBan.inst(), 0);
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            new BukkitRunnable() {
                                public void run() {
                                    EditorGui.open(p, name);
                                }
                            }.runTaskLater(AEPhoBan.inst(), 0);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<error>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }

                case "editprefix": {
                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                    FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                    String formattedPrefix = mess.replace("§", "&");
                    room.set("Prefix", formattedPrefix);
                    FileManager.saveFileConfig(room, file);
                    
                    Game existingGame = Game.getGame(name);
                    if (existingGame != null) {
                        existingGame.setConfig(room);
                    }

                    editor.remove(p);

                    new BukkitRunnable() {
                        public void run() {
                            EditorGui.open(p, name);
                        }
                    }.runTaskLater(AEPhoBan.inst(), 0);
                    break;
                }

                case "edittime": {
                    try {
                        int time = Integer.parseInt(mess);

                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                        room.set("Time", time);
                        FileManager.saveFileConfig(room, file);

                        try {
                            game.setMaxTime(time);
                        } catch (Exception ignored) {

                        }

                        editor.remove(p);

                        new BukkitRunnable() {
                            public void run() {
                                EditorGui.open(p, name);
                            }
                        }.runTaskLater(AEPhoBan.inst(), 0);
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            new BukkitRunnable() {
                                public void run() {
                                    EditorGui.open(p, name);
                                }
                            }.runTaskLater(AEPhoBan.inst(), 0);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<error>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }

                case "editboss_control": {
                    String id = editor.get(p).split(":")[2];
                    switch (mess.toLowerCase()) {
                        case "add": {
                            editor.remove(p);
                            editor.put(p, name + ":editboss_type:" + id);

                            p.sendMessage(Messages.get("EditBoss_Step3"));
                            break;
                        }
                        case "remove": {
                            File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                            FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                            room.set("Boss", null);
                            FileManager.saveFileConfig(room, file);

                            p.sendMessage("RemoveMob");

                            new BukkitRunnable() {
                                public void run() {
                                    EditorGui.open(p, name);
                                }
                            }.runTaskLater(AEPhoBan.inst(), 0);
                            break;
                        }
                        case "exit": {
                            new BukkitRunnable() {
                                public void run() {
                                    EditorGui.open(p, name);
                                }
                            }.runTaskLater(AEPhoBan.inst(), 0);
                            break;
                        }
                        default: {
                            p.sendMessage(Messages.get("EditBoss_Step2"));
                            break;
                        }
                    }
                    break;
                }
                case "editboss_type": {
                    String id = editor.get(p).split(":")[2];
                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                    FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                    room.set("Boss.Type", mess);
                    FileManager.saveFileConfig(room, file);

                    editor.remove(p);
                    editor.put(p, name + ":editboss_amount:" + id);

                    p.sendMessage(Messages.get("EditBoss_Step4"));
                    break;
                }
                case "editboss_amount": {
                    try {
                        String id = editor.get(p).split(":")[2];
                        int amount = Integer.parseInt(mess);

                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                        room.set("Boss.Amount", amount);
                        FileManager.saveFileConfig(room, file);

                        editor.remove(p);
                        editor.put(p, name + ":editboss_time:" + id);

                        p.sendMessage(Messages.get("EditMob_Step5"));
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            new BukkitRunnable() {
                                public void run() {
                                    EditorGui.open(p, name);
                                }
                            }.runTaskLater(AEPhoBan.inst(), 0);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<error>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }
                case "editboss_time": {
                    try {
                        int time = Integer.parseInt(mess);

//                        String id = editor.get(p).split(":")[2];
                        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                        FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                        room.set("Boss.Time", time);
                        FileManager.saveFileConfig(room, file);
                        if (game != null) game.setConfig(room);

                        editor.remove(p);

                        new BukkitRunnable() {
                            public void run() {
                                EditorGui.open(p, name);
                            }
                        }.runTaskLater(AEPhoBan.inst(), 0);
                    } catch (Exception ex) {
                        if (ex.getMessage() == null) {
                            new BukkitRunnable() {
                                public void run() {
                                    EditorGui.open(p, name);
                                }
                            }.runTaskLater(AEPhoBan.inst(), 0);
                            return;
                        }

                        if (ex.getMessage().contains("For input string:")) {
                            p.sendMessage(Messages.get("NotInt"));
                            break;
                        }
                        p.sendMessage(Messages.get("Error").replace("<error>", ex.getMessage()));
                        ex.printStackTrace();
                    }
                    break;
                }

                case "edittype": {
                    File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + name + ".yml");
                    FileConfiguration room = YamlConfiguration.loadConfiguration(file);
                    room.set("Type", mess);
                    FileManager.saveFileConfig(room, file);

                    editor.remove(p);

                    new BukkitRunnable() {
                        public void run() {
                            EditorGui.open(p, name);
                        }
                    }.runTaskLater(AEPhoBan.inst(), 0);
                    break;
                }

            }
        }
    }


    private ItemStack superultrablazerod(String room, String mob) {
        ItemStack item = new ItemStack(Utils.matchMaterial("blaze_rod"), 1);

        /*NBTItem nbt = new NBTItem(item);
        nbt.setString("Room", room);
        nbt.setString("Mob", mob);

        return nbt.getItem().clone();*/
        ItemStack item2 = NBTTag.setKey(item, "Room", room);
        item2 = NBTTag.setKey(item2, "Mob", mob);
        return item2;
    }

    private ItemStack superultrablazerod(String room, String mob, String parent) {
        ItemStack item = new ItemStack(Utils.matchMaterial("blaze_rod"), 1);

        /*NBTItem nbt = new NBTItem(item);
        nbt.setString("Room", room);
        nbt.setString("Mob", mob);
        nbt.setString("Parent", parent);

        return nbt.getItem().clone();*/
        ItemStack item2 = NBTTag.setKey(item, "Room", room);
        item2 = NBTTag.setKey(item2, "Mob", mob);
        item2 = NBTTag.setKey(item2, "Parent", parent);
        return item2;
    }

    private void clearBlazeRod(Player player) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            if (item.getType().equals(Material.AIR)) continue;

            /*NBTItem nbt = new NBTItem(item);
            if(nbt.hasKey("Room") && nbt.hasKey("Mob")) {
                player.getInventory().setItem(i, null);
            }*/
            String keyRoom = NBTTag.getKey(item, "Room");
            String keyMob = NBTTag.getKey(item, "Mob");
            if (keyRoom != null && !keyRoom.isEmpty() && keyMob != null && !keyMob.isEmpty()) {
                player.getInventory().setItem(i, null);
            }
        }
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;

        ItemStack item = e.getItem();

        if (item == null) return;
        if (item.getType().equals(Material.AIR)) return;

        /*NBTItem nbt = new NBTItem(item);

        if(!nbt.hasKey("Room")) return;
        if(!nbt.hasKey("Mob")) return;*/
        String keyRoom = NBTTag.getKey(item, "Room");
        String keyMob = NBTTag.getKey(item, "Mob");
        if (keyRoom == null || keyRoom.isEmpty() || keyMob == null || keyMob.isEmpty()) return;
        e.setCancelled(true);

        String parent = "";
//		if(nbt.hasKey("Parent")) parent = nbt.getString("Parent");
        String keyParent = NBTTag.getKey(item, "Parent");
        if (keyParent != null && !keyParent.isEmpty()) parent = keyParent;

        Block block = e.getClickedBlock();
        if (block == null) return;
        Location loc = block.getLocation();

        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + keyRoom + ".yml");
        FileConfiguration rooms = YamlConfiguration.loadConfiguration(file);
        String id = UUID.randomUUID().toString();

        String path = "";
        if (parent.isEmpty()) path = keyMob + "." + id;
        else path = keyMob + "." + parent + "." + id;

        if (keyMob.equalsIgnoreCase("boss")) rooms.set("Boss.Location", loc);
        else rooms.set(path + ".Location", loc);

        FileManager.saveFileConfig(rooms, file);

        editor.remove(p);
        if (keyParent != null && !keyParent.isEmpty()) {
            editor.put(p, keyRoom + ":edit" + keyMob + "_type:" + id + ":" + parent);
        } else editor.put(p, keyRoom + ":edit" + keyMob + "_control:" + id);

        p.getInventory().setItemInMainHand(null);

        if (keyParent != null && !keyParent.isEmpty()) {
            p.sendMessage(Messages.get("EditMob_Step3"));
        } else {
            if (keyMob.equalsIgnoreCase("boss")) p.sendMessage(Messages.get("EditBoss_Step2"));
            else p.sendMessage(Messages.get("EditMob_Step2"));
        }

    }


    private static boolean isEdited(String room, String mob) {
        File file = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + room + ".yml");
        FileConfiguration rooms = YamlConfiguration.loadConfiguration(file);

        if (rooms.contains(mob)) {
            ConfigurationSection section = rooms.getConfigurationSection(mob);
            if (section != null)
                return section.getKeys(false).stream().anyMatch(s ->
                        rooms.contains(mob + "." + s + ".Type") &&
                                rooms.contains(mob + "." + s + ".Amount") &&
                                rooms.contains(mob + "." + s + ".Location"));
        }

        return false;
    }


    private static String containsPhobanType(String type) {
        FileConfiguration phoban = FileManager.getFileConfig(Files.PHOBAN);
        for (String key : phoban.getKeys(false)) {
            if (!phoban.contains(key + ".Type")) continue;
            if (phoban.getString(key + ".Type", "").equalsIgnoreCase(type)) return phoban.getString(key + ".Type");
        }
        return "deo-co-con-cac-gi-o-day-het";
    }

}
