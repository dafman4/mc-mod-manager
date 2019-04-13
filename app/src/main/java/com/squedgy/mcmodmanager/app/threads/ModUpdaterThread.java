package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import com.squedgy.mcmodmanager.app.util.PathUtils;
import com.squedgy.mcmodmanager.app.util.Result;
import javafx.util.Callback;
import org.slf4j.Logger;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;


public class ModUpdaterThread extends Thread {

	private static final Logger log = getLogger(ModUpdaterThread.class);
	private final List<ModVersion> updates;
	private final Callback<Map<ModVersion, Result>, Void> callback;
	private final Map<ModVersion, Thread> updateThreads = new HashMap<>();

	public ModUpdaterThread(List<ModVersion> updates, Callback<Map<ModVersion, Result>, Void> callback) {
		this.updates = updates;
		this.callback = callback;
	}

	@Override
	public void run() {
		Map<ModVersion, Result> results = new HashMap<>();
		updates.forEach(update -> {
			updateThreads.put(update, buildThread(update, results));
			updateThreads.get(update).start();
		});

		while (updateThreads.size() > 0) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error("", e);
			}
		}

		callback.call(results);
	}

	private Thread buildThread(ModVersion update, Map<ModVersion, Result> results) {
		return new Thread(() -> {
			boolean downloaded = false;
			try (InputStream file = ModChecker.download(update)){
				ModVersion oldMod = ModUtils.getInstance().getMod(update.getModId());
				String fileLocation = PathUtils.findModLocation(oldMod);
				if (file != null && new File(fileLocation).exists()) {
					try (
						FileOutputStream outFile = new FileOutputStream(new File(fileLocation));
						ReadableByteChannel in = Channels.newChannel(file);
						FileChannel out = outFile.getChannel()
					) {
						out.transferFrom(in, 0, Long.MAX_VALUE);
						downloaded = true;
					}catch (IOException e) {
						log.error(e.getMessage(), getClass());
					}
				}
			} catch (IOException e) {
				log.error("", e);
			} finally {
				if (downloaded) results.put(update, new Result(true, "succeeded"));
				else results.put(update, new Result(false, "failed: Couldn't download"));
				log.info("removed: " + updateThreads.remove(update));
			}
		});
	}
}
