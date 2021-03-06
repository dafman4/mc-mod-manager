package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class Cacher<ValueType> {

	private static final Logger log = getLogger(Cacher.class);
	private Map<String, ValueType> cachedMods;
	private String fileName;

	private Cacher(String file, JsonDeserializer<Map<String, ValueType>> deserializer) {
		fileName = file;
		loadCache(deserializer);
	}

	private Cacher(String file) {
		fileName = file;
		cachedMods = loadCache();
	}

	public static <T> Cacher<T> reading(String file, JsonDeserializer<Map<String, T>> deserializer) {
		return new Cacher<>(file, deserializer);
	}

	public static Cacher<JsonNode> reading(String file) {
		return new Cacher<>(file);
	}

	public synchronized void loadCache(JsonDeserializer<Map<String, ValueType>> deserializer) {
		try {
			TypeReference<Map<String, ValueType>> ref = new TypeReference<Map<String, ValueType>>() {
			};
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
			log.error("", e);
			cachedMods = new HashMap<>();
		}
	}

	public synchronized <T> Map<String, T> loadCache() {
		HashMap<String, T> ret = new HashMap<>();
		try {
			new ObjectMapper()
				.readTree(new File(fileName))
				.fields()
				.forEachRemaining(field -> ret.put(field.getKey(), (T) field.getValue()));
			return ret;
		} catch (FileNotFoundException e) {
			ret.clear();
		} catch (Exception e) {
			log.error("", e);
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

	public void clearUnmatched(Map<String, ValueType> mods) {
		Set<String> keys = mods.keySet();
		List<ValueType> version = new LinkedList<>(mods.values());
		cachedMods = cachedMods.entrySet()
			.stream()
			.filter(e -> keys.contains(e.getKey()) && version.contains(e.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public void putItem(String modId, ValueType version) {
		cachedMods.put(modId, version);
	}

	public ValueType getItem(String modId) {
		return cachedMods.get(modId);
	}

}
