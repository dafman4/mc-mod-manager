package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import javafx.util.Callback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.squedgy.mcmodmanager.app.Startup.DOT_MINECRAFT_LOCATION;

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
            if(ModChecker.download(update, DOT_MINECRAFT_LOCATION, update.getMinecraftVersion())){
                //delete after I'm sure this works
                System.out.println(update);
                param.put(update, true);
            }else{
                param.put(update, false);
            }
        });

        callback.call(param);
    }
}
