package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.threads.ModLoadingThread;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class TableViewController {

	private static TableViewController instance;
	private ModVersionTableController table;
	@FXML
	private MenuItem badJars;
	@FXML
	private WebView objectView;
	@FXML
	private GridPane listGrid;
	@FXML
	private ScrollPane root;
	@FXML
	private VBox content;
	private ModCheckingThread checking;
	private ModInfoThread gathering;

	private TableViewController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("main.fxml"));
		loader.setController(this);
		loader.load();
	}

	public static TableViewController getInstance() throws IOException {
		if (instance == null) instance = new TableViewController();
		return instance;
	}

	@FXML
	public void initialize() throws IOException {
		loadMods();
	}

	public void loadMods() throws IOException {
		WebView w = new LoadingController().getRoot();

		root.setContent(w);
		ModLoadingThread t = new ModLoadingThread((mods) -> {
			try {
				table = new ModVersionTableController(mods.toArray(new ModVersion[0]));
			} catch (IOException ignored) {
			}

			table.addOnChange((obs, old, neu) -> {
				updateObjectView("<h1>Loading...</h1>");
				if ((gathering == null || !gathering.isAlive()) && neu != null) {
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

	public List<ModVersion> getItems() {
		return table.getItems();
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
		setItems(Arrays.asList(ModUtils.getInstance().getMods()));
	}

	@FXML
	public void searchForUpdates(Event e) {
		if (checking == null || !checking.isAlive()) {
			Modal modal;
			LoadingController c;
			try {
				modal = Modal.getInstance();
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

		}
	}

	@FXML
	public void showBadJars(Event e) throws IOException {

		Modal m = Modal.getInstance();
		BadJarsController c = new BadJarsController();
		m.setContent(c.getRoot());
		m.open(Startup.getParent().getWindow());

	}

	@FXML
	public void setJarIds(Event e) throws IOException {
		Modal m = Modal.getInstance();

		m.setContent(new SetJarIdController().getRoot());

		m.openAndWait(Startup.getParent().getWindow());
		m.setAfterClose(e2 -> {
			try {
				loadMods();
			} catch (IOException e1) {
				AppLogger.error(e1, getClass());
			}
		});
	}

	@FXML
	public void newMods(Event e) throws IOException {
		Modal m = Modal.getInstance();
		m.setContent(new NewModsController().getRoot());
		m.openAndWait(Startup.getParent().getWindow());
	}

}
