package com.squedgy.mcmodmanager.app;

import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import com.squedgy.mcmodmanager.app.util.PathUtils;

import java.io.File;

import static com.squedgy.mcmodmanager.app.util.PathUtils.allSubDirsMatch;

public class Startup {

	public static void main(String[] args) {
		System.setProperty("sun.java2d.opengl", "true");
		String os = System.getProperty("os.name");
		ModUtils c = ModUtils.getInstance();

		String mcDir;
		//If custom set, otherwise looking for defaults
		if (c.CONFIG.getProperty(Config.CUSTOM_MC_DIR) != null) {
			mcDir = c.CONFIG.getProperty(Config.CUSTOM_MC_DIR);
		}
		else if (os.matches(".*[Ww]indows.*")) {
			mcDir = System.getenv("APPDATA") + File.separator + ".minecraft";
		}
		else if (os.matches(".*[Mm]ac [Oo][Ss].*")) {
			mcDir = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft";
		}
		else mcDir = System.getProperty("user.home") + File.separator + ".minecraft";

		File dotMc = new File(mcDir);
		if (!dotMc.exists() || !dotMc.isDirectory() || !allSubDirsMatch(dotMc, "mods", "versions", "resourcepacks")) {
			mcDir = null;
		}
		PathUtils.setMinecraftDirectory(mcDir);

		if(!PathUtils.PROJECT_HOME.toFile().exists()){

		}

		App.launch(args);
	}

}
