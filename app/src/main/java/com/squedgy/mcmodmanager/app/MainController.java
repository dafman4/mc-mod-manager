package com.squedgy.mcmodmanager.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.app.config.Config;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class MainController extends Application {

    public static String DOT_MINECRAFT_LOCATION;
    public static String MINECRAFT_VERSION = "1.12.2";

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

        factory.withName(modInfo.get("name").textValue());
        String[] names = modJar.getName().split("[\\\\/]");
        factory.withFileName(names[names.length-1]);
        factory.withModId(modInfo.get("modid").textValue());
        factory.withMcVersion(modInfo.get("mcversion").textValue().replaceAll("[^0-9.]",""));
        factory.withUrl(modInfo.get("url").textValue());
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
                    if(e == null) throw new Exception();
                    JsonNode root = mapper.readValue(file.getInputStream(e), JsonNode.class);
                    if(root.isArray()){
                        JsonNode jsonInfo = root.get(0);
                        if(jsonInfo.isObject()){
                            ret.add(readNode(jsonInfo, file));
                        }
                    }else{
                        ret.add(readNode(root, file));
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
            }
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
