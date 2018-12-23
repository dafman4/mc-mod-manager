package com.squedgy.mcmodmanager.app.components;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class MainScene extends HBox {

    public MainScene(){
        final Label l = new Label("Mods");

        this.getChildren().addAll(
                l
        );
    }

}
