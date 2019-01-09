package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.components.DisplayVersion;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.util.ImageUtils;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class ModVersionTableController {

	@FXML
	private TableView<DisplayVersion> root;
	private ModInfoThread gathering;

	public ModVersionTableController(ModVersion... mods) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/modVersionTable.fxml"));
		loader.setController(this);
		loader.load();
		//Set mod list
		setItems(FXCollections.observableArrayList(Arrays.stream(mods).map(DisplayVersion::new).collect(Collectors.toList())));
		root.getColumns().setAll(root.getColumns().sorted((a, b) -> ModUtils.getInstance().CONFIG.compareColumns(a.getText(), b.getText())));
		root.refresh();
		root.setRowFactory(tv -> {
			TableRow<DisplayVersion> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if(e.getClickCount() == 2 && !row.isEmpty()){
					ModVersion v = row.getItem();
					if(v != null)
					System.out.println("double click: " + v.getModId());
				}
			});

			return row;
		});
		root.getColumns().forEach(column -> {
			if(column.getText().equals("Active")){
				Callback<TableColumn.CellDataFeatures<DisplayVersion, ImageView>, ObservableValue<ImageView>> meth = i ->{
					ImageView v = i.getValue().getImage();
					v.maxHeight(20);
					v.maxWidth(20);
					return new SimpleObjectProperty<>(v);
				};
				((TableColumn<DisplayVersion, ImageView>)column).setCellValueFactory(meth);
			}
		});
	}

	public void addOnChange(ChangeListener<ModVersion> listener) {
		root.getSelectionModel().selectedItemProperty().addListener(listener);
	}

	public List<ModVersion> getItems() {
		return new ArrayList<>(root.getItems());
	}

	public void setItems(ObservableList<DisplayVersion> items) {
		root.setItems(items);
		root.refresh();
	}

	public void setColumns(List<TableColumn<DisplayVersion, ?>> cols){
		root.getColumns().setAll(cols);
		root.refresh();
	}

	public List<TableColumn<DisplayVersion, ?>> getColumns() {
		return root.getColumns();
	}

	public TableView<DisplayVersion> getRoot() {
		return root;
	}
}
