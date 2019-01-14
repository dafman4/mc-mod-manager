package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.util.ImageUtils;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.scene.image.ImageView;

public class DisplayVersion extends Version {

	public DisplayVersion(ModVersion v) {
		setModId(v.getModId());
		setFileName(v.getFileName());
		setDescription(v.getDescription());
		setModName(v.getModName());
		setDownloadUrl(v.getDownloadUrl());
		setId(v.getId());
		setTypeOfRelease(v.getTypeOfRelease());
		setUploadedAt(v.getUploadedAt());
		setMinecraftVersion(v.getMinecraftVersion());
	}

	public ImageView getImage() {
		ImageUtils u = ImageUtils.getInstance();
		return new ImageView(ModUtils.getInstance().modActive(this) ? u.GOOD : ImageUtils.getInstance().BAD);
	}
}
