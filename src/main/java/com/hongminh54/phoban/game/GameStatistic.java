package com.hongminh54.phoban.game;

import org.bukkit.entity.Player;

import java.util.HashMap;

public final class GameStatistic {

    public static HashMap<Player, Long> protect = new HashMap<>();
    public static HashMap<Player, Long> lastDeath = new HashMap<>();

}
