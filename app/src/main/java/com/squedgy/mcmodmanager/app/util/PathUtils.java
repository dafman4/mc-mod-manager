package com.squedgy.mcmodmanager.app.util;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.config.Config;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static com.squedgy.mcmodmanager.app.config.Config.minecraftVersion;


public class PathUtils {

	private static String minecraftDirectory;

	public static void setMinecraftDirectory(String dir){
		File f = new File(dir);
		if(f.exists() && f.isDirectory() && allSubDirsMatch(f, "mods", "versions", "resourcepacks")){
			minecraftDirectory = dir;
		}
	}

	public static boolean allSubDirsMatch(File dir, String... subDirs){
		if(dir.exists() && dir.isDirectory()){
			for(String s : subDirs){
				File f = dir.toPath().resolve(s).toFile();
				if(!f.exists() || !f.isDirectory()) return false;
			}
		}

		return true;
	}

	public static String getMinecraftDirectory() { return minecraftDirectory; }

	public static String getModsDir(){ return minecraftDirectory + File.separator + "mods"; }

	public static String getStorageDir() { return minecraftDirectory + File.separator + minecraftVersion; }

	public static String getModLocation(ModVersion v){ return getModsDir() + File.separator + v.getFileName(); }

	public static String getModStorage(ModVersion v) { return getStorageDir() + File.separator + v.getFileName(); }

	public static String findModLocation(ModVersion v) {
		File f = new File(getModLocation(v));
		return f.exists() ? f.getAbsolutePath() : new File(getModStorage(v)).getAbsolutePath();
	}

	public static void ensureMinecraftDirectory(){
		Config utils = ModUtils.getInstance().CONFIG;
		while(minecraftDirectory == null){
			DirectoryChooser fc = new DirectoryChooser();
			File location = fc.showDialog(null);
			if(location.exists() && PathUtils.allSubDirsMatch(location, "mods", "versions", "saves")){
				minecraftDirectory = location.getAbsolutePath();
				utils.setProperty(Config.CUSTOM_DIR, minecraftDirectory);
				utils.writeProps();
			}
		}
	}


	public static URL getResource(String resource) {
		return Thread.currentThread().getContextClassLoader().getResource(resource);
	}

	public static URL getOustideLocalResource(String path) {
		File f = new File(path);
		if (!f.toPath().getParent().toFile().exists()) if (!f.toPath().getParent().toFile().mkdirs()) return null;
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public static URL getHttpResource(String link) {
		try {
			return new URL(link);
		} catch (MalformedURLException e) {
			return null;
		}
	}

}
