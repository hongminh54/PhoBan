package com.hongminh54.phoban.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.game.GameStatus;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.ItemBuilder;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;

import me.orineko.pluginspigottools.NBTTag;

public class PhoBanGui implements Listener {

    public static HashMap<Player, PhoBanGui> viewers = new HashMap<>();

    public ArrayList<Inventory> pages = new ArrayList<>();
    public int curPage = 0;
    public String type;

    public PhoBanGui(Player p, int pg, String type) {
        if (p == null || pg < 0) return;
        this.type = type;

        this.curPage = pg;
        Inventory page = gui();
        int firstEmpty = Utils.firstEmpty(page.getSize() / 9);

        for (String name : Game.listGame()) {
            Game g = Game.getGame(name);
            if (g == null) continue;
            String t = g.getType();
            if (!t.equalsIgnoreCase(type)) continue;
            FileConfiguration room = g.getConfig();

            HashMap<String, List<String>> replace = new HashMap<>();
            String prefixWithColor = room.getString("Prefix", "").replace("&", "§");
            replace.put("<prefix>", Collections.singletonList(prefixWithColor));
            replace.put("<current>", Collections.singletonList(String.valueOf(g.getPlayers().size())));
            replace.put("<max>", Collections.singletonList(String.valueOf(room.getInt("Player"))));
            List<String> lores = new ArrayList<>();
            for (Player player : g.getPlayers()) {
                lores.add(FileManager.getFileConfig(Files.GUI).getString("PhoBanGui.PlayerFormat", "").replace("&", "§").replace("<player>", player.getName()));
            }
            replace.put("<players>", lores);
            replace.put("<time>", Collections.singletonList(timeFormat(g.getTimeLeft())));
            replace.put("<status>", Collections.singletonList(FileManager.getFileConfig(Files.GUI).getString("PhoBanGui.StatusFormat." + g.getStatus().toString(), "").replace("&", "§")));

            ItemStack item = (g.getStatus().equals(GameStatus.WAITING)) ? ItemBuilder.build(Files.GUI, "PhoBanGui.WaitingRoom", replace) : (g.getStatus().equals(GameStatus.STARTING)) ? ItemBuilder.build(Files.GUI, "PhoBanGui.StartingRoom", replace) : ItemBuilder.build(Files.GUI, "PhoBanGui.PlayingRoom", replace);

            /*NBTItem nbt = new NBTItem(item.clone());
            nbt.setString("PhoBanGui_ClickType", "JoinRoom");
            nbt.setString("PhoBanGui_Room", name);*/
            ItemStack item2 = NBTTag.setKey(item.clone(), "PhoBanGui_ClickType", "JoinRoom");
            item2 = NBTTag.setKey(item2, "PhoBanGui_Room", name);

            if (page.firstEmpty() == firstEmpty) {
                page.addItem(item2);
                pages.add(page);
                page = gui();
            } else page.addItem(item2);
        }

        pages.add(page);
        if (PhoBanGui.viewers.containsKey(p)) {
            PhoBanGui.viewers.remove(p);
        }
        p.openInventory(pages.get(curPage));
        PhoBanGui.viewers.put(p, this);
    }

