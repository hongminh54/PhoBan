package com.hongminh54.phoban.utils;

import com.hongminh54.phoban.manager.FileManager;
import com.hongminh54.phoban.manager.FileManager.Files;

public class Messages {
	
	public static String get(String message) {
		return FileManager.getFileConfig(Files.MESSAGE)
				.getString(message, message).replace("&", "§");
	}
	
	public static boolean has(String message) {
		return FileManager.getFileConfig(Files.MESSAGE).contains(message);
	}

}
