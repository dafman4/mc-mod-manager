package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import com.squedgy.mcmodmanager.app.config.Config;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import javafx.util.Callback;

public class ModInfoThread extends Thread {

	private final ModVersion toFind;
	private final Callback<ModVersion, ?> callback;
	private final Callback<Void, ?> couldntFind;

	public ModInfoThread(ModVersion toFind, Callback<ModVersion, ?> callback) {
		this(toFind, callback, null);
	}

	public ModInfoThread(ModVersion toFind, Callback<ModVersion, ?> callback, Callback<Void, ?> couldntFind) {
		this.couldntFind = couldntFind;
		this.toFind = toFind;
		this.callback = callback;
	}

	@Override
	public void run() {
		ModVersion ret = Config.getInstance().getCachedMods().getItem(toFind.getModId());
		if(ret != null) {
			callback.call(ret);
			return;
		}
		CurseForgeResponse resp = null;
		try {
			resp = ModChecker.getForVersion(toFind.getModId(), toFind.getMinecraftVersion());
		} catch (ModIdFailedException e) {
			try {
				resp = ModChecker.getForVersion(
					ModUtils.formatModName(toFind.getModName()),
					toFind.getMinecraftVersion()
				);
			} catch (Exception e1) {
				AppLogger.error(e1.getMessage(), getClass());
			}
		} catch (Exception e) {
			AppLogger.error(e.getMessage(), getClass());
		}

		if (resp != null) {

			ret = resp.getVersions()
				.stream()
				.filter(e -> e.getFileName().equals(toFind.getFileName()))
				.findFirst()
				.orElse(null);
			if (ret != null) {
				callback.call(ret);
				return;
			}

		}
		this.couldntFind.call(null);
	}
}
