package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.MainController;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.web.WebView;

import java.util.ArrayList;


public class TableViewController {

    @FXML
    private TableView<ModVersion> listView;
    @FXML
    private Button columns;
    @FXML
    private Button updates;
    @FXML
    private WebView objectView;

    private ModCheckingThread checking;
    private ModInfoThread gathering;

    @FXML
    public void setListView(){
        listView.setItems(FXCollections.observableArrayList(MainController.getMods()));
        listView.refresh();
    }

    @FXML
    public void initialize() {
        setListView();
        //Set mod list
        listView.getColumns().setAll(listView.getColumns().sorted( (a, b) -> Config.compareColumns(a.getText(), b.getText())));
        listView.refresh();
        //When selecting one load it into the description into WebViewer
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, neu) -> {
            updateObjectView("<h1>Loading...</h1>");
            if(gathering == null || !gathering.isAlive()) {
                gathering = new ModInfoThread(neu, version -> {
                    Platform.runLater(() -> updateObjectView(version.getDescription()));
                    return null;
                }, n -> {
                    Platform.runLater(() -> updateObjectView(("<h2>Error Loading, couldn't find a matching version!</h2>")));
                    return null;
                });
                gathering.start();
            }
        });
        //Set the default view to a decent looking background
        updateObjectView("");
	    objectView.getEngine().setJavaScriptEnabled(false);
    }

    public TableView<ModVersion> getListView() { return listView; }

    private synchronized void updateObjectView(String n){
        objectView.getEngine().loadContent("<style>body{background-color:#434343; color:#aaa;}img{max-width:100%;height:auto;}</style>" + n);
    }

    @FXML
    public void setColumns(Event e){ Config.writeColumnOrder(listView.getColumns()); }

    @FXML
    public void searchForUpdates(Event e){
        if(checking == null || !checking.isAlive()){
            checking = new ModCheckingThread(new ArrayList<>(listView.getItems()),MainController.MINECRAFT_VERSION, l -> {
                //do something with the returned list
                System.out.println("updateables");
                System.out.println(l);
                return null;
            } );
            checking.start();
        }else{
            AppLogger.info("checking is still alive!", getClass());
        }
    }



}
