package com.hongminh54.phoban.game;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.mythicmobs.BukkitAPIHelper;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;
import com.hongminh54.phoban.utils.Random;
import com.hongminh54.phoban.utils.ItemUtil;
import fr.skytasul.glowingentities.GlowingEntities;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Game {

	public static final int maxStage = 10;

	
	private static LinkedHashMap<String, Game> game = new LinkedHashMap<>();
	
	public static LinkedHashMap<String, Game> game() {
		return game;
	}
	
	public static Game getGame(String name) {
		if(!game.containsKey(name)) return null;
		return game.get(name);
	}

	public static List<String> listGame() {
		return new ArrayList<>(game.keySet());
	}

	public static List<String> listGameWithoutCompleteSetup() {
		File folder = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator);
		if(!folder.exists()) folder.mkdirs();

		List<String> listGame = new ArrayList<>();
		for(File file : folder.listFiles()) {
			if(!file.getName().contains(".yml")) continue;
			String name = file.getName().replace(".yml", "");
			listGame.add(name);
		}
		return listGame;
	}

	public static void convertData() {
		File oldFile = new File(AEPhoBan.inst().getDataFolder(), "room.yml");
		if(!oldFile.exists()) return;
		FileConfiguration room = YamlConfiguration.loadConfiguration(oldFile);
		for(String key : room.getKeys(false)) {
			File newFile = new File(AEPhoBan.inst().getDataFolder(), File.separator + "room" + File.separator + key + ".yml");
			if(!newFile.exists()) try { newFile.createNewFile(); } catch(Exception ignored) {}
			FileConfiguration config = new YamlConfiguration();
			Utils.scanSection(room, config, key, key);
			try { config.save(newFile); } catch(Exception ignored) {}
		}
		oldFile.delete();
	}

	public static List<String> listType() {
		List<String> listType = new ArrayList<>();
		for(Game g : game().values()) {
			if(!listType.contains(g.getType())) listType.add(g.getType());
		}
		return listType;
	}
	
	public static void load() {
		File folder = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator);
		if(!folder.exists()) folder.mkdirs();
		
		for(File file : folder.listFiles()) {
			if(!file.getName().contains(".yml")) continue;
			String name = file.getName().replace(".yml", "");
			FileConfiguration room = YamlConfiguration.loadConfiguration(file);
//			if(!Game.canJoin(room)) continue;
			load(name, room, file);
		}
	}
	
	public static void load(String name, FileConfiguration room, File configFile) {
		game.remove(name);
		
		int time = room.getInt("Time") * 60;
		Location spawn = (Location) room.get("Spawn");
		int max_players = room.getInt("Player");

		List<GameMob> boss = new ArrayList<>();
		HashMap<String, Integer> timeBoss = new HashMap<>();
        if(room.contains("Boss")) {
            String boss_type = room.getString("Boss.Type");
            int boss_amount = room.getInt("Boss.Amount");
            int timeStageBoss = room.getInt("Boss.Time") * 60;
            boss.add(new GameMob("", boss_type, boss_amount));
            timeBoss.put(boss_type, timeStageBoss);
        }

		String roomType = room.getString("Type");

		Game g = new Game(name, time, spawn, max_players, boss, timeBoss, room, roomType, configFile);
		
		// Cập nhật trạng thái khóa từ file cấu hình
		g.locked = room.getBoolean("Locked", false);

		for(int i = 1; i <= maxStage; i++) {
			if(!room.contains("Mob" + i)) continue;
			Set<String> mobList = room.getConfigurationSection("Mob" + i).getKeys(false);
			if(mobList.size() == 0) continue;
			LinkedHashMap<String, List<GameMob>> stageHash = new LinkedHashMap<>();
			LinkedHashMap<String, HashMap<String, Integer>> timeStageHash = new LinkedHashMap<>();
			for(String key : mobList) {
				String type = room.getString("Mob" + i + "." + key + ".Type");
				int amount = room.getInt("Mob" + i + "." + key + ".Amount");
				int timeStage = room.getInt("Mob" + i + "." + key + ".Time") * 60;
				if(timeStage == 0) timeStage = -1;

				List<GameMob> a = new ArrayList<>();
				a.add(new GameMob(key, type, amount));
				for(String childKey : room.getConfigurationSection("Mob" + i + "." + key).getKeys(false)) {
					if(childKey.equalsIgnoreCase("Location") || childKey.equalsIgnoreCase("Type") || childKey.equalsIgnoreCase("Amount") || childKey.equalsIgnoreCase("Time")) {
						continue;
					}

					String childType = room.getString("Mob" + i + "." + key + "." + childKey + ".Type");
					int childAmount = room.getInt("Mob" + i + "." + key + "." + childKey + ".Amount");
					a.add(new GameMob(childKey, childType, childAmount));
				}

				HashMap<String, Integer> b = new HashMap<>();
				b.put(type, timeStage);

				stageHash.put(key, a);
				timeStageHash.put(key, b);
			}

			try {
				Field stageField = g.getClass().getDeclaredField("stage" + i);
				stageField.setAccessible(true);
				stageField.set(g, stageHash);
				stageField.setAccessible(false);

				Field timeStageField = g.getClass().getDeclaredField("timeStage" + i);
				timeStageField.setAccessible(true);
				timeStageField.set(g, timeStageHash);
				timeStageField.setAccessible(false);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}

		g.init();
		game.put(name, g);
	}
	
	public static void deleteRoom(String room) {
		game.remove(room);
	}
	
	
	public static boolean canJoin(FileConfiguration room) {
		int roomContains = 0;
		for(int i = 1; i <= maxStage; i++) {
			if(room.contains("Mob" + i)) {
				if(room.getConfigurationSection("Mob" + i).getKeys(false).size() > 0) roomContains++;
			}
		}
        if(room.contains("Boss")) roomContains++;
		return room.contains("Prefix") &&
				room.contains("Player") &&
				room.contains("Time") &&
				room.contains("Reward") &&
				roomContains >= 1 &&
				room.contains("RewardAmount") &&
				room.contains("Spawn") &&
				room.contains("Type") &&
				!room.getBoolean("Locked", false);
	}
	
	
	public static int getTurn(OfflinePlayer p, String type) {
		FileConfiguration data = FileManager.getFileConfig(Files.DATA);
		if(!data.contains(p.getName() + ".Turn." + type)) {
			int defaultTurn = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.DefaultTurn", 1);
			return defaultTurn;
		}
		return data.getInt(p.getName() + ".Turn." + type);
	}
	
	public static void giveTurn(OfflinePlayer p, String type, int amount) {
		FileConfiguration data = FileManager.getFileConfig(Files.DATA);
		int turn = getTurn(p, type) + amount;
		data.set(p.getName() + ".Turn." + type, turn);
		FileManager.saveFileConfig(data, Files.DATA);
	}

	public static void giveTurn(OfflinePlayer[] listPlayer, List<String> types, int amount) {
		FileConfiguration data = FileManager.getFileConfig(Files.DATA);
		for(OfflinePlayer player : listPlayer) {
			for(String type : types) {
				int turn = getTurn(player, type) + amount;
				data.set(player.getName() + ".Turn." + type, turn);
			}
		}
		FileManager.saveFileConfig(data, Files.DATA);
	}

	public static void giveTurnChangeDay(OfflinePlayer[] listPlayer, List<String> types, int amount) {
		FileConfiguration data = FileManager.getFileConfig(Files.DATA);
		for(OfflinePlayer player : listPlayer) {
			for(String type : types) {
				int currentTurn = getTurn(player, type);
				if(currentTurn > 0) continue;
				int turn = currentTurn + amount;
				data.set(player.getName() + ".Turn." + type, turn);
			}
		}
		FileManager.saveFileConfig(data, Files.DATA);
	}

	public static void takeTurn(OfflinePlayer p, String type, int amount) {
		FileConfiguration data = FileManager.getFileConfig(Files.DATA);
		int turn = getTurn(p, type) - amount;
		data.set(p.getName() + ".Turn." + type, turn);
		FileManager.saveFileConfig(data, Files.DATA);
	}
	
	public static boolean hasTurn(OfflinePlayer p, String type) {
		return getTurn(p, type) > 0;
	}


	public static void setGlobalSpawn(Location loc) {
		FileConfiguration config = FileManager.getFileConfig(Files.SPAWN);
		config.set("s", loc.clone());
		FileManager.saveFileConfig(config, Files.SPAWN);
	}

	public static Location getGlobalSpawn() {
		FileConfiguration config = FileManager.getFileConfig(Files.SPAWN);
		Object obj = config.get("s", null);
		return (obj == null) ? null : (Location) obj;
	}
	
	public static void setDefaultSpawn(Location loc) {
		FileConfiguration config = FileManager.getFileConfig(Files.SPAWN);
		config.set("default", loc.clone());
		FileManager.saveFileConfig(config, Files.SPAWN);
	}

	public static Location getDefaultSpawn() {
		FileConfiguration config = FileManager.getFileConfig(Files.SPAWN);
		Object obj = config.get("default", null);
		return (obj == null) ? null : (Location) obj;
	}
	
	
	
	private final String name;
	private GameStatus status;
	private GameTask task;
	private int maxtime;
	private Location spawn;
	private int max_players;
	
	private int time;
	private int stageTime;
	private List<Player> players;
	
	private LinkedHashMap<String, List<GameMob>> stage1;
	private LinkedHashMap<String, List<GameMob>> stage2;
	private LinkedHashMap<String, List<GameMob>> stage3;
	private LinkedHashMap<String, List<GameMob>> stage4;
	private LinkedHashMap<String, List<GameMob>> stage5;
	private LinkedHashMap<String, List<GameMob>> stage6;
	private LinkedHashMap<String, List<GameMob>> stage7;
	private LinkedHashMap<String, List<GameMob>> stage8;
	private LinkedHashMap<String, List<GameMob>> stage9;
	private LinkedHashMap<String, List<GameMob>> stage10;

	private LinkedHashMap<String, HashMap<String, Integer>> timeStage1;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage2;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage3;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage4;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage5;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage6;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage7;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage8;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage9;
	private LinkedHashMap<String, HashMap<String, Integer>> timeStage10;

	private final List<GameMob> boss;
	private final HashMap<String, Integer> timeBoss;
	private List<HashMap<String, List<GameMob>>> stage = new ArrayList<>();
	private final List<HashMap<String, HashMap<String, Integer>>> timeStage = new ArrayList<>();
	private int current_stage;
	private int stage_count;
	private HashMap<String, Integer> current_progress;
	private FileConfiguration room;
	private final File configFile;
	private final String type;

	private int realStage = 1;
	@Getter private int realTurn = 0;
	@Getter private int totalTurn = 0;

	private final Map<String, Integer> totalKill = new HashMap<>();
	private final Map<String, Double> totalDamage = new HashMap<>();
	
	// Thuộc tính để kiểm tra trạng thái khóa phòng
	private boolean locked = false;
	
	public Game(String name, int time, Location spawn, int max_players, List<GameMob> boss, HashMap<String, Integer> timeBoss, FileConfiguration room, String type, File configFile) {
		this.name = name;
		this.status = GameStatus.WAITING;
		this.maxtime = time;
		this.spawn = spawn;
		this.max_players = max_players;
		
		this.time = time;
		this.stageTime = 0;
		this.players = new ArrayList<>();

		this.boss = boss;
		this.timeBoss = timeBoss;
		this.stage = new ArrayList<>();
		this.current_stage = 1;
		this.stage_count = -1;
		this.current_progress = new HashMap<>();
		this.room = room;
		this.configFile = configFile;
		this.type = type;
	}
	
	
	public String getName() {
		return this.name;
	}
	
	
	public GameStatus getStatus() {
		return this.status;
	}
	
	public void setStatus(GameStatus status) {
		this.status = status;
	}
	
	
	public List<Player> getPlayers() {
		return this.players;
	}
	
	
	public int getTimeLeft() {
		return this.time;
	}
	
	public void time() {
		this.time = this.time - 1;
		if(this.stageTime > 0) this.stageTime = this.stageTime - 1;
	}
	
	public void resetTime() {
		this.time = this.maxtime;
	}

	public int getStageTime() {
		return this.stageTime;
	}

	public void clearCurrentStage() {
		clearMobs();
	}
	

	public void init() {
		this.initStage();
		this.task = new GameTask(this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(AEPhoBan.inst(), task, 20, 20);
	}

	private void initStage() {
		for(int i = 1; i <= maxStage; i++) {
			try {
				Field stageField = this.getClass().getDeclaredField("stage" + i);
				LinkedHashMap<String, List<GameMob>> stageHash = (LinkedHashMap<String, List<GameMob>>) stageField.get(this);
				if(stageHash == null) continue;
				for(String key : stageHash.keySet()) {
					HashMap<String, List<GameMob>> h = new HashMap<>();
					h.put(key, stageHash.get(key));
					stage.add(h);
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
        if(!boss.isEmpty()) {
            HashMap<String, List<GameMob>> h = new HashMap<>();
            h.put("Boss", boss);
            stage.add(h);
        }
		this.totalTurn = stage.size();

		for(int i = 1; i <= maxStage; i++) {
			try {
				Field timeStageField = this.getClass().getDeclaredField("timeStage" + i);
				LinkedHashMap<String, HashMap<String, Integer>> timeStageHash = (LinkedHashMap<String, HashMap<String, Integer>>) timeStageField.get(this);
				if(timeStageHash == null) continue;
				for(String key : timeStageHash.keySet()) {
					HashMap<String, HashMap<String, Integer>> h = new HashMap<>();
					h.put(key, timeStageHash.get(key));
					timeStage.add(h);
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
        if(!timeBoss.isEmpty()) {
            HashMap<String, HashMap<String, Integer>> h = new HashMap<>();
            h.put("Boss", timeBoss);
            timeStage.add(h);
        }
	}
	
	public void addProgress(String key, int value) {
		if(!this.current_progress.containsKey(key)) this.current_progress.put(key, value);
		else this.current_progress.replace(key, this.current_progress.get(key) + value);
	}
	
	
	public List<GameMob> getStage(int s) {
		if(s >= this.stage.size()) return new ArrayList<>();
		for(String key : this.stage.get(s).keySet()) {
			return this.stage.get(s).get(key);
		}
		return new ArrayList<>();
	}

	public HashMap<String, Integer> getTimeStage(int s) {
		for(String key : this.timeStage.get(s).keySet()) {
			return this.timeStage.get(s).get(key);
		}
		return new HashMap<>();
	}
	
	public String getStageKey(int s, int i) {
		int index = 0;

		for(String k : this.stage.get(s).keySet()) {
			for(GameMob mob : this.stage.get(s).get(k)) {
				if(index == i) return mob.getKey();
				index++;
			}
		}
		return "";
	}

	public Set<String> getStageKey(int s) {
		return this.stage.get(s).keySet();
	}
	
	public int keyToStage(String key) {
		for(int i = 1; i <= maxStage; i++) {
			try {
				Field stageField = this.getClass().getDeclaredField("stage" + i);
				HashMap<String, HashMap<String, Integer>> stageHash = (HashMap<String, HashMap<String, Integer>>) stageField.get(this);
				if(stageHash == null) continue;
				for(String k : stageHash.keySet()) {
					if(k.equals(key)) return i;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return maxStage + 1;
	}

	public boolean hasStage(int s) {
		if(s > maxStage) return true;
		try {
			Field stageField = this.getClass().getDeclaredField("stage" + s);
			HashMap<String, HashMap<String, Integer>> stageHash = (HashMap<String, HashMap<String, Integer>>) stageField.get(this);
			return stageHash != null;
		} catch(Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public int keyToTurn(String key) {
		int stage = keyToStage(key);

		int turn = 0;
		for(int i = 1; i <= maxStage; i++) {
			turn = 0;
			try {
				Field stageField = this.getClass().getDeclaredField("stage" + i);
				HashMap<String, HashMap<String, Integer>> stageHash = (HashMap<String, HashMap<String, Integer>>) stageField.get(this);
				if(stageHash == null) continue;
				for(String k : stageHash.keySet()) {
					if(k.equals(key)) return turn + 1;
					turn += 1;
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		return turn;
	}
	
	public void checkStage() {
		if(Utils.checkStage(this.getStage(this.stage_count), this.current_progress)) newStage();
	}
	
	public int getProgressLeft() {
		for(String key : current_progress.keySet()) {
			if(!current_progress.containsKey(key)) return 0;
			return getProgressMax() - current_progress.get(key);
		}
		return 0;
	}
	
	public int getProgressCurrent() {
		for(String key : current_progress.keySet()) {
			if(!current_progress.containsKey(key)) return 0;
			return current_progress.get(key);
		}
		return 0;
	}

	public int getProgressCurrent(String k) {
		for(String key : current_progress.keySet()) {
			if(key.equals(k)) return current_progress.get(key);
		}
		return 0;
	}
	
	public int getProgressMax() {
		List<GameMob> requireMob = this.getStage(this.stage_count);
		HashMap<String, Integer> require = new HashMap<>();
		for(GameMob gameMob : requireMob) {
			if(require.containsKey(gameMob.getType())) {
				int c = require.get(gameMob.getType());
				int n = c + gameMob.getAmount();
				require.replace(gameMob.getType(), n);
			} else require.put(gameMob.getType(), gameMob.getAmount());
		}

		for(String key : require.keySet()) {
			if(!require.containsKey(key)) return 0;
			return require.get(key);
		}
		return 0;
	}

	public int getProgressMax(String k) {
		List<GameMob> requireMob = this.getStage(this.stage_count);
		int max = 0;
		for(GameMob gameMob : requireMob) {
			if(gameMob.getType().equals(k)) max += gameMob.getAmount();
		}
		return max;
	}

	public int newStage() {
		if(this.stage_count + 1 >= this.stage.size()) {
			complete();
			return 1;
		}

		String nextStageKey = this.getStageKey(this.stage_count + 1, 0);
		int nextStage = this.keyToStage(nextStageKey);
		while(!this.hasStage(this.current_stage)) this.current_stage++;
		if(nextStage > this.current_stage) {
			this.stage_countdown = true;
			this.realStage += 1;
			return 2;
		}

		this.stageTime = 0;
		this.stage_countdown = false;
		this.stage_count += 1;
		this.current_progress = new HashMap<>();
		
		int radius = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.SpawnRadius");

		String firstKeyy = "";

		int index = 0;
		List<GameMob> currentStage = this.getStage(this.stage_count);
		Map<String, Integer> mobMap = new HashMap<>();
		for(GameMob gameMob : currentStage) {
			String key = gameMob.getType();
			int amount = gameMob.getAmount();
			if(index == 0) {
				int timeStage = this.getTimeStage(this.stage_count).get(key);
				if(timeStage > 0) this.stageTime = timeStage;
				else this.stageTime = -1;
			}

			String mob = "Boss";
			for(int i = 1; i <= maxStage; i++) {
				if(this.current_stage == i) mob = "Mob" + i;
			}
			String keyy = mob.equals("Boss") ? "" : "." + this.getStageKey(this.stage_count, index);
			if(index == 0) firstKeyy = keyy;
			String path = "";
			if(index == 0) path = mob + keyy;
			else path = mob + firstKeyy + keyy;
			Location loc = (Location) room.get(path + ".Location");
			
			loc.add(0, 1, 0);
			if(FileManager.getFileConfig(Files.CONFIG).getBoolean("Settings.TeleportNewStage")) {
				for(Player player : this.players) {
					player.teleport(loc);
				}
			}
			loc.subtract(0, 1, 0);

			BukkitAPIHelper mm = AEPhoBan.inst().getBukkitAPIHelper();

			String displayName = mm.getMythicMobDisplayNameGet(key);
			mobMap.put(Utils.randomColor() + displayName, amount);
			for(Player p : this.players) Utils.playSound(p, FileManager.getFileConfig(Files.CONFIG).getString("Sound.TurnStart", ""));

			try {
				boolean mobStartGlow = FileManager.getFileConfig(Files.CONFIG).getBoolean("Settings.MobStartGlow", true);
				int startGlowFade = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StartGlowFade", 15);
				long mobSpawnDelay = (long) (FileManager.getFileConfig(Files.CONFIG).getDouble("Settings.MobSpawnDelay", 1.5) * 1000);
				if(mobSpawnDelay <= 0) {
					Entity firstEntity = this.spawnMobs(key, amount, loc, radius, mm);
					if(mobStartGlow) {
						GlowingEntities glowingEntities = new GlowingEntities(AEPhoBan.inst());
						this.getPlayers().forEach(v -> {
							try {
								glowingEntities.setGlowing(firstEntity, v);
							} catch (ReflectiveOperationException e) {
								throw new RuntimeException(e);
							}
						});
//						firstEntity.setGlowing(true);
						if(startGlowFade > 0) {
							new BukkitRunnable() {
								public void run() {
									if(firstEntity.isDead()) return;
									glowingEntities.disable();
//									firstEntity.setGlowing(false);
								}
							}.runTaskLater(AEPhoBan.inst(), startGlowFade * 20L);
						}
					}
				} else {
					this.lastSpawn = System.currentTimeMillis() - 50L;
					boolean spawn = this.queueList.isEmpty();
					this.queueList.add(new GameSpawnQueue(key, amount - (spawn ? 1 : 0), loc));
					if(spawn) {
						Entity firstEntity = this.spawnMobs(key, 1, loc, radius, mm);
						if(mobStartGlow) {
							firstEntity.setGlowing(true);
							if(startGlowFade > 0) {
								new BukkitRunnable() {
									public void run() {
										if(firstEntity.isDead()) return;
										firstEntity.setGlowing(false);
									}
								}.runTaskLater(AEPhoBan.inst(), startGlowFade * 20L);
							}
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			index++;
		}

		this.sendInfo(mobMap);

		this.realTurn += 1;
		return 0;
	}


	private Entity spawnMobs(String key, int amount, Location loc, int radius, BukkitAPIHelper mm) {
		Entity firstSpawn = null;
		amount = Math.max(amount, 1);
		for(int i = 1; i <= amount; i++) {
			double origin = -radius;
			double bound = radius + 0.1;
			Location spawn = loc.clone().add(ThreadLocalRandom.current().nextDouble(origin, bound), 1, ThreadLocalRandom.current().nextDouble(origin, bound));
			while(Utils.isSuckBlock(spawn)) spawn = loc.clone().add(ThreadLocalRandom.current().nextDouble(origin, bound), 1, ThreadLocalRandom.current().nextDouble(origin, bound));

			if(!spawn.getChunk().isLoaded()) spawn.getChunk().load();
			Entity entity = mm.spawnMythicMob(key, spawn);
			if(i == 1) firstSpawn = entity;
			EntityData.data().put(entity, new EntityData(entity, this));
			Utils.spawnParticle(spawn, FileManager.getFileConfig(Files.CONFIG).getString("Particle.TurnStart"), this.players.size() == 0 ? null : this.players.get(0));
		}
		return firstSpawn;
	}

	
	public void nextStage() {
		this.current_stage += 1;
	}
	
	public boolean stage_countdown = false;
	
	public void start() {
		String type = room.getString("Type");
		for(Player player : this.players) {
			Game.takeTurn(player, type, 1);
		}

		this.status = GameStatus.PLAYING;
		this.current_stage = 1;
		this.stage_count = -1;
		this.realStage = 1;
		this.realTurn = 0;
		
		this.stage_countdown = false;
		this.quit_countdown = false;

		this.lastSpawn = 0;

		this.totalKill.clear();
		this.totalDamage.clear();
		this.queueList.clear();
		
		newStage();
	}
	
	public void forceStop() {
		this.leaveAllAfterComplete();
	}
	
	public void join(Player p) {
		PlayerData.data().remove(p);
		
		Location loc = p.getLocation().clone();
		p.teleport(this.spawn);
		p.setGameMode(GameMode.SURVIVAL);
		
		PlayerData.data().put(p, new PlayerData(p, this, loc));
		this.players.add(p);
		
		p.sendMessage(Messages.get("LeaveOnJoin"));
		
		String msg = Messages.get("PlayerJoin").replace("<player>", p.getName()).replace("<joined>", String.valueOf(this.players.size())).replace("<max>", String.valueOf(this.max_players));
		
		this.players.forEach(player -> {
			player.sendMessage(msg);
		});
	}
	
	public void leave(Player p, boolean message, boolean pointLose, boolean quit) {
		if(!PlayerData.data().containsKey(p)) return;
		
		this.players.remove(p);

		Location location = Game.getGlobalSpawn();
		if(location == null) location = PlayerData.data().get(p).getLocation();
//		if(location == null) location = p.getRespawnLocation();

		PlayerData.data().remove(p);

		if(location != null) {
			if (quit) {
				p.teleport(location);
			} else {
				Location finalLocation = location;
				Bukkit.getScheduler().scheduleSyncDelayedTask(AEPhoBan.inst(), () -> {
					p.teleport(finalLocation);
					if(this.quit_countdown) {
						p.sendMessage(Messages.get("TeleportComplete"));
					}
				}, 1);
			}
		} else {
			Utils.sendError("Game", p.getName()+" không có chổ để dịch chuyển về.");
		}
		
		p.setGameMode(GameMode.SURVIVAL);

		if(this.status.equals(GameStatus.PLAYING) && pointLose) {
			FileConfiguration dataF = FileManager.getFileConfig(Files.DATA);
			int curPoint = dataF.getInt(p.getName() + ".Point", 0);
			int point = Utils.parseInt(FileManager.getFileConfig(Files.CONFIG).getString("Point.Death", ""));
			if((curPoint - point) < 0 && !FileManager.getFileConfig(Files.CONFIG).getBoolean("Point.AllowNegative")) point = curPoint;
			dataF.set(p.getName() + ".Point", curPoint - point);
			FileManager.saveFileConfig(dataF, Files.DATA);
			p.sendMessage(Messages.get("PointLose").replace("<point>", String.valueOf(point)));
		}
		
		if(message) {
			String msg = Messages.get("PlayerQuit").replace("<player>", p.getName()).replace("<joined>", String.valueOf(this.players.size())).replace("<max>", String.valueOf(this.max_players));
			
			this.players.forEach(player -> {
				player.sendMessage(msg);
			});
		}
	}
	
	private void clearMobs() {
		List<EntityData> edata = new ArrayList<>(EntityData.data().values());
		for(EntityData e : edata) {
			if(!e.getGame().equals(this)) continue;
			
			EntityData.data().remove(e.getEntity());
			e.getEntity().remove();
		}
	}
	
	public void restore() {
		this.players = new ArrayList<>();
		this.current_stage = 1;
		this.stage_count = -1;
		this.totalKill.clear();
		this.totalDamage.clear();
		this.queueList.clear();
		
		clearMobs();
		
		this.status = GameStatus.WAITING;
		this.time = maxtime;
	}
	
	public void complete() {
		List<Player> playerss = new ArrayList<Player>(this.players);
		
		// Đảm bảo mã màu được xử lý đúng cách
		String prefix = room.getString("Prefix", "").replace("&", "§");
		Bukkit.broadcastMessage(Messages.get("BroadcastComplete").replace("&", "§").replace("<player>", this.players.get(0).getName()).replace("<prefix>", prefix));

		FileConfiguration data = FileManager.getFileConfig(Files.DATA);

		for(Player p : playerss) {
			int curPoint = data.getInt(p.getName() + ".Point", 0);
			int point = Utils.parseInt(FileManager.getFileConfig(Files.CONFIG).getString("Point.Win", ""));
			data.set(p.getName() + ".Point", curPoint + point);
			this.sendStatistic(p);
			p.sendMessage(Messages.get("PointWin").replace("<point>", String.valueOf(point)));
			p.sendMessage(Messages.get("Complete"));
			reward(p);
		}

		FileManager.saveFileConfig(data, Files.DATA);

		this.quit_countdown = true;
		task.setCountdown(FileManager.getFileConfig(Files.CONFIG).getInt("Settings.QuitCountdown"));
	}
	
	public void leaveAllAfterComplete() {
		List<Player> playerss = new ArrayList<>(this.players);
		
		for(Player p : playerss) {
			if(FileManager.getFileConfig(Files.CONFIG).getBoolean("Settings.UseDefaultSpawnAfterComplete", true)) {
				// Sử dụng spawn mặc định sau khi hoàn thành phó bản
				Location defaultSpawnLoc = Game.getDefaultSpawn();
				if(defaultSpawnLoc != null) {
					// Thay thế địa điểm cũ trong PlayerData
					if(PlayerData.data().containsKey(p)) {
						PlayerData.data().get(p).setLocation(defaultSpawnLoc);
					}
				}
			}
			leave(p, false, false, false);
		}
		
		restore();
	}
	
	public boolean quit_countdown = false;
	
	public void reward(Player p) {
		Random random = new Random();
		boolean hasReward = false;
		int rewardCount = 0;
		List<GameReward> rewards = new ArrayList<>();
		
		// Kiểm tra xem phó bản có phần thưởng không
		if (room.contains("Reward") && room.getConfigurationSection("Reward") != null && 
		    !room.getConfigurationSection("Reward").getKeys(false).isEmpty()) {
		
			// Phân chia phần thưởng thành hai danh sách: 100% và ngẫu nhiên
			List<String> guaranteedRewards = new ArrayList<>();
			
			for(String key : room.getConfigurationSection("Reward").getKeys(false)) {
				int chance = room.getInt("Reward." + key + ".Chance");
				if(room.contains("Reward." + key + ".Command")) {
					String command = room.getString("Reward." + key + ".Command", "");
					if(chance >= 100) {
						guaranteedRewards.add(key);
					} else {
						random.addChance(key, chance);
					}
				} else if(room.contains("Reward." + key + ".Item")) {
					if(chance >= 100) {
						guaranteedRewards.add(key);
					} else {
						random.addChance(key, chance);
					}
				}
			}
			
			// Trao phần thưởng 100%
			for(String key : guaranteedRewards) {
				if(room.contains("Reward." + key + ".Command")) {
					String command = room.getString("Reward." + key + ".Command", "");
					hasReward = true;
					rewardCount++;
					
					String friendlyMessage = createFriendlyMessage(command);
					
					// Thực thi lệnh
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", p.getName()));
					
					// Tạo đối tượng phần thưởng và thêm vào danh sách
					rewards.add(new GameReward(friendlyMessage, command));
					p.sendMessage(Messages.get("RewardCommandSuccess").replace("<command>", friendlyMessage));
				} else if(room.contains("Reward." + key + ".Item")) {
					// Lấy item từ config và tạo một bản sao chính xác sử dụng ItemUtil
					ItemStack item = ItemUtil.copyItem(room.getItemStack("Reward." + key + ".Item"));
					hasReward = true;
					rewardCount++;
					
					// Thêm item vào inventory người chơi
					HashMap<Integer, ItemStack> failed = p.getInventory().addItem(item);
					
					// Nếu inventory đầy, thả item xuống đất
					if (!failed.isEmpty()) {
						for (ItemStack dropItem : failed.values()) {
							p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
						}
						p.sendMessage(Messages.get("InventoryFull"));
					}
					
					// Tạo đối tượng phần thưởng và thêm vào danh sách
					String itemName = "";
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
						itemName = item.getItemMeta().getDisplayName();
					} else {
						itemName = Utils.formatItemName(item.getType().name());
					}
					rewards.add(new GameReward(itemName, item));
					p.sendMessage(Messages.get("RewardReceived").replace("<reward_name>", itemName));
				}
			}
			
			// Kiểm tra từng phần thưởng ngẫu nhiên theo đúng tỉ lệ
			if(random.getChoices() > 0) {
				// Tạo danh sách các phần thưởng đã được chọn để tránh trùng lặp
				Set<String> selectedRewards = new HashSet<>();
				
				// Tạo bản sao của danh sách các phần thưởng để xử lý riêng biệt
				// Không cần lặp cố định số lần, ta sẽ kiểm tra mỗi phần thưởng một lần
				List<String> rewardKeys = new ArrayList<>();
				ConfigurationSection rewardSection = room.getConfigurationSection("Reward");
				for(String key : rewardSection.getKeys(false)) {
					int chance = room.getInt("Reward." + key + ".Chance");
					if(chance < 100) {  // Chỉ xét các phần thưởng có tỉ lệ < 100%
						rewardKeys.add(key);
					}
				}
				
				// Duyệt qua từng phần thưởng và kiểm tra tỉ lệ
				for(String key : rewardKeys) {
					if(selectedRewards.contains(key)) continue;
					
					int chance = room.getInt("Reward." + key + ".Chance");
					// Roll ngẫu nhiên và kiểm tra xem có trúng không
					int roll = ThreadLocalRandom.current().nextInt(1, 101);  // 1-100
					
					if(roll <= chance) {  // Nếu roll <= tỉ lệ thì trúng
						selectedRewards.add(key);
						
						// Xử lý phần thưởng đã trúng
						if(room.contains("Reward." + key + ".Command")) {
							String command = room.getString("Reward." + key + ".Command", "");
							hasReward = true;
							rewardCount++;
							
							// Tạo thông báo thân thiện dựa trên lệnh
							String friendlyMessage = createFriendlyMessage(command);
							
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", p.getName()));
							
							rewards.add(new GameReward(friendlyMessage, command));
							p.sendMessage(Messages.get("RewardCommandSuccess").replace("<command>", friendlyMessage));
						} else if(room.contains("Reward." + key + ".Item")) {
							// Lấy item từ config và tạo một bản sao chính xác sử dụng ItemUtil
							ItemStack item = ItemUtil.copyItem(room.getItemStack("Reward." + key + ".Item"));
							hasReward = true;
							rewardCount++;
							
							// Thêm item vào inventory người chơi
							HashMap<Integer, ItemStack> failed = p.getInventory().addItem(item);
							
							// Nếu inventory đầy, thả item xuống đất
							if (!failed.isEmpty()) {
								for (ItemStack dropItem : failed.values()) {
									p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
								}
								p.sendMessage(Messages.get("InventoryFull"));
							}
							
							String itemName = "";
							if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
								itemName = item.getItemMeta().getDisplayName();
							} else {
								itemName = Utils.formatItemName(item.getType().name());
							}
							rewards.add(new GameReward(itemName, item));
							p.sendMessage(Messages.get("RewardReceived").replace("<reward_name>", itemName));
						}
					}
				}
			}
		}
		
		// Xử lý các lệnh thưởng từ config
		List<String> configCommands = FileManager.getFileConfig(Files.CONFIG).getStringList("Settings.RewardCommand");
		if (!configCommands.isEmpty()) {
			for(String cmd : configCommands) {
				hasReward = true;
				rewardCount++;
				
				// Tạo thông báo thân thiện dựa trên lệnh
				String friendlyMessage = createFriendlyMessage(cmd);
				
				String finalCmd = cmd.replace("<player>", p.getName());
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
				
				rewards.add(new GameReward(friendlyMessage, cmd));
				p.sendMessage(Messages.get("RewardCommandSuccess").replace("<command>", friendlyMessage));
			}
		}
		
		// Hiển thị thông báo tổng kết về phần thưởng
		if (hasReward) {
			// Hiển thị title thông báo
			Utils.sendTitle(p, 
				Messages.get("RewardTitle.Title"), 
				Messages.get("RewardTitle.Subtitle").replace("<count>", String.valueOf(rewardCount))
			);
			
			// Thực hiện hiệu ứng khi nhận phần thưởng (tiếng và hiệu ứng)
			Utils.playSound(p, "ENTITY_PLAYER_LEVELUP|LEVEL_UP");
			try {
				p.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, p.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
			} catch (Exception ex) {
				// Xử lý phiên bản cũ hơn
				try {
					p.getWorld().playEffect(p.getLocation(), org.bukkit.Effect.valueOf("HAPPY_VILLAGER"), 1);
				} catch (Exception ignored) {}
			}
		} else {
			// Thông báo nếu không có phần thưởng
			p.sendMessage(Messages.get("NoReward"));
		}
	}
	
	public void starting() {
		this.status = GameStatus.STARTING;
	}
	
	public boolean isFull() {
		return this.players.size() >= this.max_players;
	}
	
	
	public void setMaxPlayer(int max) {
		this.max_players = max;
	}
	
	public void setMaxTime(int time) {
		this.maxtime = time;
	}
	
	public void setSpawn(Location spawn) {
		this.spawn = spawn;
	}
	
	public void spectator(Player p) {
		p.setGameMode(GameMode.SPECTATOR);
	}
	
	
	public boolean isLeader(Player p) {
		if(this.players.size() == 0) return false;
		return this.players.get(0).getName().equals(p.getName());
	}
	
	public Location mobLocation(int index) {
		String mob = "Boss";
		for(int i = 1; i <= maxStage; i++) {
			if(this.current_stage == i) mob = "Mob" + i;
		}
		String keyy = mob.equals("Boss") ? "" : "." + this.getStageKey(this.stage_count, index);
		return (Location) room.get(mob + keyy + ".Location");
	}


	public FileConfiguration getConfig() {
		return this.room;
	}

	public void setConfig(FileConfiguration config) {
		this.room = config;
	}

	public File getConfigFile() {
		return this.configFile;
	}

	public String getType() {
		return this.type;
	}

	/**
	 * Kiểm tra xem phòng có bị khóa không
	 * 
	 * @return true nếu phòng bị khóa
	 */
	public boolean isLocked() {
		return this.locked;
	}
	
	/**
	 * Đặt trạng thái khóa cho phòng
	 * 
	 * @param locked trạng thái khóa
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
		this.room.set("Locked", locked);
		FileManager.saveFileConfig(this.room, this.configFile);
	}
	
	/**
	 * Lấy trạng thái khóa từ file cấu hình
	 * 
	 * @return trạng thái khóa từ file cấu hình
	 */
	public boolean getLockedFromConfig() {
		return this.room.getBoolean("Locked", false);
	}

	public void sendInfo(Map<String, Integer> mobMap) {
		int stage = this.realStage;
		int turn = this.keyToTurn(this.getStageKey(this.stage_count, 0));
		int current = this.realTurn;
		int max = this.totalTurn - (boss.isEmpty() ? 0 : 1);
		String progress = Utils.getProgress(current + (boss.isEmpty() ? 1 : 0), max);
		int percent = (int) (((double) (current + (boss.isEmpty() ? 1 : 0)) / (double) max) * 100d);

		for(String message : FileManager.getFileConfig(Files.FORMAT).getStringList("format")) {
			message = message
					.replace("<stage>", (current == max && !boss.isEmpty() ? "Boss" : String.valueOf(stage)))
					.replace("<turn>", String.valueOf(turn))
					.replace("<progress>", progress)
					.replace("<percent>", String.valueOf(percent))
					.replace("&", "§");
			if(message.toLowerCase().contains("<display>")) {
				for(String displayName : mobMap.keySet()) {
					int amount = mobMap.get(displayName);
					String messageDisplay = message
							.replace("<display>", displayName)
							.replace("<amount>", String.valueOf(amount))
							.replace("&", "§");
					for(Player p : this.players) p.sendMessage(messageDisplay);
				}
				continue;
			}
			for(Player p : this.players) p.sendMessage(message);
		}
	}


	public void nextStageParticle(boolean playing) {
		if(!playing) {
			this.current_stage = 1;
			this.stage_count = -1;
		}

		List<GameMob> nextStage = this.getStage(this.stage_count + 1);
		if(!nextStage.isEmpty()) {
			Set<String> stageKeyList = this.getStageKey(this.stage_count + 1);
			for(String stageKey : stageKeyList) {
				String mob = "Boss";
				for(int i = 1; i <= maxStage; i++) {
					if(room.contains("Mob" + i + "." + stageKey)) {
						mob = "Mob" + i;
						break;
					}
				}
				String keyy = mob.equals("Boss") ? "" : "." + stageKey;
				Location loc = (Location) room.get(mob + keyy + ".Location");
				Utils.spawnParticle(loc, FileManager.getFileConfig(Files.CONFIG).getString("Particle.NextTurn"), this.players.size() == 0 ? null : this.players.get(0));
				for(String childKey : room.getConfigurationSection(mob + keyy).getKeys(false)) {
					if(childKey.equalsIgnoreCase("Location") || childKey.equalsIgnoreCase("Type") || childKey.equalsIgnoreCase("Amount") || childKey.equalsIgnoreCase("Time")) {
						continue;
					}

					Location childLoc = (Location) room.get(mob + keyy + "." + childKey + ".Location");
					Utils.spawnParticle(childLoc, FileManager.getFileConfig(Files.CONFIG).getString("Particle.NextTurn"), this.players.size() == 0 ? null : this.players.get(0));
				}
				break;
			}
		}
	}


	private long lastSpawn = 0;
	private final List<GameSpawnQueue> queueList = new ArrayList<>();


	public void checkSpawnMobs() {
		long mobSpawnDelay = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.MobSpawnDelay", 2) * 1000L;
		long current = System.currentTimeMillis();
		Iterator<GameSpawnQueue> iterator = this.queueList.iterator();
		while(iterator.hasNext()) {
			GameSpawnQueue spawnQueue = iterator.next();
			if(!spawnQueue.canSpawn() || spawnQueue.getSpawnQueue().isEmpty() || spawnQueue.getQueueLoc() == null) {
				iterator.remove();
				continue;
			}

			if(current - this.lastSpawn >= mobSpawnDelay) {
				this.lastSpawn = System.currentTimeMillis() - 50L;
				spawnQueue.decQueue();

				int radius = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.SpawnRadius");
				BukkitAPIHelper mm = AEPhoBan.inst().getBukkitAPIHelper();

				this.spawnMobs(spawnQueue.getSpawnQueue(), 1, spawnQueue.getQueueLoc(), radius, mm);
				break;
			}
		}
	}


	public void addKill(String name, int kill) {
		if(this.totalKill.containsKey(name)) {
			int k = this.totalKill.get(name) + kill;
			this.totalKill.replace(name, k);
		} else this.totalKill.put(name, kill);
	}

	public void addDamage(String name, double damage) {
		if(this.totalDamage.containsKey(name)) {
			double d = this.totalDamage.get(name) + damage;
			this.totalDamage.replace(name, d);
		} else this.totalDamage.put(name, damage);
	}

	public void sendStatistic(Player p) {
		List<Map.Entry<String, Integer>> pointList = new ArrayList<>(this.totalKill.entrySet());
		List<Map.Entry<String, Double>> damageList = new ArrayList<>(this.totalDamage.entrySet());

		pointList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		damageList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		List<String> sent = new ArrayList<>();

		List<String> formatComplete = FileManager.getFileConfig(Files.FORMAT).getStringList("format-complete");
		for(String message : formatComplete) {
			message = message.replace("&", "§");
			if(message.toLowerCase().contains("<statistic>")) {
				int stt = 1;
				for (Map.Entry<String, Integer> entry : pointList) {
					String name = entry.getKey();
					if (!sent.contains(name)) {
						int kill = entry.getValue();
						int damage = this.totalDamage.containsKey(name) ? (int) (double) this.totalDamage.get(name) : 0;

						String statistic = FileManager.getFileConfig(Files.FORMAT).getString("statistic", "")
								.replace("<stt>", String.valueOf(stt))
								.replace("<player>", name)
								.replace("<killed>", String.valueOf(kill))
								.replace("<damage>", String.valueOf(damage))
								.replace("&", "§");
						p.sendMessage(statistic);
						sent.add(name);
						stt++;
					}
				}
				for (Map.Entry<String, Double> entry : damageList) {
					String name = entry.getKey();
					if (!sent.contains(name)) {
						int kill = this.totalKill.getOrDefault(name, 0);
						int damage = (int) (double) entry.getValue();

						String statistic = FileManager.getFileConfig(Files.FORMAT).getString("statistic", "")
								.replace("<stt>", String.valueOf(stt))
								.replace("<player>", name)
								.replace("<killed>", String.valueOf(kill))
								.replace("<damage>", String.valueOf(damage))
								.replace("&", "§");
						p.sendMessage(statistic);
						sent.add(name);
						stt++;
					}
				}
				continue;
			}
			p.sendMessage(message);
		}
	}


	public void glowAllMob() {
		HashMap<Entity, EntityData> entityMap = EntityData.data();
		for(Entity entity : entityMap.keySet()) {
			EntityData entityData = entityMap.get(entity);
			GlowingEntities glowingEntities = new GlowingEntities(AEPhoBan.inst());
			if(entityData.getGame().equals(this)) {
//				entity.setGlowing(true);
				this.getPlayers().forEach(v -> {
					try {
						glowingEntities.setGlowing(entity, v);
					} catch (ReflectiveOperationException e) {
//						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	/**
	 * Tạo thông báo thân thiện từ lệnh
	 * @param command Lệnh gốc
	 * @return Thông báo thân thiện
	 */
	private String createFriendlyMessage(String command) {
		String lowerCommand = command.toLowerCase();
		
		// Xử lý lệnh eco/economy
		if (lowerCommand.startsWith("eco ") || lowerCommand.startsWith("economy ")) {
			String[] parts = command.split(" ");
			if (parts.length >= 4 && (parts[1].equalsIgnoreCase("give") || parts[1].equalsIgnoreCase("add"))) {
				try {
					String amount = parts[3];
					return amount + " tiền";
				} catch (Exception e) {
					// Nếu không phân tích được lệnh, trả về thông báo chung
					return "Phần thưởng tiền";
				}
			}
		}
		
		// Xử lý lệnh exp, mpoint, point
		if (lowerCommand.contains("exp") || lowerCommand.contains("xp")) {
			String[] parts = command.split(" ");
			if (parts.length >= 3) {
				try {
					String amount = parts[parts.length-1];
					return amount + " kinh nghiệm";
				} catch (Exception e) {
					return "Phần thưởng kinh nghiệm";
				}
			}
		}
		
		// Xử lý lệnh give item
		if (lowerCommand.startsWith("give ") || lowerCommand.startsWith("i ")) {
			String[] parts = command.split(" ");
			if (parts.length >= 3) {
				String itemName = parts[2].replace("_", " ");
				return "Vật phẩm " + itemName;
			}
		}
		
		// Trường hợp mặc định, trả về thông báo chung
		return "Phần thưởng đặc biệt";
	}

}
