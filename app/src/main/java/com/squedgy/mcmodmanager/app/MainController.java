package com.squedgy.mcmodmanager.app;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.CacheRetrievalException;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.cache.CachingFailedException;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.app.config.Config;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class MainController extends Application {

    public static String DOT_MINECRAFT_LOCATION;
    public static final String MINECRAFT_VERSION = "1.12.2";
    private static final Map<String,String> badJars = new HashMap<>();

    public static Map<String,String> getBadJars(){
        return Collections.unmodifiableMap(badJars);
    }

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("main.fxml"));

        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        stage.setTitle("Minecraft Mod Manager");
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(700);
        stage.show();

    }

    public static ModVersion readNode(JsonNode modInfo, JarFile modJar){
        ModVersionFactory factory = new ModVersionFactory();
        if(modInfo.has("name")) factory.withName(modInfo.get("name").textValue());
        else factory.withName("");

        String[] names = modJar.getName().split("[\\\\/]");
        factory.withFileName(names[names.length-1]);

        if(modInfo.has("modid"))factory.withModId(modInfo.get("modid").textValue());
        else{
            String modId = modInfo.get("name").textValue().toLowerCase().replace(' ' , '-');
            if(modId.contains(":")) modId = modId.substring(0, modId.indexOf(":"));
            modId = modId.replaceAll("[^-a-z0-9]", "");
            factory.withModId(modId);
        }

        if(modInfo.has("mcversion")) factory.withMcVersion(modInfo.get("mcversion").textValue().replaceAll("[^0-9.]", ""));
        else factory.withMcVersion("");

        if(modInfo.has("url"))factory.withUrl(modInfo.get("url").textValue());
        else factory.withUrl("");

        modJar.stream()
                .min(Comparator.comparing(ZipEntry::getLastModifiedTime))
                .ifPresent(e -> factory.uploadedAt(LocalDateTime.ofInstant(e.getLastModifiedTime().toInstant(), ZoneId.systemDefault())));

        return factory.build();
    }

    public static List<ModVersion> scanForMods(File folder){
        List<ModVersion> ret = new LinkedList<>();
        for(File f : Objects.requireNonNull(folder.listFiles())){
            if(f.isDirectory()){
                ret.addAll(scanForMods(f));
            }else if(f.getName().endsWith(".jar")){
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JarFile file = new JarFile(f);
                    ZipEntry e = file.getEntry("mcmod.info");
                    if(e == null){
                        badJars.put(f.getName(), "mcmod.info doesn't exist");
                        continue;
                    }
                    JsonNode root;
                    try(BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
                         root = mapper.readValue(r.lines().map(l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()), JsonNode.class);
                    }catch(JsonParseException e1){
                        AppLogger.error(new ModIdNotFoundException(""/*"For File:" + f.getAbsolutePath()*/), MainController.class);
//                        badJars.put()
                        continue;
                    }

                    if(root.isArray())root = root.get(0);

                    try {
                        ModVersion v = ModChecker.getCurrentVersion(Cacher.getJarModId(file), MINECRAFT_VERSION);
                        ret.add(v);
                        continue;
                    }catch(CacheRetrievalException ex){
                        ModVersion m = null;
                        try {
                            m = ModChecker.getNewest(Cacher.getJarModId(file), MINECRAFT_VERSION);
                        }catch(ModIdNotFoundException ex2){
                            if(root.has("name")){
                                String name = root.get("name").textValue().toLowerCase().replace(' ', '-');
                                if(name.contains(":"))name = name.substring(0, name.indexOf(':'));
                                try {
                                    m = ModChecker.getNewest(name.replaceAll("[^-a-z0-9]+", ""), MINECRAFT_VERSION);
                                }catch(ModIdNotFoundException ignored){}
                            }
                        }
                        if(m != null){
                            try {
                                new Cacher().writeCache(m, Cacher.getJarModId(file));
                            }catch(CachingFailedException ignored){ }
                        }
                    }

                    try {
                        ret.add(readNode(root, file));
                    } catch (ModIdNotFoundException e1){
                        badJars.put(f.getName(), e1.getMessage());
                    }
                } catch (Exception e) { AppLogger.error(e, MainController.class);}
            }
        }
        return ret;
    }

    public static ModVersion[] getMods(){
        List<ModVersion> strings = new LinkedList<>();
        File f = new File(DOT_MINECRAFT_LOCATION);
        if(f.exists() && f.isDirectory()){
            f = FileSystems.getDefault().getPath(DOT_MINECRAFT_LOCATION).resolve("mods").toFile();
            if(f.exists() && f.isDirectory()){
                strings.addAll(scanForMods(f));
                System.out.println(badJars);
            }
        }else{
            System.out.println("minecraft directory doesn't exist or isn't a directory");
        }
        return strings.toArray(new ModVersion[0]);
    }

    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name");

        //If custom set, otherwise looking for defaults
        if(Config.getProperty("mc-dir") != null) DOT_MINECRAFT_LOCATION = Config.getProperty("mc-dir");
        else if (os.matches(".*[Ww]indows.*")) DOT_MINECRAFT_LOCATION = System.getenv("APPDATA") + "\\.minecraft\\";
        else if (os.matches(".*[Mm]ac [Oo][Ss].*")) DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + "/Library/Application Support/minecraft";
        else DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + "/.minecraft";

        launch(args);
    }

}
