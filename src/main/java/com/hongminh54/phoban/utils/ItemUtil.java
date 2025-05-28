package com.hongminh54.phoban.utils;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Tiện ích để sao chép ItemStack đảm bảo giữ nguyên tất cả thuộc tính
 */
public class ItemUtil {

    /**
     * Sao chép một ItemStack đảm bảo giữ nguyên tất cả thuộc tính
     * Sử dụng kỹ thuật serialize và deserialize để tạo bản sao sâu (deep copy)
     *
     * @param original ItemStack gốc
     * @return Bản sao chính xác của ItemStack
     */
    public static ItemStack copyItem(ItemStack original) {
        if (original == null) {
            return null;
        }
        
        try {
            // Tạo bản sao sâu thông qua serialize/deserialize
            return deserializeItem(serializeItem(original));
        } catch (Exception e) {
            // Nếu có lỗi thì thử phương pháp khác
            return createCopy(original);
        }
    }
    
    /**
     * Sao chép một ItemStack bằng phương pháp sao chép thủ công các thuộc tính
     * 
     * @param original ItemStack gốc
     * @return Bản sao của ItemStack
     */
    private static ItemStack createCopy(ItemStack original) {
        if (original == null) {
            return null;
        }
        
        try {
            // Tạo item mới với cùng loại và số lượng
            ItemStack copy = new ItemStack(original.getType(), original.getAmount(), original.getDurability());
            
            // Sao chép MaterialData nếu có
            if (original.getData() != null) {
                copy.setData(original.getData().clone());
            }
            
            // Sao chép metadata nếu có
            if (original.hasItemMeta()) {
                copy.setItemMeta(original.getItemMeta().clone());
            }
            
            return copy;
        } catch (Exception e) {
            // Nếu vẫn có lỗi thì trả về clone() thông thường
            return original.clone();
        }
    }
    
    /**
     * Chuyển đổi ItemStack thành dạng serialized để lưu trữ
     * 
     * @param item ItemStack cần serialize
     * @return Map chứa dữ liệu serialized
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> serializeItem(ItemStack item) {
        try {
            if (item == null) {
                return null;
            }
            
            // Sử dụng phương thức serialize của ItemStack
            if (item instanceof ConfigurationSerializable) {
                return ((ConfigurationSerializable) item).serialize();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Tạo ItemStack từ dữ liệu serialized
     * 
     * @param serialized Map chứa dữ liệu serialized
     * @return ItemStack được tạo từ dữ liệu
     */
    private static ItemStack deserializeItem(Map<String, Object> serialized) {
        try {
            if (serialized == null) {
                return null;
            }
            
            // Sử dụng phương thức deserialize để tạo lại ItemStack
            return ItemStack.deserialize(serialized);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Kiểm tra xem hai item có giống nhau không (bao gồm cả metadata)
     * 
     * @param item1 Item thứ nhất
     * @param item2 Item thứ hai
     * @return true nếu hai item giống nhau, false nếu khác nhau
     */
    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        
        return item1.isSimilar(item2);
    }
} 