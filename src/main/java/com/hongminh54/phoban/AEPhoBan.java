package com.hongminh54.phoban;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.game.GameListener;
import com.hongminh54.phoban.game.GameStatus;
import com.hongminh54.phoban.game.PlayerData;
import com.hongminh54.phoban.game.listener.mm.MMListener;
import com.hongminh54.phoban.game.listener.sapi.SAPIListener;
import com.hongminh54.phoban.gui.ChooseTypeGui;
import com.hongminh54.phoban.gui.EditorGui;
import com.hongminh54.phoban.gui.PhoBanGui;
import com.hongminh54.phoban.gui.RewardGui;
import com.hongminh54.phoban.gui.RewardPreviewGui;
import com.hongminh54.phoban.gui.TopGui;
import com.hongminh54.phoban.listener.PlayerCommandPreprocessListener;
import com.hongminh54.phoban.listener.PlayerQuitListener;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.mythicmobs.BukkitAPIHelper;
import com.hongminh54.phoban.utils.AEPhoBanTabCompleter;
import com.hongminh54.phoban.utils.ItemUtil;
import com.hongminh54.phoban.utils.Messages;
import com.hongminh54.phoban.utils.PhoBanExpansion;
import com.hongminh54.phoban.utils.Utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public final class AEPhoBan extends JavaPlugin {

    private static AEPhoBan plugin;

    public static AEPhoBan inst() {
        return plugin;
    }

    private static String perm = "";

    private static LuckPerms luckperms;

    @Override
    public void onEnable() {
        disableWarnASW();
        plugin = this;
        Utils.checkVersion();
        FileManager.setup(this);
        this.bukkitAPIHelper = new BukkitAPIHelper();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            Bukkit.getPluginManager().registerEvents(new EditorGui(), this);
            Bukkit.getPluginManager().registerEvents(new RewardGui(), this);
            Bukkit.getPluginManager().registerEvents(new PhoBanGui(null, -1, ""), this);
            Bukkit.getPluginManager().registerEvents(new ChooseTypeGui(null, -1), this);
            Bukkit.getPluginManager().registerEvents(new GameListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerCommandPreprocessListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
            Bukkit.getPluginManager().registerEvents(new TopGui(), this);
            Bukkit.getPluginManager().registerEvents(new RewardPreviewGui(), this);

            if (bukkitAPIHelper.packageName.equals("io.lumine.mythic.bukkit")) {
                Bukkit.getPluginManager().registerEvents(new MMListener(), this);
            }

            if (Bukkit.getPluginManager().isPluginEnabled("SkillAPI") || Bukkit.getPluginManager().isPluginEnabled("ProSkillAPI")) {
                Bukkit.getPluginManager().registerEvents(new SAPIListener(), this);
            }

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new PhoBanExpansion().register();
            }

            if (Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")) {
                perm = "pex";
            } else if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    luckperms = provider.getProvider();
                }
                perm = "lp";
            }

            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                Calendar c = Calendar.getInstance();
                boolean everyDay = FileManager.getFileConfig(Files.CONFIG).getString("Settings.AutoReset").equalsIgnoreCase("day");
                boolean checkHour = !everyDay || c.getTime().getHours() == 0;
                if (checkHour && c.getTime().getMinutes() == 0 && c.getTime().getSeconds() == 0) {
                    int defaultTurn = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.DefaultTurn");

                    List<String> listType = Game.listType();
                    OfflinePlayer[] listPlayer = Bukkit.getOfflinePlayers();
                    Game.giveTurnChangeDay(listPlayer, listType, defaultTurn);
                }
            }, 20, 20);

            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                Game.convertData();
                Game.load();
            }, 20);

            Arrays.asList("AEPhoBan", "PhoBan").forEach(v -> {
                PluginCommand pluginCommand = getCommand(v);
                if (pluginCommand != null) {
                    pluginCommand.setExecutor(this);
                    pluginCommand.setTabCompleter(new AEPhoBanTabCompleter());
                }
            });
            
            // Thông tin plugin
            Bukkit.getConsoleSender().sendMessage("§6§lAE-PhoBan");
            Bukkit.getConsoleSender().sendMessage("§aAuthor: §aTYBZI");
            Bukkit.getConsoleSender().sendMessage("§aSupport Verions: §a1.8 - 1.20.4");
        });
    }

    @Override
    public void onDisable() {
        try {
            for (String key : Game.game().keySet()) {
                Game.game().get(key).forceStop();
            }
        } catch (Exception ignored) {

        }
    }

    public static HashMap<String, String> addRewardsMap = new HashMap<>();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Messages.get("NotPlayer"));
                return true;
            }
            if (Utils.isJail((Player) sender)) return true;

            new ChooseTypeGui((Player) sender, 0);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create": {
                if (!sender.hasPermission("phoban.edit")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                if (args.length == 1) {
                    return true;
                }

                Player p = (Player) sender;

                if (Game.game().containsKey(args[1])) {
                    p.sendMessage(Messages.get("RoomExist"));
                    return true;
                }

                String name = args[1];
                EditorGui.open(p, name);
                File file = new File(getDataFolder(), "room" + File.separator + name + ".yml");
                try {
                    file.createNewFile();
                } catch (Exception ignored) {
                }

                return true;
            }

            case "edit": {
                if (!sender.hasPermission("phoban.edit")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                if (args.length == 1) {
                    return true;
                }

                Player p = (Player) sender;

                if (!Game.listGameWithoutCompleteSetup().contains(args[1])) {
                    p.sendMessage(Messages.get("RoomNotExist"));
                    return true;
                }

                EditorGui.open(p, args[1]);

                return true;
            }

            case "add": {
                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                if (args.length == 1) return true;
                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("all")) {
                        List<String> listType = Game.listType();
                        OfflinePlayer[] listPlayer = Bukkit.getOfflinePlayers();
                        Game.giveTurn(listPlayer, listType, FileManager.getFileConfig(Files.CONFIG).getInt("Settings.DefaultTurn"));
                        sender.sendMessage(Messages.get("GiveTurnAll"));
                    }
                    return true;
                }
                if (args.length == 3) return true;

                String player = args[1];
                String type = args[2];
                int amount = Integer.parseInt(args[3]);

                if (!Bukkit.getOfflinePlayer(player).isOnline()) {
                    sender.sendMessage(Messages.get("NotOnline"));
                    return true;
                }

                Game.giveTurn(Bukkit.getPlayer(player), type, amount);

                sender.sendMessage(Messages.get("GiveTurn").replace("<player>", player).replace("<amount>", amount + ""));
                return true;
            }

            case "reload": {
                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                sender.sendMessage("§aReloading...");
                try {
                    FileManager.setup(this);
                    sender.sendMessage("§aReload complete.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sender.sendMessage("§cReload failed. Check console");
                }
                return true;
            }

            case "start": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                Player p = (Player) sender;
                PlayerData data = PlayerData.data().get(p);

                if (data == null) {
                    return true;
                }

                if (!data.getGame().getPlayers().get(0).getName().equals(p.getName())) {
                    return true;
                }

                if (!data.getGame().getStatus().equals(GameStatus.WAITING)) {
                    return true;
                }

                data.getGame().starting();

                return true;
            }

            case "help": {
                if (sender.hasPermission("phoban.admin") || sender.hasPermission("phoban.edit")) {
                    for (String mess : FileManager.getFileConfig(Files.MESSAGE).getStringList("HelpAdmin")) {
                        sender.sendMessage(mess.replace("&", "§"));
                    }
                } else {
                    for (String mess : FileManager.getFileConfig(Files.MESSAGE).getStringList("HelpMember")) {
                        sender.sendMessage(mess.replace("&", "§"));
                    }
                }
                return true;
            }

            case "leave": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                Player p = (Player) sender;

                if (!PlayerData.data().containsKey(p)) {
                    return true;
                }

                PlayerData.data().get(p).getGame().leave(p, true, true, false);

                return true;
            }

            case "list": {
                if (!sender.hasPermission("phoban.edit")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (String name : Game.listGameWithoutCompleteSetup()) sb.append(name).append(" ");

                sender.sendMessage(Messages.get("ListRoom").replace("<rooms>", sb.toString()));

                return true;
            }

            case "join": {
                if (args.length == 1) return true;
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                Player p = (Player) sender;
                String name = args[1];
                Game game = Game.getGame(name);
                if (game == null) {
                    p.sendMessage(Messages.get("RoomNotExist"));
                    return true;
                }

                if (Utils.isJail(p)) return true;

                if (game.getStatus().equals(GameStatus.WAITING) || game.getStatus().equals(GameStatus.STARTING)) {
                    if (game.isFull()) {
                        p.sendMessage(Messages.get("RoomFull"));
                        return true;
                    }

                    if (!Game.canJoin(game.getConfig())) {
                        p.sendMessage(Messages.get("JoinRoomNotConfig"));
                        return true;
                    }
                    
                    if (game.isLocked() && !p.hasPermission("phoban.admin")) {
                        p.sendMessage(Messages.get("RoomLocked"));
                        return true;
                    }
                    
                    if (game.isLockedByLeader() && !p.hasPermission("phoban.admin")) {
                        p.sendMessage(Messages.get("RoomLocked"));
                        return true;
                    }

                    if (!Game.hasTurn(p, game.getType())) {
                        p.sendMessage(Messages.get("NoTurn"));
                        return true;
                    }

                    game.join(p);

                    if (game.isLeader(p)) {
                        boolean autoStartSingle = FileManager.getFileConfig(Files.CONFIG).getBoolean("Settings.AutoStartSingle", false);
                        if (autoStartSingle) {
                            game.starting();
                        } else {
                            p.sendMessage(Messages.get("LeaderStart"));
                        }
                    }
                    return true;
                }

                if (game.getStatus().equals(GameStatus.PLAYING)) {
                    p.sendMessage(Messages.get("RoomStarted"));
                    return true;
                }

                return true;
            }

            case "setspawn": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                Location loc = ((Player) sender).getLocation();
                Game.setGlobalSpawn(loc);
                sender.sendMessage(Messages.get("GlobalSpawnSet"));
                return true;
            }

            case "setdefaultspawn": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                Location loc = ((Player) sender).getLocation();
                Game.setDefaultSpawn(loc);
                sender.sendMessage(Messages.get("DefaultSpawnSet"));
                return true;
            }

            case "top": {
                if (!(sender instanceof Player)) return true;
                TopGui.open((Player) sender);
                return true;
            }

            case "givepoint": {
                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                if (args.length <= 2) return true;
                String player = args[1];
                int point = Integer.parseInt(args[2]);

                if (!Bukkit.getOfflinePlayer(player).isOnline()) {
                    sender.sendMessage(Messages.get("NotOnline"));
                    return true;
                }

                FileConfiguration data = FileManager.getFileConfig(Files.DATA);
                int curPoint = data.getInt(player + ".Point", 0);
                data.set(player + ".Point", curPoint + point);
                FileManager.saveFileConfig(data, Files.DATA);

                sender.sendMessage(Messages.get("GivePoint").replace("<player>", player).replace("<amount>", point + ""));
                return true;
            }

            case "takepoint": {
                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }

                if (args.length <= 2) return true;
                String player = args[1];
                int point = Integer.parseInt(args[2]);

                if (!Bukkit.getOfflinePlayer(player).isOnline()) {
                    sender.sendMessage(Messages.get("NotOnline"));
                    return true;
                }

                FileConfiguration data = FileManager.getFileConfig(Files.DATA);
                int curPoint = data.getInt(player + ".Point", 0);
                if ((curPoint - point) < 0 && !FileManager.getFileConfig(Files.CONFIG).getBoolean("Point.AllowNegative"))
                    point = curPoint;
                data.set(player + ".Point", curPoint - point);
                FileManager.saveFileConfig(data, Files.DATA);

                sender.sendMessage(Messages.get("TakePoint").replace("<player>", player).replace("<amount>", point + ""));
                return true;
            }

            case "addrewards":
                if (!sender.hasPermission("phoban.edit")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }

                if (args.length < 2) return true;
                Player player = (Player) sender;
                ItemStack itemHand = ItemUtil.copyItem(player.getItemInHand());
                if(itemHand.getType().equals(Material.AIR)){
                    player.sendMessage(Messages.get("Error").replace("<e>", "Không có vật phẩm trên tay."));
                    return true;
                }

                Game game = Game.getGame(args[1]);

                if(game == null) {
                    sender.sendMessage(Messages.get("RoomNotExist"));
                    return true;
                }

                File configFile = new File(AEPhoBan.inst().getDataFolder(), "room" + File.separator + game.getName() + ".yml");
                if (!configFile.exists()) try {
                    configFile.createNewFile();
                } catch (Exception ignored) {
                }
                String id = UUID.randomUUID().toString();
                FileConfiguration room = game.getConfig();

                room.set("Reward." + id + ".Item", itemHand);
                room.set("Reward." + id + ".Chance", 100);
                FileManager.saveFileConfig(room, configFile);
                
                game.setLocked(true);
                player.sendMessage(Messages.get("RoomLockedAfterEdit"));

                RewardGui.editor.put(player, game.getName() + ":editchance:" + id+":2");
                player.sendMessage(Messages.get("EditChance"));
                addRewardsMap.put(player.getName(), game.getName());
                return true;
                
            case "lock":
                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(Messages.get("LockCommandUsage"));
                    return true;
                }
                
                game = Game.getGame(args[1]);
                
                if (game == null) {
                    sender.sendMessage(Messages.get("RoomNotExist"));
                    return true;
                }
                
                if (game.getStatus().equals(GameStatus.PLAYING) || game.getStatus().equals(GameStatus.STARTING)) {
                    List<Player> players = new ArrayList<>(game.getPlayers());
                    
                    for (Player p : players) {
                        p.sendMessage(Messages.get("AdminForceEndRoom"));
                    }
                    
                    game.forceStop();
                    
                    sender.sendMessage(Messages.get("ForceEndRoomSuccess").replace("<room>", game.getName())
                            .replace("<players>", String.valueOf(players.size())));
                }
                
                game.setLocked(true);
                sender.sendMessage(Messages.get("RoomLockedSuccess").replace("<room>", game.getName()));
                return true;
                
            case "unlock":
                if (!sender.hasPermission("phoban.admin")) {
                    sender.sendMessage(Messages.get("NoPermissions"));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(Messages.get("UnlockCommandUsage"));
                    return true;
                }
                
                game = Game.getGame(args[1]);
                
                if (game == null) {
                    sender.sendMessage(Messages.get("RoomNotExist"));
                    return true;
                }
                
                game.setLocked(false);
                sender.sendMessage(Messages.get("RoomUnlocked").replace("<room>", game.getName()));
                return true;
                
            case "kick":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }
                
                Player kickerPlayer = (Player) sender;
                
                // Kiểm tra xem người chơi có trong phó bản không
                if (!PlayerData.data().containsKey(kickerPlayer)) {
                    kickerPlayer.sendMessage(Messages.get("NotInRoom"));
                    return true;
                }
                
                // Lấy thông tin phòng
                Game kickerGame = PlayerData.data().get(kickerPlayer).getGame();
                
                // Kiểm tra xem người chơi có phải là chủ phòng không
                if (!kickerGame.isLeader(kickerPlayer)) {
                    kickerPlayer.sendMessage(Messages.get("KickPlayerNoPermission"));
                    return true;
                }
                
                // Kiểm tra trạng thái phòng
                if (!kickerGame.getStatus().equals(GameStatus.WAITING) && !kickerGame.getStatus().equals(GameStatus.STARTING)) {
                    kickerPlayer.sendMessage(Messages.get("KickPlayerWrongStatus"));
                    return true;
                }
                
                // Kiểm tra tham số
                if (args.length < 2) {
                    kickerPlayer.sendMessage(Messages.get("KickPlayerUsage"));
                    return true;
                }
                
                // Tìm người chơi bị đá
                String targetName = args[1];
                Player targetPlayer = null;
                
                for (Player p : kickerGame.getPlayers()) {
                    if (p.getName().equalsIgnoreCase(targetName)) {
                        targetPlayer = p;
                        break;
                    }
                }
                
                if (targetPlayer == null) {
                    kickerPlayer.sendMessage(Messages.get("KickPlayerNotFound"));
                    return true;
                }
                
                // Đá người chơi
                if (kickerGame.kickPlayer(kickerPlayer, targetPlayer)) {
                    kickerPlayer.sendMessage(Messages.get("KickPlayer").replace("<player>", targetPlayer.getName()));
                }
                
                return true;
                
            case "lockroom":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("NotPlayer"));
                    return true;
                }
                
                Player locker = (Player) sender;
                
                // Kiểm tra xem người chơi có trong phó bản không
                if (!PlayerData.data().containsKey(locker)) {
                    locker.sendMessage(Messages.get("NotInRoom"));
                    return true;
                }
                
                // Lấy thông tin phòng
                Game lockerGame = PlayerData.data().get(locker).getGame();
                
                // Kiểm tra xem người chơi có phải là chủ phòng không
                if (!lockerGame.isLeader(locker)) {
                    locker.sendMessage(Messages.get("LockRoomNoPermission"));
                    return true;
                }
                
                // Kiểm tra trạng thái phòng
                if (!lockerGame.getStatus().equals(GameStatus.WAITING) && !lockerGame.getStatus().equals(GameStatus.STARTING)) {
                    locker.sendMessage(Messages.get("LockRoomWrongStatus"));
                    return true;
                }
                
                // Đảo ngược trạng thái khóa phòng
                boolean newState = !lockerGame.isLockedByLeader();
                lockerGame.setLockedByLeader(newState);
                
                // Thông báo
                if (newState) {
                    locker.sendMessage(Messages.get("LockRoomByLeader"));
                } else {
                    locker.sendMessage(Messages.get("UnlockRoomByLeader"));
                }
                
                return true;
        }
        return false;
    }


    public boolean hasPerm(Player p, String permission) {
        switch (perm.toLowerCase()) {
            case "pex": {
                PermissionUser user = PermissionsEx.getUser(p);
                return user.getPermissions("world").contains(permission);
            }
            case "lp": {
                User user = luckperms.getUserManager().getUser(p.getName());
                return user != null && user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            }
            default:
                return false;
        }
    }


    private BukkitAPIHelper bukkitAPIHelper;

    public BukkitAPIHelper getBukkitAPIHelper() {
        return this.bukkitAPIHelper;
    }


    public void disableWarnASW() {
        File aswf = new File(getDataFolder().getParentFile(), File.separator + "AutoSaveWorld" + File.separator + "config.yml");
        if (aswf.exists()) {
            FileConfiguration asw = new YamlConfiguration();
            try {
                asw.load(aswf);
            } catch (Exception ex) {

            }
            if (asw.getBoolean("networkwatcher.mainthreadnetaccess.warn")) {
                asw.set("networkwatcher.mainthreadnetaccess.warn", false);
                try {
                    asw.save(aswf);
                } catch (Exception ex) {

                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "asw reload");
            }
        }
    }
}
