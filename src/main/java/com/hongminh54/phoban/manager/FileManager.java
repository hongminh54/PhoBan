package com.hongminh54.phoban.manager;

import com.hongminh54.phoban.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;

public class FileManager {
	
	private static HashMap<Files, File> file = new HashMap<Files, File>();
	private static HashMap<Files, FileConfiguration> configuration = new HashMap<Files, FileConfiguration>();
	
	public static void setup(Plugin plugin) {
		boolean legacy = Utils.isLegacy();
		for(Files f : Files.values()) {
			String location = legacy ? f.getLegacyLocation() : f.getLocation();
			File fl = new File(plugin.getDataFolder(), location);
			if(!fl.exists()) {
				fl.getParentFile().mkdirs();
				plugin.saveResource(location, false);
			}
			FileConfiguration config = new YamlConfiguration();
			try {
				config.load(fl);
			} catch(Exception ex) {
				
			}
			file.put(f, fl);
			configuration.put(f, config);
		}
	}
	
	public static FileConfiguration getFileConfig(Files f) {
		return configuration.get(f);
	}
	
	public static void saveFileConfig(FileConfiguration data, Files f) {
		try {
			data.save(file.get(f));
		} catch(Exception ex) {
			
		}
	}

	public static void saveFileConfig(FileConfiguration data, File f) {
		try {
			data.save(f);
		} catch(Exception ex) {

		}
	}
	
	
	
	public enum Files {
		
		CONFIG("config.yml", "config.yml"),
		MESSAGE("message.yml", "message.yml"),
		GUI("gui-1.13.yml", "gui.yml"),
		DATA("data.yml", "data.yml"),
		PHOBAN("phoban.yml", "phoban.yml"),
		SPAWN("spawn.yml", "spawn.yml"),
		FORMAT("format.yml", "format.yml"),
		GUI_TOP("gui-top-1.13.yml", "gui-top.yml"),
		;
		
		private String location;
		private String legacyLocation;
		
		Files(String l, String legacyLocation) {
			this.location = l;
			this.legacyLocation = legacyLocation;
		}
		
		public String getLocation() {
			return this.location;
		}

		public String getLegacyLocation() {
			return this.legacyLocation;
		}
		
	}

}
