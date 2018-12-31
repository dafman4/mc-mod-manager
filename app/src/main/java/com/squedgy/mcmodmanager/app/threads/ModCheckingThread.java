package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ModCheckingThread extends Thread {

	private final List<ModVersion> IDS;
	private final String mc;
	private final List<ModVersion> updateables = new LinkedList<>();
	private final Callback<List<ModVersion>, ?> callback;

	public ModCheckingThread(List<ModVersion> modIds, String mcVersion, Callback<List<ModVersion>, ?> callback) {
		IDS = new ArrayList<>(modIds);
		this.mc = mcVersion;
		this.callback = callback;
	}

	@Override
	public void run() {
		IDS.forEach(id -> {


			try {
				ModVersion resp;
				try {
					resp = ModChecker.getNewest(id.getModId(), mc);
				} catch (ModIdNotFoundException ignored) {
					resp = ModChecker.getNewest(id.getModName().toLowerCase().replace(' ', '-').replaceAll("[^-a-z0-9]", ""), mc);
				}
				String key = ModUtils.getInstance().getKey(resp);
				((Version)resp).setModId(key);

				if (resp != null && resp.getUploadedAt().isAfter(id.getUploadedAt())) {
					updateables.add(resp);
				}
			} catch (ModIdNotFoundException | NullPointerException e) {
			} catch (Exception e) {
				AppLogger.error(e, getClass());
			}
		});
		callback.call(updateables);
	}
}
