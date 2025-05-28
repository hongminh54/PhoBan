package com.hongminh54.phoban.utils;

import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.game.PlayerData;
import com.hongminh54.phoban.gui.PhoBanGui;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class PhoBanExpansion extends PlaceholderExpansion {
	
	@Override
	public String getIdentifier() {
        return "ae-phoban";
    }

	@Override
    public String getAuthor() {
        return "TYBZI";
    }

	@Override
    public String getVersion() {
        return "1.0";
    }
    
    @Override
    public String onRequest(OfflinePlayer p, String identifier) {
    	try {
    		String[] args = identifier.split("_");
    		Player player = p.getPlayer();
    		
    		switch(args[0].toLowerCase()) {
				case "time": {
					if(!PlayerData.data().containsKey(player)) return "Not in game";
					PlayerData data = PlayerData.data().get(player);
					Game game = data.getGame();
					FileConfiguration room = game.getConfig();
					return PhoBanGui.timeFormat(game.getTimeLeft());
				}
				case "prefix": {
					if(!PlayerData.data().containsKey(player)) return "Not in game";
					PlayerData data = PlayerData.data().get(player);
					Game game = data.getGame();
					FileConfiguration room = game.getConfig();
					return room.getString("Prefix", "").replace("&", "§");
				}
				case "maxplayers": {
					if(!PlayerData.data().containsKey(player)) return "Not in game";
					PlayerData data = PlayerData.data().get(player);
					Game game = data.getGame();
					FileConfiguration room = game.getConfig();
					return String.valueOf(room.getInt("Player"));
				}
				case "minplayers": {
					if(!PlayerData.data().containsKey(player)) return "Not in game";
					PlayerData data = PlayerData.data().get(player);
					Game game = data.getGame();
					FileConfiguration room = game.getConfig();
					return String.valueOf(game.getPlayers().size());
				}

				case "point": {
					FileConfiguration data = FileManager.getFileConfig(Files.DATA);
					String target = p.getName();
					if(args.length > 1) {
						StringBuilder sb = new StringBuilder();
						for(int i = 1; i < args.length; i++) {
							sb.append(args[i]);
							if(i < args.length - 1) sb.append("_");
						}
						target = sb.toString();
					}
					return String.valueOf(data.getInt(target + ".Point", 0));
				}
				case "rank": {
					FileConfiguration data = FileManager.getFileConfig(Files.DATA);
					String target = p.getName();
					if(args.length > 1) {
						StringBuilder sb = new StringBuilder();
						for(int i = 1; i < args.length; i++) {
							sb.append(args[i]);
							if(i < args.length - 1) sb.append("_");
						}
						target = sb.toString();
					}

					if(!data.contains(target)) return "0";
					if(data.getInt(target + ".Point", 0) < 1) return "0";

					HashMap<String, Integer> pointData = new HashMap<>();
					for(String name : data.getKeys(false)) {
						int point = data.getInt(name + ".Point", 0);
						if(point > 0) pointData.put(name, point);
					}

					List<Map.Entry<String, Integer>> pointList = new ArrayList<>(pointData.entrySet());

					Collections.sort(pointList, new Comparator<Map.Entry<String, Integer>>() {
						public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
							return o2.getValue().compareTo(o1.getValue());
						}
					});

					int rank = 0;
					for(int i = 0; i < pointList.size(); i++) {
						if(pointList.get(i).getKey().equals(target)) {
							rank = i + 1;
							break;
						}
					}

					return String.valueOf(rank);
				}

				case "top": {
					if(args.length == 1) return "";

					FileConfiguration data = FileManager.getFileConfig(Files.DATA);

					HashMap<String, Integer> pointData = new HashMap<>();
					for(String name : data.getKeys(false)) {
						int point = data.getInt(name + ".Point", 0);
						if(point > 0) pointData.put(name, point);
					}

					int position = Integer.parseInt(args[1]);
					if(position < 1) return "";
					if(position > pointData.size()) return "";

					String format = "";
					if(args.length > 2) format = args[2];

					List<Map.Entry<String, Integer>> pointList = new ArrayList<>(pointData.entrySet());

					Collections.sort(pointList, new Comparator<Map.Entry<String, Integer>>() {
						public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
							return o2.getValue().compareTo(o1.getValue());
						}
					});

					for(int i = 0; i < pointList.size(); i++) {
						if(i + 1 == position) {
							Map.Entry<String, Integer> topPos = pointList.get(i);
							return format.isEmpty() ? topPos.getKey() + ": " + topPos.getValue() : format.replace("&", "§").replace("<pos>", String.valueOf(position)).replace("<player>", topPos.getKey()).replace("<point>", String.valueOf(topPos.getValue()));
						}
					}

					return "";
				}

				case "turn-left": {
					if(!PlayerData.data().containsKey(player)) return "Not in game";
					PlayerData data = PlayerData.data().get(player);
					Game game = data.getGame();
					int realTurn = game.getRealTurn();
					int totalTurn = game.getTotalTurn();
					int turnLeft = 0;
					if(realTurn < totalTurn) turnLeft = totalTurn - realTurn;
					return String.valueOf(turnLeft);
				}

				case "life": {
					if(!PlayerData.data().containsKey(player)) return "Not in game";
					PlayerData data = PlayerData.data().get(player);
					return String.valueOf(data.getRespawn());
				}

				default: {
					String name = args[0];
					Game gameTarget = Game.getGame(name);
					if(gameTarget == null) return "Game not found";
					if(args.length <= 1) return "args.length <= 1";
					FileConfiguration roomTarget = gameTarget.getConfig();
					switch(args[1].toLowerCase()) {
						case "time": {
							return PhoBanGui.timeFormat(gameTarget.getTimeLeft());
						}
						case "prefix": {
							return roomTarget.getString("Prefix", "").replace("&", "§");
						}
						case "maxplayers": {
							return String.valueOf(roomTarget.getInt("Player"));
						}
						case "minplayers": {
							return String.valueOf(gameTarget.getPlayers().size());
						}
						case "status": {
							return FileManager.getFileConfig(Files.GUI).getString("PhoBanGui.StatusFormat." + gameTarget.getStatus().toString()).replace("&", "§");
						}
						case "turn-left": {
							int realTurn = gameTarget.getRealTurn();
							int totalTurn = gameTarget.getTotalTurn();
							int turnLeft = 0;
							if(realTurn < totalTurn) turnLeft = totalTurn - realTurn;
							return String.valueOf(turnLeft);
						}
					}
				}
    		}
    	} catch(Exception ex) {
    		return "ae-phoban placeholder error: " + ex.getMessage() + " | identifier: " + identifier;
    	}
    	return "No placeholder";
    }

}
