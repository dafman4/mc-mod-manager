package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.api.response.JsonDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public abstract class ModChecker {

    public static CurseForgeResponse getForVersion(String mod, String version) throws Exception {
        return get(mod, new JsonDeserializer(version));
    }

    public static CurseForgeResponse get(String mod)throws Exception{
        return get(mod, new JsonDeserializer());
    }

    private static CurseForgeResponse get(String mod, JsonDeserializer deserializer) throws Exception{
        URL url = new URL("https://api.cfwidget.com/mc-mods/minecraft/" + mod);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new SimpleModule()
                            .addDeserializer(CurseForgeResponse.class, deserializer));

            return mapper.readValue(reader.lines().collect(Collectors.joining("")), CurseForgeResponse.class);
        }catch(FileNotFoundException e){
            throw new ModIdNotFoundException(mod);
        }catch (Exception e){
            throw new RuntimeException(String.format("Error with mod %s.", mod), e);
        }
    }

}
