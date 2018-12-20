package squedgy.mcmodchecker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ModChecker {

    public static List<ModVersion> getForVersion(String mod, String version) throws Exception {
        URL url = new URL("https://api.cfwidget.com/mc-mods/minecraft/" + mod);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))){
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(ModResponse.class, new JsonDeserializer(version));
            mapper.registerModule(module);
            ModResponse response = mapper.readValue(in.lines().collect(Collectors.joining("")), ModResponse.class);
            return response.getVersions();
        }catch (Exception e){
            System.out.println(String.format("there was an exception retrieved mod: %s v.%s", mod, version));
            e.printStackTrace();
        }
        return null;
    }

}
