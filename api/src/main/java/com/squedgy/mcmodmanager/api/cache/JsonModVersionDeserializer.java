package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.squedgy.mcmodmanager.api.abstractions.ModVersion.*;

public class JsonModVersionDeserializer extends StdDeserializer<Map<String, ModVersion>> {

	public JsonModVersionDeserializer() {
		super(JsonModVersionSerializer.DEFAULT_TYPE);
	}

	@Override
	public Map<String, ModVersion> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		Map<String, ModVersion> ret = new HashMap<>();
		ModVersionFactory f = new ModVersionFactory();

		JsonNode root = parser.getCodec().readTree(parser);
		if (root.isObject()) {
			root.fields().forEachRemaining(field -> {
				f.reset();

				JsonNode version = field.getValue();
				if (nodeHasAll(version,
					JSON_KEY_DESCRIPTION,
					JSON_KEY_DOWNLOAD_URL,
					JSON_KEY_FILE_NAME,
					JSON_KEY_ID,
					JSON_KEY_MINECRAFT_VERSION,
					JSON_KEY_MOD_ID,
					JSON_KEY_MOD_NAME,
					JSON_KEY_RELEASE_TYPE,
					JSON_KEY_UPLOADED_AT,
					JSON_KEY_BAD_JAR)
				) {
					ret.put(
						field.getKey(),
						f.withName(getText(version, JSON_KEY_MOD_NAME))
							.withFileName(getText(version, JSON_KEY_FILE_NAME))
							.withType(getText(version, JSON_KEY_RELEASE_TYPE))
							.withMcVersion(getText(version, JSON_KEY_MINECRAFT_VERSION))
							.withModId(getText(version, JSON_KEY_MOD_ID))
							.withUrl(getText(version, JSON_KEY_DOWNLOAD_URL))
							.withDescription(getText(version, JSON_KEY_DESCRIPTION).replaceAll("\\r\\n|\\n|\\r", ""))
							.withId(getLong(version, JSON_KEY_ID))
							.uploadedAt(LocalDateTime.parse(getText(version, JSON_KEY_UPLOADED_AT)))
							.badJar(getBoolean(version, JSON_KEY_BAD_JAR))
							.build()
					);
				}
			});
		}

		return ret;
	}

	private boolean nodeHasAll(JsonNode n, String... subNodes) {
		for (String id : subNodes) if (!n.has(id)) return false;
		return true;
	}

	private String getText(JsonNode node, String id) { return node.get(id).textValue(); }

	private long getLong(JsonNode node, String id) { return node.get(id).longValue(); }

	private boolean getBoolean(JsonNode node, String id) { return node.get(id).booleanValue(); }
}
