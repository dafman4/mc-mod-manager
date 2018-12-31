package com.squedgy.mcmodmanager.app.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static com.squedgy.mcmodmanager.app.Startup.DOT_MINECRAFT_LOCATION;
import static com.squedgy.mcmodmanager.app.Startup.MINECRAFT_VERSION;

public class ModUtils {


	private static final Map<String, String> badJars = new HashMap<>();
	private static final Map<String, ModVersion> mods = new HashMap<>();
	private static ModUtils instance;
	public final Config CONFIG;

	private ModUtils() {
		CONFIG = Config.getInstance();
	}

	public static Map<String, String> viewBadJars() {
		return new HashMap<>(badJars);
	}

	public static ModUtils getInstance() {
		if (instance == null) instance = new ModUtils();
		return instance;
	}

	public ModVersion readNode(JsonNode modInfo, JarFile modJar) throws ModIdFailedException {
		System.out.println("reading node");
		try {
			ObjectMapper map = new ObjectMapper();
			System.out.println(map.writerWithDefaultPrettyPrinter().writeValueAsString(map.readValue(modInfo.toString(), Object.class)));
		}
		catch (IOException ignored) { }
		ModVersionFactory factory = new ModVersionFactory();
		if (modInfo.has("name")) factory.withName(modInfo.get("name").textValue());
		else throw new ModIdFailedException("Node doesn't have a name within the mcmod.info");

		String[] names = modJar.getName().split("[\\\\/]");
		factory.withFileName(names[names.length - 1]);

		if (modInfo.has("modid")) factory.withModId(modInfo.get("modid").textValue());
		else factory.withModId(formatModName(modInfo.get("name").textValue()));

		if (modInfo.has("mcversion"))
			factory.withMcVersion(modInfo.get("mcversion").textValue().replaceAll("[^0-9.]", ""));
		else factory.withMcVersion("");

		if (modInfo.has("url")) factory.withUrl(modInfo.get("url").textValue());
		else factory.withUrl("");

		if (modInfo.has("description")) factory.withDescription(modInfo.get("description").textValue());
		else factory.withDescription("<h1>Couldn't find a description</h1>");

		modJar.stream().min(Comparator.comparing(ZipEntry::getLastModifiedTime))
			.ifPresent(e -> factory.uploadedAt(LocalDateTime.ofInstant(e.getLastModifiedTime().toInstant(), ZoneId.systemDefault())));

		return factory.build();
	}

	public void scanForMods(File folder) {
		for (File f : Objects.requireNonNull(folder.listFiles())) {
			if (f.isDirectory()) scanForMods(f);
			else if (f.getName().endsWith(".jar")) {

				JarFile file;
				try {
					file = new JarFile(f);
				} catch (IOException e) {
					continue;
				}

				ZipEntry e = file.getEntry("mcmod.info");
				if (e == null) {
					addBadJar(f.getName(), "mcmod.info doesn't exist");
					continue;
				}
				try {
					String jarId;
					try {
						jarId = Cacher.getJarModId(file);
					} catch (IOException e1) {
						AppLogger.debug("mod: " + file.getName() + " didn't contain an mcmod.info", Startup.class);
						continue;
					}
					try {
						searchWithId(jarId, jarId, f);
						continue;
					} catch (ModIdNotFoundException ignored) {
					}

					JsonNode root;
					try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
						root = new ObjectMapper()
							.readValue(
								r.lines().map(l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
								JsonNode.class
							);
					} catch (JsonParseException e1) {
						addBadJar(jarId, "Error parsing Json");
						continue;
					}
					if (root.has("modList")) root = root.get("modList");//this will be an array

					if (!checkNode(root, jarId, f, root.isArray() ? root.size() : 0)) {
						addMod(jarId, readNode(root.isArray() ? root.get(0) : root, file));
						addBadJar(f.getName(), "Couldn't find a working mod Id within the mcmod.info");
					}
				} catch (Exception e2) {
					AppLogger.error(e2, Startup.class);
				}
			}
		}
	}

	private boolean checkNode(JsonNode array, String jarId, File jarFile, int length) {
		int i = 0;
		do {
			JsonNode root = length > 0 ? array.get(i) : array;
			i++;
			if (root == null) continue;
			try {
				searchWithId(root.get("modid").textValue(), jarId, jarFile);
				return true;
			} catch (ModIdNotFoundException | ModIdFailedException ex2) {
				try {
					searchWithId(formatModName(root.get("name").textValue()), jarId, jarFile);
					return true;
				} catch (ModIdNotFoundException | ModIdFailedException ignored) {
				}
			}
		} while (i < length);
		return false;
	}

	private void searchWithId(String id, String jarId, File file) throws ModIdNotFoundException {
		try {
			ModVersion v = CONFIG.getCachedMods().getMod(id);
			if (v == null || v.getDescription() == null || !v.getFileName().equals(file.getName())) {
				if(v != null){
					id = v.getModId();
				}
				try {
					v = ModChecker.getForVersion(id, MINECRAFT_VERSION)
						.getVersions()
						.stream()
						.filter(e -> e.getFileName().equals(file.getName()))
						.findFirst()
						.orElseThrow(() -> new ModIdNotFoundException(""));
					((Version) v).setModId(id);
				} catch (ModIdNotFoundException | ModIdFailedException ignore) {
				}
			}

			if (v != null) {
				addMod(jarId, v);
				return;
			}
		} catch (Exception e) {
			AppLogger.error(e, getClass());
		}

		throw new ModIdNotFoundException("Id not found: " + id);
	}

	public void addMod(String modId, ModVersion mod) {
		StackTraceElement elements = Thread.currentThread().getStackTrace()[2];
		System.out.println("Adding " + modId + ": "+ mod.getFileName());
		System.out.println(elements.getFileName() + ": " + elements.getLineNumber());
		Config.getInstance().getCachedMods().addMod(modId, mod);
		mods.put(modId, mod);
	}

	public ModVersion[] getMods() {
		if (mods.size() == 0) setMods();
		return mods.values().toArray(new ModVersion[0]);
	}

	public void setMods() {
		mods.clear();
		File f = new File(DOT_MINECRAFT_LOCATION);
		if (f.exists() && f.isDirectory()) {
			f = FileSystems.getDefault().getPath(DOT_MINECRAFT_LOCATION).resolve("mods").toFile();
			if (f.exists() && f.isDirectory()) {
				scanForMods(f);
				try {
					CONFIG.getCachedMods().writeCache();
				} catch (IOException e) {
					AppLogger.error(e, Startup.class);
				}
			}
		}
	}

	public String formatModName(String name) {
		name = name.toLowerCase().replace(' ', '-');
		if (name.contains(":")) name = name.substring(0, name.indexOf(':'));
		name = name
			.replaceAll("[^-a-z0-9]", "")
			.replaceAll("([^-])([0-9]+)|([0-9]+)([^-])", "$1-$2");
		return name;
	}

	public void addBadJar(String file, String reason) {
		badJars.put(file, reason);
	}
}
