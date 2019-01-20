package com.squedgy.mcmodmanager.app.config;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.cache.JsonFileFormat;
import com.squedgy.mcmodmanager.api.cache.JsonModVersionDeserializer;
import com.squedgy.mcmodmanager.app.components.DisplayVersion;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;
import javafx.scene.control.TableColumn;

import java.applet.Applet;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Config {

	public static final Path CACHE_DIRECTORY = PathUtils.getPathFromProjectDir("cache" + File.separator);
	public static final String CUSTOM_MC_DIR = "mc-dir";
	private static final Path CONFIG_DIRECTORY = PathUtils.getPathFromProjectDir("config" + File.separator);
	private static final Path CONFIG_FILE_PATH = CONFIG_DIRECTORY.resolve("manager.json");
	private static final String MINECRAFT_VERSION_KEY = "minecraft-version";
	private static final JsonFileFormat format = new JsonFileFormat();
	private static final FileReader<Map<String, String>> READER = new FileReader<>(CONFIG_FILE_PATH.toFile().getAbsolutePath(), format);
	private static final FileWriter<Map<String, String>> WRITER = new FileWriter<>(CONFIG_FILE_PATH.toFile().getAbsolutePath(), format, false);

	public static String minecraftVersion = "1.12.2";

	private static Map<String, String> CONFIG;
	private static Config instance;
	private Cacher<ModVersion> cachedMods;

	private Config() {
		CONFIG = readProps();
		AppLogger.debug(String.format("CONFIG %s %s", (CONFIG.containsKey(MINECRAFT_VERSION_KEY) ? "contains": "doesn't contain"), MINECRAFT_VERSION_KEY), getClass());
		if(CONFIG.containsKey(MINECRAFT_VERSION_KEY)) minecraftVersion = CONFIG.get(MINECRAFT_VERSION_KEY);
		else CONFIG.put(MINECRAFT_VERSION_KEY, minecraftVersion);
		AppLogger.debug(CONFIG.get(MINECRAFT_VERSION_KEY), getClass());
		setCacher(minecraftVersion);
	}

	public static Config getInstance() {
		if (instance == null) instance = new Config();
		return instance;
	}

	public Cacher<ModVersion> getCachedMods() {
		return cachedMods;
	}

	public String getProperty(String key) {
		return CONFIG.get(key);
	}

	public String setProperty(String key, String prop) {
		return CONFIG.put(key, prop);
	}

	public void deleteProperty(String key) {
		CONFIG.remove(key);
	}

	public Map<String, String> readProps() {
		AppLogger.info(String.format("Config file location %s", CONFIG_FILE_PATH.toFile().getAbsolutePath()), getClass());
		return readProps(CONFIG_FILE_PATH.toFile().getAbsolutePath());
	}

	public Map<String, String> readProps(String file) {
		READER.setFileLocation(file);
		return READER.read();
	}

	public void writeProps() {
		writeProps(CONFIG_FILE_PATH.toFile().getAbsolutePath(), CONFIG);
	}

	public <T> void writeProps(Map<String, T> config) {
		//Props = CONFIG
		Map<String, String> props = new HashMap<>(CONFIG);
		//Add all the new props (therefore overriding existing if necessary)
		props.putAll(
			config.entrySet()
				.stream()
				.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
		);
		writeProps(CONFIG_FILE_PATH.toFile().getAbsolutePath(), props);
	}

	public void writeProps(String file, Map<String, String> props) {
		try {
			WRITER.setFileLocation(file);
			WRITER.write(props);
		} catch (Exception e) {
			throw new RuntimeException("The file " + file + " couldn't be written to, check logs for more information.", e);
		}
	}


	public int compareColumns(String tableName, String a, String b) {
		Integer one = null, two = null;
		try {
			one = Integer.valueOf(getProperty(tableName + "." + a));
		} catch (NumberFormatException ignore) {
		}
		try {
			two = Integer.valueOf(getProperty(tableName + "." + b));
		} catch (NumberFormatException ignore) {
		}
		if (one == null && two == null) return 0;
		else if (one == null) return -1;
		else if (two == null) return 1;

		return one.compareTo(two);
	}

	public void writeColumnOrder(String tableName, List<TableColumn<DisplayVersion, ?>> order) {
		Map<String, String> props = new HashMap<>();
		for (int i = 0; i < order.size(); i++) props.put(order.get(i).getText(), String.valueOf(i));
		//Rewrite the columns keys to table.{column_name} so it's within an inner object
		props.entrySet()
			.stream()
			.map(e -> new AbstractMap.SimpleEntry<>(tableName + "." + e.getKey(), e.getValue()))
			.forEach(e -> CONFIG.put(e.getKey(), e.getValue()));
		//Add CONFIG so it's all nice and dandy
		CONFIG.putAll(props);
		writeProps();
	}

	public void setCacher(String mcVersion) {
		cachedMods = Cacher.reading(CACHE_DIRECTORY.resolve(minecraftVersion + ".json").toFile().getAbsolutePath(), new JsonModVersionDeserializer());
	}

}
