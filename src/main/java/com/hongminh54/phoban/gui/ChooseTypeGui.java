package com.hongminh54.phoban.gui;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.ItemBuilder;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;
import me.orineko.pluginspigottools.NBTTag;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ChooseTypeGui implements Listener {
	
	public static HashMap<Player, ChooseTypeGui> viewers = new HashMap<Player, ChooseTypeGui>();
	
	public ArrayList<Inventory> pages = new ArrayList<Inventory>();
    public int curpage = 0;
    
    public ChooseTypeGui(Player p, int pg) {
    	if(p == null || pg < 0) return;

    	this.curpage = pg;
    	Inventory page = gui();
    	int firstempty = Utils.firstEmpty(page.getSize() / 9);
    	
    	List<String> type = new ArrayList<>();
		List<String> sortedType = new ArrayList<>();
    	
    	for(String name : Game.listGame()) {
    		Game game = Game.getGame(name);
			if(game == null) continue;
    		if(!type.contains(game.getType())) type.add(game.getType());
    	}

		String lastType = null;
		int lastTypeValue = Integer.MAX_VALUE;
		for(String ignored : new ArrayList<>(type)) {
			for(String t2 : new ArrayList<>(type)) {
				String key = parseType(t2);
				int priority = FileManager.getFileConfig(Files.PHOBAN).getInt(key + ".Priority", 100);
				if(priority < lastTypeValue) {
					lastTypeValue = priority;
					lastType = t2;
				}
			}

			if(lastType != null) {
				sortedType.add(lastType);
				type.remove(lastType);
				lastType = null;
				lastTypeValue = Integer.MAX_VALUE;
			}
		}
    	
    	for(String t : sortedType) {
    		String status = (!Game.hasTurn(p, t)) ? FileManager.getFileConfig(Files.GUI)
					.getString("ChooseTypeGui.Format.NoTurn", "")
					.replace("&", "§") : FileManager.getFileConfig(Files.GUI)
					.getString("ChooseTypeGui.Format.Join", "").replace("&", "§");
    		
    		HashMap<String, List<String>> replace = new HashMap<>();
    		replace.put("<type>", Collections.singletonList(t));
    		replace.put("<status>", Collections.singletonList(status));
    		
    		ItemStack item = null;
    		String key = parseType(t);
    		if(key.equals("deo-co-con-cac-gi-o-day-het")) item = ItemBuilder.build(Files.GUI, "ChooseTypeGui.TypeFormat", replace);
    		else item = ItemBuilder.build(Files.PHOBAN, key, replace);
    		
    		/*NBTItem nbt = new NBTItem(item.clone());
    		nbt.setString("ChooseTypeGui_ClickType", "ChooseType");
    		nbt.setString("ChooseTypeGui_Type", t);
    		
    		if(page.firstEmpty() == firstempty) {
    			page.addItem(nbt.getItem().clone());
    			pages.add(page);
    			page = gui();
    		} else page.addItem(nbt.getItem().clone());*/

			ItemStack item2 = NBTTag.setKey(item.clone(), "ChooseTypeGui_ClickType", "ChooseType");
			item2 = NBTTag.setKey(item2, "ChooseTypeGui_Type", t);

			if(page.firstEmpty() == firstempty) {
				page.addItem(item2);
				pages.add(page);
				page = gui();
			} else page.addItem(item2);

    	}
    	
    	pages.add(page);
		p.openInventory(pages.get(curpage));
		ChooseTypeGui.viewers.put(p, this);
	}
    
    private Inventory gui() {
    	FileConfiguration gui = FileManager.getFileConfig(Files.GUI);
    	int rows = gui.getInt("ChooseTypeGui.Rows");
    	if(rows < 3) rows = 3;
    	Inventory inv = Bukkit.createInventory(null, rows * 9, ChatColor.translateAlternateColorCodes('&', gui.getString("PhoBanGui.Title", "")));
    	
    	ItemStack blank = ItemBuilder.build(Files.GUI, "ChooseTypeGui.Blank", new HashMap<>());
    	for(int slot : gui.getIntegerList("ChooseTypeGui.Blank.Slot")) {
			if(slot >= (gui.getInt("ChooseTypeGui.Rows") * 9)) continue;
			
			if(slot <= -1) {
				for(int i = 0; i < (gui.getInt("ChooseTypeGui.Rows") * 9); i++) inv.setItem(i, blank.clone());
				break;
			}
			
			inv.setItem(slot, blank.clone());
		}
    	
    	ItemStack nextpage = ItemBuilder.build(Files.GUI, "ChooseTypeGui.NextPage", new HashMap<>());

		/*NBTItem nbt = new NBTItem(nextpage.clone());
    	nbt.setString("ChooseTypeGui_ClickType", "NextPage");*/

		ItemStack nextPage2 = NBTTag.setKey(nextpage.clone(), "ChooseTypeGui_ClickType", "NextPage");

    	for(int slot : gui.getIntegerList("ChooseTypeGui.NextPage.Slot")) {
			if(slot >= (gui.getInt("ChooseTypeGui.Rows") * 9)) continue;
			
			if(slot <= -1) {
				for(int i = 0; i < (gui.getInt("ChooseTypeGui.Rows") * 9); i++) inv.setItem(i, nextPage2);
				break;
			}
			
			inv.setItem(slot, nextPage2);
		}
    	
    	ItemStack previouspage = ItemBuilder.build(Files.GUI, "ChooseTypeGui.PreviousPage", new HashMap<>());

		/*nbt = new NBTItem(previouspage.clone());
    	nbt.setString("ChooseTypeGui_ClickType", "PreviousPage");*/
		ItemStack previousPage2 = NBTTag.setKey(previouspage.clone(), "ChooseTypeGui_ClickType", "PreviousPage");

    	for(int slot : gui.getIntegerList("ChooseTypeGui.PreviousPage.Slot")) {
			if(slot >= (gui.getInt("ChooseTypeGui.Rows") * 9)) continue;
			
			if(slot <= -1) {
				for(int i = 0; i < (gui.getInt("ChooseTypeGui.Rows") * 9); i++) inv.setItem(i, previousPage2);
				break;
			}
			
			inv.setItem(slot, previousPage2);
		}
    	
    	for(int room_slot : gui.getIntegerList("ChooseTypeGui.TypeSlot")) inv.setItem(room_slot, new ItemStack(Material.AIR));
    	
    	return inv;
    }
    
    
    @EventHandler
    public void onClick(InventoryClickEvent e) {
    	Player p = (Player) e.getWhoClicked();
    	if(viewers.containsKey(p)) {
    		e.setCancelled(true);
    		
    		ItemStack click = e.getCurrentItem();
			
			if(click == null) return;
			if(click.getType().equals(Material.AIR)) return;
			
			/*NBTItem nbt = new NBTItem(click);
			if(!nbt.hasKey("ChooseTypeGui_ClickType")) return;*/
			String keyClick = NBTTag.getKey(click, "ChooseTypeGui_ClickType");
			String typeClick = NBTTag.getKey(click, "ChooseTypeGui_Type");
			if(keyClick == null || keyClick.isEmpty()) return;
			if(typeClick == null || typeClick.isEmpty()) return;

			switch(keyClick.toLowerCase()) {
			case "nextpage": {
				ChooseTypeGui inv = viewers.get(p);
				if(inv.curpage >= inv.pages.size() - 1) {
					return;
				} else {
					new ChooseTypeGui(p, inv.curpage + 1);
				}
				return;
			}
			case "previouspage": {
				ChooseTypeGui inv = viewers.get(p);
				if(inv.curpage > 0) {
					new ChooseTypeGui(p, inv.curpage - 1);
				}
				return;
			}
			case "choosetype": {

				if(!Game.hasTurn(p, typeClick)) {
					p.sendMessage(Messages.get("NoTurn"));
					return;
				}
				
				p.closeInventory();
				new BukkitRunnable() {
					public void run() {
						new PhoBanGui(p, 0, typeClick);
					}
				}.runTaskLater(AEPhoBan.inst(), 1);
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
    
    
    public String parseType(String type) {
    	FileConfiguration phoban = FileManager.getFileConfig(Files.PHOBAN);
    	
    	for(String str : phoban.getKeys(false)) {
    		if(!phoban.contains(str + ".Type")) continue;
    		if(phoban.getString(str + ".Type", "").equalsIgnoreCase(type)) return str;
    	}
    	
    	return "deo-co-con-cac-gi-o-day-het";
    }

}
