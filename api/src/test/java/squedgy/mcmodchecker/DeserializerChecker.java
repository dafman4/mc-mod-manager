package squedgy.mcmodchecker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DeserializerChecker {

    private static final String url="https://www.curseforge.com/minecraft/mc-mods/lavasources/download/12345679",
                                name = "lavasources-1.12.2-1.0.0.jar",
                                type = "release",
                                version = "1.12.2",
                                title = "Author",
                                username = "squedgy";
    private static final long id = 12345679;

    private static final String JSON = "{" +
                "\"versions\":{" +
                    "\"1.12.2\":[" +
                        "{" +
                            "\"id\":" + id + "," +
                            "\"url\":\"" + url + "\"," +
                            "\"name\":\"" + name + "\"," +
                            "\"type\":\"" + type + "\"," +
                            "\"version\":\"" + version + "\"" +
                        "}" +
                    "]" +
                "}," +
                "\"members\":[" +
                    "{" +
                        "\"title\":\"" + title + "\"," +
                        "\"username\":\"" + username + "\"" +
                    "}" +
                "]" +
            "}";

    @Test
    public void testDeserializer() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ModResponse.class, new JsonDeserializer());
        mapper.registerModule(module);

        ModResponse response = mapper.readValue(JSON, ModResponse.class);

        Assertions.assertEquals(1, response.getMatchingVersions("1.12.2").size());
        ModVersion v = response.getMatchingVersions("1.12.2").get(0);
        Assertions.assertEquals(type, v.getTypeOfRelease());
        Assertions.assertEquals(url, v.getDownloadUrl());
        Assertions.assertEquals(name, v.getFileName());
        Assertions.assertEquals(version, v.getMinecraftVersion());
        Assertions.assertEquals(id, v.getId());

        ModMember member = response.getMembers().get(0);

        Assertions.assertEquals(title, member.getTitle());
        Assertions.assertEquals(username, member.getUsername());

    }


}
