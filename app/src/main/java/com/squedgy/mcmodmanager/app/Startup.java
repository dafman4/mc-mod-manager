package com.squedgy.mcmodmanager.app;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.controllers.TableViewController;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Startup extends Application {

	public static final String CUSTOM_DIR = "mc-dir";
	public static String DOT_MINECRAFT_LOCATION;
	public static String MINECRAFT_VERSION = "1.12.2";
	private static Scene PARENT;
	private static Startup instance;
	private static TableViewController MAIN_VIEW;

	public Startup() throws IOException {
		if (MAIN_VIEW == null) MAIN_VIEW = TableViewController.getInstance();
		if (PARENT == null) PARENT = new Scene(MAIN_VIEW.getRoot());
	}

	public static String getModsDir() {
		return DOT_MINECRAFT_LOCATION + File.separator + "mods";
	}

	public static Startup getInstance() throws IOException {
		if (instance == null) instance = new Startup();
		return instance;
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		System.out.println(System.getProperty("javax.net.ssl.trustStore"));
		File f = new File(System.getProperty("javax.net.ssl.trustStore"));
		System.out.println(f.exists());
		System.out.println(System.getProperty("javax.net.ssl.trustStorePassword"));
		String os = System.getProperty("os.name");
		ModUtils c = ModUtils.getInstance();

		//If custom set, otherwise looking for defaults
		if (c.CONFIG.getProperty(CUSTOM_DIR) != null) DOT_MINECRAFT_LOCATION = c.CONFIG.getProperty(CUSTOM_DIR);
		else if (os.matches(".*[Ww]indows.*"))
			DOT_MINECRAFT_LOCATION = System.getenv("APPDATA") + File.separator + ".minecraft";
		else if (os.matches(".*[Mm]ac [Oo][Ss].*"))
			DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft";
		else DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + File.separator + ".minecraft";

		File dotMc = new File(DOT_MINECRAFT_LOCATION);
		if (!dotMc.exists() || !dotMc.toPath().resolve("mods").toFile().exists()) {
			DOT_MINECRAFT_LOCATION = null;
			while (!chooseMinecraftDirectory()) chooseMinecraftDirectory();
		}


		if (DOT_MINECRAFT_LOCATION != null) launch(args);
		else AppLogger.info("Minecraft Directory not set, shutting down.", Startup.class);
	}

	public static boolean chooseMinecraftDirectory() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}

			@Override
			public String getDescription() {
				return "Directories";
			}
		});
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int result = chooser.showSaveDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = new File(chooser.getSelectedFile().getAbsolutePath());
			if (f.exists() && f.toPath().resolve("mods").toFile().exists()) {
				DOT_MINECRAFT_LOCATION = f.getAbsolutePath();
				Config.getInstance().setProperty(CUSTOM_DIR, DOT_MINECRAFT_LOCATION);
				Config.getInstance().writeProps();
			} else return false;
		}
		return true;
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

	public static Scene getParent() {
		return PARENT;
	}

	public TableViewController getMainView() {
		return MAIN_VIEW;
	}

	@Override
	public void start(Stage stage) throws IOException {

		getInstance();

		stage.setTitle("Minecraft Mod Manager");
		stage.setScene(PARENT);
		stage.show();
		stage.setMinHeight(500);
		stage.setMinWidth(700);
	}

}
