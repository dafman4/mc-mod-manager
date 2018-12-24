package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.StartUp;
import com.squedgy.mcmodmanager.app.config.VersionTableOrder;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;


public class TableViewController {

    @FXML
    private TableView<ModVersion> listView;

//    @FXML
    public void setListView(){
        listView.setItems(FXCollections.observableArrayList(StartUp.getMods()));
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

    public TableView<ModVersion> getListView() {
        return listView;
    }
}
