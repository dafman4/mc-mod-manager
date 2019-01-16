package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.threads.ModUpdaterThread;
import com.squedgy.mcmodmanager.app.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class ModUpdaterController {

	private static final String TABLE_NAME = "updater";
	@FXML
	public VBox root;
	@FXML
	public TilePane buttons;
	@FXML
	public Button updateAll;
	public ModVersionTableController table;
	public ModUpdaterThread updates;

	public ModUpdaterController(List<ModVersion> updates) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/updates.fxml"));
		loader.setController(this);
		loader.load();
		table = new ModVersionTableController(TABLE_NAME, updates.toArray(new ModVersion[0]));
		root.getChildren().add(0, table.getRoot());
		VBox.setVgrow(table.getRoot(), Priority.ALWAYS);
		updateAll.setVisible(table.getItems().size() > 0);
	}

	@FXML
	public void close(Event e){
		try {
			Modal.getInstance().close();
		} catch (IOException e1) {
			AppLogger.error(e1.getMessage(), getClass());
		}
	}

	@FXML
	public void updateAll(Event e)  {
		try {
			Modal modal = Modal.loading();
			if (updates == null) {
				updates = new ModUpdaterThread(
					table.getItems(),
					results -> {
						TableView<Map.Entry<ModVersion, Result>> resultTable = getResultTable();
						System.gc();

						results.forEach((key, value) -> {
							if (value.isResult()) handleSuccessfulDownload(key, value);
							resultTable.getItems().add(new AbstractMap.SimpleEntry<>(key, value));
						});
						Platform.runLater(() -> {
							modal.setContent(resultTable);
							try {
								ModUtils.getInstance().setMods();
								Startup.getInstance().getMainView().updateModList();
							} catch (IOException ignored) {
							}
						});
						return null;
					});

			}
			if (!updates.isAlive()) updates.start();
		} catch (IOException e1) {
			AppLogger.error(e1.getMessage(), getClass());
		}
	}

	private void handleSuccessfulDownload(ModVersion key, Result value) {
		ModVersion old = ModUtils.getInstance().getMod(key.getModId());

		File newMod = new File(PathUtils.findModLocation(key));
		File oldMod = new File(PathUtils.findModLocation(old));

		AppLogger.info("Old File ||| New File\n" + oldMod.getAbsolutePath() + "|||" + newMod.getAbsolutePath(), getClass());

		value.setResult(oldMod.delete());
		value.setReason(value.isResult() ? "Succeeded!" : "couldn't delete the old file");
		if (!value.isResult()) {
			if (newMod.delete()) {
				value.setReason("Couldn't locate/delete previous file, deleted new one to ensure runnability of MC");
			} else {
				value.setReason("Couldn't delete the new file after not locating/deleting the old.\n" +
					"You should delete " + oldMod.getAbsolutePath() + " to have no issues AND keep the new mod.\n" +
					"Otherwise delete " + newMod.getAbsolutePath() + " to keep the old version.");
			}
		}

		if (value.isResult()) {
			String jarId;
			try {
				jarId = ModUtils.getJarModId(new JarFile(newMod));
			} catch (IOException e1) {
				jarId = old.getModId();
			}
			ModUtils.getInstance().addMod(jarId, key, true);
		}
	}

	private TableView<Map.Entry<ModVersion, Result>> getResultTable() {
		TableView<Map.Entry<ModVersion, Result>> ret = new TableView<>();
		ret.getStyleClass().addAll("all-padding", "mod-table");
		TableColumn<Map.Entry<ModVersion, Result>, ImageView> image = JavafxUtils.makeColumn("Succeeded", e -> {
			ImageUtils u = ImageUtils.getInstance();
			boolean succeed = e.getValue().getValue().isResult();
			return new SimpleObjectProperty<>(new ImageView(succeed ? u.GOOD : u.BAD));
		});
		TableColumn<Map.Entry<ModVersion, Result>, String> mod = JavafxUtils.makeColumn("Mod", e -> new SimpleStringProperty(e.getValue().getKey().getModName()));
		TableColumn<Map.Entry<ModVersion, Result>, String> reason = JavafxUtils.makeColumn("Reason", e -> {
			boolean succeed = e.getValue().getValue().isResult();

			return new SimpleStringProperty(succeed ? "Succeeded" : e.getValue().getValue().getReason());
		});

		ret.getColumns().addAll(
			image,
			mod,
			reason
		);

		return ret;
	}

	public VBox getRoot() {
		return root;
	}

}
