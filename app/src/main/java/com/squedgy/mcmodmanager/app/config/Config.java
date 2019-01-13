package com.squedgy.mcmodmanager.app.config;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.cache.JsonFileFormat;
import com.squedgy.mcmodmanager.api.cache.JsonModVersionDeserializer;
import com.squedgy.mcmodmanager.app.components.DisplayVersion;
import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;
import javafx.scene.control.TableColumn;

import java.io.File;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Config {

	public static final String CACHE_DIRECTORY = "cache" + File.separator;
	public static final String CUSTOM_DIR = "mc-dir";
	private static final String CONFIG_DIRECTORY = "config" + File.separator;
	private static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + "manager.json";
	private static final JsonFileFormat format = new JsonFileFormat();
	private static final FileReader<Map<String, String>> READER = new FileReader<>(CONFIG_FILE_PATH, format);
	private static final FileWriter<Map<String, String>> WRITER = new FileWriter<>(CONFIG_FILE_PATH, format, false);

	public static String minecraftVersion = "1.12.2";

	private static Map<String, String> CONFIG;
	private static Config instance;
	private Cacher<ModVersion> cachedMods;

	private Config() {
		CONFIG = readProps();
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
		return readProps(CONFIG_FILE_PATH);
	}

	public Map<String, String> readProps(String file) {
		READER.setFileLocation(file);
		return READER.read();
	}

	public void writeProps() {
		writeProps(CONFIG_FILE_PATH, CONFIG);
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
		writeProps(CONFIG_FILE_PATH, props);
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
		cachedMods = Cacher.reading(CACHE_DIRECTORY + minecraftVersion + ".json", new JsonModVersionDeserializer());
	}

}
