package com.squedgy.mcmodmanager.app.threads;

import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ModCheckingThread extends Thread {

    private final List<ModVersion> IDS;
    private final String mc;
    private final List<ModVersion> updateables = new LinkedList<>();
    private final Callback<List<ModVersion>,?> callback;

    public ModCheckingThread(List<ModVersion> modIds, String mcVersion, Callback<List<ModVersion>, ?> callback){
        IDS = new ArrayList<>(modIds);
        this.mc = mcVersion;
        this.callback = callback;
    }

    @Override
    public void run() {
        IDS.forEach(id -> {
            try {
                CurseForgeResponse resp;
                try{
                    resp = ModChecker.getForVersion(id.getModId(), mc);
                }catch(ModIdFailedException ignored){
                    resp = ModChecker.getForVersion(id.getModName().toLowerCase().replace(' ', '-').replaceAll("[^-a-z0-9]", ""), mc);
                }
                resp.getVersions()
                    .stream()
                    .max(Comparator.comparing(ModVersion::getUploadedAt))
                    .ifPresent( v -> {
                        if(v.getUploadedAt().isAfter(id.getUploadedAt())){
                            updateables.add(v);
                        }
                    });
            }catch(ModIdNotFoundException | NullPointerException e){  }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
        callback.call(updateables);
    }
}
