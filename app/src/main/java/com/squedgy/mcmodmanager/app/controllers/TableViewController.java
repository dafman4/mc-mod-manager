package com.squedgy.mcmodmanager.app.controllers;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.components.Modal;
import com.squedgy.mcmodmanager.app.components.PublicNode;
import com.squedgy.mcmodmanager.app.threads.ModCheckingThread;
import com.squedgy.mcmodmanager.app.threads.ModInfoThread;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class TableViewController {

    private ModVersionTableController table;
    @FXML
    private Button columns;
    @FXML
    private Button updates;
    @FXML
    private Button badJars;
    @FXML
    private WebView objectView;
    @FXML
    private GridPane listGrid;
    @FXML
    private ScrollPane root;

    private ModCheckingThread checking;
    private ModInfoThread gathering;

    @FXML
    public void initialize() throws IOException {
        table = new ModVersionTableController(ModUtils.getInstance().getMods());
        listGrid.add(table.getRoot(), 0,0);

        badJars.setVisible(ModUtils.viewBadJars().size() > 0);
        System.out.println(badJars.visibleProperty().getValue());
        //Set the default view to a decent looking background
        updateObjectView("");
	    objectView.getEngine().setJavaScriptEnabled(true);
	    listGrid.prefHeightProperty().bind(root.heightProperty().multiply(.8));
	    listGrid.maxWidthProperty().bind(root.widthProperty().subtract(2));
	    table.addOnChange((obs, old, neu) -> {
            updateObjectView("<h1>Loading...</h1>");
            if(gathering == null || !gathering.isAlive()) {
                gathering = new ModInfoThread(neu, version -> {
                    Platform.runLater(() -> updateObjectView(version.getDescription()));
                    return null;
                }, n -> {
                    Platform.runLater(() -> updateObjectView(("<h2>Error Loading, couldn't find a matching version!</h2>")));
                    return null;
                });
                gathering.start();
            }
        });
    }

    private synchronized void updateObjectView(String description){
        objectView.getEngine().loadContent(
            "<style>" +
                "body{background-color:#303030; color:#ddd;}" +
                "img{max-width:100%;height:auto;}" +
                "a{color:#ff9000;text-decoration:none;} " +
                "a:visited{color:#544316;}" +
            "</style>" + description);
    }

    @FXML
    public void setColumns(Event e){ ModUtils.getInstance().CONFIG.writeColumnOrder(table.getColumns()); }

    @FXML
    public void searchForUpdates(Event e){
        if(checking == null || !checking.isAlive()){
            checking = new ModCheckingThread(new ArrayList<>(table.getItems()), Startup.MINECRAFT_VERSION, l -> {
                //do something with the returned list
                Platform.runLater(() -> {
                    Stage modal = new Stage();
                    ModVersionTableController table;
                    try {
                        table = new ModVersionTableController(l.toArray(new ModVersion[0]));
                    } catch (IOException e1) {
                        throw new RuntimeException();
                    }

                    Scene scene = new Scene(table.getRoot());

                    modal.setScene(scene);
                    modal.setMinHeight(table.getRoot().getMinHeight());
                    modal.setMinWidth(table.getRoot().getMinWidth());
                    modal.initOwner(Startup.getParent().getWindow());
                    modal.initModality(Modality.APPLICATION_MODAL);
                    modal.showAndWait();
                });
                return null;
            } );
            checking.start();
        }else{
            AppLogger.info("checking is still alive!", getClass());
        }
    }

    @FXML
    public void showBadJars(Event e) throws IOException {
        Stage modal = new Stage();
        Modal m = new Modal();
        TableView<PublicNode> t = new TableView<>();
        ObservableList<PublicNode> list = FXCollections.observableArrayList(ModUtils.viewBadJars().entrySet().stream().map(PublicNode::new).collect(Collectors.toList()));
        System.out.println(list);
        t.setItems(list);
        ObservableList<TableColumn<PublicNode, String>> columns = FXCollections.observableArrayList();
        TableColumn<PublicNode, String> toAdd = new TableColumn<>();
        toAdd.setText("File");
        toAdd.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().getKey()));
        columns.add(toAdd);
        toAdd = new TableColumn<>();
        toAdd.setText("Reason");
        columns.add(toAdd);
        toAdd.setCellValueFactory(item -> new SimpleStringProperty(item.getValue().getValue()));

        t.getColumns().setAll(columns);

        m.setContents(t);

        modal.setScene(new Scene(m.getRoot()));
        modal.setMinWidth(m.getRoot().getMinWidth());
        modal.setMinHeight(m.getRoot().getMinHeight());
        modal.initOwner(Startup.getParent().getWindow());
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.showAndWait();
        t.refresh();
        t.getStyleClass().setAll("mod-table");
    }

}
