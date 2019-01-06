package com.squedgy.mcmodmanager.api.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class CurseForgeResponseDeserializer extends StdDeserializer<CurseForgeResponse> {

	public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
		.append(DateTimeFormatter.ISO_LOCAL_DATE)
		.appendLiteral('T')
		.append(DateTimeFormatter.ISO_LOCAL_TIME)
		.appendLiteral('Z')
		.toFormatter();
	private final String VERSION;

	public CurseForgeResponseDeserializer() {
		this(CurseForgeResponse.class);
	}

	public CurseForgeResponseDeserializer(String version) {
		super(CurseForgeResponse.class);
		this.VERSION = version;
	}

	private CurseForgeResponseDeserializer(Class<?> vc) {
		super(vc);
		this.VERSION = null;
	}

	@Override
	public CurseForgeResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		try {
			ModInformation ret = new ModInformation();

			JsonNode node = parser.getCodec().readTree(parser);
			String[] urlId = node.get("urls").get("project").textValue().split("/");
			String modName = node.get("title").asText(),
				modId = urlId[urlId.length - 1];
			String desc = node.get("description").textValue();

			node.get("versions").fields().forEachRemaining(versionNumber -> {
				versionNumber.getValue().elements().forEachRemaining(modVersion -> {
					if (this.VERSION == null
						|| (modVersion.get("version") != null
						&& VERSION.equals(modVersion.get("version").asText()))
					) {
						ret.addVersion(buildVersion(modVersion, modId, desc, modName, VERSION == null ? modVersion.get("version").textValue() : VERSION));
					} else {
						ArrayNode versions = ((ArrayNode) modVersion.get("versions"));
						for (int i = 0; i < versions.size(); i++) {
							if (versions.get(i).textValue().equals(VERSION)) {
								ret.addVersion(buildVersion(modVersion, modId, desc, modName, VERSION));
								i = versions.size();
							}
						}
					}
				});
			});

			node.get("members").elements().forEachRemaining(member -> {
				String title = member.get("title").asText(),
					username = member.get("username").asText();
				ret.addMember(new Member(title, username));
			});

			return ret;
		} catch (NullPointerException e) {
			throw new IOException(e);
		}
	}

	public String getVersion() {
		return VERSION;
	}

	private ModVersion buildVersion(JsonNode node, String modId, String desc, String modName, String version) {
		return new ModVersionFactory()
			.withId(node.get("id").asLong())
			.withUrl(node.get("url").asText())
			.withFileName(node.get("name").asText())
			.withType(node.get("type").asText())
			.withMcVersion(node.get("version").asText())
			.uploadedAt(LocalDateTime.parse(node.get("uploaded_at").asText(), formatter))
			.withDescription(desc)
			.withModId(modId)
			.withName(modName)
			.build();
	}
}
