package com.hongminh54.phoban.game;

import lombok.Getter;
import org.bukkit.Location;

public class GameSpawnQueue {

    @Getter private String spawnQueue;
    @Getter private int queue;
    @Getter private Location queueLoc;


    public GameSpawnQueue(String spawnQueue, int queue, Location queueLoc) {
        this.spawnQueue = spawnQueue;
        this.queue = queue;
        this.queueLoc = queueLoc;
    }


    public void decQueue() {
        this.queue -= 1;
    }

    public boolean canSpawn() {
        return this.queue > 0;
    }

}
