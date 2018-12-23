package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.components.pieces.ModVersionData;
import javafx.scene.control.ListCell;

public class ModVersionCell extends ListCell<ModVersion> {

    @Override
    protected void updateItem(ModVersion item, boolean empty) {
        super.updateItem(item, empty);
        if(item != null){
            ModVersionData data = null;
            data = new ModVersionData();
            data.setData(item);
            setGraphic(data.getBox());
        }
    }
}
