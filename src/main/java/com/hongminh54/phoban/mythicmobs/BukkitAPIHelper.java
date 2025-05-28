package com.hongminh54.phoban.mythicmobs;

import com.hongminh54.phoban.AEPhoBan;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class BukkitAPIHelper {

    private List<String> mmPackageAPI = Arrays.asList(
            "io.lumine.mythic.bukkit", // 5. Free
            "io.lumine.xikage.mythicmobs.api.bukkit", // 4. Free
            "io.lumine.xikage.mythicmobs.api.bukkit"); // 5. Pre

    private Object apiClass;
    public String packageName = "";

    public BukkitAPIHelper() {
        this.scanPackage();
    }

    private void scanPackage() {
        for(String packagee : mmPackageAPI) {
            try {
                String className = packagee + ".BukkitAPIHelper";
                apiClass = Class.forName(className).newInstance();
                this.packageName = packagee;
                AEPhoBan.inst().getLogger().info("MythicMobs API Class: " + className);
                break;
            } catch(Exception ignored) {
            }
        }
    }


    public Entity spawnMythicMob(String key, Location loc) {
        try {
            return (Entity) apiClass.getClass().getMethod("spawnMythicMob", String.class, Location.class).invoke(apiClass, key, loc);
        } catch(InvocationTargetException e) {
            e.getCause().printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMythicMob(Entity entity) {
        try {
            return (boolean) apiClass.getClass().getMethod("isMythicMob", Entity.class).invoke(apiClass, entity);
        } catch(InvocationTargetException e) {
            e.getCause().printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getMythicMobDisplayNameGet(Entity entity) {
        try {
            Object activeInstance = apiClass.getClass().getMethod("getMythicMobInstance", Entity.class).invoke(apiClass, entity);
            Object mmInstance = activeInstance.getClass().getMethod("getType").invoke(activeInstance);
            Object displayNamePlaceholder = mmInstance.getClass().getMethod("getDisplayName").invoke(mmInstance);
            if(displayNamePlaceholder instanceof String) return (String) displayNamePlaceholder;
            Object displayName = displayNamePlaceholder.getClass().getMethod("get").invoke(displayNamePlaceholder);
            return displayName == null ? "" : (String) displayName;
        } catch(InvocationTargetException e) {
            e.getCause().printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getMythicMobDisplayNameGet(String key) {
        try {
            Object mmInstance = apiClass.getClass().getMethod("getMythicMob", String.class).invoke(apiClass, key);
            Object displayNamePlaceholder = mmInstance.getClass().getMethod("getDisplayName").invoke(mmInstance);
            if(displayNamePlaceholder instanceof String) return (String) displayNamePlaceholder;
            Object displayName = displayNamePlaceholder.getClass().getMethod("get").invoke(displayNamePlaceholder);
            return displayName == null ? "" : (String) displayName;
        } catch(InvocationTargetException e) {
            e.getCause().printStackTrace();
        } catch(Exception e) {}
        return key;
    }

    public String getMythicMobInternalName(Entity entity) {
        try {
            Object activeInstance = apiClass.getClass().getMethod("getMythicMobInstance", Entity.class).invoke(apiClass, entity);
            Object mmInstance = activeInstance.getClass().getMethod("getType").invoke(activeInstance);
            Object internalName = mmInstance.getClass().getMethod("getInternalName").invoke(mmInstance);
            return (String) internalName;
        } catch(InvocationTargetException e) {
            e.getCause().printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
