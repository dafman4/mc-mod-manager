package com.squedgy.mcmodmanager.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.app.components.TableViewController;
import com.squedgy.mcmodmanager.app.config.Config;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class StartUp extends Application {

    public static String DOT_MINECRAFT_LOCATION;

    @FXML
    private static TableViewController tableViewController;

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("main.fxml"));

        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        URL l = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("main.css"));
        scene.getStylesheets().add(l.toExternalForm());

        stage.setTitle("Mc Mod Manager");
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(700);
        stage.show();
        System.out.println(tableViewController);
    }

    public static ModVersion readNode(JsonNode modInfo, File modJar){
        ModVersionFactory factory = new ModVersionFactory();

        factory.withName(modInfo.get("name").textValue());
        factory.withFileName(modJar.getName());
        factory.withModId(modInfo.get("modid").textValue());
        factory.withMcVersion(modInfo.get("mcversion").textValue().replaceAll("[^0-9.]",""));
        factory.withUrl(modInfo.get("url").textValue());

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
                            ret.add(readNode(jsonInfo, f));
                        }
                    }else{
                        ret.add(readNode(root, f));
                    }
                    continue;
                } catch (Exception ignored) { }
            }
        }
        return ret;
    }

    public static ModVersion[] getMods(){
        List<ModVersion> strings = new LinkedList<>();
        File f = new File(DOT_MINECRAFT_LOCATION);
        if(f.exists() && f.isDirectory()){
            f = f.toPath().resolve("mods").toFile();
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
        else if (os.matches(".*[Ww]indows.*")) DOT_MINECRAFT_LOCATION = System.getenv().get("APPDATA") + "\\.minecraft\\";
        else if (os.matches(".*[Mm]ac [Oo][Ss].*")) DOT_MINECRAFT_LOCATION = "~/Library/Application Support/minecraft";
        else DOT_MINECRAFT_LOCATION = "~/.minecraft";

        launch(args);
    }

}
