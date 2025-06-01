package com.hongminh54.phoban.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.hongminh54.phoban.game.Game;
import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;
import com.hongminh54.phoban.utils.ItemUtil;
import com.hongminh54.phoban.utils.Utils;

public class RewardPreviewGui implements Listener {

    private static final HashMap<Player, String> viewers = new HashMap<>();
    
    /**
     * Mở GUI xem trước phần thưởng cho người chơi
     * 
     * @param player Người chơi
     * @param roomName Tên phòng phó bản
     * @return true nếu mở GUI thành công, false nếu phòng không có phần thưởng
     */
    public static boolean open(Player player, String roomName) {
        Game game = Game.getGame(roomName);
        if (game == null) return false;
        
        FileConfiguration roomConfig = game.getConfig();
        if (roomConfig == null) return false;
        
        // Kiểm tra phòng có phần thưởng không
        if (!roomConfig.contains("Reward") || roomConfig.getConfigurationSection("Reward") == null || 
            roomConfig.getConfigurationSection("Reward").getKeys(false).isEmpty()) {
            return false;
        }
        
        FileConfiguration guiConfig = FileManager.getFileConfig(Files.GUI);
        
        // Tạo inventory
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("RewardPreviewGui.Title", "&6&lPhần Thưởng | <prefix>")
                .replace("<prefix>", roomConfig.getString("Prefix", roomName)));
                
        int rows = guiConfig.getInt("RewardPreviewGui.Rows", 6);
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Thêm border bằng glass pane
        ConfigurationSection borderSection = guiConfig.getConfigurationSection("RewardPreviewGui.Border");
        
        String borderMaterial = borderSection != null ? borderSection.getString("ID", "STAINED_GLASS_PANE") : "STAINED_GLASS_PANE";
        byte borderData = (byte) (borderSection != null ? borderSection.getInt("Data", 15) : 15);
        String borderName = borderSection != null ? ChatColor.translateAlternateColorCodes('&', borderSection.getString("Name", "&r")) : "§r";
        
        ItemStack border = new ItemStack(Utils.matchMaterial(borderMaterial), 1, borderData);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(borderName);
        border.setItemMeta(borderMeta);
        
        // Đặt các viền theo cấu hình
        for (int slot : guiConfig.getIntegerList("RewardPreviewGui.TopBorderSlots")) {
            inventory.setItem(slot, border.clone());
        }
        
        for (int slot : guiConfig.getIntegerList("RewardPreviewGui.BottomBorderSlots")) {
            inventory.setItem(slot, border.clone());
        }
        
        for (int slot : guiConfig.getIntegerList("RewardPreviewGui.SideBorderSlots")) {
            inventory.setItem(slot, border.clone());
        }
        
        // Thêm các phần thưởng vào GUI
        ConfigurationSection rewardSection = roomConfig.getConfigurationSection("Reward");
        
