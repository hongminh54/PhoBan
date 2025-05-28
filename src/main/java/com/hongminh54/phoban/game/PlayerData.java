package com.hongminh54.phoban.game;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.hongminh54.phoban.utils.Utils;

import lombok.Getter;

public class PlayerData {
	
	private static HashMap<Player, PlayerData> data = new HashMap<>();
	
	public static HashMap<Player, PlayerData> data() {
		return data;
	}
	
	
	
	private Player player;
	private Location location;
	private Game game;

	@Getter private int respawn;
	private Location lastDeathLocation;
	private boolean respawning = false;
	private int respawnCountdown = 0;
	
	public PlayerData(Player p, Game g, Location loc) {
		this.player = p;
		this.location = loc;
		this.game = g;
		this.respawn = Utils.getRespawnTurn(p);
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	public Game getGame() {
		return this.game;
	}
	
	public Location getLocation() {
		return this.location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}


	public boolean canRespawn() {
		return this.respawn > 0;
	}

	public void minusRespawn() {
		this.respawn -= 1;
	}

	public int remainRespawn() {
		return this.respawn;
	}


	public boolean hasDeath() {
		return this.lastDeathLocation != null;
	}

	public Location getLastDeath() {
		return this.lastDeathLocation;
	}

	public void setLastDeath(Location loc) {
		this.lastDeathLocation = loc;
	}


	public boolean isRespawning() {
		return this.respawning;
	}

	public void setRespawning(boolean respawning) {
		this.respawning = respawning;
	}


	public void setRespawnCountdown(int count) {
		this.respawnCountdown = count;
	}

	public int getRespawnCountdown() {
		return this.respawnCountdown;
	}

	public void respawnCountdown() {
		this.respawnCountdown -= 1;
	}

}
