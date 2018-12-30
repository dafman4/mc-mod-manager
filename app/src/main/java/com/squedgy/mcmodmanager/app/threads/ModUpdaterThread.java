package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.Startup;
import javafx.util.Callback;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModUpdaterThread extends Thread{

    private final List<ModVersion> updates;
    private final Callback<Map<ModVersion, Boolean>, Void> callback;

    public ModUpdaterThread(List<ModVersion> updates, Callback<Map<ModVersion,Boolean>, Void> callback){
        this.updates = updates;
        this.callback = callback;
    }

    @Override
    public void run() {
        Map<ModVersion,Boolean> param = new HashMap<>();
        updates.forEach(update -> {
            System.out.println("Attempting to download: " + update.getModId());
            if(ModChecker.download(update, Startup.getModsDir(), update.getMinecraftVersion())){

                param.put(update, true);
            }else{
                param.put(update, false);
            }
        });

        callback.call(param);
    }
}
