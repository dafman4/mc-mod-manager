package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFoundConnectionFailed;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.ModUtils;
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

public class SetJarIdController {

	@FXML
	private VBox root;

	public SetJarIdController() throws IOException {
		FXMLLoader loader = new FXMLLoader(Startup.getResource("components/jar-ids.fxml"));
		loader.setController(this);
		loader.load();
	}

	@FXML
	public void initialize() {
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
					b.onMouseReleasedProperty().setValue(e -> {
						try {
							CurseForgeResponse cfr = ModChecker.getForVersion(input.getText(), Startup.MINECRAFT_VERSION);
							ModVersion found = cfr.getVersions()
								.stream()
								.filter(v -> v.getFileName().equals(version.getFileName()))
								.findFirst()
								.orElse(null);
							if (found == null) {
								b.setText("Ignore Failure, and Save");
								((Version) id.mod).setModId(input.getText());
								b.onMouseReleasedProperty().setValue(e1 -> {
									ModUtils.getInstance().addMod(id.jarId, id.mod);
									try {
										ModUtils.getInstance().CONFIG.getCachedMods().writeCache();
									} catch (IOException e2) {
										AppLogger.error(e2.getMessage(), getClass());
									}
								});
							} else {
								((Version) found).setModId(input.getText());
								ModUtils.getInstance().addMod(id.jarId, found);
								ModUtils.getInstance().CONFIG.getCachedMods().writeCache();
								holder.getChildren().setAll(label, new Label(input.getText()), new Label("- success"));
							}
						} catch (ModIdFoundConnectionFailed | IOException modIdFoundConnectionFailed) {
							AppLogger.error(modIdFoundConnectionFailed.getMessage(), getClass());
						}
					});
					HBox.setMargin(b, new Insets(5, 5, 5, 5));

					holder.getChildren().addAll(label, input, b);

					root.getChildren().add(holder);
				}
			}
		});
	}

	public VBox getRoot() {
		return root;
	}

}
