package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.Version;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.util.Callback;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ModCheckingThread extends Thread {

	private final List<ModVersion> IDS;
	private final String mc;
	private final List<ModVersion> updateables = new LinkedList<>();
	private final Callback<List<ModVersion>, ?> callback;
	private final Map<ModVersion, Thread> updateCheckers;

	public ModCheckingThread(Callback<List<ModVersion>, ?> callback) {
		this(Arrays.asList(ModUtils.getInstance().getMods()), Config.minecraftVersion, callback);
	}

	public ModCheckingThread(List<ModVersion> modIds, String mcVersion, Callback<List<ModVersion>, ?> callback) {
		IDS = new ArrayList<>(modIds);
		updateCheckers = new HashMap<>();
		this.mc = mcVersion;
		this.callback = callback;
	}

	private void removeThread(ModVersion v) {
		updateCheckers.remove(v);
	}

	@Override
	public void run() {
		IDS.forEach(id -> {
			Thread toRun = new Thread(() -> {
				try {
					ModVersion resp;
					try {
						resp = ModChecker.getNewest(id.getModId(), mc);
					} catch (ModIdNotFoundException ignored) {
						resp = ModChecker.getNewest(id.getModName().toLowerCase().replace(' ', '-').replaceAll("[^-a-z0-9]", ""), mc);
					}
					String jarId = ModUtils.getInstance().getKey(id);
					((Version) resp).setModId(jarId);

					if (resp != null && resp.getUploadedAt().isAfter(id.getUploadedAt())) {
						updateables.add(resp);
					}
				} catch (ModIdNotFoundException | NullPointerException e) {
				} catch (Exception e) {
					AppLogger.error(e, getClass());
				} finally {
					removeThread(id);
				}
			});
			updateCheckers.put(id, toRun);
			toRun.start();
		});
		while (updateCheckers.size() > 0) {
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				AppLogger.error(e, getClass());
			}
		}
		callback.call(updateables);
	}
}
