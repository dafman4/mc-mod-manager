package com.squedgy.mcmodmanager.app;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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

    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name");
        ModUtils c = ModUtils.getInstance();

        //If custom set, otherwise looking for defaults
        if(c.CONFIG.getProperty("mc-dir") != null) DOT_MINECRAFT_LOCATION = c.CONFIG.getProperty("mc-dir");
        else if (os.matches(".*[Ww]indows.*")) DOT_MINECRAFT_LOCATION = System.getenv("APPDATA") + "\\.minecraft\\";
        else if (os.matches(".*[Mm]ac [Oo][Ss].*")) DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + "/Library/Application Support/minecraft";
        else DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + "/.minecraft";

        launch(args);
    }

}
