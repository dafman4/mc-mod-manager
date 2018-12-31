package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.Result;
import javafx.util.Callback;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ModUpdaterThread extends Thread {

	private final List<ModVersion> updates;
	private final Callback<Map<ModVersion, Result>, Void> callback;

	public ModUpdaterThread(List<ModVersion> updates, Callback<Map<ModVersion, Result>, Void> callback) {
		this.updates = updates;
		this.callback = callback;
	}

	@Override
	public void run() {
		Map<ModVersion, Result> param = new HashMap<>();
		updates.forEach(update -> {
			if (ModChecker.download(update, Startup.getModsDir() + File.separator, update.getMinecraftVersion())) {
				param.put(update, new Result(true));
			} else {
				param.put(update, new Result(false, "failed: Couldn't download"));
			}
		});
		callback.call(param);
	}
}
