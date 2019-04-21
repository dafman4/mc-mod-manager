package com.squedgy.mcmodmanager.app.config;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.cache.JsonFileFormat;
import com.squedgy.mcmodmanager.api.cache.JsonModVersionDeserializer;
import com.squedgy.mcmodmanager.app.App;
import com.squedgy.mcmodmanager.app.components.DisplayVersion;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;
import static org.slf4j.LoggerFactory.getLogger;

public class Config {

	private static final Logger log = getLogger(Config.class);
	public static final Path CACHE_DIRECTORY = PathUtils.PROJECT_HOME.resolve("cache");
	public static final String CUSTOM_MC_DIR = "mc-dir";
	private static final Path CONFIG_DIRECTORY = PathUtils.PROJECT_HOME.resolve("config");
	private static final Path CONFIG_FILE_PATH = CONFIG_DIRECTORY.resolve("manager.json");
	private static final String MINECRAFT_VERSION_KEY = "minecraft-version";
	private static final JsonFileFormat format = new JsonFileFormat();
	private static final FileReader<Map<String, String>> READER = new FileReader<>(format);
	private static final FileWriter<Map<String, String>> WRITER = new FileWriter<>(CONFIG_FILE_PATH.toFile().getAbsolutePath(), format, false);

	private static String minecraftVersion;

	private static Map<String, String> CONFIG;
	private static Config instance;
	private Cacher<ModVersion> cachedMods;

	private Config() {
		try {
			CONFIG = readProps();
		} catch (IOException e) {
			log.error("", e);
		}
		log.info("Config: " + CONFIG);
	}

	private void displaySetWindow(){
		Platform.runLater(() -> {
			Stage s = new Stage();
			Label l = new Label("Choose a Minecraft Version: ");
			ChoiceBox<String> choices = new ChoiceBox<>(FXCollections.observableArrayList(PathUtils.getPossibleMinecraftVersions()));
			HBox start = new HBox(l,choices);
			start.setAlignment(Pos.CENTER);
			start.setPadding(new Insets(5,0,5,0));
			Button b = new Button("Set");
			HBox button = new HBox(b);
			button.setPadding(new Insets(5,0,5,0));
			button.setAlignment(Pos.CENTER_RIGHT);
			Scene sc = new Scene(new VBox(start, button));
			b.onMouseReleasedProperty().setValue( e ->{
				setMinecraftVersion(choices.getValue());
				s.close();
			});
			sc.getStylesheets().setAll(getResource("main.css").toString());
			s.setScene(sc);
			s.minHeightProperty().bind(new SimpleDoubleProperty(105));
			s.minWidthProperty().bind(new SimpleDoubleProperty(300));
			s.maxWidthProperty().bind(new SimpleDoubleProperty(300));
			s.maxHeightProperty().bind(new SimpleDoubleProperty(105));
			s.initOwner(App.getParent().getWindow());
			s.initModality(Modality.WINDOW_MODAL);
			s.setAlwaysOnTop(true);
			s.showAndWait();
		});
	}

	private void decideMinecraftVersion() {
		if(CONFIG.containsKey(MINECRAFT_VERSION_KEY)) minecraftVersion = CONFIG.get(MINECRAFT_VERSION_KEY);
		else if (minecraftVersion == null){
			displaySetWindow();
			while(minecraftVersion == null) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		}
		else CONFIG.put(MINECRAFT_VERSION_KEY, minecraftVersion);
		writeProps();
		setCacher(minecraftVersion);
	}

	public static String getMinecraftVersion() {
		if(minecraftVersion == null && instance != null) instance.decideMinecraftVersion();
		return minecraftVersion;
	}

	public static void setMinecraftVersion(String minecraftVersion) {
		Config.minecraftVersion = minecraftVersion;
		CONFIG.put(MINECRAFT_VERSION_KEY, minecraftVersion);
		Config.getInstance().setCacher(minecraftVersion);
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

	public Map<String, String> readProps() throws IOException {
		log.info(String.format("Config file location %s", CONFIG_FILE_PATH.toFile().getAbsolutePath()), getClass());
		if(!CONFIG_FILE_PATH.toFile().exists()){
			try {
				Files.createDirectories(CONFIG_FILE_PATH.getParent());
				Files.createFile(CONFIG_FILE_PATH);
				Files.write(CONFIG_FILE_PATH, "{}".getBytes(Charset.forName("UTF-8")));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return readProps(CONFIG_FILE_PATH.toAbsolutePath().toString());
	}

	public Map<String, String> readProps(String file) throws IOException {
		return READER.read(file);
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
			.forEach(e -> CONFIG.put(tableName + "." + e.getKey(), e.getValue()));
		//Add CONFIG so it's all nice and dandy
		CONFIG.putAll(props);
		writeProps();
	}

	public void setCacher(String mcVersion) {
		cachedMods = Cacher.reading(CACHE_DIRECTORY.resolve(minecraftVersion + ".json").toFile().getAbsolutePath(), new JsonModVersionDeserializer());
	}

}
