package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.api.reader.StreamReader;
import com.squedgy.mcmodmanager.api.response.JsonDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ModChecker {

    public static CurseForgeResponse getForVersion(String mod, String version) throws Exception {
        return get(mod, new JsonDeserializer(version));
    }

    public static CurseForgeResponse get(String mod)throws Exception{
        return get(mod, new JsonDeserializer());
    }

    private static CurseForgeResponse get(String mod, JsonDeserializer deserializer) throws Exception{
        URL url = new URL("http://api.cfwidget.com/mc-mods/minecraft/" + mod);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        try{
            StreamReader<String,String> reader = new StreamReader<>();
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new SimpleModule()
                            .addDeserializer(CurseForgeResponse.class, deserializer));

            return mapper.readValue(response.lines().collect(Collectors.joining("")), CurseForgeResponse.class);
        }catch (Exception e){
            throw new RuntimeException(String.format("Error with mod %s.", mod), e);
        }
    }

}
