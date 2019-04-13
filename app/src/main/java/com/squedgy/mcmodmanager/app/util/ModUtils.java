package com.squedgy.mcmodmanager.app.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.*;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.threads.ModLocatorThread;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static org.slf4j.LoggerFactory.getLogger;

public class ModUtils {

	private static final Logger log = getLogger(ModUtils.class);
	public static final String NO_MOD_INFO = "mcmod.info doesn't exist";
	private static final Map<IdResult, String> badJars = new HashMap<>();
	private static final Map<String, ModVersion> mods = new HashMap<>();
	private static final Map<String, ModVersion> inactiveMods = new HashMap<>();
	private static ModUtils instance;
	public final Config CONFIG;
	private Map<String, Thread> runningThreads = new HashMap<>();

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

	public static String formatModName(String name) {
		name = name.toLowerCase().replace(' ', '-');
		if (name.contains(":")) name = name.substring(0, name.indexOf(':'));
		name = name
			.replaceAll("[^-a-z0-9]", "")
			.replaceAll("([^-])([0-9]+)|([0-9]+)([^-])", "$1-$2");
		return name;
	}

	private static String[] getJarModNames(JarFile file) {
		ZipEntry e = file.getEntry("mcmod.info");
		if (e != null && !e.isDirectory()) {
			ObjectMapper mapper = new ObjectMapper();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
				JsonNode root = mapper.readValue(reader.lines().map(
					l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
					JsonNode.class
				);
				if (root.has("modList")) root = root.get("modList");

				if (root.isArray()) return readNodeForNames((ArrayNode) root);
				else return new String[]{root.get("name").textValue()};

			} catch (IOException ignored) {
			}
		}
		return new String[0];
	}

	private static String[] readNodeForNames(ArrayNode node) {
		String[] ret = new String[node.size()];
		for (int i = 0; i < node.size(); i++) {
			if (node.get(i).has("name")) ret[i] = node.get(i).get("name").textValue();
			else ret[i] = null;
		}
		return ret;
	}

	private static String[] readNodeForIds(ArrayNode node) {
		String[] ret = new String[node.size()];
		for (int i = 0; i < node.size(); i++) {
			if (node.get(i).has("modid")) ret[i] = node.get(i).get("modid").textValue();
			else ret[i] = null;
		}
		return ret;
	}

	private static String[] getJarModIds(JarFile file) {
		ZipEntry e = file.getEntry("mcmod.info");
		if (e != null && !e.isDirectory()) {
			ObjectMapper mapper = new ObjectMapper();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
				JsonNode root = mapper.readValue(reader.lines().map(
					l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
					JsonNode.class
				);
				if (root.has("modList")) root = root.get("modList");

				if (root.isArray()) return readNodeForIds((ArrayNode) root);
				else if (root.has("modid")) return new String[]{root.get("modid").textValue()};

			} catch (IOException ignored) {
			}
		}
		return new String[0];
	}

	private static JarFile getJarFile(File f, String fileName) {
		try {
			return new JarFile(f);
		} catch (IOException e) {
			Version v = new Version();
			v.setFileName(fileName);
			addBadJar(new IdResult(v, fileName), "file couldn't be loaded as a jar.");
			return null;
		}
	}

	private static ZipEntry getMcmodInfo(JarFile file) {
		//Ensure that an mcmod.info file exists
		ZipEntry e = file.getEntry("mcmod.info");
		if (e == null) {
			Version v = new Version();
			v.setFileName(file.getName());
//			addBadJar(new IdResult(v, file.getName()), NO_MOD_INFO);
		}
		return e;
	}

	private static void addBadJar(IdResult node, String reason) {
		badJars.put(node, reason);
	}

	public static String getJarModId(JarFile file) {
		ZipEntry e = getMcmodInfo(file);
		return getJarModId(file, e);
	}

