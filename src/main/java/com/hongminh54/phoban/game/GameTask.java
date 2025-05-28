package com.hongminh54.phoban.game;

import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GameTask implements Runnable {
	
	private Game game;
	private int countdown;
	private int waitingKick = 0;
	
	
	public GameTask(Game g) {
		this.game = g;
		this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StartCountdown");
	}
	
	
	
	@Override
	public void run() {
		if(game.getStatus().equals(GameStatus.WAITING)) {
			this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StartCountdown");
			if(game.isFull()) game.starting();
			if(game.getPlayers().size() > 0) {
				this.waitingKick++;
				if(this.waitingKick >= FileManager.getFileConfig(Files.CONFIG).getInt("Settings.WaitingKick", 600)) {
					game.leaveAllAfterComplete();
					this.waitingKick = 0;
				}
			} else this.waitingKick = 0;
		}
		if(game.getStatus().equals(GameStatus.STARTING)) {
			this.waitingKick = 0;
			if(game.getPlayers().size() > 0) {
				if(Messages.has("StartCountdown." + countdown)) {
					for(Player p : game.getPlayers()) {
						String title = Messages.get("StartCountdown." + countdown + ".Title");
						String subtitle = Messages.get("StartCountdown." + countdown + ".Subtitle");
						Utils.sendTitle(p, title, subtitle);
					}
				}
				this.countdown -= 1;

				game.nextStageParticle(false);
				if(this.countdown <= -1) {
					game.start();
					this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StageCountdown");
				}
			} else {
				game.setStatus(GameStatus.WAITING);
				this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StartCountdown");
			}
		}
		
		if(game.getStatus().equals(GameStatus.PLAYING)) {
			this.waitingKick = 0;
			if(Messages.has("TimeRemaining." + game.getTimeLeft())) {
				for(Player p : game.getPlayers()) {
					p.sendMessage(Messages.get("TimeRemaining." + game.getTimeLeft()));
				}
			}

			int mobEndingGlow = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.MobEndingGlow");
			if(game.getTimeLeft() <= mobEndingGlow) {
				game.glowAllMob();
			}

			for(Player player : game.getPlayers()) {
				PlayerData data = PlayerData.data().get(player);
				if(data == null) continue;
				if(!data.isRespawning()) continue;

				if(data.getRespawnCountdown() <= 0) {
					data.setRespawning(false);
					player.setGameMode(GameMode.SURVIVAL);
					player.sendMessage(Messages.get("Respawn").replace("<amount>", String.valueOf(data.remainRespawn())));
					if(FileManager.getFileConfig(Files.CONFIG).getBoolean("Settings.Checkpoint")) {
						if(data.hasDeath()) player.teleport(data.getLastDeath());
						else player.teleport(game.mobLocation(0));
					} else player.teleport(game.mobLocation(0));
				} else {
					player.setGameMode(GameMode.SPECTATOR);

					String title = Messages.get("Respawning.Title").replace("<time>", String.valueOf(data.getRespawnCountdown()));
					String subtitle = Messages.get("Respawning.Subtitle").replace("<time>", String.valueOf(data.getRespawnCountdown()));
					player.sendTitle(title, subtitle);

					data.respawnCountdown();
				}
			}

			if(!game.quit_countdown) game.time();
			game.nextStageParticle(true);
			game.checkSpawnMobs();
			
			if(game.getTimeLeft() <= 0) {
				for(Player player : game.getPlayers()) {
					player.sendMessage(Messages.get("TimeOut"));
				}
				
				game.forceStop();
				game.restore();
				game.resetTime();
				return;
			}
			
			if(game.getPlayers().size() > 0) {
				if(game.stage_countdown) {
					if(Messages.has("StageCountdown." + countdown)) {
						for(Player p : game.getPlayers()) {
							String title = Messages.get("StageCountdown." + countdown + ".Title");
							String subtitle = Messages.get("StageCountdown." + countdown + ".Subtitle");
							Utils.sendTitle(p, title, subtitle);
						}
					}
					this.countdown -= 1;
					
					if(this.countdown <= -1) {
						game.nextStage();
						int retcode = game.newStage();
						switch(retcode) {
							case 0: {
								this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StageCountdown");
								break;
							}
							case 1: {
								this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.QuitCountdown");
								break;
							}
							case 2: {
								int attempt = 3;
								while(attempt > 0) {
									game.nextStage();
									int retryCode = game.newStage();
									if(retryCode == 0) attempt = 0;
									else attempt -= 1;
								}
							}
						}
					}
				} else if(game.quit_countdown) {
					if(Messages.has("QuitCountdown." + countdown)) {
						for(Player p : game.getPlayers()) {
							p.sendMessage(Messages.get("QuitCountdown." + countdown));
						}
					}
					this.countdown -= 1;
					
					if(this.countdown <= 0) {
						game.leaveAllAfterComplete();
						this.countdown = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.StartCountdown");
					}
				} else {
					if(Messages.has("StageTime." + game.getStageTime())) {
						for(Player p : game.getPlayers()) {
							p.sendMessage(Messages.get("StageTime." + game.getStageTime()));
						}
					}

					if(game.getStageTime() <= 0 && game.getStageTime() != -1) {
						game.clearCurrentStage();
						game.newStage();
					} else game.checkStage();
				}
			} else {
				game.setStatus(GameStatus.WAITING);
				game.restore();
				game.resetTime();
			}
		}
	}


	public void setCountdown(int countdown) {
		this.countdown = countdown;
	}

}
