package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.StartUp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;


public class ListViewController {

    private static final ListViewController instance = new ListViewController();

    @FXML
    private TableView<ModVersion> listView;

    @FXML
    public void setListView(){
        ObservableList<ModVersion> t = FXCollections.observableArrayList(StartUp.getMods());
        listView.setItems(t);
        listView.refresh();
//        listView.setCellFactory(cell -> new ModVersionCell());
    }

    @FXML
    public void initialize() { setListView(); }
}
