package com.squedgy.mcmodmanager.app;

import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class Startup extends Application {

    public static String DOT_MINECRAFT_LOCATION;
    public static String MINECRAFT_VERSION = "1.12.2";


    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = Objects.requireNonNull(getResource("main.fxml"));

        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        stage.setTitle("Minecraft Mod Manager");
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(700);
        stage.show();

    }

    public static void main(String[] args) {
        String os = System.getProperty("os.name");
        ModUtils c = ModUtils.getInstance();

        //If custom set, otherwise looking for defaults
        if(c.CONFIG.getProperty("mc-dir") != null) DOT_MINECRAFT_LOCATION = c.CONFIG.getProperty("mc-dir");
        else if (os.matches(".*[Ww]indows.*")) DOT_MINECRAFT_LOCATION = System.getenv("APPDATA") + "\\.minecraft\\";
        else if (os.matches(".*[Mm]ac [Oo][Ss].*")) DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + "/Library/Application Support/minecraft";
        else DOT_MINECRAFT_LOCATION = System.getProperty("user.home") + "/.minecraft";

        launch(args);
    }

    public static URL getResource(String resource){
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

}
