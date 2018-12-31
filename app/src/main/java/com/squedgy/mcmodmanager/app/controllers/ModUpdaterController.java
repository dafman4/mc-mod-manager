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
			Modal modal = new Modal();
			modal.setContent(new LoadingController().getRoot());
			modal.open(Startup.getParent().getWindow());
			updates = new ModUpdaterThread(table.getItems(), results -> {
				//do other stuff as necessary later for now just let me know it stopped
				VBox v = new VBox();
				System.gc();

				results.forEach((key, value) -> {
					if (value.isResult()) {
						try {
							String fileName = Startup.getInstance().getMainView().getItems().stream().filter(id -> id.getModId().equals(key.getModId())).findFirst().orElseThrow(() -> new ModIdNotFoundException("")).getFileName();
							File newJar = new File(Startup.getModsDir() + File.separator + key.getFileName());

							File f = new File(Startup.getModsDir() + File.separator + fileName);
							value.setResult(f.delete());
							value.setReason(value.isResult() ? "Succeeded!" : "failed: couldn't delete the old file");
							if (!value.isResult()) {
								value.setReason(newJar.delete() ? value.getReason() : value.getReason() + " and I couldn't delete the new file");
							} else {
								try {
									Config.getInstance().getCachedMods().addMod(Cacher.getJarModId(new JarFile(newJar)), key);
								} catch (IOException e1) {
									AppLogger.error(e1, getClass());
								}
							}
						} catch (ModIdNotFoundException ignored) { } catch (IOException e1) {
							throw new ThreadFailedException();
						}

					}
					v.getChildren().add(new Label(key.getModId() + ": " + value.getReason()));
				});
				try { Config.getInstance().getCachedMods().writeCache(); }
				catch (IOException e1) { AppLogger.error(e1, getClass());}
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
