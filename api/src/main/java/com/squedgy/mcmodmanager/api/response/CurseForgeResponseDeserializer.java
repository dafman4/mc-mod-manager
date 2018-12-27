package com.squedgy.mcmodmanager.api.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class CurseForgeResponseDeserializer extends StdDeserializer<CurseForgeResponse> {

    private final String VERSION;
    public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendLiteral('Z')
            .toFormatter();

    public CurseForgeResponseDeserializer(){ this(CurseForgeResponse.class); }

    public CurseForgeResponseDeserializer(String version){
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
            String[] urlId = node.get("files").get(0).get("url").asText().split("/");
            String modName = node.get("title").asText(),
                    modId = urlId[urlId.length - 3];
            String desc = node.get("description").textValue();

            node.get("versions").elements().forEachRemaining(versionNumber -> {
                versionNumber.elements().forEachRemaining(modVersion -> {
                    if (this.VERSION == null
                            || (modVersion.get("version") != null
                            && VERSION.equals(modVersion.get("version").asText()))) {
                        ModVersion toAdd = new ModVersionFactory()
                            .withId(modVersion.get("id").asLong())
                            .withUrl(modVersion.get("url").asText())
                            .withFileName(modVersion.get("name").asText())
                            .withType(modVersion.get("type").asText())
                            .withMcVersion(modVersion.get("version").asText())
                            .uploadedAt(LocalDateTime.parse(modVersion.get("uploaded_at").asText(), formatter))
                            .withDescription(desc)
                            .withModId(modId)
                            .withName(modName)
                            .build();
                        ret.addVersion(toAdd);
                    }
                });
            });

            node.get("members").elements().forEachRemaining(member -> {
                String title = member.get("title").asText(),
                        username = member.get("username").asText();
                ret.addMember(new Member(title, username));
            });

            return ret;
        }catch(NullPointerException e){
            throw new IOException(e);
        }
    }

    public String getVersion() { return VERSION; }
}
