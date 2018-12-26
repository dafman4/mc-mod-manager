package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import javafx.util.Callback;

import java.util.Comparator;

public class ModInfoThread extends Thread {

    private final ModVersion toFind;
    private final Callback<ModVersion, ?> callback;
    private final Callback<Void,?> couldntFind;

	public ModInfoThread(ModVersion toFind, Callback<ModVersion, ?> callback){
		this(toFind, callback, null);
	}

	public ModInfoThread(ModVersion toFind, Callback<ModVersion, ?> callback, Callback<Void,?> couldntFind){
		this.couldntFind = couldntFind;
		this.toFind = toFind;
		this.callback = callback;
	}

	@Override
	public void run() {

		CurseForgeResponse resp = null;
		try {
			resp = ModChecker.getForVersion(toFind.getModId(), toFind.getMinecraftVersion());
		}catch(ModIdFailedException e){
			try {
				resp = ModChecker.getForVersion(toFind.getModName()
						.replace(' ', '-').toLowerCase()
						.replaceAll("[^-a-z0-9]", ""),
						toFind.getMinecraftVersion()
				);
			} catch (Exception e1) {
				AppLogger.error(e1, getClass());
			}
		} catch (Exception e) {
			AppLogger.error(e, getClass());
		}

		System.out.println(resp);
		
		if(resp != null){
			ModVersion ret = resp.getVersions()
					.stream()
					.filter(e -> e.getUploadedAt().toLocalDate().equals(toFind.getUploadedAt().toLocalDate()))
					.min(Comparator.comparing(e -> Math.abs(e.getUploadedAt().toLocalTime().toSecondOfDay() - toFind.getUploadedAt().toLocalTime().toSecondOfDay())))
					.orElse(null);
			if (ret != null){
				callback.call(ret);
				return;
			}

		}
		this.couldntFind.call(null);
		throw new ThreadFailedException();
	}
}
