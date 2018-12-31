package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class ModVersionTableController {

	@FXML
	private TableView<ModVersion> root;
	private ModInfoThread gathering;

	public ModVersionTableController(ModVersion... mods) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/modVersionTable.fxml"));
		loader.setController(this);
		loader.load();
		//Set mod list
		setItems(FXCollections.observableArrayList(mods));
		root.getColumns().setAll(root.getColumns().sorted((a, b) -> ModUtils.getInstance().CONFIG.compareColumns(a.getText(), b.getText())));
		root.refresh();
	}

	public void addOnChange(ChangeListener<ModVersion> listener) {
		root.getSelectionModel().selectedItemProperty().addListener(listener);
	}

	public List<ModVersion> getItems() {
		return new ArrayList<>(root.getItems());
	}

	public void setItems(ObservableList<ModVersion> items) {
		root.setItems(items);
	}

	public List<TableColumn<ModVersion, ?>> getColumns() {
		return root.getColumns();
	}

	public TableView<ModVersion> getRoot() {
		return root;
	}


}
