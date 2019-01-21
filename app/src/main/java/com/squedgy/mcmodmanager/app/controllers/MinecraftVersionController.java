package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.PriorityQueue;
import java.util.Queue;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class MinecraftVersionController {

	@FXML
	private ChoiceBox<String> choices;
	@FXML
	private VBox root;
	private Thread storage;
	private Queue<Runnable> movings = new PriorityQueue<>();
	private boolean changed = false;

	public MinecraftVersionController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/version.fxml"));
		loader.setController(this);
		loader.load();
		choices.setItems(FXCollections.observableArrayList(PathUtils.getPossibleMinecraftVersions()));
		choices.setValue(Config.getMinecraftVersion());

		Button button = new Button("Set Version");
		button.onMouseReleasedProperty().setValue(this::setVersion);

		Modal modal = Modal.getInstance();
		modal.setFooter(new HBox(button));
		modal.setMaxHeight(root.maxHeightProperty().doubleValue());
		modal.setMaxWidth(root.maxWidthProperty().doubleValue());
	}

	public VBox getRoot() { return root; }

	public void setVersion(Event e){
		if(!choices.getValue().equals(Config.getMinecraftVersion())){
			changed = true;
			try {
				MainController.getInstance().getRoot().getChildren().setAll(new LoadingController().getRoot());
			} catch (IOException e1) {
				AppLogger.error(e1, getClass());
			}
			Runnable t = () -> {
				File mods = new File(PathUtils.getModsDir());
				File currentStorage = new File(PathUtils.getStorageDir());
				Config.setMinecraftVersion(choices.getValue());
				File newStorage = new File(PathUtils.getStorageDir());
				try {
					System.gc();
					for (File file : mods.listFiles()) {
						try { Files.move(file.toPath(), currentStorage.toPath().resolve(file.getName())); }
						catch (IOException e1) { AppLogger.error(e1, getClass()); }
					}
					System.gc();
					if (newStorage.exists() && newStorage.isDirectory()) {
						for (File file : newStorage.listFiles()) {
							try { Files.move(file.toPath(), mods.toPath().resolve(file.getName())); }
							catch (IOException e1) { AppLogger.error(e1,getClass()); }
						}
					}
					ModUtils.getInstance().setMods();
					Platform.runLater(() -> {
						try {
							MainController.getInstance().loadMods();
						} catch (IOException e1) {
							AppLogger.error(e1, getClass());
						}
					});
				}catch(NullPointerException e1){
					AppLogger.error(e1, getClass());
				}
				Config.getInstance().writeProps();
			};
			movings.add(t);
			if(storage == null || !storage.isAlive() || storage.isInterrupted()){
				storage = new Thread(() -> {
					while(movings.peek() != null){
						movings.remove().run();
					}
				});
				storage.start();
			}
		}
		try {
			Modal.getInstance().close();
		} catch (IOException e1) {
			AppLogger.error(e1, getClass());
		}
	}

}
