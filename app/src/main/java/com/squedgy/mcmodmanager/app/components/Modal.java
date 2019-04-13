package com.squedgy.mcmodmanager.app.components;


import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.controllers.LoadingController;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;

import java.io.IOException;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;
import static org.slf4j.LoggerFactory.getLogger;

public class Modal {

	private static final Logger log = getLogger(Modal.class);
	private static Modal instance;
	@FXML
	private VBox root;
	@FXML
	private ScrollPane holder;
	@FXML
	private TilePane footer;
	private static final EventHandler<WindowEvent> DEFAULT_ACTION = e -> reset(instance.stage);
	private Stage stage;

	private Modal(Window window) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/modal.fxml"));
		loader.setController(this);
		loader.load();
		setUp(window);
		setAfterClose(DEFAULT_ACTION);
	}

	@FXML
	public void close(Event e){
		close();
	}

	public void setFooter(HBox... nodes){
		footer.getChildren().setAll(footer.getChildren().get(0));
		footer.getChildren().addAll(nodes);
		for(HBox box : nodes) box.setAlignment(Pos.CENTER);
		footer.setPrefColumns(footer.getChildren().size());
		footer.prefTileWidthProperty().bind(footer.prefWidthProperty().divide(footer.getChildren().size()).subtract(5));
		if(nodes.length > 0) nodes[nodes.length-1].alignmentProperty().setValue(Pos.CENTER_RIGHT);
	}

	public static Modal getInstance() throws IOException{
		return getInstance(null);
	}

	public static Modal getInstance(Window window) throws IOException {
		if (instance == null) instance = new Modal(window);
		return instance;
	}

	private static void reset(Stage stage) {
		try {
			Modal m = getInstance();
			m.setFooter();
			m.setMaxHeight(null);
			m.setMaxWidth(null);
			m.bindMinHeight(new SimpleDoubleProperty(150));
			m.bindMinWidth(new SimpleDoubleProperty(200));
			stage.onHidingProperty().setValue(DEFAULT_ACTION);
		} catch (IOException e) {
			log.error(e.getMessage(), Modal.class);
		}
	}

	public Window getWindow(){
		if(stage == null) return null;
		return stage.getOwner();
	}

	public static Modal loading() throws IOException {
		Modal ret = getInstance();
		reset(ret.stage);
		ret.setContent(new LoadingController().getRoot());
		ret.open(Startup.getParent().getWindow());
		return ret;
	}

	public void setContent(Control node) {
		Platform.runLater(() -> {
			if(node.minWidthProperty().get() > 200) bindMinWidth(node.minWidthProperty().add(20));
			if(node.minHeightProperty().get() > 150)bindMinHeight(node.minHeightProperty().add(40).add(45));
			holder.setContent(node);
			node.prefWidthProperty().bind(holder.widthProperty());
		});
	}

	public void setContent(Region node) {
		Platform.runLater(() -> {
			if(node.minWidthProperty().get() > 200) bindMinWidth(node.minWidthProperty().add(20));
			if(node.minHeightProperty().get() > 150)bindMinHeight(node.minHeightProperty().add(40).add(45));
			holder.setContent(node);
			node.prefWidthProperty().bind(holder.widthProperty());
		});
	}

	public void setContent(WebView node) {
		Platform.runLater(() -> {
			if(node.minWidthProperty().get() > 200) bindMinWidth(node.minWidthProperty().add(20));
			if(node.minHeightProperty().get() > 150)bindMinHeight(node.minHeightProperty().add(40).add(45));
			holder.setContent(node);
			node.prefWidthProperty().bind(holder.widthProperty());
//			node.minHeightProperty().bind(holder.prefViewportHeightProperty().subtract(10));
		});
	}

	private void bindMinHeight(ObservableNumberValue v) {
		if(stage != null) stage.minHeightProperty().bind(v);
	}

	private void bindMinWidth(ObservableNumberValue v) {
		if(stage != null) stage.minWidthProperty().bind(v);
	}

	public VBox getRoot() {
		return root;
	}

	public void setAfterClose(EventHandler<WindowEvent> e) {
		if(e == null) stage.onHidingProperty().setValue(DEFAULT_ACTION);
		else if (stage != null){
			stage.onHidingProperty().setValue(ev -> {
				e.handle(ev);
				reset(stage);
			});
		}else {
			log.debug("STAGE NULL");
		}
	}

	public void open(){
		if (!stage.isShowing()) stage.show();
	}

	public void open(Window owner) {
		open();
	}

	public void openAndWait(){
		if (!stage.isShowing()) stage.showAndWait();
	}

	public void openAndWait(Window window) {
		openAndWait();
	}

	public void setMaxHeight(Double height){
		if(height != null && height < 50) height =50.;
		if(height == null) stage.maxHeightProperty().bind(new SimpleDoubleProperty(Double.MAX_VALUE));
		else stage.maxHeightProperty().bind(new SimpleDoubleProperty(height + 40 + 45));
	}

	public void setMaxWidth(Double width){
		if(width != null && width < 200) width = 200.;
		if(width == null) stage.maxWidthProperty().bind(new SimpleDoubleProperty(Double.MAX_VALUE));
		else stage.maxWidthProperty().bind(new SimpleDoubleProperty(width + 20));
	}

	private void setUp(Window window) {
		if (stage == null) {
			stage = new Stage();
			stage.onHidingProperty().set(DEFAULT_ACTION);
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
