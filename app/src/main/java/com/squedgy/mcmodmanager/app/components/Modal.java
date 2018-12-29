package com.squedgy.mcmodmanager.app.components;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class Modal {

    @FXML
    public ScrollPane root;
    @FXML
    public VBox holder;

    public Modal() throws IOException {
        FXMLLoader loader = new FXMLLoader(getResource("components/modal.fxml"));
        loader.setController(this);
        loader.load();
    }

    @FXML
    public void initialize(){
        holder.prefWidthProperty().bind(root.widthProperty().subtract(2));
        holder.prefHeightProperty().bind(root.heightProperty().subtract(2));
        root.minWidthProperty().setValue(500);
        root.minHeightProperty().setValue(300);
    }

    public void setContents(Node... nodes){ for(Node node : nodes) holder.getChildren().add(node); }

    public ScrollPane getRoot(){ return root; }

    public void openModal(){
        Scene s = new Scene(root);

    }

}
