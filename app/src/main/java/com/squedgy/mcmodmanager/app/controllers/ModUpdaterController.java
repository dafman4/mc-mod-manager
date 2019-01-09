package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.threads.ModUpdaterThread;
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
import java.util.List;
import java.util.jar.JarFile;

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
							if (old == null) {
							} else {
							}
							File newJar = new File(Startup.getModsDir() + File.separator + key.getFileName());
							AppLogger.info((old != null ? old.getFileName() : null) + "|||" + key.getFileName(), getClass());
							if (old != null) {
								File f = new File(Startup.getModsDir() + File.separator + old.getFileName());
								if (!f.exists()) {
									f = new File(Startup.getModsDir() + File.separator + old.getFileName().replace(' ', '+'));
								}
								value.setResult(f.delete());
								value.setReason(value.isResult() ? "Succeeded!" : "couldn't delete the old file");
							}
							if (old == null || !value.isResult()) {
								if (newJar.delete()) {
									value.setReason("Couldn't locate/delete previous file, deleted new one to ensure runnability of MC");
								} else {
									value.setReason("Couldn't delete the new file after not locating/deleting the old.\nYou should delete " + Startup.getModsDir() + File.separator + key.getFileName() + " to have no issues.");
								}
							}

							if(value.isResult()){
								String jarId = ModUtils.getJarModId(new JarFile(newJar));
								ModUtils.getInstance().addMod(jarId, key, true);
							}
						} catch (Exception e1) {
						}

					}
					v.getChildren().add(new Label(key.getModId() + " - " + (value.isResult() ? "Succeeded" : "Failed" + ": " + value.getReason())));
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
