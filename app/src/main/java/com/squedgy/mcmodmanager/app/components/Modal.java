package com.squedgy.mcmodmanager.app.components;


import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.controllers.LoadingController;
import javafx.beans.value.ObservableNumberValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class Modal {

	private static Modal instance;
	@FXML
	public ScrollPane root;
	@FXML
	public VBox holder;
	private Stage stage;

	private Modal() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/modal.fxml"));
		loader.setController(this);
		loader.load();
		root.minWidthProperty().setValue(500);
		root.minHeightProperty().setValue(500);
//		root.setPadding(new Insets(5, 5, 5, 5));
	}

	public static Modal getInstance() throws IOException {
		if (instance == null) instance = new Modal();
		return instance;
	}

	public static Modal loading() throws IOException {
		Modal ret = getInstance();
		ret.setContent(new LoadingController().getRoot());
		ret.open(Startup.getParent().getWindow());
		return ret;
	}

	public void setContent(Control node) {
		holder.getChildren().setAll(node);
		node.prefWidthProperty().bind(holder.widthProperty());
		node.prefHeightProperty().bind(holder.heightProperty());
	}

	public void setContent(Region node) {
		holder.getChildren().setAll(node);
		node.prefWidthProperty().bind(holder.widthProperty());
		node.prefHeightProperty().bind(holder.heightProperty());
	}

	public void setContent(WebView node) {
		holder.getChildren().setAll(node);
		node.prefWidthProperty().bind(holder.prefWidthProperty());
		node.prefHeightProperty().bind(holder.prefHeightProperty());
	}

	public void bindMinHeight(ObservableNumberValue v) {
		root.minHeightProperty().bind(v);
	}

	public void bindMinWidth(ObservableNumberValue v) {
		root.minWidthProperty().bind(v);
	}

	public ScrollPane getRoot() {
		return root;
	}

	public void open(Window owner) {
		setUp(owner);
		if (stage.isShowing()) stage.close();
		stage.show();
	}

	public void setAfterClose(EventHandler<WindowEvent> e) {
		if (stage != null) stage.onCloseRequestProperty().setValue(e);
	}

	public void openAndWait(Window window) {
		setUp(window);
		if (stage.isShowing()) stage.close();
		stage.showAndWait();
	}

	private void setUp(Window window) {
		if (stage == null) {
			stage = new Stage();
			stage.onCloseRequestProperty().set(e -> stage.close());
			Scene scene = new Scene(root);
			scene.setRoot(root);
			stage.setScene(scene);
			stage.initOwner(window);
			stage.initModality(Modality.APPLICATION_MODAL);
		}
		if (!stage.isShowing()) {
			stage.setMinHeight(root.getMinHeight());
			stage.setMinWidth(root.getMinWidth());
		}
	}

	public void close() {
		stage.close();
	}

}
