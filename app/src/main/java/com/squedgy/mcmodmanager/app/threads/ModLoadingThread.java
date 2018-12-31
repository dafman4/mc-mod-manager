package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.List;

public class ModLoadingThread extends Thread {

	private final Callback<List<ModVersion>, ?> callback;

	public ModLoadingThread(Callback<List<ModVersion>, ?> callback) {
		this.callback = callback;
	}

	@Override
	public void run() {
		ModUtils utils = ModUtils.getInstance();
		ModVersion[] mods = utils.getMods();
		callback.call(Arrays.asList(mods));
	}
}
