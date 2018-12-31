package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.components.PublicNode;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.threads.ModLoadingThread;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class TableViewController {

	private ModVersionTableController table;
	@FXML
	private Button columns;
	@FXML
	private Button updates;
	@FXML
	private Button badJars;
	@FXML
	private WebView objectView;
	@FXML
	private GridPane listGrid;
	@FXML
	private ScrollPane root;
	@FXML
	private VBox content;
	@FXML
	private HBox buttons;

	private ModCheckingThread checking;
	private ModInfoThread gathering;

	public TableViewController() throws IOException {

		StackTraceElement e = Thread.currentThread().getStackTrace()[4];
		System.out.println(e.getFileName() + ": " + e.getLineNumber());

		FXMLLoader loader = new FXMLLoader(getResource("main.fxml"));
		loader.setController(this);
		loader.load();


		root.setContent(new LoadingController().getRoot());
		ModLoadingThread t = new ModLoadingThread((mods) -> {
			try {
				table = new ModVersionTableController(mods.toArray(new ModVersion[0]));
			} catch (IOException ignored) {
			}

			table.addOnChange((obs, old, neu) -> {
				updateObjectView("<h1>Loading...</h1>");
				if (gathering == null || !gathering.isAlive()) {
					System.out.println("gathering" + neu);
					gathering = new ModInfoThread(neu, version -> {
						Platform.runLater(() -> updateObjectView(version.getDescription()));
						return null;
					}, n -> {
						Platform.runLater(() -> updateObjectView(("<h2>Error Loading, couldn't find a description!</h2>")));
						return null;
					});
					gathering.start();
				}
			});
			Platform.runLater(() -> {
				listGrid.add(table.getRoot(), 0, 0);
				badJars.setVisible(ModUtils.viewBadJars().size() > 0);
				//Set the default view to a decent looking background
				objectView.getEngine().setJavaScriptEnabled(true);
				listGrid.prefHeightProperty().bind(root.heightProperty().multiply(.8));
				listGrid.maxWidthProperty().bind(root.widthProperty().subtract(2));
				updateObjectView("<h1>&nbsp;</h1>");
				root.setContent(content);
				Platform.runLater(() -> {
					Startup.getParent().getWindow().setHeight(content.heightProperty().getValue());
					Startup.getParent().getWindow().setWidth(content.widthProperty().getValue() + 20);
					Startup.getParent().getWindow().centerOnScreen();
				});
			});
			return null;
		});
		t.start();
	}

	public ScrollPane getRoot() {
		return root;
	}

	public void setItems(List<ModVersion> mods) {
		table.setItems(FXCollections.observableArrayList(mods));
	}


	private synchronized void updateObjectView(String description) {
		objectView.getEngine().loadContent(
			"<style>" +
				"body{background-color:#303030; color:#ddd;}" +
				"img{max-width:100%;height:auto;}" +
				"a{color:#ff9000;text-decoration:none;} " +
				"a:visited{color:#544316;}" +
				"</style>" + description);
	}

	@FXML
	public void setColumns(Event e) {
		ModUtils.getInstance().CONFIG.writeColumnOrder(table.getColumns());
	}

	public void updateModList() {
		table.setItems(FXCollections.observableArrayList(ModUtils.getInstance().getMods()));
	}

	@FXML
	public void searchForUpdates(Event e) {
		if (checking == null || !checking.isAlive()) {
			Modal modal;
			LoadingController c;
			try {
				modal = new Modal();
				c = new LoadingController();
			} catch (IOException e1) {
				throw new RuntimeException();
			}

			modal.setContent(c.getRoot());
			modal.open(Startup.getParent().getWindow());

			checking = new ModCheckingThread(new ArrayList<>(table.getItems()), Startup.MINECRAFT_VERSION, l -> {
				//do something with the returned list
				Platform.runLater(() -> {

					ModUpdaterController table;
					try {
						table = new ModUpdaterController(l);
					} catch (IOException e1) {
						throw new RuntimeException();
					}
					modal.close();
					modal.setContent(table.getRoot());
					modal.openAndWait(Startup.getParent().getWindow());

				});
				return null;
			});
			checking.start();
		} else {
			AppLogger.info("checking is still alive!", getClass());
		}
	}

	@FXML
	public void showBadJars(Event e) throws IOException {

		Modal m = new Modal();

		TableView<PublicNode> t = new TableView<>();
		ObservableList<PublicNode> list = FXCollections.observableArrayList(ModUtils.viewBadJars().entrySet().stream().map(PublicNode::new).collect(Collectors.toList()));
		t.setItems(list);
		ObservableList<TableColumn<PublicNode, String>> columns = FXCollections.observableArrayList();

		TableColumn<PublicNode, String> toAdd = new TableColumn<>();
		toAdd.setText("File");
		toAdd.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().getKey()));
		columns.add(toAdd);
		toAdd = new TableColumn<>();
		toAdd.setText("Reason");
		columns.add(toAdd);
		toAdd.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().getValue()));
		t.getColumns().setAll(columns);
		t.refresh();

		m.setContent(t);
		m.open(Startup.getParent().getWindow());
		t.getStyleClass().add("mod-table");
	}

}
