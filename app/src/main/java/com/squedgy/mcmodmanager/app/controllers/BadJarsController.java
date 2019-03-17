package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.app.components.PublicNode;
import com.squedgy.mcmodmanager.app.util.JavafxUtils;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class BadJarsController {

	@FXML
	private TableView<PublicNode> root;

	public BadJarsController() throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/bad-jars.fxml"));
		loader.setController(this);
		loader.load();
	}

	@FXML
	public void initialize() {
		root.setItems(FXCollections.observableArrayList(ModUtils.viewBadJars().entrySet().stream().map(PublicNode::new).collect(Collectors.toList())));
		TableColumn<PublicNode, String> one = JavafxUtils.makeColumn("File", v -> {
			String fileName = v.getValue().getKey().getFileName();
			return new SimpleStringProperty(fileName.contains(File.separator) ? fileName.substring((PathUtils.getModsDir() + File.separator).length()) : fileName);
		});
		TableColumn<PublicNode, String> two = JavafxUtils.makeColumn("Reason", v -> new SimpleStringProperty(v.getValue().getValue()));

		root.getColumns().setAll(
			one,
			two
		);
		root.refresh();
	}

	public TableView getRoot() {
		return root;
	}

}
