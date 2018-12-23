package com.squedgy.mcmodmanager.app.components.pieces;

import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.io.IOException;

public class ModVersionData {

    @FXML
    private HBox hbox;
    @FXML
    private Label name;
    @FXML
    private Label modId;
    @FXML
    private Label fileName;
    @FXML
    private Label mcVersion;

    public ModVersionData(){
        FXMLLoader loader = new FXMLLoader(Thread.currentThread().getContextClassLoader().getResource("components/modVersion.fxml"));
        loader.setController(this);

        try { loader.load(); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public void setData(ModVersion info){
        name.setText(info.getModName());
        modId.setText(info.getModId());
        fileName.setText(info.getFileName());
        mcVersion.setText(info.getMinecraftVersion());
    }

    public HBox getBox(){ return hbox; }

}
