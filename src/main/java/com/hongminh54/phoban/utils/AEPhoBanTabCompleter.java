package com.hongminh54.phoban.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;

public class AEPhoBanTabCompleter implements TabCompleter {

    private static final List<String> ADMIN_COMMANDS = Arrays.asList(
            "create", "edit", "add", "reload", "list", "setspawn", "setdefaultspawn", 
            "givepoint", "takepoint", "addrewards",
            "help", "leave", "join", "start", "top",
            "lock", "unlock"
    );
    
    private static final List<String> PLAYER_COMMANDS = Arrays.asList(
            "join", "leave", "start", "help", "top"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Nếu người gửi không phải là người chơi, chỉ trả về lệnh cần quyền admin
        if (!(sender instanceof Player)) {
            if (args.length == 1) {
                return filterCompletions(ADMIN_COMMANDS, args[0]);
            }
            return completions;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            // Lệnh chính
            List<String> commands = new ArrayList<>(PLAYER_COMMANDS);
            if (player.hasPermission("phoban.admin")) {
                commands.addAll(ADMIN_COMMANDS);
            }
            return filterCompletions(commands, args[0]);
        } else if (args.length == 2) {
            // Các lệnh phụ
            switch (args[0].toLowerCase()) {
                case "join":
                    if (args.length == 2) {
                        return filterCompletions(Game.listGame(), args[1]);
                    }
                    break;
                case "edit":
                case "lock":
                case "unlock":
                case "addrewards":
                    if (player.hasPermission("phoban.admin")) {
                        return filterCompletions(Game.listGameWithoutCompleteSetup(), args[1]);
                    }
                    break;
                case "create":
                    if (player.hasPermission("phoban.admin")) {
                        // Gợi ý tên phòng mới - không hiển thị phòng đã tồn tại
                        List<String> suggestions = Arrays.asList("newroom", "pve", "pvp", "easy", "normal", "hard", "boss");
                        List<String> existingRooms = Game.listGameWithoutCompleteSetup();
                        return filterCompletions(
                                suggestions.stream()
                                        .filter(name -> !existingRooms.contains(name))
                                        .collect(Collectors.toList()),
                                args[1]);
                    }
                    break;
                case "add":
                    if (player.hasPermission("phoban.admin")) {
                        List<String> options = new ArrayList<>();
                        options.add("all");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            options.add(p.getName());
                        }
                        return filterCompletions(options, args[1]);
                    }
                    break;
                case "givepoint":
                case "takepoint":
                    if (player.hasPermission("phoban.admin")) {
                        List<String> playerNames = new ArrayList<>();
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            playerNames.add(p.getName());
                        }
                        return filterCompletions(playerNames, args[1]);
                    }
                    break;
            }
        } else if (args.length == 3) {
            // Tham số thứ ba
            if (args[0].toLowerCase().equals("add") && !args[1].equalsIgnoreCase("all") && player.hasPermission("phoban.admin")) {
                return filterCompletions(Game.listType(), args[2]);
            }
        } else if (args.length == 4) {
            // Tham số thứ tư
            if (args[0].toLowerCase().equals("add") && !args[1].equalsIgnoreCase("all") && player.hasPermission("phoban.admin")) {
                // Gợi ý số lượt
                int defaultTurn = FileManager.getFileConfig(Files.CONFIG).getInt("Settings.DefaultTurn");
                return Arrays.asList("1", String.valueOf(defaultTurn), "5", "10");
            } else if ((args[0].toLowerCase().equals("givepoint") || args[0].toLowerCase().equals("takepoint")) && player.hasPermission("phoban.admin")) {
                // Gợi ý điểm thưởng/trừ
                return Arrays.asList("1", "5", "10", "50", "100");
            }
        }
        
        return completions;
    }
    
    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
} 