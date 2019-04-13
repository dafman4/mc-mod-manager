package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFoundConnectionFailed;
import com.squedgy.mcmodmanager.app.util.ModUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;

import static org.slf4j.LoggerFactory.getLogger;

public class ModLocatorThread extends Thread {

	private static final Logger log = getLogger(ModLocatorThread.class);
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
				log.info("Looking for matches for: " + jarId, getClass());
				toReturn = utils.matchesExistingId(jarId, fileName);
				if (toReturn != null) {
					log.info("Found a match for: " + jarId, getClass());
					return;
				}
			} catch (IOException | ModIdFoundConnectionFailed e) {
				log.error(e.getMessage(), getClass());
			}

			//Attempt to find an online ModVersion matching the installed one
			log.info("getting the real mod id: " + jarId, getClass());
			ModUtils.IdResult id = utils.getRealModId(file);
			jarId = id.jarId;
			toReturn = id.mod;
			log.info("found a real id: " +id.jarId, getClass());
		}catch(Exception e){
			log.error("", e);
		}finally {
			this.interrupt();
			if(toReturn != null) found.accept(jarId, toReturn);
			else failedToLocate.run();
		}
		log.info("returning: " + jarId, getClass());
		log.info("");
	}
}
