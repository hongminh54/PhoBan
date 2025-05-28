package com.hongminh54.phoban.utils;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.game.GameMob;
import com.hongminh54.phoban.manager.FileManager;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IEssentials;
import me.orineko.pluginspigottools.MethodDefault;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	
	private static boolean LEGACY = true;
	private static boolean sendCommandFeedbackUpdate = false;
	
	public static void checkVersion() {
		String version = Bukkit.getBukkitVersion().split("-")[0];
		int versionMajor = Integer.parseInt(version.split("\\.")[0]);
		int versionMinor = Integer.parseInt(version.split("\\.")[1]);
		int versionMicro = 0;
		try {
			versionMicro = Integer.parseInt(version.split("\\.")[2]);
		} catch(Exception ex) {}

		if(versionMinor >= 13) LEGACY = false;
	}
	
	public static boolean isLegacy() {
		return LEGACY;
	}
	
	public static Material matchMaterial(String mat) {
		return LEGACY ? Material.matchMaterial(mat) : Material.matchMaterial(mat, LEGACY);
	}
	
	
	public static int firstEmpty(int rows) {
		if(rows < 3) rows = 3;
		switch(rows) {
		case 3: return 16;
		case 4: return 25;
		case 5: return 34;
		case 6: return 43;
		}
		return 43;
	}
	
	
	public static boolean checkStage(
			List<GameMob> requireMob,
			HashMap<String, Integer> current) {

		HashMap<String, Integer> require = new HashMap<>();
		for(GameMob gameMob : requireMob) {
			if(require.containsKey(gameMob.getType())) {
				int c = require.get(gameMob.getType());
				int n = c + gameMob.getAmount();
				require.replace(gameMob.getType(), n);
			} else require.put(gameMob.getType(), gameMob.getAmount());
		}
			
		for(String key : require.keySet()) {
			if(!current.containsKey(key)) return false;
			if(current.get(key) < require.get(key)) return false;
		}
			
		return true;
	}

	public static boolean isSuckBlock(Location loc) {
		Location loc1 = loc.clone();
		Location loc2 = loc.clone().add(0, 1, 0);
		return !loc1.getBlock().getType().equals(Material.AIR) && !loc2.getBlock().getType().equals(Material.AIR);
	}


	public static void scanSection(FileConfiguration configScan, FileConfiguration newConfig, String key, String arenaName) {
		if(!configScan.contains(key)) return;
		for(String k : configScan.getConfigurationSection(key).getKeys(false)) {
			if(configScan.isConfigurationSection(key + "." + k)) scanSection(configScan, newConfig, key + "." + k, arenaName);
			else newConfig.set((key + "." + k).replaceFirst(arenaName + ".", ""), configScan.get(key + "." + k));
		}
	}


	public static boolean isJail(Player p) {
		if(!Bukkit.getPluginManager().isPluginEnabled("Essentials")) return false;
		try {
			Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
			return ess.getUser(p).isJailed();
		} catch(Exception e) {
			IEssentials ess = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
			return ess.getUser(p).isJailed();
		}
	}


	public static int getRespawnTurn(Player p) {
		for(int i = 100; i > 0; i--) {
			if(AEPhoBan.inst().hasPerm(p, "phoban.respawn." + i)) return i;
		}
		return FileManager.getFileConfig(FileManager.Files.CONFIG).getInt("Settings.Respawn.Amount");
	}

	public static void sendError(String title, String message){
		Bukkit.getConsoleSender().sendMessage(MethodDefault.formatColor("&a&l[AEPhoBan] &4"+title+" -> &c"+message));
	}

	public static void sendTitle(Player p, String title, String subtitle) {
		p.sendTitle(title.replace("&", "§"), subtitle.replace("&", "§"));
	}


	public static String getProgress(int current, int max) {
		FileConfiguration format = FileManager.getFileConfig(FileManager.Files.FORMAT);
		String character = format.getString("progress.char");
		int maxCharacter = format.getInt("progress.max-char");
		String active = format.getString("progress.active").replace("&", "§");
		String inactive = format.getString("progress.inactive").replace("&", "§");
		int activeChar = max == 0 || current == 0 ? 0 : (int) (((double) current / (double) max) * maxCharacter);
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i <= maxCharacter; i++) {
			if(i <= activeChar) sb.append(active).append(character);
			else sb.append(inactive).append(character);
		}
		return sb.toString();
	}


	public static void playSound(Player p, String strSound) {
		String[] sounds = strSound.split("\\|");
		for(String sound : sounds) {
			try {
				Sound s = Sound.valueOf(sound.toUpperCase());
				p.playSound(p.getLocation(), s, 1f, 1f);
				break;
			} catch(Exception ex) {
				continue;
			}
		}
	}


	public static void spawnParticle(Location loc, String strParticle, Player run) {
		if(loc == null) return;

		loc = loc.clone().add(0, 1.5, 0);
		String[] particles = strParticle.split("\\|");
		for(String particle : particles) {
			if(particle.startsWith("particle") && run != null) {
				boolean opState = run.isOp();
				run.setOp(true);
				run.performCommand(particle
						.replace("<x>", String.valueOf(loc.getX()))
						.replace("<y>", String.valueOf(loc.getY()))
						.replace("<z>", String.valueOf(loc.getZ()))
				);
				run.setOp(opState);
				break;
			}
			try {
				String[] particleSplit = particle.split(" ");
				String particleName = particleSplit[0];
				Particle p = Particle.valueOf(particleName.toUpperCase());
				switch(particleSplit.length) {
					case 4: {
						double offsetX = Double.parseDouble(particleSplit[1]);
						double offsetY = Double.parseDouble(particleSplit[2]);
						double offsetZ = Double.parseDouble(particleSplit[3]);
						loc.getWorld().spawnParticle(p, loc, 0, offsetX, offsetY, offsetZ);
						break;
					}
					case 5: {
						double offsetX = Double.parseDouble(particleSplit[1]);
						double offsetY = Double.parseDouble(particleSplit[2]);
						double offsetZ = Double.parseDouble(particleSplit[3]);
						double speed = Double.parseDouble(particleSplit[4]);
						loc.getWorld().spawnParticle(p, loc, 0, offsetX, offsetY, offsetZ, speed);
						break;
					}
					case 6: {
						double offsetX = Double.parseDouble(particleSplit[1]);
						double offsetY = Double.parseDouble(particleSplit[2]);
						double offsetZ = Double.parseDouble(particleSplit[3]);
						double speed = Double.parseDouble(particleSplit[4]);
						int count = Integer.parseInt(particleSplit[5]);
						loc.getWorld().spawnParticle(p, loc, count, offsetX, offsetY, offsetZ, speed);
						break;
					}
					case 7: {
						double offsetX = Double.parseDouble(particleSplit[1]);
						double offsetY = Double.parseDouble(particleSplit[2]);
						double offsetZ = Double.parseDouble(particleSplit[3]);
						double speed = Double.parseDouble(particleSplit[4]);
						int count = Integer.parseInt(particleSplit[5]);
						boolean force = particleSplit[6].equalsIgnoreCase("force");
						if(isLegacy()) loc.getWorld().spawnParticle(p, loc, count, offsetX, offsetY, offsetZ, speed, null);
						else loc.getWorld().spawnParticle(p, loc, count, offsetX, offsetY, offsetZ, speed, null, force);
						break;
					}
				}
				break;
			} catch(Exception ex) {}
			try {
				Effect e = Effect.valueOf(particle.toUpperCase());
				loc.getWorld().playEffect(loc, e, 1);
				break;
			} catch(Exception ex) {}
		}
	}


	public static int parseInt(String strInteger) {
		if(strInteger.contains(":")) {
			int min = Integer.parseInt(strInteger.split(":")[0]);
			int max = Integer.parseInt(strInteger.split(":")[1]);
			if(min == max) return min;
			if(min > max) {
				int temp = max;
				max = min;
				min = temp;
			}
			return ThreadLocalRandom.current().nextInt(min, max);
		}
		return Integer.parseInt(strInteger);
	}


	public static String randomColor() {
		UUID uid = UUID.randomUUID();
		String s = uid.toString().replace("-", "");
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < Math.min(7, s.length()); i++) {
			stringBuilder.append("&").append(s.charAt(i));
		}
		return stringBuilder.append("&r").toString().replace("&", "§");
	}

	/**
	 * Định dạng tên vật phẩm từ enum của Material để hiển thị đẹp hơn
	 * Ví dụ: DIAMOND_SWORD -> Diamond Sword
	 * 
	 * @param materialName Tên của Material
	 * @return Tên đã được định dạng
	 */
	public static String formatItemName(String materialName) {
		if (materialName == null || materialName.isEmpty()) {
			return "Unknown Item";
		}
		
		String[] words = materialName.split("_");
		StringBuilder result = new StringBuilder();
		
		for (String word : words) {
			if (!word.isEmpty()) {
				result.append(word.substring(0, 1).toUpperCase())
					  .append(word.substring(1).toLowerCase())
					  .append(" ");
			}
		}
		
		return result.toString().trim();
	}

}

