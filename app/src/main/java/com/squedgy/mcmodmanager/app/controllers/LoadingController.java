package com.squedgy.mcmodmanager.app.controllers;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class LoadingController {

    @FXML
    private WebView root;
    private static Stage s;

    public LoadingController() throws IOException {
        FXMLLoader loader = new FXMLLoader(getResource("components/loading.fxml"));
        loader.setController(this);
        loader.load();
    }

    @FXML
    public void initialize(){
        System.out.println(getResource("components/img/loading.png"));
        root.getEngine().loadContent(
                "<style>" +
                    "body{" +
                        "background-color:#222;" +
                        "color:#ccc;" +
                    "} " +
                    "@keyframes spin{" +
                        "from{transform:rotate(0deg);} " +
                        "to{transform:rotate(360deg);} " +
                    "} " +
                    ".img {" +
                        "background: url(\"" + getResource("components/img/loading.png") + "\") center center no-repeat;" +
                        "background-size:contain;" +
                        "animation:spin 3s linear infinite;" +
                        "min-height:100px;" +
                        "min-width:100px;" +
                    "} " +
                "</style>" +
                "<div style=\"display:flex;justify-content:center;align-items:center;height:100%;\">" +
                    "<div class='img' id='img'></div>" +
                "</div>"
        );
    }


    public WebView getRoot(){
        return root;
    }

}
