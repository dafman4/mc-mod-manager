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

	public void scanForMods(File folder) {
		for (File f : Objects.requireNonNull(folder.listFiles())) {
			if (f.isDirectory()) scanForMods(f);
			else if (f.getName().endsWith(".jar")) {
				//Get it as a JarFile
				JarFile file;
				try { file = new JarFile(f); }
				catch (IOException e) {
					addBadJar(f.getName(), "file couldn't be loaded as a jar.");
					continue;
				}
				//Ensure that an mcmod.info file exists
				ZipEntry e = file.getEntry("mcmod.info");
				if (e == null) {
					addBadJar(f.getName(), "mcmod.info doesn't exist");
					continue;
				}
				try {
					//Attempt to find a cached or online ModVersion matching the installed one
					try {
						IdResult id = getRealModId(file);
						addMod(id);
						continue;
					} catch(ModIdNotFoundException e1) {
						AppLogger.info(e1.getMessage(), getClass());
					}
					//Otherwise read the mcmod.info as a json node and find the first working one as a stand-in
					addBadJar(f.getName(), "Couldn't find a cached mod-id as well as the name, and mcmod.info modid(s) didn't match anything online");
					JsonNode root;
					try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
						root = new ObjectMapper()
							.readValue(
								r.lines().map(l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
								JsonNode.class
							);
					} catch (JsonParseException e1) {
						continue;
					}
					if (root.has("modList")) root = root.get("modList");//this will be an array

					if(root.isArray()) checkNode(root, file, root.size());
					else addMod(f.getName(), readNode(root, file));
				} catch (Exception e2) {
					AppLogger.error(e2, getClass());
				}
			}
		}
	}

	public ModVersion readNode(JsonNode modInfo, JarFile modJar) throws ModIdFailedException {

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

	public IdResult getRealModId(JarFile file) throws ModIdNotFoundException{
		;
		String[] ids = Cacher.getJarModIds(file);
		String[] names = Cacher.getJarModNames(file);
		IdResult ret = new IdResult();
		ModVersion test;
		for(String id : ids){
			AppLogger.debug("trying: " + id, getClass());
			test = matchesExistingId(id, new File(file.getName()).getName().replace('+', ' '));
			if(test != null){
				AppLogger.debug("MATCHED!", getClass());
				ret.jarId = id;
				ret.mod = test;
				return ret;
			}
		}
		for(String name: names){
			AppLogger.debug("trying: " + formatModName(name), getClass());
			test = matchesExistingId(formatModName(name), new File(file.getName()).getName().replace('+', ' '));
			if(test != null){
				AppLogger.debug("MATCHED!", getClass());
				ret.jarId = formatModName(name);
				ret.mod = test;
				return ret;
			}
		}
		throw new ModIdNotFoundException(file.getName() + " didn't contain a cached or online mod Id");
	}

	public ModVersion matchesExistingId(String id, String fileName){
		ModVersion ret = Config.getInstance().getCachedMods().getMod(id);
		if(ret == null || !ret.getFileName().equals(fileName)){
			try {
				ret = ModChecker.getForVersion(ret == null ? id : ret.getModId(), MINECRAFT_VERSION)
					.getVersions()
					.stream()
					.filter(v -> v.getFileName().replace('+', ' ').equals(fileName))
					.findFirst()
					.orElse(null);
			} catch (Exception e) {
				AppLogger.debug(e.getMessage(), getClass());
				ret = null;
			}
		}
		return ret;
	}

	private void checkNode(JsonNode array, JarFile jarFile, int length) {
		int i = 0;
		do {
			try {
				JsonNode root = length > 0 ? array.get(i) : array;
				i++;
				if (root == null) continue;
				ModVersion v = readNode(root, jarFile);
				addMod(jarFile.getName(), v);
				return;
			}catch(Exception ignored){}
		} while (i < length);
	}

	private void addMod(IdResult mod){ addMod(mod.jarId, mod.mod); }

	public void addMod(String modId, ModVersion mod) {
		Config.getInstance().getCachedMods().addMod(modId, mod);
		mods.put(modId, mod);
	}

	public ModVersion getMod(String id){
		return mods.get(id);
	}

	public ModVersion[] getMods() {
		if (mods.size() == 0) setMods();
		return mods.values().toArray(new ModVersion[0]);
	}

	public String getKey(ModVersion m){
		for(Map.Entry<String, ModVersion> e : mods.entrySet()) if(m.equals(e.getValue())) return e.getKey();
		return null;
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

	public void addBadJar(String file, String reason) { badJars.put(file, reason); }

	public static class IdResult{
		public ModVersion mod;
		public String jarId;

		public IdResult(){ }

	}
}
