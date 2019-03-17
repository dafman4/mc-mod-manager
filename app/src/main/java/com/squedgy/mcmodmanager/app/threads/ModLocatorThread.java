package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFoundConnectionFailed;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.app.util.ModUtils;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;

public class ModLocatorThread extends Thread {

	private BiConsumer<String,ModVersion> found;
	private Runnable failedToLocate;
	private String jarId, fileName;
	private JarFile file;
	private ModVersion toReturn = null;

	public ModLocatorThread(String jarId, String fileName, JarFile file, BiConsumer<String, ModVersion> found, Runnable failedToLocate){
		this.found = found;
		this.failedToLocate = failedToLocate;
		this.jarId = jarId;
		this.fileName = fileName;
		this.file = file;
		this.setName(file.getName());
	}

	@Override
	public void run() {
		try{
			ModUtils utils = ModUtils.getInstance();
			try {
				AppLogger.info("Looking for matches for: " + jarId, getClass());
				toReturn = utils.matchesExistingId(jarId, fileName);
				if (toReturn != null) {
					AppLogger.info("Found a match for: " + jarId, getClass());
					return;
				}
			} catch (IOException | ModIdFoundConnectionFailed e) {
				AppLogger.error(e.getMessage(), getClass());
			}

			//Attempt to find an online ModVersion matching the installed one
			AppLogger.info("getting the real mod id: " + jarId, getClass());
			ModUtils.IdResult id = utils.getRealModId(file);
			jarId = id.jarId;
			toReturn = id.mod;
			AppLogger.info("found a real id: " +id.jarId, getClass());
		}catch(Exception e){
			AppLogger.error(e, getClass());
		}finally {
			this.interrupt();
			if(toReturn != null) found.accept(jarId, toReturn);
			else failedToLocate.run();
		}
		AppLogger.info("returning: " + jarId, getClass());
		AppLogger.info("", getClass());
		return;
	}
}
