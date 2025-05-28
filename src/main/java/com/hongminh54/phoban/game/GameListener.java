package com.hongminh54.phoban.game;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.mythicmobs.BukkitAPIHelper;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class GameListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void entityExplosive(ExplosionPrimeEvent e){
		BukkitAPIHelper mm = AEPhoBan.inst().getBukkitAPIHelper();
		if(!mm.isMythicMob(e.getEntity())) return;

		if(!EntityData.data().containsKey(e.getEntity())) return;
		EntityData data = EntityData.data().get(e.getEntity());

		Game game = data.getGame();

		game.addProgress(mm.getMythicMobInternalName(e.getEntity()), 1);

		String internalName = mm.getMythicMobInternalName(e.getEntity());
		int max = game.getProgressMax(internalName);
		int current = game.getProgressCurrent(internalName);
		String name = mm.getMythicMobDisplayNameGet(e.getEntity());

		for(Player player : game.getPlayers()) {
			player.sendMessage(Messages.get("MobsLeft").replace("<name>", name).replace("<max>", max + "").replace("<current>", current + ""));
		}

		EntityData.data().remove(e.getEntity());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onListener(EntityDeathEvent e) {
		BukkitAPIHelper mm = AEPhoBan.inst().getBukkitAPIHelper();
		
		if(!mm.isMythicMob(e.getEntity())) return;
		
		if(!EntityData.data().containsKey(e.getEntity())) return;
		EntityData data = EntityData.data().get(e.getEntity());
		
		Game game = data.getGame();
		
		game.addProgress(mm.getMythicMobInternalName(e.getEntity()), 1);

		String internalName = mm.getMythicMobInternalName(e.getEntity());
		int max = game.getProgressMax(internalName);
		int current = game.getProgressCurrent(internalName);
		String name = mm.getMythicMobDisplayNameGet(e.getEntity());

		Player killer = e.getEntity().getKiller();
		if(killer != null) {
			game.addKill(killer.getName(), 1);
		}
		
		for(Player player : game.getPlayers()) {
			player.sendMessage(Messages.get("MobsLeft").replace("<name>", name).replace("<max>", max + "").replace("<current>", current + ""));
		}
		
		EntityData.data().remove(e.getEntity());
	}


	private boolean noProtect(Player p) {
		if(!GameStatistic.protect.containsKey(p)) return true;
		long current = System.currentTimeMillis();
		int respawnProtect = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.RespawnProtect");
		long deathTime = GameStatistic.protect.get(p);
		return (current - deathTime) / 1000 > (respawnProtect + FileManager.getFileConfig(Files.CONFIG).getInt("Settings.Respawn.Countdown", 3));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if(!(e.getEntity() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		if(!noProtect(p)) {
			e.setCancelled(true);
			return;
		}

		if(this.predictDeath(e)) {
			if(!PlayerData.data().containsKey(p)) return;

			if(!GameStatistic.lastDeath.containsKey(p)) GameStatistic.lastDeath.put(p, System.currentTimeMillis());
			else {
				long last = GameStatistic.lastDeath.get(p);
				if(System.currentTimeMillis() - last < 1000) return;
			}
			GameStatistic.lastDeath.replace(p, System.currentTimeMillis());

			if(FileManager.getFileConfig(Files.CONFIG).getBoolean("Settings.Respawn.Enable")) {
				PlayerData data = PlayerData.data().get(p);
				if(data == null) return;
				Game game = data.getGame();
				data.setLastDeath(p.getLocation().clone());

				if(!data.canRespawn()) {
					e.setCancelled(true);
					p.setHealth(p.getMaxHealth());
					game.leave(p, false, true, false);
					p.sendMessage(Messages.get("NoMoreRespawn"));
					return;
				}

				FileConfiguration dataF = FileManager.getFileConfig(Files.DATA);
				int curPoint = dataF.getInt(p.getName() + ".Point", 0);
				int point = Utils.parseInt(FileManager.getFileConfig(Files.CONFIG).getString("Point.Death"));
				if((curPoint - point) < 0 && !FileManager.getFileConfig(Files.CONFIG).getBoolean("Point.AllowNegative")) point = curPoint;
				dataF.set(p.getName() + ".Point", curPoint - point);
				FileManager.saveFileConfig(dataF, Files.DATA);
				p.sendMessage(Messages.get("PointDeath").replace("<point>", String.valueOf(point)));

				e.setCancelled(true);
				data.minusRespawn();
				data.setRespawning(true);
				data.setRespawnCountdown(FileManager.getFileConfig(Files.CONFIG).getInt("Settings.Respawn.Countdown", 3));
				p.setHealth(p.getMaxHealth());
				p.setFoodLevel(20);
				p.setGameMode(GameMode.SPECTATOR);
				p.teleport(data.getLastDeath());

				if(GameStatistic.protect.containsKey(p)) GameStatistic.protect.remove(p);
				GameStatistic.protect.put(p, System.currentTimeMillis());
			} else {
				e.setCancelled(true);
				p.setHealth(p.getMaxHealth());
				p.setFoodLevel(20);
				PlayerData.data().get(p).getGame().leave(p, false, true, false);

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
			}
		}
	}


	private boolean predictDeath(EntityDamageByEntityEvent e) {
		if(!(e.getEntity() instanceof Player)) return false;
		Player p = (Player) e.getEntity();
		boolean predict = p.getHealth() <= e.getFinalDamage();
		if(predict) AEPhoBan.inst().getLogger().info("Vanilla predict death player " + p.getName() + ": health " + p.getHealth() + " | final dmg: " + e.getFinalDamage());
		return predict;
	}


	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void damageCount(EntityDamageByEntityEvent e) {
		if(e.getEntity() instanceof LivingEntity && e.getDamager() instanceof Player) {
			BukkitAPIHelper mm = AEPhoBan.inst().getBukkitAPIHelper();
			if(!mm.isMythicMob(e.getEntity())) return;

			Player p = (Player) e.getDamager();
			if(!PlayerData.data().containsKey(p)) return;

			PlayerData data = PlayerData.data().get(p);
			if(data == null) return;
			Game game = data.getGame();

			double damage = e.getFinalDamage();
			game.addDamage(p.getName(), damage);
		}
	}

}
