package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.squedgy.mcmodmanager.api.abstractions.ModVersion.*;

public class JsonModVersionSerializer extends StdSerializer<Map<String, ModVersion>> {

	public final static Class<Map<String, ModVersion>> DEFAULT_TYPE = (Class<Map<String, ModVersion>>) (Class) (Map.class);

	protected JsonModVersionSerializer() {
		super(DEFAULT_TYPE);
	}

	@Override
	public void serialize(Map<String, ModVersion> map, JsonGenerator gen, SerializerProvider provider) throws IOException {
		ObjectNode root = new ObjectNode(JsonNodeFactory.instance);
		System.out.println("serializing: " + map.keySet());
		map.forEach((key, value) -> {
			if(value != null) {
				ObjectNode n = new ObjectNode(JsonNodeFactory.instance);
				try{
					n.put(JSON_KEY_FILE_NAME, value.getFileName());
					n.put(JSON_KEY_MOD_NAME, value.getModName());
					n.put(JSON_KEY_DESCRIPTION, value.getDescription().replaceAll("\\r\\n|\\r|\\n", ""));
					n.put(JSON_KEY_DOWNLOAD_URL, value.getDownloadUrl());
					n.put(JSON_KEY_ID, value.getId() + "");
					n.put(JSON_KEY_MINECRAFT_VERSION, value.getMinecraftVersion());
					n.put(JSON_KEY_MOD_ID, value.getModId());
					n.put(JSON_KEY_RELEASE_TYPE, value.getTypeOfRelease());
					n.put(JSON_KEY_UPLOADED_AT, value.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
					root.set(key, n);
				}catch(Exception ignored){}
			}
		});
		gen.setPrettyPrinter(new DefaultPrettyPrinter()).getCodec().writeTree(gen, root);
	}
}
