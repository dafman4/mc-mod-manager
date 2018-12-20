package squedgy.mcmodchecker;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class JsonDeserializer extends StdDeserializer<ModResponse> {

    private final String version;

    public JsonDeserializer(){ this((Class<?>)null); }

    public JsonDeserializer(String version){
        super((Class<?>)null);
        this.version = version;
        System.out.println("version = " + version);
    }

    public JsonDeserializer(Class<?> vc) {
        super(vc);
        this.version = null;
    }

    @Override
    public ModResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {

        ModResponse ret = new ModResponse();

        JsonNode node = parser.getCodec().readTree(parser);

        node.get("versions").elements().forEachRemaining(versionNumber -> {
            versionNumber.elements().forEachRemaining(modVersion -> {
                if(this.version == null || (modVersion.get("version") != null && version.equals(modVersion.get("version").asText()))) {
                    ModVersion toAdd = new ModVersion();
                    toAdd.setId(modVersion.get("id").asLong());
                    toAdd.setDownloadUrl(modVersion.get("url").asText());
                    toAdd.setFileName(modVersion.get("name").asText());
                    toAdd.setTypeOfRelease(modVersion.get("type").asText());
                    toAdd.setMinecraftVersion(modVersion.get("version").asText());
                    ret.addVersion(toAdd);
                }
            });
        });

        node.get("members").elements().forEachRemaining(member -> {
            String title = member.get("title").asText(),
                    username = member.get("username").asText();
            ret.addMember(new ModMember(title, username));
        });


        return ret;
    }
}
