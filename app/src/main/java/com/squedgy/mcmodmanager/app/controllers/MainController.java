package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import com.squedgy.mcmodmanager.app.components.DisplayVersion;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.threads.ModLoadingThread;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import com.squedgy.mcmodmanager.app.util.JavafxUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Callback;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class MainController {

	private static final String TABLE_NAME = "home";
	private static MainController instance;
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

	private MainController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("main.fxml"));
		loader.setController(this);
		loader.load();
	}

	public static MainController getInstance() throws IOException {
		if (instance == null) instance = new MainController();
		return instance;
	}

	@FXML
	public void initialize() throws IOException {
		//Set the default view to a decent looking background
		updateObjectView("<h1>&nbsp;</h1>");
		objectView.getEngine().setJavaScriptEnabled(true);
		loadMods();
	}

	public void loadMods() throws IOException {
		root.setContent(new LoadingController().getRoot());
		PathUtils.ensureMinecraftDirectory();

		ModLoadingThread t = new ModLoadingThread((mods) -> {
			try {
				initializeTable(mods);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Platform.runLater(() -> {
				listGrid.add(table.getRoot(), 0, 0);
				badJars.setVisible(ModUtils.viewBadJars().size() > 0);
				root.setContent(content);
				Platform.runLater(() -> Startup.getParent().getWindow().centerOnScreen());
			});
			return null;
		});
		t.start();
	}

	private void initializeTable(List<ModVersion> mods) throws IOException{
		table = new ModVersionTableController(TABLE_NAME, mods.toArray(new ModVersion[0]));

		//Add the active/deactive image column here
		TableColumn<DisplayVersion, ImageView> col = new TableColumn<>();
		Callback<TableColumn.CellDataFeatures<DisplayVersion, ImageView>, ObservableValue<ImageView>> imageCellFactory = i -> new SimpleObjectProperty<>(i.getValue().getImage());
		col.setCellValueFactory(imageCellFactory);
		table.addColumn(0, col);

		//The following two are made in code as
		//it could potentially not be necessary
		//for these to exist elsewhere
		//(De)activation toggling
		table.setOnDoubleClick(mod -> {
			ModUtils utils = ModUtils.getInstance();
			try {
				if (utils.modActive(mod)) {
					utils.deactivateMod(mod);
				} else {
					utils.activateMod(mod);
				}
			}catch(Exception e){
				throw new RuntimeException(e);
			}
			return null;
		});

		//Selection updating
		table.addOnChange((obs, old, neu) -> {
			updateObjectView("<h1>Loading...</h1>");
			ModInfoThread gathering = new ModInfoThread(neu, version -> {
				Platform.runLater(() -> updateObjectView(version.getDescription()));
				return null;
			}, n -> {
				Platform.runLater(() -> updateObjectView(("<h2>Error Loading, couldn't find a description!</h2>")));
				return null;
			});
			JavafxUtils.putSetterAndStart(objectView, gathering);
		});
	}

	public ScrollPane getRoot() { return root; }

	public List<ModVersion> getItems() { return table.getItems(); }

	public void setItems(List<ModVersion> mods) { table.setItems(FXCollections.observableArrayList(mods.stream().map(DisplayVersion::new).collect(Collectors.toList()))); }

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
	public void setColumns(Event e) { ModUtils.getInstance().CONFIG.writeColumnOrder(TABLE_NAME, table.getColumns()); }

	public void updateModList() { setItems(Arrays.asList(ModUtils.getInstance().getMods())); }

	@FXML
	public void searchForUpdates(Event e) {
		Modal modal;

		try {
			modal = Modal.loading();
		} catch (IOException e1) {
			throw new RuntimeException();
		}

		if (checking == null || !checking.isAlive()) {

			checking = new ModCheckingThread( l -> {
				//do something with the returned list
				Platform.runLater(() -> {

					ModUpdaterController table;
					try {
						table = new ModUpdaterController(l);
					} catch (IOException e1) {
						throw new RuntimeException();
					}
					modal.setContent(table.getRoot());
					modal.openAndWait(Startup.getParent().getWindow());

				});
				return null;
			});
			checking.start();
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
		m.setAfterClose(e2 -> {
			try {
								m.close();
				loadMods();
			} catch (IOException e1) {
				AppLogger.error(e1, getClass());
			}
		});

		m.openAndWait(Startup.getParent().getWindow());
	}

	@FXML
	public void newMods(Event e) throws IOException {
		Modal m = Modal.getInstance();
		m.setContent(new NewModsController().getRoot());
		m.openAndWait(Startup.getParent().getWindow());
	}

}
