package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.util.ArrayList;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class TableViewController {

    @FXML
    private TableView<ModVersion> listView;
    @FXML
    private Button columns;
    @FXML
    private Button updates;
    @FXML
    private Button badJars;
    @FXML
    private WebView objectView;
    @FXML
    private GridPane listGrid;
    @FXML
    private ScrollPane root;

    private ModCheckingThread checking;
    private ModInfoThread gathering;

    @FXML
    public void setListView(){
        listView.setItems(FXCollections.observableArrayList(ModUtils.getInstance().getMods()));
        listView.refresh();
    }

    @FXML
    public void initialize() {
        badJars.setVisible(ModUtils.viewBadJars().size() <= 0);
        setListView();
        //Set mod list
        listView.getColumns().setAll(listView.getColumns().sorted( (a, b) -> ModUtils.getInstance().CONFIG.compareColumns(a.getText(), b.getText())));
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
	    objectView.getEngine().setJavaScriptEnabled(true);
	    listGrid.prefHeightProperty().bind(root.heightProperty().multiply(.8));
	    listGrid.maxWidthProperty().bind(root.widthProperty().subtract(2));
    }

    public TableView<ModVersion> getListView() { return listView; }

    private synchronized void updateObjectView(String n){
        System.out.println(root.prefWidthProperty().getValue());
        System.out.println(root.widthProperty().getValue());
        System.out.println(root.widthProperty().multiply(.9).getValue());
        System.out.println(listGrid.widthProperty().getValue());

        objectView.getEngine().loadContent("<style>" +
            "body{background-color:#303030; color:#ddd;}" +
            "img{max-width:100%;height:auto;}" +
            "a{color:#ff9000;text-decoration:none;} a:visited{color:#544316;}" +
            "</style>" + n);
    }

    @FXML
    public void setColumns(Event e){ ModUtils.getInstance().CONFIG.writeColumnOrder(listView.getColumns()); }

    @FXML
    public void searchForUpdates(Event e){
        if(checking == null || !checking.isAlive()){
            checking = new ModCheckingThread(new ArrayList<>(listView.getItems()), Startup.MINECRAFT_VERSION, l -> {
                //do something with the returned list
                return null;
            } );
            checking.start();
        }else{
            AppLogger.info("checking is still alive!", getClass());
        }
    }

    @FXML
    public void showBadJars(Event e) throws IOException {
        Scene modal = new Scene(new FXMLLoader(getResource("")).load());
    }
}
