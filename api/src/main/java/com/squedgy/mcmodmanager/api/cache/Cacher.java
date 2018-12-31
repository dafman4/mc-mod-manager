package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class Cacher {

	public static final String MOD_CACHE_DIRECTORY = "cache" + File.separator;
	private static Cacher instance;
	private Map<String, ModVersion> cachedMods;
	private String fileName;

	private Cacher(String mcVersion) {
		try {
			fileName = MOD_CACHE_DIRECTORY + mcVersion + ".json";
			loadCache(mcVersion);
		} catch (Exception e) {
			AppLogger.error(e, getClass());
			throw new RuntimeException();
		}

	}

	public static Cacher getInstance(String mc) {
		if (instance == null) {
			instance = new Cacher(mc);
		}
		return instance;
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

	public synchronized void loadCache(String mcVersion) {
		try {
			TypeReference<Map<String, ModVersion>> ref = new TypeReference<Map<String, ModVersion>>() {
			};
			SimpleModule module = new SimpleModule();
			Class<Map<String, ModVersion>> clz;
			clz = (Class<Map<String, ModVersion>>) (Class) Map.class;
			module.addDeserializer(clz, new JsonModVersionDeserializer());
			cachedMods = new ObjectMapper()
				.registerModule(module)
				.readValue(new File(fileName), ref);
		} catch (FileNotFoundException e) {
			cachedMods = new HashMap<>();
		} catch (Exception e) {
			AppLogger.error(e, getClass());
			cachedMods = new HashMap<>();
		}
	}

	public synchronized void writeCache() throws IOException {
		File f = new File(fileName);
		ObjectMapper mapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(new JsonModVersionSerializer()));
		try {
			mapper.writeValue(f, cachedMods);
		} catch (FileNotFoundException e) {
			if (f.toPath().getParent().toFile().mkdirs() && f.createNewFile()) {
				mapper.writeValue(f, cachedMods);
			}
		}
	}

	public void addMod(String modId, ModVersion version) { cachedMods.put(modId, version); }

	public ModVersion getMod(String modId) { return cachedMods.get(modId); }

}
