package com.hongminh54.phoban.game.listener.mm;

import com.hongminh54.phoban.AEPhoBan;
import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.game.GameStatistic;
import com.hongminh54.phoban.game.PlayerData;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.mythicmobs.BukkitAPIHelper;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.Utils;
import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MMListener implements Listener {

    private boolean noProtect(Player p) {
        if(!GameStatistic.protect.containsKey(p)) return true;
        long current = System.currentTimeMillis();
        int respawnProtect = FileManager.getFileConfig(FileManager.Files.CONFIG).getInt("Settings.RespawnProtect");
        long deathTime = GameStatistic.protect.get(p);
        return (current - deathTime) / 1000 > (respawnProtect + FileManager.getFileConfig(FileManager.Files.CONFIG).getInt("Settings.Respawn.Countdown", 3));
    }

    private boolean predictDeath(MythicDamageEvent e) {
        Entity entity = e.getTarget().getBukkitEntity();
        if(!(entity instanceof Player)) return false;
        Player p = (Player) entity;
        boolean predict = p.getHealth() <= e.getDamage();
        return predict;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicDamage(MythicDamageEvent e) {
        Entity entity = e.getTarget().getBukkitEntity();
        if(!(entity instanceof Player)) return;
        Player p = (Player) entity;
        if(!noProtect(p)) {
            e.setCancelled(true);
            return;
        }

        if(this.predictDeath(e)) {
            if(!PlayerData.data().containsKey(p)) return;

            if(!GameStatistic.lastDeath.containsKey(p)) GameStatistic.lastDeath.put(p, System.currentTimeMillis());
            else {
                long last = GameStatistic.lastDeath.get(p);
                if(System.currentTimeMillis() - last < 1000) return;
            }
            GameStatistic.lastDeath.replace(p, System.currentTimeMillis());

            if(FileManager.getFileConfig(FileManager.Files.CONFIG).getBoolean("Settings.Respawn.Enable")) {
                PlayerData data = PlayerData.data().get(p);
                if(data == null) return;
                Game game = data.getGame();
                data.setLastDeath(p.getLocation().clone());

                if(!data.canRespawn()) {
                    game.leave(p, false, true, false);
                    p.sendMessage(Messages.get("NoMoreRespawn"));
                    return;
                }

                FileConfiguration dataF = FileManager.getFileConfig(FileManager.Files.DATA);
                int curPoint = dataF.getInt(p.getName() + ".Point", 0);
                int point = Utils.parseInt(FileManager.getFileConfig(FileManager.Files.CONFIG).getString("Point.Death"));
                if((curPoint - point) < 0 && !FileManager.getFileConfig(FileManager.Files.CONFIG).getBoolean("Point.AllowNegative")) point = curPoint;
                dataF.set(p.getName() + ".Point", curPoint - point);
                FileManager.saveFileConfig(dataF, FileManager.Files.DATA);
                p.sendMessage(Messages.get("PointDeath").replace("<point>", String.valueOf(point)));

                e.setCancelled(true);
                data.minusRespawn();
                data.setRespawning(true);
                data.setRespawnCountdown(FileManager.getFileConfig(FileManager.Files.CONFIG).getInt("Settings.Respawn.Countdown", 3));
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(data.getLastDeath());

                if(GameStatistic.protect.containsKey(p)) GameStatistic.protect.remove(p);
                GameStatistic.protect.put(p, System.currentTimeMillis());
            } else {
                e.setCancelled(true);
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                PlayerData.data().get(p).getGame().leave(p, false, true, false);

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void damageCount(MythicDamageEvent e) {
        if(e.getTarget().getBukkitEntity() instanceof LivingEntity && e.getCaster().getEntity().getBukkitEntity() instanceof Player) {
            BukkitAPIHelper mm = AEPhoBan.inst().getBukkitAPIHelper();
            if(!mm.isMythicMob(e.getTarget().getBukkitEntity())) return;

            Player p = (Player) e.getCaster().getEntity().getBukkitEntity();
            if(!PlayerData.data().containsKey(p)) return;

            PlayerData data = PlayerData.data().get(p);
            if(data == null) return;
            Game game = data.getGame();

            double damage = e.getDamage();
            game.addDamage(p.getName(), damage);
        }
    }

}
