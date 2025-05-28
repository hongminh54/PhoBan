package com.hongminh54.phoban.game;

import lombok.Getter;

public class GameMob {

    @Getter private String key;
    @Getter private String type;
    @Getter private int amount;

    public GameMob(String key, String type, int amount) {
        this.key = key;
        this.type = type;
        this.amount = amount;
    }

}
