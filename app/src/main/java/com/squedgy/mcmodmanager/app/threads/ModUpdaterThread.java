package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.util.Result;
import javafx.util.Callback;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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
			boolean downloaded = false;
			InputStream file = ModChecker.download(update);
			String fileLocation = Startup.getModsDir() + File.separator + (update.getFileName().endsWith(".jar") ? update.getFileName() : update.getFileName() + ".jar");
			if(file != null) {
				try (
					FileOutputStream outFile = new FileOutputStream(new File(fileLocation));
					ReadableByteChannel in = Channels.newChannel(file);
					FileChannel out = outFile.getChannel()
				) {
					out.transferFrom(in, 0, Long.MAX_VALUE);
					downloaded = true;
				} catch (IOException e) { AppLogger.error(e.getMessage(), getClass()); }
			}
			if (downloaded) {
				param.put(update, new Result(true, update.getFileName()));
			} else {
				param.put(update, new Result(false, update.getFileName(), "failed: Couldn't download"));
			}
		});
		callback.call(param);
	}
}
