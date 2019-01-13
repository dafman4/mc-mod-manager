package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class NewModsController {

	@FXML
	public VBox root;
	@FXML
	public TextArea mods;

	public NewModsController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components" + File.separator + "new-mods.fxml"));
		loader.setController(this);
		loader.load();
	}

	@FXML
	public void initialize(){ }

	private boolean downloadMod(ModVersion v, String output, ModUtils utils){
		try (
			FileOutputStream outFile = new FileOutputStream(output);
			ReadableByteChannel in = Channels.newChannel(ModChecker.download(v));
			FileChannel out = outFile.getChannel()
		) {
			out.transferFrom(in, 0, Long.MAX_VALUE);
			utils.addMod(ModUtils.getJarModId(new JarFile(PathUtils.getModsDir() + File.separator + v.getFileName())), v,false,  "", true);
			return true;
		} catch (ModIdNotFoundException | IOException ignored) {
			return false;
		}
	}

	private static class Result{
		private ModVersion version;
		private boolean succeded;
		private String reason;
	}

	@FXML
	public void addMods(Event e) throws IOException {
		//A mod id consists of "-" and alpha-numerical, things that aren't those are the delimiters
		Modal.loading();
		Thread t = new Thread(() -> {

			List<ModVersion> updates = new LinkedList<>();
			ModUtils utils = ModUtils.getInstance();
			Map<String, Result> results = new HashMap<>();

			for(String id : mods.getText().split("([^-a-zA-Z0-9]|[\r\n])+")){
				//Check normal and deactivated mods
				ModVersion mod = utils.getAnyModFromId(id);
				Result result = new Result();
				result.succeded = false;

				if(mod == null) {
					try { checkCurseForge(id, result); }
					catch (ModIdNotFoundException ignored) { result.reason = "Failed to locate a mod with id: " + id; }
				}else{
					updates.add(mod);
					result.reason = "Mod already exists, currently attempting to update";
				}
				results.put(id, result);
			}
			try {
				new ModUpdaterController(updates).updateAll(null);
				afterAdd(results);
			}
			catch (IOException ignored) { }
			e.consume();
		});
		t.start();
	}

	private void afterAdd(Map<String, Result> results) throws IOException {
		Modal m = Modal.getInstance();
		VBox resultBox = new VBox();
		results.forEach((key, value) -> resultBox.getChildren().add(new Label(key + ": " + (value.succeded ? "Succeeded!!" : value.reason))));
		Button b = new Button("Back");
		resultBox.getChildren().addAll(new Label(), b);
		b.setOnMouseReleased(event ->{
			Platform.runLater(() ->{
				mods.clear();
				m.setContent(root);
			});
		});
		Platform.runLater(() -> m.setContent(resultBox));
	}

	private Result checkCurseForge(String id, Result result) throws ModIdNotFoundException{
		ModUtils utils = ModUtils.getInstance();
		ModVersion mod = ModChecker.getNewest(id, Config.minecraftVersion);
		if(downloadMod(mod, PathUtils.getModLocation(mod), utils)){
			result.succeded = true;
			result.version = mod;
		}else{
			result.reason = "Failed to download: " + mod.getModId();
		}
		return result;
	}

	public VBox getRoot() { return root; }

}
