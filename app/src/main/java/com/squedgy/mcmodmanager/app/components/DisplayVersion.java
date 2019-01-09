package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.util.ImageUtils;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.scene.image.ImageView;

import java.net.URISyntaxException;

public class DisplayVersion extends Version {

	public DisplayVersion(ModVersion v){
		setModId(v.getModId());
		setFileName(v.getFileName());
		setDescription(v.getDescription());
		setModName(v.getModName());
	}

	public ImageView getImage(){
		try {
			ImageView ret = new ImageView(ModUtils.getInstance().modActive(this) ? ImageUtils.getInstance().GOOD : ImageUtils.getInstance().BAD);
			ret.maxWidth(20);
			ret.maxHeight(20);
			return ret;
		} catch (URISyntaxException e) {
			return null;
		}
	}
}
