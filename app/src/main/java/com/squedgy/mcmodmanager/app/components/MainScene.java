package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.sun.javafx.collections.ImmutableObservableList;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import java.awt.Label;
import java.util.LinkedList;
import java.util.List;

public class MainScene extends HBox {

    public MainScene(){
        final Label l = new Label("Mods");
        List<ModCell> cells = new LinkedList<>();
        ListView<ModCell> view = new ListView<>();

        Button button = new Button("Button");
        button.setOnAction( e -> {
            cells.add(new ModCell(new ModVersionFactory().withName("mod").withFileName("mod.jar").build()));
            view.setItems(new ImmutableObservableList<>(cells.toArray(new ModCell[0])));
            view.refresh();
        });
        ObservableList<ModCell> toView = new ImmutableObservableList<>();
        toView.addAll(cells);
        this.getChildren().addAll(
                button,
                new ListView<ModCell>(toView)
        );
    }

    public static class ModCell extends ListCell<ModVersion>{

        private ModVersion v;

        public ModCell(ModVersion v){
            super();
            this.v = v;
        }



    }

}
