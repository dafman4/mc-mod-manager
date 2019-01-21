package com.squedgy.mcmodmanager.app.util;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.config.Config;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class PathUtils {

	private static String minecraftDirectory;

	public static boolean allSubDirsMatch(File dir, String... subDirs) {
		if (dir.exists() && dir.isDirectory()) {
			for (String s : subDirs) {
				File f = dir.toPath().resolve(s).toFile();
				if (!f.exists() || !f.isDirectory()) return false;
			}
		}

		return true;
	}

	public static String getMinecraftDirectory() {
		return minecraftDirectory;
	}

	public static void setMinecraftDirectory(String dir) {
		if(dir != null) {
			File f = new File(dir);
			if (f.exists() && f.isDirectory() && allSubDirsMatch(f, "mods", "versions", "resourcepacks")) {
				minecraftDirectory = dir;
			}
		}
	}

	public static String getModsDir() {
		return minecraftDirectory + File.separator + "mods";
	}

	public static String getStorageDir() {
		return minecraftDirectory + File.separator + Config.getMinecraftVersion();
	}

	public static String getModLocation(ModVersion v) {
		return getModsDir() + File.separator + v.getFileName();
	}

	public static String getModStorage(ModVersion v) {
		return getStorageDir() + File.separator + v.getFileName();
	}

	public static String findModLocation(ModVersion v) {
		File f = new File(getModLocation(v));
		if(!f.exists()) f = new File(getModStorage(v));
		if(!f.exists()) f = new File(getModsDir() + File.separator + v.getFileName().replace(' ', '+'));
		if(!f.exists()) f = new File(getStorageDir() + File.separator + v.getFileName().replace(' ', '+'));
		return f.getAbsolutePath();
	}

	public static void ensureMinecraftDirectory() {
		Config utils = ModUtils.getInstance().CONFIG;
		while (minecraftDirectory == null) {
			DirectoryChooser fc = new DirectoryChooser();
			File location = fc.showDialog(null);
			if(location == null){
				System.exit(0);
			}
			if (location.exists() && PathUtils.allSubDirsMatch(location, "mods", "versions", "saves")) {
				minecraftDirectory = location.getAbsolutePath();
				utils.setProperty(Config.CUSTOM_MC_DIR, minecraftDirectory);
				utils.writeProps();
			}
		}
	}

	public static File getProjectDir(){
		try {
			return new File(URLDecoder.decode(PathUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8")).toPath().getParent().toFile();
		} catch (UnsupportedEncodingException e) {
			AppLogger.error(e.getMessage(), PathUtils.class);
			return null;
		}
	}

	public static Path getPathFromProjectDir(String path){
		File base = getProjectDir();
		if(base != null){
			return base.toPath().resolve(path);
		}
		return Paths.get(path);
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

	public static Set<String> getPossibleMinecraftVersions(){
		Set<String> returns = new HashSet<>();
		File[] versions = new File(getMinecraftDirectory() + File.separator + "versions").listFiles();
		if(versions != null) {
			for (File f : versions) {
				System.out.println(f);
				System.out.println(f.getName());
				System.out.println(f.getName().matches("[0-9.]+-[Ff]orge.*"));
				System.out.println();
				if(f.getName().matches("[0-9.]+-[Ff]orge.*")) returns.add(f.getName().substring(0, f.getName().indexOf('-')));
			}
		}
		return returns;
	}

}
