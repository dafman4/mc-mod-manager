package com.squedgy.mcmodmanager.api.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class JsonDeserializer extends StdDeserializer<CurseForgeResponse> {

    private final String version;
    public static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendLiteral('Z')
            .toFormatter();

    public JsonDeserializer(){ this((Class<?>)null); }

    public JsonDeserializer(String version){
        super((Class<?>)null);
        this.version = version;
    }

    public JsonDeserializer(Class<?> vc) {
        super(vc);
        this.version = null;
    }

    @Override
    public CurseForgeResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        try {
            ModInformation ret = new ModInformation();

            JsonNode node = parser.getCodec().readTree(parser);
            String[] urlId = node.get("files").get(0).get("url").asText().split("/");
            String modName = node.get("title").asText(),
                    modId = urlId[urlId.length - 3];

            node.get("versions").elements().forEachRemaining(versionNumber -> {
                versionNumber.elements().forEachRemaining(modVersion -> {
                    if (this.version == null
                            || (modVersion.get("version") != null
                            && version.equals(modVersion.get("version").asText()))) {
                        Version toAdd = new Version();
                        toAdd.setId(modVersion.get("id").asLong());
                        toAdd.setDownloadUrl(modVersion.get("url").asText());
                        toAdd.setFileName(modVersion.get("name").asText());
                        toAdd.setTypeOfRelease(modVersion.get("type").asText());
                        toAdd.setMinecraftVersion(modVersion.get("version").asText());
                        toAdd.setUploadedAt(LocalDateTime.parse(modVersion.get("uploaded_at").asText(), formatter));
                        toAdd.setModId(modId);
                        toAdd.setModName(modName);
                        ret.addVersion(toAdd);
                    }
                });
            });

            node.get("members").elements().forEachRemaining(member -> {
                String title = member.get("title").asText(),
                        username = member.get("username").asText();
                ret.addMember(new Member(title, username));
            });
            if(ret.getVersions().size() > 0) System.out.println("mId: " + ret.getVersions().get(0).getModId());

            return ret;
        }catch(NullPointerException e){
            throw new IOException(e);
        }
    }
}
