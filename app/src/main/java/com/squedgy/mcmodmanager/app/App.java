package com.squedgy.mcmodmanager.app;

import com.squedgy.mcmodmanager.app.controllers.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

	private static Scene PARENT;
	private static App instance;
	private static MainController MAIN_VIEW;

	private App() throws IOException {
		if (MAIN_VIEW == null) MAIN_VIEW = MainController.getInstance();
		if (PARENT == null) PARENT = new Scene(MAIN_VIEW.getRoot());
	}

	public static App getInstance() throws IOException {
		if (instance == null) instance = new App();
		return instance;
	}

	public static Scene getParent() {
		return PARENT;
	}

	public MainController getMainView() {
		return MAIN_VIEW;
	}

	@Override
	public void start(Stage stage) throws IOException {

		getInstance();
		stage.setTitle("Minecraft Mod Manager");
		stage.setMinHeight(MAIN_VIEW.getRoot().getMinHeight());
		stage.setMinWidth(MAIN_VIEW.getRoot().getMinWidth());
		stage.setScene(PARENT);
		stage.onCloseRequestProperty().setValue(e -> {
			System.exit(0);
		});
		stage.centerOnScreen();
		stage.show();
	}

}
