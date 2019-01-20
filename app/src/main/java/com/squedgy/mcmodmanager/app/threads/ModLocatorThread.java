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
				ModVersion v = utils.matchesExistingId(jarId, fileName);
				if (v != null) {
					found.accept(jarId, v);
					return;
				}
			} catch (IOException | ModIdFoundConnectionFailed e) {
				AppLogger.error(e.getMessage(), getClass());
			}

			//Attempt to find an online ModVersion matching the installed one
			ModUtils.IdResult id = utils.getRealModId(file);
			found.accept(id.jarId, id.mod);
		}catch(Exception e){
			AppLogger.error(e, getClass());
			failedToLocate.run();
		}finally {
			this.interrupt();
		}
	}
}
