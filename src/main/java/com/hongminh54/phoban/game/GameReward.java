package com.hongminh54.phoban.game;

import org.bukkit.inventory.ItemStack;

/**
 * Class lưu thông tin về phần thưởng nhận được trong phó bản
 */
public class GameReward {
    
    /**
     * Các loại phần thưởng có thể nhận được
     */
    public enum RewardType {
        ITEM,       // Vật phẩm
        COMMAND     // Lệnh
    }
    
    private final RewardType type;  // Loại phần thưởng
    private final String name;      // Tên phần thưởng
    private final Object data;      // Dữ liệu phần thưởng (ItemStack hoặc String command)
    
    /**
     * Tạo phần thưởng từ vật phẩm
     * 
     * @param name Tên hiển thị của phần thưởng
     * @param item Vật phẩm
     */
    public GameReward(String name, ItemStack item) {
        this.type = RewardType.ITEM;
        this.name = name;
        this.data = item;
    }
    
    /**
     * Tạo phần thưởng từ lệnh
     * 
     * @param name Tên hiển thị của phần thưởng
     * @param command Lệnh thực thi
     */
    public GameReward(String name, String command) {
        this.type = RewardType.COMMAND;
        this.name = name;
        this.data = command;
    }
    
    /**
     * Lấy loại phần thưởng
     * 
     * @return Loại phần thưởng
     */
    public RewardType getType() {
        return this.type;
    }
    
    /**
     * Lấy tên hiển thị của phần thưởng
     * 
     * @return Tên phần thưởng
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Lấy vật phẩm phần thưởng (nếu loại là ITEM)
     * 
     * @return Vật phẩm hoặc null nếu không phải loại ITEM
     */
    public ItemStack getItem() {
        return (this.type == RewardType.ITEM) ? (ItemStack) this.data : null;
    }
    
    /**
     * Lấy lệnh phần thưởng (nếu loại là COMMAND)
     * 
     * @return Lệnh hoặc null nếu không phải loại COMMAND
     */
    public String getCommand() {
        return (this.type == RewardType.COMMAND) ? (String) this.data : null;
    }
    
    /**
     * Lấy dữ liệu phần thưởng dưới dạng Object
     * 
     * @return Dữ liệu phần thưởng
     */
    public Object getData() {
        return this.data;
    }
} 