package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;

import static com.squedgy.mcmodmanager.app.Startup.*;

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
			utils.addMod(ModUtils.getJarModId(new JarFile(Startup.getModsDir() + File.separator + v.getFileName())), v,false,  "", true);
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
		Modal m = Modal.getInstance();
		m.setContent(new LoadingController().getRoot());
		m.open(Startup.getParent().getWindow());
		Thread t = new Thread(() -> {
			ModUtils utils = ModUtils.getInstance();
			String[] ids = mods.getText().split("([^-a-zA-Z0-9]|[\r\n])+");
			AppLogger.info(Arrays.toString(ids), getClass());
			Map<String, Result> results = new HashMap<>();
			for(String id : ids){
				ModVersion v = utils.getMod(id);
				Result r = new Result();
				r.succeded = false;
				if(v ==null) v= utils.isVersionId(id);
				AppLogger.info(id + (v == null ? "-missing":"-found"), getClass());
				if(v == null) {
					AppLogger.info("id: " + id + " didn't have a matching downloaded mod", getClass());
					try{
						v = ModChecker.getNewest(id, MINECRAFT_VERSION);
					}catch(ModIdNotFoundException ex){
						v = null;
					}
					if(v != null) {
						if(downloadMod(v, Startup.getModsDir() + File.separator + v.getFileName(), utils)){
							r.succeded = true;
							r.version = v;
							results.put(id,r);
						}else{
							r.reason = "Failed to download: " + v.getModId();
						}
					}else{
						r.reason = "Failed to locate a mod with id: " + id;
					}
				}else{
					ModVersion neu;
					try {
						neu = ModChecker.getNewest(v.getModId(), MINECRAFT_VERSION);
					}catch(ModIdNotFoundException ex){
						neu = null;
					}
					if(neu != null && downloadMod(neu, Startup.getModsDir() + File.separator  + v.getFileName(), utils)){
						File old = new File(Startup.getModsDir() + File.separator + v.getFileName());
						if(!old.delete()){
							r.reason = "Failed to delete old jar file";
							File neuJar = new File(Startup.getModsDir() + File.separator + neu.getFileName());
							if(!neuJar.delete()) r.reason = r.reason + " AND failed to deleted old file, you will need to clean up the files yourself";
							else r.reason = r.reason + " deleted the new file for runnability";
						}else{
							r.succeded = true;
							r.version = neu;
						}
					}

				}
			}
			e.consume();
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
		});
		t.start();
	}

	public VBox getRoot() { return root; }

}
