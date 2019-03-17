package com.squedgy.mcmodmanager.app.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.web.WebView;

import java.io.IOException;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class LoadingController {

	@FXML
	private WebView root;

	public LoadingController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/loading.fxml"));
		loader.setController(this);
		loader.load();
		root.getEngine().load(getResource("components/pages/loading.html").toExternalForm());
	}

	public WebView getRoot() {
		return root;
	}

}
