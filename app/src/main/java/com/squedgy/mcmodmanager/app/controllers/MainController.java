package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.DisplayVersion;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.threads.ModLoadingThread;
import com.squedgy.mcmodmanager.app.util.ImageUtils;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.Startup.DOT_MINECRAFT_LOCATION;
import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class MainController {

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
	private ModInfoThread gathering;

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
		loadMods();
	}

	public void loadMods() throws IOException {
		WebView w = new LoadingController().getRoot();
		while(DOT_MINECRAFT_LOCATION == null){
			DirectoryChooser fc = new DirectoryChooser();
			File location = fc.showDialog(null);
			if(location.exists() && Startup.allSubDirsMatch(location, "mods", "versions", "saves")){
				DOT_MINECRAFT_LOCATION = location.getAbsolutePath();
				ModUtils.getInstance().CONFIG.setProperty(Startup.CUSTOM_DIR, DOT_MINECRAFT_LOCATION);
				ModUtils.getInstance().CONFIG.writeProps();
			}
		}
		root.setContent(w);
		ModLoadingThread t = new ModLoadingThread((mods) -> {
			try {
				table = new ModVersionTableController(mods.toArray(new ModVersion[0]));

				TableColumn<DisplayVersion, ImageView> col = new TableColumn<>();
				Callback<TableColumn.CellDataFeatures<DisplayVersion, ImageView>, ObservableValue<ImageView>> meth = i ->{
					ImageView v = i.getValue().getImage();
					return new SimpleObjectProperty<>(v);
				};
				col.setCellValueFactory(meth);
				table.addColumn(0, col);

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

			} catch (IOException e) {
				throw new RuntimeException(e);
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
		table.setItems(FXCollections.observableArrayList(mods.stream().map(DisplayVersion::new).collect(Collectors.toList())));
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
