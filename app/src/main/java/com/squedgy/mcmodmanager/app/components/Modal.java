package com.squedgy.mcmodmanager.app.components;


import com.squedgy.mcmodmanager.AppLogger;
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

	private Modal(Window window) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/modal.fxml"));
		loader.setController(this);
		loader.load();
		setUp(window);
	}

	public static Modal getInstance() throws IOException{
		return getInstance(null);
	}

	public static Modal getInstance(Window window) throws IOException {
		if (instance == null) instance = new Modal(window);
		return instance;
	}

	private static void reset() {
		try {
			Modal m = getInstance();
			m.getWindow().onCloseRequestProperty().setValue(null);
		} catch (IOException e) {
			AppLogger.error(e.getMessage(), Modal.class);
		}
	}

	public Window getWindow(){
		if(stage == null) return null;
		return stage.getOwner();
	}

	public static Modal loading() throws IOException {
		Modal ret = getInstance();
		ret.setContent(new LoadingController().getRoot());
		ret.open(Startup.getParent().getWindow());
		return ret;
	}

	public void setContent(Control node) {
		if(node.minWidthProperty().get() > 50) bindMinWidth(node.minWidthProperty().add(30));
		if(node.minHeightProperty().get() > 50)bindMinHeight(node.minHeightProperty().add(40));
		holder.getChildren().setAll(node);
		node.prefWidthProperty().bind(holder.widthProperty());
		node.prefHeightProperty().bind(holder.heightProperty());
	}

	public void setContent(Region node) {
		if(node.minWidthProperty().get() > 50) bindMinWidth(node.minWidthProperty().add(30));
		if(node.minHeightProperty().get() > 50)bindMinHeight(node.minHeightProperty().add(40));
		holder.getChildren().setAll(node);
		node.prefWidthProperty().bind(holder.widthProperty());
		node.prefHeightProperty().bind(holder.heightProperty());
	}

	public void setContent(WebView node) {
		if(node.minWidthProperty().get() > 50) bindMinWidth(node.minWidthProperty().add(30));
		if(node.minHeightProperty().get() > 50)bindMinHeight(node.minHeightProperty().add(40));
		holder.getChildren().setAll(node);
		node.prefWidthProperty().bind(holder.widthProperty());
		node.prefHeightProperty().bind(holder.heightProperty());
	}

	public void bindMinHeight(ObservableNumberValue v) {
		if(stage != null) stage.minHeightProperty().bind(v);
	}

	public void bindMinWidth(ObservableNumberValue v) {
		if(stage != null) stage.minWidthProperty().bind(v);
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
		if(e == null) stage.onCloseRequestProperty().setValue(null);
		else if (stage != null){
			stage.onCloseRequestProperty().setValue(ev -> {
				e.handle(ev);
				reset();
			});
		}else {
			AppLogger.debug("STAGE NULL", getClass());
		}
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
			stage.setMinWidth(200);
			stage.setMinHeight(150);
			stage.setScene(scene);
			stage.initOwner(window);
			stage.initModality(Modality.APPLICATION_MODAL);
		}
	}

	public void close() {
		stage.close();
	}

}
