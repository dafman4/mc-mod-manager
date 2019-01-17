package com.squedgy.mcmodmanager.app;

import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.controllers.MainController;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

import static com.squedgy.mcmodmanager.app.util.PathUtils.allSubDirsMatch;

public class Startup extends Application {

	private static Scene PARENT;
	private static Startup instance;
	private static MainController MAIN_VIEW;

	public Startup() throws IOException {
		if (MAIN_VIEW == null) MAIN_VIEW = MainController.getInstance();
		if (PARENT == null) PARENT = new Scene(MAIN_VIEW.getRoot());
	}

	public static Startup getInstance() throws IOException {
		if (instance == null) instance = new Startup();
		return instance;
	}

	public static void main(String[] args) {
		String os = System.getProperty("os.name");
		ModUtils c = ModUtils.getInstance();

		String mcDir;
		//If custom set, otherwise looking for defaults
		if (c.CONFIG.getProperty(Config.CUSTOM_MC_DIR) != null) mcDir = c.CONFIG.getProperty(Config.CUSTOM_MC_DIR);
		else if (os.matches(".*[Ww]indows.*"))
			mcDir = System.getenv("APPDATA") + File.separator + ".minecraft";
		else if (os.matches(".*[Mm]ac [Oo][Ss].*"))
			mcDir = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft";
		else mcDir = System.getProperty("user.home") + File.separator + ".minecraft";

		File dotMc = new File(mcDir);
		if (!dotMc.exists() || !dotMc.isDirectory() || !allSubDirsMatch(dotMc, "mods", "versions", "resourcepacks")) {
			mcDir = null;
		}
		PathUtils.setMinecraftDirectory(mcDir);

		launch(args);
	}

	public static Scene getParent() {
		return PARENT;
	}

	public MainController getMainView() {
		return MAIN_VIEW;
	}

	@Override
	public void start(Stage stage) throws IOException {
		getInstance();
		stage.setTitle("Minecraft Mod Manager");
		stage.setMinHeight(MAIN_VIEW.getRoot().getMinHeight());
		stage.setMinWidth(MAIN_VIEW.getRoot().getMinWidth());
		stage.setScene(PARENT);
		stage.centerOnScreen();
		stage.show();
	}

}