	private static String getJarModId(JarFile file, ZipEntry e){
		if (e != null && !e.isDirectory()) {
			ObjectMapper mapper = new ObjectMapper();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
				JsonNode root = mapper.readValue(
						reader.lines().map(
							l -> l.replaceAll("\n", "\\n")
						).collect(Collectors.joining()
					),
					JsonNode.class
				);
				if (root.has("modList")) root = root.get("modList");
				if (root.isArray()) root = root.get(0);
				if (root.has("modid")) return root.get("modid").textValue();
			} catch (IOException ignored) { }
		}
		return null;
	}

	public void deactivateMod(ModVersion mod) throws IOException {
		//This should clean up any file references hanging about
		System.gc();
		String key = this.getKey(mod);
		File f = new File(PathUtils.getStorageDir());
		if (!f.exists()) if (!f.mkdirs()) throw new IOException("couldn't make the de-active mods folder");
		File startDir = new File(PathUtils.getModsDir());
		if (startDir.toPath().resolve(mod.getFileName()).toFile().exists()) {
			if (f.toPath().resolve(mod.getFileName()).toFile().exists()) {
				if (!startDir.delete()) throw new IOException("couldn't deactivate!");
			} else {
				Files.move(startDir.toPath().resolve(mod.getFileName()), f.toPath().resolve(mod.getFileName()));
				inactiveMods.put(key, mods.remove(key));
			}
		} else {
			throw new IOException("The given mod didn't exist in the Mods DIR OR it was already in!");
		}
	}

	public void activateMod(ModVersion mod) throws IOException, IllegalArgumentException {
		//This should clean up any file references hanging about
		System.gc();
		String key = inactiveMods.entrySet()
			.stream()
			.filter(m -> mod.equals(m.getValue()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Mod Version: id(" + mod.getModId() + ") wasn't inactive!")).getKey();

		File f = new File(PathUtils.getStorageDir());
		File newFile = new File(PathUtils.getModsDir());
		if (f.toPath().resolve(mod.getFileName()).toFile().exists()) {
			if (newFile.toPath().resolve(mod.getFileName()).toFile().exists()) {
				if (!f.delete()) throw new IOException("Couldn't activate mod: id(" + mod.getModId() + ")");
			} else {
				Files.move(f.toPath().resolve(mod.getFileName()), newFile.toPath().resolve(mod.getFileName()));
				mods.put(key, inactiveMods.remove(key));
			}
		} else throw new IOException("Couldn't activate mod: id(" + mod.getModId() + ")");
	}

	public boolean modActive(ModVersion value) {
		return mods.values().stream().map(ModVersion::getModId).anyMatch(e -> e.equals(value.getModId()));
	}

	private void scanForMods(File folder, boolean isActive) {
		for (File f : Objects.requireNonNull(folder.listFiles())) {
			if (f.isDirectory()) scanForMods(f, isActive);
			else if (f.getName().endsWith(".jar")) {
				//Get it as a JarFile
				String fileName = f.getName().replace('+', ' ');
				JarFile file = getJarFile(f, fileName);
				if (file != null) {
					try {
						ZipEntry e = getMcmodInfo(file);
						if (e != null) {
							String jarId = getJarModId(file, e);
							String key = file.getName();
							ModLocatorThread thread = new ModLocatorThread(jarId, fileName, file, (id, v) ->{
								addMod(id, v, isActive);
								toRemove.add(key);
							}, () -> {
								//Otherwise read the mcmod.info as a json node and find the first working one as a stand-in
								try { readJson(file, (e), jarId, isActive); }
								catch (IOException e1) { log.error(e1.getMessage(), getClass()); }
								finally{ toRemove.add(key); }
							});
							runningThreads.put(file.getName(), thread);
							thread.start();
						}
					} catch (Exception e2) {
						log.error("", e2);
					}
				}
			}
		}
	}

	private void readJson(JarFile file, ZipEntry mcmodInfo, String jarId, boolean isActive) throws IOException {
		JsonNode root;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(mcmodInfo)))) {
			root = new ObjectMapper()
				.readValue(
					r.lines().map(l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
					JsonNode.class
				);
		} catch (JsonParseException e1) {
			addBadJar(new IdResult(null, jarId), e1.getMessage());
			return;
		}
		if (root.has("modList")) root = root.get("modList");//this will be an array

		if (root.isArray()) checkNode(root, file, root.size(), isActive);
		else addMod(getJarModId(file), readNode(root, file),"Couldn't find a curse forge match.", isActive);
	}

	private ModVersion readNode(JsonNode modInfo, JarFile modJar) throws ModIdFailedException {

		ModVersionFactory factory = new ModVersionFactory();
		factory.badJar(true);
		if (modInfo.has("name")) factory.withName(modInfo.get("name").textValue());
		else throw new ModIdFailedException("Node doesn't have a name within the mcmod.info");

		factory.withFileName(new File(modJar.getName()).getName());

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

	public IdResult getRealModId(JarFile file) throws ModIdNotFoundException {
		List<Exception> issues = new LinkedList<>();
		Version v = new Version();
		v.setFileName(new File(file.getName()).getName());
		String jarId = getJarModId(file),
			fileName = v.getFileName();
		List<String> testStrings = Arrays.stream(getJarModIds(file)).filter(Objects::nonNull).collect(Collectors.toList());
		testStrings.addAll(Arrays.stream(getJarModNames(file)).filter(Objects::nonNull).map(ModUtils::formatModName).collect(Collectors.toList()));
		testStrings = testStrings.stream().distinct().collect(Collectors.toList());

		IdResult ret = new IdResult();
		ModVersion test = null;
		try {
			for (String id : testStrings) {
				log.debug("testing id: " + id, getClass());
				try {
					test = matchesExistingId(id, fileName);
				} catch (IOException | ModIdNotFoundException | ModIdFoundConnectionFailed e) {
					issues.add(e);
				}
				if (test != null) {
					ret.jarId = jarId;
					ret.mod = test;
					return ret;
				}
			}
		} finally {
			if (test == null) {
				log.info("attemptable: " + testStrings);
				log.info("errors: " + issues);
			}
		}
		throw new ModIdNotFoundException(file.getName() + File.separator + "mcmod.info name/modid doesn't match curse forge id.");
	}

	public ModVersion matchesExistingId(String id, String fileName) throws IOException, ModIdFoundConnectionFailed {
		ModVersion ret = CONFIG.getCachedMods().getItem(id);

		List<ModVersion> versions;
		if (ret == null) {
			versions = ModChecker.getForVersion(id, Config.getMinecraftVersion())
				.getVersions();

		} else if (!ret.getFileName().equals(fileName)) {
			versions = ModChecker.getForVersion(ret.getModId(), Config.getMinecraftVersion())
				.getVersions();

		} else{
			return ret;
		}

		log.info("potential file matches: " + versions.stream().map(ModVersion::getFileName).collect(Collectors.joining("|||")), getClass());
		ret = versions
			.stream()
			.filter(v -> fileName.equals(v.getFileName()))
			.findFirst()
			.orElseThrow(() -> new ModIdFoundConnectionFailed(id + " contained a working id, but there was no matching file"));
		return ret;
	}

	private void checkNode(JsonNode array, JarFile jarFile, int length, boolean active) {
		int i = 0;
		do {
			try {
				JsonNode root = length > 0 ? array.get(i) : array;
				i++;
				if (root == null) continue;
				ModVersion v = readNode(root, jarFile);
				addMod(getJarModId(jarFile), v,"Couldn't find a curse forge match for any guessed ids", active);
				return;
			} catch (Exception ignored) {
			}
		} while (i < length);
	}

//	public void addMod(IdResult mod, boolean active) {
//		addMod(mod.jarId, mod.mod, active);
//	}

	public void addMod(String modId, ModVersion mod, boolean active) {
		addMod(modId, mod, "", active);
	}

	public void addMod(String modId, ModVersion mod, String issue, boolean active) {
		if(issue == null || issue.isEmpty()) issue = modId + " did not have a proper link to curse forge!";
		if (mod.isBadJar()) {
			log.info("adding BAD mod: " + modId, getClass());
			addBadJar(new IdResult(mod, modId), issue);
		}
		if (!modId.contains("/") && !modId.contains("\\")) CONFIG.getCachedMods().putItem(modId, mod);
		else log.info("Did not save " + mod.getModName() + " as it did not have a successful CurseForge match.", getClass());
		if (active) mods.put(modId, mod);
		else inactiveMods.put(modId, mod);
	}

	public ModVersion getMod(String id) {
		ModVersion v = mods.get(id);
		if(v == null){
			v = mods.values().stream().filter(e -> e.getModId().equals(id)).findFirst().orElse(null);
		}
		return v;
	}

	public ModVersion[] getMods() {
		if (mods.size() == 0) setMods();
		return mods.values().toArray(new ModVersion[0]);
	}

	public String getKey(ModVersion m) {
		for (Map.Entry<String, ModVersion> e : mods.entrySet()) if (m.equals(e.getValue())) return e.getKey();
		return null;
	}

	private static List<String> toRemove;

	public void setMods() {
		mods.clear();
		inactiveMods.clear();
		badJars.clear();
		toRemove = new LinkedList<>();
		File f = new File(PathUtils.getMinecraftDirectory());
		if (f.exists() && f.isDirectory()) {
			File mods = new File(PathUtils.getModsDir());
			f = f.toPath().resolve(Config.getMinecraftVersion()).toFile();
			if (mods.exists() && mods.isDirectory()) {
				scanForMods(mods, true);
				if (f.exists() && f.isDirectory()) scanForMods(f, false);
				while(runningThreads.size() > 0) {
					try {
						toRemove.forEach(removal -> runningThreads.remove(removal));
						runningThreads.entrySet().stream().findFirst().ifPresent(e ->{
							if(e.getKey() != null) log.debug(e.getKey(), getClass());
						} );
						TimeUnit.MILLISECONDS.sleep(250);
					}
					catch (InterruptedException ignored) { }
					catch(Exception e) {
						log.error("", e);
					}
				}
				log.info("Done loading mods");
				toRemove = null;
				try {
					Map<String, ModVersion> allMods = new HashMap<>(ModUtils.mods);
					allMods.putAll(inactiveMods);
					CONFIG.getCachedMods().clearUnmatched(allMods);
					CONFIG.getCachedMods().writeCache();
				} catch (IOException e) {
					log.error(e.getMessage(), getClass());
				}
			}
		}
	}

	public List<ModVersion> getInactiveMods() {
		return new LinkedList<>(inactiveMods.values());
	}

	public ModVersion getAnyModFromId(String id) {
		List<ModVersion> version = new LinkedList<>();
		version.addAll(mods.values());
		version.addAll(inactiveMods.values());
		return version.stream().filter(v -> v.getModId().equals(id)).findFirst().orElse(null);
	}

	public static class IdResult {
		public ModVersion mod;
		public String jarId;

		IdResult() {
		}

		IdResult(ModVersion v, String id) {
			this.mod = v;
			this.jarId = id;
		}

	}
}