        // Tạo danh sách các slot khả dụng cho phần thưởng
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            availableSlots.add(i);
        }
        
        // Nếu có slot đã được chỉ định là viền, loại bỏ khỏi danh sách khả dụng
        for (int slot : guiConfig.getIntegerList("RewardPreviewGui.TopBorderSlots")) {
            availableSlots.remove(Integer.valueOf(slot));
        }
        
        for (int slot : guiConfig.getIntegerList("RewardPreviewGui.SideBorderSlots")) {
            availableSlots.remove(Integer.valueOf(slot));
        }
        
        int slotIndex = 0;
        for (String key : rewardSection.getKeys(false)) {
            // Chỉ hiển thị phần thưởng nếu còn slot khả dụng
            if (slotIndex >= availableSlots.size()) break;
            
            int slot = availableSlots.get(slotIndex++);
            int chance = roomConfig.getInt("Reward." + key + ".Chance", 0);
            
            if (roomConfig.contains("Reward." + key + ".Item")) {
                ItemStack item = ItemUtil.copyItem(roomConfig.getItemStack("Reward." + key + ".Item"));
                ItemMeta meta = item.getItemMeta();
                
                List<String> lore = new ArrayList<>();
                if (meta.hasLore()) {
                    lore.addAll(meta.getLore());
                }
                
                // Thêm thông tin tỉ lệ theo cấu hình
                for (String loreLine : guiConfig.getStringList("RewardPreviewGui.ItemRewardLore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', loreLine).replace("<chance>", String.valueOf(chance)));
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                
                inventory.setItem(slot, item);
            } else if (roomConfig.contains("Reward." + key + ".Command")) {
                String command = roomConfig.getString("Reward." + key + ".Command", "");
                String friendlyName = createFriendlyMessage(command);
                
                ConfigurationSection commandRewardSection = guiConfig.getConfigurationSection("RewardPreviewGui.CommandReward");
                
                String cmdMaterial = commandRewardSection != null ? commandRewardSection.getString("ID", "PAPER") : "PAPER";
                String cmdName = commandRewardSection != null ? 
                    ChatColor.translateAlternateColorCodes('&', commandRewardSection.getString("Name", "&a<friendly_name>"))
                        .replace("<friendly_name>", friendlyName) : "§a" + friendlyName;
                
                ItemStack item = new ItemStack(Utils.matchMaterial(cmdMaterial));
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(cmdName);
                
                List<String> lore = new ArrayList<>();
                if (commandRewardSection != null && commandRewardSection.contains("Lore")) {
                    for (String loreLine : commandRewardSection.getStringList("Lore")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', loreLine).replace("<chance>", String.valueOf(chance)));
                    }
                } else {
                    lore.add("§7Phần thưởng dạng lệnh");
                    lore.add("");
                    lore.add("§7Tỉ lệ: §e" + chance + "%");
                }
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                
                inventory.setItem(slot, item);
            }
        }
        
        // Nút đóng GUI
        ConfigurationSection closeButtonSection = guiConfig.getConfigurationSection("RewardPreviewGui.CloseButton");
        
        String closeMaterial = closeButtonSection != null ? closeButtonSection.getString("ID", "BARRIER") : "BARRIER";
        String closeName = closeButtonSection != null ? 
            ChatColor.translateAlternateColorCodes('&', closeButtonSection.getString("Name", "&c&lĐóng")) : "§c§lĐóng";
        int closeSlot = closeButtonSection != null ? closeButtonSection.getInt("Slot", 49) : 49;
        
        ItemStack closeButton = new ItemStack(Utils.matchMaterial(closeMaterial));
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(closeName);
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(closeSlot, closeButton);
        
        player.openInventory(inventory);
        viewers.put(player, roomName);
        return true;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        if (viewers.containsKey(player)) {
            event.setCancelled(true);
            
            FileConfiguration guiConfig = FileManager.getFileConfig(Files.GUI);
            ConfigurationSection closeButtonSection = guiConfig.getConfigurationSection("RewardPreviewGui.CloseButton");
            int closeSlot = closeButtonSection != null ? closeButtonSection.getInt("Slot", 49) : 49;
            
            if (event.getSlot() == closeSlot) {
                player.closeInventory();
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        viewers.remove(player);
    }
    
    /**
     * Tạo thông báo thân thiện từ lệnh
     * @param command Lệnh gốc
     * @return Thông báo thân thiện
     */
    private static String createFriendlyMessage(String command) {
        String lowerCommand = command.toLowerCase();
        
        // Xử lý lệnh eco/economy
        if (lowerCommand.startsWith("eco ") || lowerCommand.startsWith("economy ")) {
            String[] parts = command.split(" ");
            if (parts.length >= 4 && (parts[1].equalsIgnoreCase("give") || parts[1].equalsIgnoreCase("add"))) {
                try {
                    String amount = parts[3];
                    return amount + " tiền";
                } catch (Exception e) {
                    return "Phần thưởng tiền";
                }
            }
        }
        
        // Xử lý lệnh exp, mpoint, point
        if (lowerCommand.contains("exp") || lowerCommand.contains("xp")) {
            String[] parts = command.split(" ");
            if (parts.length >= 3) {
                try {
                    String amount = parts[parts.length-1];
                    return amount + " kinh nghiệm";
                } catch (Exception e) {
                    return "Phần thưởng kinh nghiệm";
                }
            }
        }
        
        // Xử lý lệnh give item
        if (lowerCommand.startsWith("give ") || lowerCommand.startsWith("i ")) {
            String[] parts = command.split(" ");
            if (parts.length >= 3) {
                String itemName = parts[2].replace("_", " ");
                return "Vật phẩm " + itemName;
            }
        }
        
        // Trường hợp mặc định, trả về thông báo chung
        return "Phần thưởng đặc biệt";
    }
} 