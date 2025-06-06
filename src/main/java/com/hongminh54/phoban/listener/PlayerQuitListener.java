package com.hongminh54.phoban.listener;

import com.hongminh54.phoban.game.PlayerData;
import com.hongminh54.phoban.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		if(!PlayerData.data().containsKey(p)) return;
		
		Game game = PlayerData.data().get(p).getGame();
		
		// Nếu người chơi thoát là chủ phòng, mở khóa phòng
		if (game.isLeader(p) && game.isLockedByLeader()) {
			game.setLockedByLeader(false);
		}
		
		game.leave(p, true, true, true);
	}

}
