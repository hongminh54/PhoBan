package com.hongminh54.phoban.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.game.PlayerData;
import com.hongminh54.phoban.gui.RewardGui;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.Messages;

public class PlayerCommandPreprocessListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		String cmd = e.getMessage().replaceFirst("/", "");
		StringBuilder sb = new StringBuilder();
		String[] arr = cmd.split(" ");
		for(int i = 1; i < arr.length; i++) {
			sb.append(arr[i]).append(" ");
		}
		String addRewardName = AEPhoBan.addRewardsMap.getOrDefault(e.getPlayer().getName(), null);
		if(arr.length > 0 && addRewardName != null){
			if(arr[0].equalsIgnoreCase("addRewards")){
				e.setMessage("/aephoban addrewards "+addRewardName);
			} else if(arr[0].equalsIgnoreCase("complete")) {
				RewardGui.open(e.getPlayer(), addRewardName);
				AEPhoBan.addRewardsMap.remove(e.getPlayer().getName());
				e.setCancelled(true);
			}
		}
		for(String c : FileManager.getFileConfig(Files.CONFIG).getStringList("Settings.CommandAliases")) {
			if(arr[0].equalsIgnoreCase(c)) {
				if(sb.toString().isEmpty()) {
					e.setMessage("/aephoban");
				} else {
					e.setMessage("/aephoban " + sb);
				}
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand2(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		if(PlayerData.data().containsKey(p)) {
			String cmd = e.getMessage().replaceFirst("/", "").toLowerCase();
			String[] arr = cmd.split(" ");
			if(arr.length > 1 && arr[1].equalsIgnoreCase("start")) return;
			if(arr.length > 1 && arr[1].equalsIgnoreCase("leave")) return;

			boolean block = true;

			for(String cw : FileManager.getFileConfig(Files.CONFIG).getStringList("Settings.CommandWhitelist")) {
				Pattern pat = Pattern.compile(cw.toLowerCase());
				Matcher mat = pat.matcher(cmd);
				if(mat.matches()) block = false;
			}

			if(block) {
				e.setCancelled(true);
				p.sendMessage(Messages.get("NoCommand"));
			}
		}
	}

}
