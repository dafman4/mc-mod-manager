package com.squedgy.mcmodmanager.app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.Objects;

public class StartUp extends Application {

    public static String DOT_MINECRAFT_LOCATION;

    public static void main(String[] args) throws Exception {

        Config conf = ConfigFactory.parseFile(new File("config/manager.json"));

        String os = System.getProperty("os.name");
        if(conf.getString("mc-dir") != null){
            DOT_MINECRAFT_LOCATION = conf.getString("mc-dir");
        }else if (os.matches(".*[Ww]indows.*")) {
            DOT_MINECRAFT_LOCATION = System.getenv().get("APPDATA") + "\\.minecraft\\mods";
        } else if (os.matches(".*[Ll]inux.*")) {
            DOT_MINECRAFT_LOCATION = "~/.minecraft";
        } else if (os.matches(".*[Mm]ac [Oo][Ss].*")) {
            DOT_MINECRAFT_LOCATION = "~/Library/Application Support/minecraft";
        } else {
            DOT_MINECRAFT_LOCATION = "~/.minecraft";
        }
        File f = FileSystems.getDefault().getPath(DOT_MINECRAFT_LOCATION).toFile();
        System.out.println(f.getAbsolutePath());
        System.out.println(f.exists());
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("main.fxml"));
        System.out.println(fxml);

        Parent root = FXMLLoader.load(fxml);

        Scene scene = new Scene(root);
        URL l = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("main.css"));

        System.out.println(l);

        scene.getStylesheets().add(l.toExternalForm());

        stage.setTitle("Mc Mod Manager");
        stage.setScene(scene);
        stage.setMinWidth(500);
        stage.setMinHeight(600);
        stage.show();

        System.out.println(stage.getMinWidth());

    }

}
