package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class SetJarIdController {

	@FXML
	public VBox root;

	public SetJarIdController() throws IOException {
		FXMLLoader loader = new FXMLLoader(Startup.getResource("components/jar-ids.fxml"));
		loader.setController(this);
		loader.load();
	}

	@FXML
	public void initialize(){
		ModUtils.viewBadJars().forEach((id, reason) -> {
			if(!reason.equals(ModUtils.NO_MOD_INFO)){
				new HBox()
			}
		});
	}

}
