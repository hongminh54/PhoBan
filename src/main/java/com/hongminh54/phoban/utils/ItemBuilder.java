package com.hongminh54.phoban.utils;

import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.manager.FileManager;
import me.orineko.pluginspigottools.MethodDefault;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemBuilder {
	
	public static ItemStack build(FileManager.Files file, String path, HashMap<String, List<String>> replace) {
		FileConfiguration config = FileManager.getFileConfig(file);
		
		ItemBuilder builder = new ItemBuilder();
		builder.material(Utils.matchMaterial(config.getString(path + ".ID")));
		if(config.contains(path + ".Amount")) {
			builder.amount(config.getInt(path + ".Amount"));
		}
		if(config.contains(path + ".Data")) {
			builder.data((byte) config.getInt(path + ".Data"));
		}
		if(config.contains(path + ".Name")) {
			String name = ChatColor.translateAlternateColorCodes('&', config.getString(path + ".Name"));
			for(String old : replace.keySet()) {
				for(String value : replace.get(old)) {
					if (value != null) {
						name = name.replace(old, value);
					} else {
						name = name.replace(old, "");
					}
				}
			}
			builder.name(name);
		}
		if(config.contains(path + ".Lore")) {
			List<String> lores = new ArrayList<String>();
			f1 : for(String lore : config.getStringList(path + ".Lore")) {
				String newLore = ChatColor.translateAlternateColorCodes('&', lore);

				for(int i = 1; i <= Game.maxStage; i++) {
					if(lore.contains("<mob" + i + ">")) {
						List<String> mobValues = replace.get("<mob" + i + ">");
						if (mobValues != null) {
							for(String value : mobValues) {
								lores.add(ChatColor.translateAlternateColorCodes('&', value));
							}
						}
						continue f1;
					}
				}
				if(lore.contains("<players>")) {
					List<String> playerValues = replace.get("<players>");
					if (playerValues != null) {
						for(String value : playerValues) {
							lores.add(ChatColor.translateAlternateColorCodes('&', value));
						}
					}
					continue;
				}
				
				for(String old : replace.keySet()) {
					for(String value : replace.get(old)) {
						if (value != null) {
							newLore = newLore.replace(old, value);
						} else {
							newLore = newLore.replace(old, "");
						}
					}
				}
				lores.add(newLore);
			}
			builder.lore(lores);
		}
		if(config.contains(path + ".CustomModel")){
			String version = Bukkit.getServer().getClass().getPackage().getName()
					.replace(".", ",").split(",")[3].split("_")[1];
			if(MethodDefault.formatNumber(version, 0) >= 16){
				int customModel = config.getInt(path + ".CustomModel", 0);
				if(customModel > 0) {
					builder.customModel(customModel);
				}
			}
		}
		return builder.build();
	}

	public static ItemStack skull(ItemStack origin, String target) {
		String material = origin.getType().name().toLowerCase();
		if(material.contains("player_head") || (material.contains("skull_item") && origin.getData().getData() == 3)) {
			SkullMeta meta = (SkullMeta) origin.getItemMeta();
			meta.setOwner(target);
			origin.setItemMeta(meta);
		}
		return origin;
	}
	
	
	
	private Material material;
	private int amount;
	private String name;
	private List<String> lores;
	private int customModel;
	private byte data;
	private String skullowner;
	private boolean glow;
	
	public ItemBuilder() {
		this.material = Material.STONE;
		this.amount = 1;
		this.customModel = 0;
		this.data = 0;
		this.glow = false;
	}
	
	public ItemBuilder material(Material m) {
		this.material = m;
		return this;
	}
	
	public ItemBuilder amount(int a) {
		this.amount = a;
		return this;
	}
	
	public ItemBuilder name(String n) {
		this.name = n;
		return this;
	}
	
	public ItemBuilder lore(List<String> l) {
		this.lores = l;
		return this;
	}

	public ItemBuilder customModel(int customModel) {
		this.customModel = customModel;
		return this;
	}

	public ItemBuilder data(byte d) {
		this.data = d;
		return this;
	}
	
	public ItemBuilder skull(String s) {
		this.skullowner = s;
		return this;
	}
	
	public ItemBuilder glow(boolean g) {
		this.glow = g;
		return this;
	}
	
	public ItemStack build() {
		ItemStack item = new ItemStack(this.material, this.amount, this.data);
		ItemMeta meta = item.getItemMeta();
		SkullMeta smeta = null;
		if(this.data > 0) {
			item.setDurability(this.data);
		}
		if(meta != null) {
			if(this.name != null) {
				meta.setDisplayName(this.name);
			}
			if(this.lores != null) {
				meta.setLore(this.lores);
			}
			if(this.glow) {
				meta.addEnchant(Enchantment.DURABILITY, 11, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}
			if(this.customModel > 0) {
				meta.setCustomModelData(customModel);
			}
			item.setItemMeta(meta);
		}
		if(this.skullowner != null) {
			smeta = (SkullMeta) item.getItemMeta();
			if(smeta != null) {
				smeta.setOwner(this.skullowner);
				item.setItemMeta(smeta);
			}
		}

		return item;
	}

}
