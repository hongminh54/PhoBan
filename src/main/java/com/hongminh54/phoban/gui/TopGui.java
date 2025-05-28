package com.hongminh54.phoban.gui;

import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TopGui implements Listener {

    public static void open(Player p) {
        FileConfiguration gui = FileManager.getFileConfig(FileManager.Files.GUI_TOP);
        FileConfiguration data = FileManager.getFileConfig(FileManager.Files.DATA);

        Inventory inv = Bukkit.createInventory(null, gui.getInt("Rows") * 9, gui.getString("Title").replace("&", "§"));
        for(String content : gui.getConfigurationSection("Content").getKeys(false)) {
            ItemStack item = ItemBuilder.build(FileManager.Files.GUI_TOP, "Content." + content, new HashMap<>());
            for(int slot : gui.getIntegerList("Content." + content + ".Slot")) {
                if(slot >= (gui.getInt("Rows") * 9)) continue;

                if(slot <= -1) {
                    for(int i = 0; i < (gui.getInt("Rows") * 9); i++) inv.setItem(i, item);
                    break;
                }

                inv.setItem(slot, item);
            }
        }

        HashMap<String, Integer> pointData = new HashMap<>();
        for(String name : data.getKeys(false)) {
            int point = data.getInt(name + ".Point", 0);
            if(point > 0) pointData.put(name, point);
        }

        List<Map.Entry<String, Integer>> pointList = new ArrayList<>(pointData.entrySet());

        Collections.sort(pointList, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        HashMap<String, List<String>> replace = new HashMap<>();

        List<Integer> slots = gui.getIntegerList("TopSlot");
        for(int i = 0; i < Math.min(slots.size(), pointList.size()); i++) {
            replace.clear();

            int slot = slots.get(i);
            String name = pointList.get(i).getKey();
            int point = pointList.get(i).getValue();

            replace.put("<top>", Arrays.asList(String.valueOf(i + 1)));
            replace.put("<player>", Arrays.asList(name));
            replace.put("<point>", Arrays.asList(String.valueOf(point)));

            ItemStack item = ItemBuilder.build(FileManager.Files.GUI_TOP, "TopFormat", replace);
            inv.setItem(slot, ItemBuilder.skull(item, name));
        }

        replace.clear();

        int rank = 0;
        if(pointData.containsKey(p.getName())) {
            for(int i = 0; i < pointList.size(); i++) {
                if(pointList.get(i).getKey().equals(p.getName())) {
                    rank = i + 1;
                    break;
                }
            }
        }

        replace.put("<top>", Arrays.asList(String.valueOf(rank)));
        replace.put("<player>", Arrays.asList(p.getName()));
        replace.put("<point>", Arrays.asList(String.valueOf(data.getInt(p.getName() + ".Point", 0))));

        ItemStack item = ItemBuilder.build(FileManager.Files.GUI_TOP, "MyTopFormat", replace);
        inv.setItem(gui.getInt("MyTopSlot"), item);

        p.openInventory(inv);
        if(!viewers.contains(p)) viewers.add(p);
    }

    private static List<Player> viewers = new ArrayList<>();


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if(viewers.contains(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if(viewers.contains(p)) viewers.remove(p);
    }

}
