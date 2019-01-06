package com.squedgy.mcmodmanager.app.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.*;
import com.squedgy.mcmodmanager.app.components.PublicNode;
import com.squedgy.mcmodmanager.app.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static com.squedgy.mcmodmanager.app.Startup.DOT_MINECRAFT_LOCATION;
import static com.squedgy.mcmodmanager.app.Startup.MINECRAFT_VERSION;

public class ModUtils {


	private static final Map<IdResult, String> badJars = new HashMap<IdResult, String>();
	private static final Map<String, ModVersion> mods = new HashMap<>();
	public static final String NO_MOD_INFO = "mcmod.info doesn't exist";
	private static ModUtils instance;
	public final Config CONFIG;

	private ModUtils() {
				CONFIG = Config.getInstance();
	}

	public static Map<IdResult, String> viewBadJars() {
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
					Version v = new Version();
					v.setFileName(f.getName());
					addBadJar(new IdResult(v, f.getName()), "file couldn't be loaded as a jar.");
					continue;
				}
				//Ensure that an mcmod.info file exists
				ZipEntry e = file.getEntry("mcmod.info");
				if (e == null) {
					Version v = new Version();
					v.setFileName(file.getName());
					addBadJar(new IdResult(v, file.getName()), NO_MOD_INFO);
					continue;
				}
				try {
					String jarId = getJarModId(file);
					ModVersion v = this.CONFIG.getCachedMods().getItem(jarId);
					if(v != null){
						addMod(jarId, v);
						continue;
					}

					//Attempt to find an online ModVersion matching the installed one
					try {
						IdResult id = getRealModId(file);
						addMod(id);
						continue;
					} catch(ModIdNotFoundException e1) {
						AppLogger.info(e1.getMessage(), getClass());
					}
					//Otherwise read the mcmod.info as a json node and find the first working one as a stand-in
					JsonNode root;
					try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
						root = new ObjectMapper()
							.readValue(
								r.lines().map(l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
								JsonNode.class
							);
					} catch (JsonParseException e1) {

						addBadJar(new IdResult(null, getJarModId(file)), e1.getMessage());
						continue;
					}
					if (root.has("modList")) root = root.get("modList");//this will be an array

					if(root.isArray()) checkNode(root, file, root.size());
					else addMod(getJarModId(file), readNode(root, file), true, "Couldn't find a curse forge match.");
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
		List<Exception> issues = new LinkedList<>();
		String jarId = getJarModId(file);
		String[] ids = getJarModIds(file);
		String[] names = getJarModNames(file);
		IdResult ret = new IdResult();
		ModVersion test = null;
		try {
			for (String id : ids) {
				try { test = matchesExistingId(id, new File(file.getName()).getName().replace('+', ' ')); }
				catch (IOException | ModIdNotFoundException e) { issues.add(e); }
				if (test != null) {
					ret.jarId = jarId;
					ret.mod = test;
					return ret;
				}
			}
			for (String name : names) {
				try { test = matchesExistingId(formatModName(name), new File(file.getName()).getName().replace('+', ' ')); }
				catch (IOException | ModIdNotFoundException e) { issues.add(e); }
				if (test != null) {
					ret.jarId = jarId;
					ret.mod = test;
					return ret;
				}
			}
		}catch(ModIdFoundConnectionFailed e){
			throw new ModIdNotFoundException(file.getName() + File.separator + " found a curse record, but it didn't contain a matching file");
		}finally {
			if(test == null) {
				AppLogger.info("attemptable: " + Arrays.toString(ids) + Arrays.toString(names), getClass());
				AppLogger.info("errors: " + issues, getClass());
			}
		}
		throw new ModIdNotFoundException(file.getName() + File.separator + "mcmod.info name/modid doesn't match curse forge id.");
	}

