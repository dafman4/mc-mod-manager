package com.squedgy.mcmodmanager.app.util;

import javafx.scene.image.Image;

import java.net.URI;
import java.net.URISyntaxException;

import static com.squedgy.mcmodmanager.app.Startup.getResource;

public class ImageUtils {

	private static ImageUtils instance;
	public final Image GOOD, BAD;

	private ImageUtils() throws URISyntaxException {
		GOOD = loadImage(getResource("components/img/good.png").toURI());
		BAD = loadImage(getResource("components/img/bad.png").toURI());
	}

	public static ImageUtils getInstance() throws URISyntaxException {
		if(instance == null) instance = new ImageUtils();
		return instance;
	}

	public Image loadImage(URI location){
		return new Image(location.toString());
	}

}
