package com.squedgy.mcmodmanager.app.util;

import javafx.scene.image.Image;

import java.net.URI;
import java.net.URISyntaxException;

import static com.squedgy.mcmodmanager.app.util.PathUtils.getResource;

public class ImageUtils {

	private static ImageUtils instance;
	public final Image GOOD, BAD;

	private ImageUtils() {
		Image good;
		try {
			good = loadImage(getResource("components/img/good.png").toURI());
		} catch (URISyntaxException e) {
			good = null;
		}
		GOOD = good;
		Image bad;
		try {
			bad = loadImage(getResource("components/img/bad.png").toURI());
		} catch (URISyntaxException e) {
			bad = null;
		}
		BAD = bad;
	}

	public static ImageUtils getInstance() {
		if (instance == null) instance = new ImageUtils();
		return instance;
	}

	public static Image loadImage(URI location) {
		return new Image(location.toString());
	}

}
