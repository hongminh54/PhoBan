package com.hongminh54.phoban.game;

import org.bukkit.entity.Entity;

import java.util.HashMap;

public class EntityData {
	
	private static HashMap<Entity, EntityData> data = new HashMap<>();
	
	public static HashMap<Entity, EntityData> data() {
		return data;
	}
	
	
	
	private Entity entity;
	private Game game;
	
	public EntityData(Entity e, Game g) {
		this.entity = e;
		this.game = g;
	}
	
	public Entity getEntity() {
		return this.entity;
	}
	
	public Game getGame() {
		return this.game;
	}

}
