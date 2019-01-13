package com.squedgy.mcmodmanager.app.util;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.web.WebView;
import javafx.util.Callback;

import java.util.HashMap;
import java.util.Map;

public class JavafxUtils {

	private static Map<WebView, Thread> viewSetters = null;

	public static void putSetterAndStart(WebView view, Runnable run) {
		putSetterAndStart(view, new Thread(run));
	}

	public static void putSetterAndStart(WebView view, Thread t) {
		putSetter(view, t);
		t.start();
	}

	private static void putSetter(WebView view, Thread t) {
		if (viewSetters == null) viewSetters = new HashMap<>();
		else {
			Thread current = viewSetters.get(view);
			if (current != null && current.isAlive())
				current.interrupt();
		}
		viewSetters.put(view, t);
	}

	public static <Obj, Value> TableColumn<Obj, Value> makeColumn(String header, Callback<TableColumn.CellDataFeatures<Obj, Value>, ObservableValue<Value>> factory) {
		TableColumn<Obj, Value> ret = new TableColumn<>();
		ret.setText(header);
		ret.setCellValueFactory(factory);
		return ret;
	}
}
