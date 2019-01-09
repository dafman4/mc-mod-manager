package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

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

	@FXML
	public void addMods(Event e){
		//A mod id consists of "-" and alpha-numerical, things that aren't those are the delimiters
		String[] ids = mods.getText().split("([^-a-zA-Z0-9]|[\r\n])+");
		for(String id : ids){
			ModVersion v = ModUtils.getInstance().getMod(id);
			if(v == null) {
				v = ModChecker.getNewest(id, MINECRAFT_VERSION);
				String output = Startup.getModsDir() + File.separator;
				try(
					FileOutputStream outFile = new FileOutputStream(output);
					ReadableByteChannel in = Channels.newChannel(ModChecker.download(v));
					FileChannel out = outFile.getChannel()
				) {
					out.transferFrom(in, 0, Long.MAX_VALUE);
				} catch (ModIdNotFoundException | IOException ignored) { v = null; }
				if (v != null) ModUtils.getInstance().addMod(id, v);
			}
		}
		e.consume();
	}

	public VBox getRoot() { return root; }

}
