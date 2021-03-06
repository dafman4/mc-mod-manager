package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

public class ModInfoThread extends Thread {

	private static final Logger log = getLogger(ModInfoThread.class);
	private final ModVersion toFind;
	private final Consumer<ModVersion> callback;
	private final Runnable couldntFind;

	public ModInfoThread(ModVersion toFind, Consumer<ModVersion> callback) {
		this(toFind, callback, null);
	}

	public ModInfoThread(ModVersion toFind, Consumer<ModVersion> callback, Runnable couldntFind) {
		this.couldntFind = couldntFind;
		this.toFind = toFind;
		this.callback = callback;
	}

	public void callback(ModVersion v) {
		if (!Thread.currentThread().isInterrupted()) callback.accept(v);
	}

	public void failed() {
		if (!Thread.currentThread().isInterrupted()) couldntFind.run();
	}

	@Override
	public void run() {
		ModVersion ret = ModUtils.getInstance().CONFIG.getCachedMods().getItem(toFind.getModId());
		if (ret != null) {
			callback(ret);
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
				log.error(e1.getMessage());
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		if (resp != null) {

			ret = resp.getVersions()
				.stream()
				.filter(e -> e.getFileName().equals(toFind.getFileName()))
				.findFirst()
				.orElse(null);
			if (ret != null) {
				callback(ret);
				return;
			}

		}
		failed();
	}
}
