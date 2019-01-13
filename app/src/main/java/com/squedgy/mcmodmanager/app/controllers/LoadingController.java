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
		root.getEngine().loadContent(
			"<style>" +
				"body{" +
				"background-color:#222;" +
				"color:#ccc;" +
				"} " +
				"@keyframes spin{" +
				"from{transform:rotate(0deg);} " +
				"to{transform:rotate(360deg);} " +
				"} " +
				".img {" +
				"background: url(\"" + getResource("components/img/loading.svg") + "\") center center no-repeat;" +
				"background-size:cover;" +
				"animation:spin 3s linear infinite;" +
				"min-height:100px;" +
				"min-width:100px;" +
				"max-height:40rem;" +
				"max-width:40rem;" +
				"} " +
				"</style>" +
				"<div style=\"display:flex;justify-content:center;align-items:center;height:100%;\">" +
				"<div class='img' id='img'></div>" +
				"</div>"
		);
	}

	public WebView getRoot() {
		return root;
	}

}
