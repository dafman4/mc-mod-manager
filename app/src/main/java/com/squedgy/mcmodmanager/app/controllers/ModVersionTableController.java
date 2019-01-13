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
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class ModVersionTableController {

	@FXML
	private TableView<DisplayVersion> root;
	private ModInfoThread gathering;
	private Function<ModVersion, ?> doubleClick = null;
	private final String TABLE_NAME;

	public ModVersionTableController(String name, ModVersion... mods) throws IOException {
		FXMLLoader loader = new FXMLLoader(getResource("components/modVersionTable.fxml"));
		loader.setController(this);
		loader.load();
		//Set mod list
		setItems(FXCollections.observableArrayList(Arrays.stream(mods).map(DisplayVersion::new).collect(Collectors.toList())));
		TABLE_NAME = name;
	}

	@FXML
	public void initialize(){
		root.getColumns().setAll(root.getColumns().sorted((a, b) -> ModUtils.getInstance().CONFIG.compareColumns(TABLE_NAME, a.getText(), b.getText())));
		root.refresh();
		root.setRowFactory(tv -> {
			TableRow<DisplayVersion> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if(e.getClickCount() == 2 && !row.isEmpty()){
					if(doubleClick != null){
						doubleClick.apply(row.getItem());
						root.refresh();
					}

				}
			});

			return row;
		});
	}

	public void addOnChange(ChangeListener<ModVersion> listener) {
		root.getSelectionModel().selectedItemProperty().addListener(listener);
	}

	public void setOnDoubleClick(Function<ModVersion, ?> func){ this.doubleClick = func; }

	public void addColumn(int index, TableColumn<DisplayVersion, ImageView> column){
		root.getColumns().add(index, column);
	}

	public List<ModVersion> getItems() { return new ArrayList<>(root.getItems()); }

	public void setItems(ObservableList<DisplayVersion> items) {
		root.setItems(items);
		root.refresh();
	}

	public void setColumns(List<TableColumn<DisplayVersion, ?>> cols){
		root.getColumns().setAll(cols);
		root.refresh();
	}

	public List<TableColumn<DisplayVersion, ?>> getColumns() { return root.getColumns(); }

	public TableView<DisplayVersion> getRoot() { return root; }
}
