package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFoundConnectionFailed;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.IOException;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class SetJarIdController {

	@FXML
	private VBox root;
	private boolean updated;

	public SetJarIdController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/jar-ids.fxml"));
		loader.setController(this);
		loader.load();
		updated = false;
	}

	public boolean isUpdated() {
		return updated;
	}

	@FXML
	public void initialize() throws IOException {

		ModUtils.viewBadJars().forEach((id, reason) -> {
			if (!reason.equals(ModUtils.NO_MOD_INFO)) {
				ModVersion version = id.mod;
				if (version != null) {
					HBox holder = new HBox();

					Label label = new Label(version.getModName() + ":");
					HBox.setMargin(label, new Insets(5, 5, 5, 5));
					label.fontProperty().setValue(new Font(16));
					TextInputControl input = new TextField(version.getModId());
					HBox.setMargin(input, new Insets(5, 5, 5, 5));
					Button b = new Button("Save");
					HBox.setMargin(b, new Insets(5, 5, 5, 5));

					b.onMouseReleasedProperty().setValue(e -> onMouseReleased(e, id, input, b, holder, label));

					holder.getChildren().addAll(label, input, b);

					root.getChildren().add(holder);
				}
			}
		});
	}

	private void onMouseReleased(Event e, ModUtils.IdResult id, TextInputControl input, Button b, HBox holder, Label label) {
		ModUtils utils = ModUtils.getInstance();
		try {
			ModVersion found = getLatestVersion(input.getText(), Config.minecraftVersion, id.mod.getFileName());
			if (found == null) {
				b.setText(" Ignore Failure, and Save?");
				((Version) id.mod).setModId(input.getText());
				b.onMouseReleasedProperty().setValue(e1 -> {
					if (id.mod.getModId().equals(input.getText())) {
						try {
							utils.addMod(id.jarId, id.mod, true);
							utils.CONFIG.getCachedMods().writeCache();
							holder.getChildren().setAll(label, new Label(input.getText()), new Label(" - saved"));
							updated = true;
						} catch (IOException e2) {
							AppLogger.error(e2.getMessage(), getClass());
						}
					} else {
						onMouseReleased(e, id, input, b, holder, label);
					}
				});
			} else {
				((Version) found).setModId(input.getText());
				utils.addMod(id.jarId, found, true);
				utils.CONFIG.getCachedMods().writeCache();
				holder.getChildren().setAll(label, new Label(input.getText()), new Label(" - success"));
				updated = true;
			}
		} catch (ModIdFoundConnectionFailed | IOException modIdFoundConnectionFailed) {
			AppLogger.error(modIdFoundConnectionFailed.getMessage(), getClass());
		}
	}

	private ModVersion getLatestVersion(String id, String mcVersion, String fileName) throws ModIdFoundConnectionFailed, IOException {
		CurseForgeResponse cfr = ModChecker.getForVersion(id, mcVersion);
		return cfr.getVersions()
			.stream()
			.filter(v -> v.getFileName().equals(fileName))
			.findFirst()
			.orElse(null);
	}

	public VBox getRoot() {
		return root;
	}

}
