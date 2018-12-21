package com.squedgy.mcmodmanager.app;

import com.squedgy.mcmodmanager.app.components.MainScene;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StartUp extends Application
{

    public static String DOT_MINECRAFT_LOCATION;

    public static void main(String[] args) throws Exception {

        String os = System.getProperty("os.name");
        if (os.matches(".*[Ww]indows.*")) {
            DOT_MINECRAFT_LOCATION = "D:\\Roaming\\local\\.minecraft\\mods";
        } else if (os.matches(".*[Ll]inux.*")) {
            DOT_MINECRAFT_LOCATION = "//";
        } else if (os.matches(".*[Mm]ac [Oo][Ss].*")) {
            DOT_MINECRAFT_LOCATION = "/Users/far3879/Desktop/git/personal/mods/";
        } else {
            DOT_MINECRAFT_LOCATION = "//";
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("Mc Mod Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private Parent createRoot() {
        return new MainScene();
    }
}
