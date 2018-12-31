package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.threads.ModUpdaterThread;
import com.squedgy.mcmodmanager.app.threads.ThreadFailedException;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class ModUpdaterController {

	@FXML
	public VBox root;
	@FXML
	public HBox buttons;

	public ModVersionTableController table;
	public ModUpdaterThread updates;

	public ModUpdaterController(List<ModVersion> updates) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/updates.fxml"));
		loader.setController(this);
		loader.load();
		table = new ModVersionTableController(updates.toArray(new ModVersion[0]));
		root.getChildren().add(table.getRoot());
	}

	@FXML
	public void updateAll(Event e) throws IOException {

		if (updates == null || !updates.isAlive()) {
			Modal modal = Modal.getInstance();
			modal.setContent(new LoadingController().getRoot());
			modal.open(Startup.getParent().getWindow());
			updates = new ModUpdaterThread(table.getItems(), results -> {
				VBox v = new VBox();
				System.gc();

				results.forEach((key, value) -> {
					if (value.isResult()) {
						try {
							ModVersion old = ModUtils.getInstance().getMod(key.getModId());
							if(old == null){
							}else {
							}
							File newJar = new File(Startup.getModsDir() + File.separator + key.getFileName());
							if(old != null) {
								File f = new File(Startup.getModsDir() + File.separator + old.getFileName());
								value.setResult(f.delete());
								value.setReason(value.isResult() ? "Succeeded!" : "couldn't delete the old file");
							}
							if (old == null || !value.isResult()) {
								value.setResult(false);
								if(newJar.delete()){
									value.setReason("Couldn't locate/delete previous file, deleted new one to ensure runnability of MC");
								}else{
									value.setReason("Couldn't delete the new file after not locating/deleting the old.\nYou should delete " + Startup.getModsDir() + File.separator + key.getFileName() + " to have no issues.");
								}
							}
						} catch (Exception e1) {}

					}
					v.getChildren().add(new Label(key.getModId() + ": " + value.getReason()));
				});
				ModUtils.getInstance().setMods();
				Platform.runLater(() -> {
					modal.setContent(v);
					try {
						Startup.getInstance().getMainView().updateModList();
					} catch (IOException ig) {
					}
				});
				return null;
			});

			updates.start();
		}
	}

	public VBox getRoot() {
		return root;
	}

}