    private Inventory gui() {
        FileConfiguration gui = FileManager.getFileConfig(Files.GUI);
        int rows = gui.getInt("PhoBanGui.Rows");
        if (rows < 3) rows = 3;
        Inventory inv = Bukkit.createInventory(null, rows * 9, ChatColor.translateAlternateColorCodes('&', gui.getString("PhoBanGui.Title")));

        ItemStack blank = ItemBuilder.build(Files.GUI, "PhoBanGui.Blank", new HashMap<>());
        for (int slot : gui.getIntegerList("PhoBanGui.Blank.Slot")) {
            if (slot >= (gui.getInt("PhoBanGui.Rows") * 9)) continue;

            if (slot <= -1) {
                for (int i = 0; i < (gui.getInt("PhoBanGui.Rows") * 9); i++) inv.setItem(i, blank.clone());
                break;
            }

            inv.setItem(slot, blank.clone());
        }

        ItemStack nextpage = ItemBuilder.build(Files.GUI, "PhoBanGui.NextPage", new HashMap<>());
        /*NBTItem nbt = new NBTItem(nextpage.clone());
        nbt.setString("PhoBanGui_ClickType", "NextPage");*/
        ItemStack nextPage2 = NBTTag.setKey(nextpage.clone(), "PhoBanGui_ClickType", "NextPage");
        for (int slot : gui.getIntegerList("PhoBanGui.NextPage.Slot")) {
            if (slot >= (gui.getInt("PhoBanGui.Rows") * 9)) continue;

            if (slot <= -1) {
                for (int i = 0; i < (gui.getInt("PhoBanGui.Rows") * 9); i++) inv.setItem(i, nextPage2);
                break;
            }

            inv.setItem(slot, nextPage2);
        }

        ItemStack previouspage = ItemBuilder.build(Files.GUI, "PhoBanGui.PreviousPage", new HashMap<>());
        /*nbt = new NBTItem(previouspage.clone());
        nbt.setString("PhoBanGui_ClickType", "PreviousPage");*/
        ItemStack previousPage2 = NBTTag.setKey(previouspage.clone(), "PhoBanGui_ClickType", "PreviousPage");
        for (int slot : gui.getIntegerList("PhoBanGui.PreviousPage.Slot")) {
            if (slot >= (gui.getInt("PhoBanGui.Rows") * 9)) continue;

            if (slot <= -1) {
                for (int i = 0; i < (gui.getInt("PhoBanGui.Rows") * 9); i++) inv.setItem(i, previousPage2);
                break;
            }

            inv.setItem(slot, previousPage2);
        }

        for (int room_slot : gui.getIntegerList("PhoBanGui.RoomSlot"))
            inv.setItem(room_slot, new ItemStack(Material.AIR));

        return inv;
    }


    public static String timeFormat(int time) {
        int minute = time / 60;
        int second = time % 60;

        FileConfiguration gui = FileManager.getFileConfig(Files.GUI);

        StringBuilder sb = new StringBuilder();
        if (minute > 0) {
            sb.append(gui.getString("PhoBanGui.TimeFormat.Minute").replace("&", "§").replace("<minute>", minute + ""));
            sb.append(" ");
        }
        if (second > 0) {
            sb.append(gui.getString("PhoBanGui.TimeFormat.Second").replace("&", "§").replace("<second>", second + ""));
        }

        return sb.toString();
    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (viewers.containsKey(p)) {
            e.setCancelled(true);

            ItemStack click = e.getCurrentItem();

            if (click == null) return;
            if (click.getType().equals(Material.AIR)) return;

            /*NBTItem nbt = new NBTItem(click);
            if(!nbt.hasKey("PhoBanGui_ClickType")) return;*/
            String keyClick = NBTTag.getKey(click, "PhoBanGui_ClickType");
            if (keyClick == null || keyClick.isEmpty()) return;
            switch (keyClick.toLowerCase()) {
                case "nextpage": {
                    PhoBanGui inv = viewers.get(p);
                    if (inv.curPage >= inv.pages.size() - 1) {
                        return;
                    } else {
                        new PhoBanGui(p, inv.curPage + 1, inv.type);
                    }
                    return;
                }
                case "previouspage": {
                    PhoBanGui inv = viewers.get(p);
                    if (inv.curPage > 0) {
                        new PhoBanGui(p, inv.curPage - 1, inv.type);
                    }
                    return;
                }
                case "joinroom": {
                    /*String name = nbt.getString("PhoBanGui_Room");*/
                    String name = NBTTag.getKey(click, "PhoBanGui_Room");
                    Game game = Game.getGame(name);

                    if (game != null && game.getStatus().equals(GameStatus.WAITING)) {
                        if (game.isFull()) {
                            p.sendMessage(Messages.get("RoomFull"));
                            return;
                        }

                        if (!Game.canJoin(game.getConfig())) {
                            p.sendMessage(Messages.get("JoinRoomNotConfig"));
                            return;
                        }

                        game.join(p);

                        if (game.isLeader(p)) p.sendMessage(Messages.get("LeaderStart"));
                        return;
                    }

                    if ((game != null && game.getStatus().equals(GameStatus.STARTING)) || (game != null && game.getStatus().equals(GameStatus.PLAYING))) {
                        p.sendMessage(Messages.get("RoomStarted"));
                        return;
                    }

                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (viewers.containsKey(p)) viewers.remove(p);
    }

}
