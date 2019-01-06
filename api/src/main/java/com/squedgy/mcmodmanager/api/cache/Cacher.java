package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Cacher <ValueType>{

	public static final String MOD_CACHE_DIRECTORY = "cache" + File.separator;
	private Map<String, ValueType> cachedMods;
	private String fileName;

	private Cacher(String file, JsonDeserializer<Map<String,ValueType>> deserializer) {
		fileName = file;
		loadCache(deserializer);
	}

	private Cacher(String file){
		fileName = file;
		cachedMods = loadCache();
	}

	public static <T>  Cacher<T> reading(String file, JsonDeserializer<Map<String,T>> deserializer) {
		return new Cacher<>(file, deserializer);
	}

	public static Cacher<JsonNode> reading(String file){
		return new Cacher<>(file);
	}

	public synchronized void loadCache(JsonDeserializer<Map<String,ValueType>> deserializer) {
		try {
			TypeReference<Map<String, ValueType>> ref = new TypeReference<Map<String, ValueType>>() {};
			SimpleModule module = new SimpleModule();
			Class<Map<String, ValueType>> clz;
			clz = (Class<Map<String, ValueType>>) (Class) Map.class;
			module.addDeserializer(clz, deserializer);
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

	public synchronized <T> Map<String,T> loadCache(){
		HashMap<String, T> ret = new HashMap<>();
		try{
			new ObjectMapper()
				.readTree(new File(fileName))
				.fields()
				.forEachRemaining(field -> ret.put(field.getKey(), (T)field.getValue()));
			return ret;
		}catch(FileNotFoundException e){
			ret.clear();
		}catch(Exception e){
			AppLogger.error(e, getClass());
			ret.clear();
		}
		return ret;
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

	public void putItem(String modId, ValueType version) { cachedMods.put(modId, version); }

	public ValueType getItem(String modId) {
		AppLogger.debug(String.format("getting %s", modId), getClass());
		return cachedMods.get(modId);
	}

}
