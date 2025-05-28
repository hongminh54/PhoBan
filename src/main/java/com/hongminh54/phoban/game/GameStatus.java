package com.hongminh54.phoban.game;

public enum GameStatus {
	
	WAITING("WAITING"),
	STARTING("STARTING"),
	PLAYING("PLAYING"),
	
	;
	
	private String key;
	
	GameStatus(String k) {
		this.key = k;
	}
	
	public String toString() {
		return this.key;
	}

}
