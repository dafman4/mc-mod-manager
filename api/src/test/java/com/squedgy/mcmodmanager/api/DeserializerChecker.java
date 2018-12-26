package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.squedgy.mcmodmanager.api.response.CurseForgeResponseDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.ModMember;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.IOException;
import java.time.LocalDateTime;

public class DeserializerChecker {

    private static final LocalDateTime uploadedTime = LocalDateTime.now();
    private static final String url="https://www.curseforge.com/minecraft/mc-mods/lavasources/download/12345679",
                                name = "lavasources-1.12.2-1.0.0.jar",
                                type = "release",
                                version = "1.12.2",
                                title = "Author",
                                username = "squedgy",
                                uploaded_at = uploadedTime.format(CurseForgeResponseDeserializer.formatter),
                                modName = "Lava Sources",
                                modId = "lavasources";
    private static final long id = 12345679;

    private static final String JSON = "{" +
                "\"versions\":{" +
                    "\"1.12.2\":[" +
                        "{" +
                            "\"id\":" + id + "," +
                            "\"url\":\"" + url + "\"," +
                            "\"name\":\"" + name + "\"," +
                            "\"type\":\"" + type + "\"," +
                            "\"version\":\"" + version + "\"," +
                            "\"uploaded_at\":\"" + uploaded_at + "\"" +
                        "}," +
                        "{" +
                            "\"id\":" + (id+1) + "," +
                            "\"url\":\"" + "1" + url + "\"," +
                            "\"name\":\"" + name + "\"," +
                            "\"type\":\"" + type + "\"," +
                            "\"version\":\"" + version + "\"," +
                            "\"uploaded_at\":\"" + uploadedTime.plusDays(1).format(CurseForgeResponseDeserializer.formatter) + "\"" +
                        "}" +
                    "]" +
                "}," +
                "\"members\":[" +
                    "{" +
                        "\"title\":\"" + title + "\"," +
                        "\"username\":\"" + username + "\"" +
                    "}" +
                "]," +
                "\"files\": [" +
                    "{" +
                        "\"url\":\"" + url + "\"" +
                    "}" +
                "]," +
                "\"title\":\"" + modName + "\"" +
            "}";

    private ObjectMapper mapper;

    @BeforeEach
    public void init(){
        mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule().addDeserializer(CurseForgeResponse.class, new CurseForgeResponseDeserializer()));
    }

    @Test
    public void testDeserializer() throws IOException {

        CurseForgeResponse response = mapper.readValue(JSON, CurseForgeResponse.class);

        Assertions.assertEquals(2, response.getRelatedVersions("1.12.2").size());
        ModVersion v = response.getRelatedVersions("1.12.2").get(0);
        Assertions.assertEquals(type, v.getTypeOfRelease());
        Assertions.assertEquals(url, v.getDownloadUrl());
        Assertions.assertEquals(name, v.getFileName());
        Assertions.assertEquals(version, v.getMinecraftVersion());
        Assertions.assertEquals(id, v.getId());
        Assertions.assertEquals(uploadedTime, v.getUploadedAt());
        Assertions.assertEquals(modId, v.getModId());
        Assertions.assertEquals(modName, v.getModName());

        ModMember member = response.getMembers().get(0);

        Assertions.assertEquals(title, member.getTitle());
        Assertions.assertEquals(username, member.getUsername());

    }

    @Test
    public void testGetLatest() throws Exception {
        CurseForgeResponse response = mapper.readValue(JSON ,CurseForgeResponse.class);
        Assertions.assertEquals(uploadedTime.plusDays(1), response.getLatestVersion("1.12.2").getUploadedAt());
    }

}