	public ModVersion matchesExistingId(String id, String fileName) throws IOException, ModIdFoundConnectionFailed {
		ModVersion ret = Config.getInstance().getCachedMods().getItem(id);
		List<ModVersion> versions;
		if(ret == null ){
			versions = ModChecker.getForVersion(id, MINECRAFT_VERSION)
				.getVersions();
		}else if(!ret.getFileName().equals(fileName)){
			versions = ModChecker.getForVersion(ret.getModId(), MINECRAFT_VERSION)
				.getVersions();
		}else return ret;

		AppLogger.info("potential file matches: " + versions.stream().map(ModVersion::getFileName).collect(Collectors.joining("|||")), getClass());
		ret = versions
			.stream()
			.filter(v -> v.getFileName().replace('+', ' ').equals(fileName.replace('+', ' ' )))
			.findFirst()
			.orElseThrow(() -> new ModIdFoundConnectionFailed(id + " contained a working id, but there was no matching file"));
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
				addMod(getJarModId(jarFile), v, true, "Couldn't find a curse forge match for any guessed ids");
				return;
			}catch(Exception ignored){}
		} while (i < length);
	}

	private void addMod(IdResult mod){ addMod(mod.jarId, mod.mod); }

	public void addMod(String modId, ModVersion mod) {
		addMod(modId, mod, false, null);
	}

	public void addMod(String modId, ModVersion mod, boolean bad, String issue){
		//if it's a bad mod we probably shouldn't cache it
		if(bad){
			AppLogger.info("adding bad mod: " + modId, getClass());
			addBadJar(new IdResult(mod, modId), issue);
			//if doesn't contain a / or a \ (files at that point)
		}
		else if(!modId.contains("/") && !modId.contains("\\")) Config.getInstance().getCachedMods().putItem(modId, mod);
		else AppLogger.info("Did not save " + mod.getModName() + " as it did not have a successful CurseForge match.", getClass());
		mods.put(modId, mod);
	}

	public ModVersion getMod(String id){ return mods.get(id); }

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
					AppLogger.error(e.getMessage(), getClass());
				}
			}
		}
	}

	public static String formatModName(String name) {
		name = name.toLowerCase().replace(' ', '-');
		if (name.contains(":")) name = name.substring(0, name.indexOf(':'));
		name = name
			.replaceAll("[^-a-z0-9]", "")
			.replaceAll("([^-])([0-9]+)|([0-9]+)([^-])", "$1-$2");
		return name;
	}

	public void addBadJar(IdResult node, String reason) { badJars.put(node, reason); }

	public static class IdResult{
		public ModVersion mod;
		public String jarId;

		public IdResult(){ }

		public IdResult(ModVersion v, String id){
			this.mod = v;
			this.jarId = id;
		}

	}

	public static String[] getJarModNames(JarFile file){
		ZipEntry e = file.getEntry("mcmod.info");
		if(e != null && !e.isDirectory()){
			ObjectMapper mapper = new ObjectMapper();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(e)))){
				JsonNode root = mapper.readValue(reader.lines().map(
					l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
					JsonNode.class
				);
				if(root.has("modList"))root = root.get("modList");

				if(root.isArray()) return readNodeForNames((ArrayNode) root);
				else return new String[] {root.get("name").textValue()};

			}catch(IOException ignored ){ }
		}
		return new String[0];
	}

	private static String[] readNodeForNames(ArrayNode node){
		String[] ret = new String[node.size()];
		for(int i = 0; i < node.size(); i++){
			if(node.get(i).has("name")) ret[i] = node.get(i).get("name").textValue();
			else ret[i] = null;
		}
		return ret;
	}

	private static String[] readNodeForIds(ArrayNode node){
		String[] ret = new String[node.size()];
		for(int i = 0; i < node.size(); i++){
			if(node.get(i).has("modid")) ret[i] = node.get(i).get("modid").textValue();
			else ret[i] = null;
		}
		return ret;
	}

	public static String[] getJarModIds(JarFile file) {
		ZipEntry e = file.getEntry("mcmod.info");
		if(e != null && !e.isDirectory()){
			ObjectMapper mapper = new ObjectMapper();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(e)))){
				JsonNode root = mapper.readValue(reader.lines().map(
					l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
					JsonNode.class
				);
				if(root.has("modList"))root = root.get("modList");

				if(root.isArray()) return readNodeForIds((ArrayNode) root);
				else if(root.has("modid")) return new String[] {root.get("modid").textValue()};

			}catch(IOException ignored ){ }
		}
		return new String[0];
	}

	private String getJarModId(JarFile file) {
		ZipEntry e = file.getEntry("mcmod.info");
		if(e != null && !e.isDirectory()){
			ObjectMapper mapper = new ObjectMapper();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(e)))){
				JsonNode root = mapper.readValue(
					reader.lines().map(
						l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()
					),
					JsonNode.class
				);
				if(root.has("modList")) root = root.get("modList");
				if(root.isArray()) root = root.get(0);
				if(root.has("modid")) return root.get("modid").textValue();
			} catch (IOException ignored) { }
		}
		return null;
	}
}
