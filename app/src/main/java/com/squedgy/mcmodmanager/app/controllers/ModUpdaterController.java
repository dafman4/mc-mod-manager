package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.threads.ModUpdaterThread;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class ModUpdaterController {

    @FXML
    public VBox root;
    @FXML
    public HBox buttons;

    public ModVersionTableController table;
    public ModUpdaterThread updates;

    public ModUpdaterController(List<ModVersion> updates) throws IOException {
        FXMLLoader loader = new FXMLLoader(getResource("components/updates.fxml"));
        loader.setController(this);
        loader.load();
        table = new ModVersionTableController(updates.toArray(new ModVersion[0]));
        root.getChildren().add(table.getRoot());
    }

    @FXML
    public void updateAll(Event e){
        System.out.println("Update All Called");
        if(updates == null || !updates.isAlive()){
            updates = new ModUpdaterThread(table.getItems(), results -> {

                //do other stuff as necessary later for now just let me know it stopped
                Platform.runLater(() -> {
                    try {
                        Modal modal = new Modal();
                        VBox v = new VBox();
                        results.entrySet().forEach(entry -> {
                            v.getChildren().add(new Label(entry.getKey().getModId() + ": " + entry.getValue()));
                        });
                        modal.setContent(v);
                        modal.open(Startup.getParent().getWindow());
                    }
                    catch (IOException e1) { throw new RuntimeException(); }
                });
                return null;
            });

            updates.start();
        }
    }

    public VBox getRoot(){ return root; }

}
