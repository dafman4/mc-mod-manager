package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.MainController;
import com.squedgy.mcmodmanager.app.config.VersionTableOrder;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import java.util.ArrayList;


public class TableViewController {

    @FXML
    private TableView<ModVersion> listView;
    @FXML
    private Button columns;
    @FXML
    private Button updates;
    private ModCheckingThread checking;

//    @FXML
    public void setListView(){
        listView.setItems(FXCollections.observableArrayList(MainController.getMods()));
        listView.refresh();
    }

    @FXML
    public void initialize() {
        setListView();
        listView.getColumns().setAll(listView.getColumns().sorted( (a, b) -> VersionTableOrder.compareColumns(a.getText(), b.getText())));
        listView.refresh();
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, neu) -> {

        });
    }

    public TableView<ModVersion> getListView() { return listView; }

    @FXML
    public void setColumns(Event e){
        VersionTableOrder.writeColumnOrder(listView.getColumns());
    }

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
